Nginx homebrew command
`sudo brew services start nginx`

Starting the java application
`SERVER_PORT=8090 ./mvnw spring-boot:run`

## Jenkins CI/CD

Create a Pipeline job using this repository's `main` branch and `Jenkinsfile`
as its script path. The pipeline uses a Windows Jenkins executor with `agent
any` and Windows batch steps; Docker Desktop and WSL must be available to the
Windows account running Jenkins. Enable **GitHub hook trigger for GITScm
polling** to build on pushes. On a successful main-branch build it streams the
verified image over SSH to EC2; it does not require Docker Hub credentials.
For a Jenkins instance on a developer PC, forward port `8081` with ngrok and
configure the GitHub repository webhook to `<ngrok-url>/github-webhook/`
(JSON content type, `push` event). Keep ngrok running while expecting pushes.
Run the job once after configuring SCM so Jenkins loads the trigger declared
in the Jenkinsfile.

The EC2 deployment is configured for host port `8080` on the private
`private-net` Docker network and will connect to PostgreSQL at
`db-service:5432` after Ansible provisions its runtime credentials. H2 remains
the default for local Spring runs. Ansible generates separate PostgreSQL admin
and application passwords on the Ansible controller; the generated password
files under `infra/ansible/` are ignored by Git. PostgreSQL receives its admin
settings from `/opt/devops-java-002/.env`. The app receives only its
least-privilege `java_app` credentials from the root-owned
`/opt/devops-java-002/app.env` file (mode `0600`), passed to the app container
at runtime with Docker `--env-file`. These secrets are not part of the image
or Jenkins parameters. PostgreSQL is not published on a host port. Jenkins
also runs a disposable PostgreSQL startup test using temporary CI-only
credentials before deployment.

For a local full-stack Compose run, copy `.env.example` to `.env`, set distinct
strong values for `DB_PASSWORD` and `APP_DB_PASSWORD`, then run
`docker compose up --build`. Restrict the local env file with `chmod 600 .env`.
Compose provisions the non-admin app role before starting the backend. Plain
`./mvnw spring-boot:run` continues to use H2.

Ansible must configure the EC2 host before the first deployment of an image
that requires PostgreSQL. To rotate the app database password, replace the
ignored `infra/ansible/.app_db_password` file with a newly generated password,
rerun Ansible, then redeploy the app so its container receives the new
environment. This rotates the DB role and changes the app env file; the
currently running container must be redeployed immediately to use the new
password. Restrict EC2/Docker access: Docker administrators can inspect
container environment values.

### Demo rollback UI

The stable demo console is served publicly on port `8081` at `/deploy` so
anyone can view it during testing. Jenkins updates the console after each
forward deployment, while application rollbacks only replace the app on port
`8080`. Use the stable console URL from Terraform's
`rollback_console_url` output so the controls remain available when an older
application image is restored. The app's `/deploy` route is a convenience, not
the durable operator entry point.

The console lists `java-app:ci-*` images still retained in EC2's local Docker
image store and the currently running image. It accepts an 8-digit demo PIN,
requires a final browser confirmation, and rate-limits five wrong PIN attempts
per client address for 15 minutes. Recent rollback requests and their status
are recorded in PostgreSQL. The image selection is checked again by a
restricted EC2 helper; the app container has no Docker socket access. The
helper only starts an existing `java-app:ci-*` image through the
health-checking deployment script. If the new image fails health checks, the
script attempts to restore the version that was running before the request.
The interface reports each actual phase (stopping, starting, health checking,
and restoring if necessary) and explains the expected 30–90 second operation
(up to 60 seconds of health checks); a rollback briefly interrupts the app.
The console includes operator steps for selecting a retained build and
comparing its app pages on port `8080`; the console itself stays on port `8081`
so it remains available across application rollbacks. Rollback actions still
require the generated PIN. This public, HTTP-only access is for testing only:
anyone can view the dashboard, and a PIN entered over HTTP is not encrypted in
transit. Do not use this setup for production or reuse a real password or PIN.

Ansible generates the demo PIN locally in the ignored
`infra/ansible/.rollback_pin` file (mode `0600`) and installs only its
PBKDF2-SHA256 hash into the root-only EC2 app environment. Read the PIN on the
Ansible machine and deliver it to operators through a private channel; never
commit, log, or paste it into a ticket/chat. Ansible also generates a separate
agent token. The restricted helper listens on Docker's host-gateway address,
not a public application port, and accepts requests only with that token.
Keep access to EC2, Docker, and the Ansible controller limited to trusted
operators.

This PIN is a basic shared-secret gate for a demo, not production
authentication. The public demo currently serves HTTP, so a PIN entered over
that connection is not encrypted in transit. Use only the generated demo PIN;
do not reuse a real password or PIN. For production, put the site behind HTTPS
and replace the shared PIN with named operator accounts and stronger
authentication. This control executes rollback directly on EC2; regular
build-and-deploy releases continue through Jenkins.

Ansible installs the root-owned deployment script at
`/opt/devops-java-002/deploy-app.sh`; it checks the endpoint and attempts to
restore the previous image if the new container is unhealthy. The EC2 address
is the `DEPLOY_HOST` Jenkins parameter and must be updated if the instance's
public IP changes after a stop/start. The CI code-quality stage remains a
placeholder and does not run a quality tool.

## Automated AWS environment

The `infra` directory provisions a fresh Ubuntu EC2 instance in `us-east-1`
with Terraform and configures Docker plus a private PostgreSQL container with
Ansible. PostgreSQL has a persistent Docker volume and the deployed Spring Boot
app uses a separate application role. Local Spring Boot runs still default to
in-memory H2. The app is intended to be exposed on port `8080`. Jenkins is
separate and is not installed by this setup. The default `t3.micro` is suitable
only for a low-traffic demo; use a larger instance if the app and database
compete for its limited memory. AWS resources use the `space-hyper-cicd` name
prefix by default.

### Prerequisites

- AWS credentials configured for the account and region, with permissions to
  create VPC/networking and EC2 resources (`aws sts get-caller-identity` can
  confirm the active identity).
- Terraform 1.5 or newer, Ansible, and OpenSSH available in the Linux/WSL
  environment used to run the scripts.
- The public IPv4 CIDR from which Ansible/Jenkins will SSH to the instance.
  Do not expose SSH to `0.0.0.0/0`. Terraform refuses to create the instance
  until at least one trusted SSH CIDR is configured.

### Provision and configure

From the repository root in Ubuntu/WSL:

```bash
bash infra/scripts/install-tools.sh
aws configure
cp infra/terraform/terraform.tfvars.example infra/terraform/terraform.tfvars
```

For local Compose runs, copy `.env.example` to `.env` and replace the example
database password with a strong value. `.env` is ignored by Git and must not be
committed.

`aws configure` stores credentials in your home directory; do not copy them to
the repository. Get the public IPv4 address of the machine that will run
Terraform/Ansible (for example, with `curl -4 https://checkip.amazonaws.com`)
and set `allowed_ssh_cidrs` in `infra/terraform/terraform.tfvars` to its `/32`,
for example `["203.0.113.10/32"]`. Add Jenkins's egress `/32` as another list
entry when it needs SSH deployment access. Then provision and configure with
one command; Terraform shows the plan and asks for approval before creating
billable resources:

```bash
bash infra/scripts/setup.sh
```

The SSH keypair is generated locally and only the public key is registered with
AWS. The private key and generated PostgreSQL password are not committed. The
PostgreSQL container has no published host port; the app port is public, while
the rollback console port is restricted to `allowed_ssh_cidrs`. Terraform
outputs the instance IP, app URL, and operator-only `rollback_console_url`.
When upgrading an existing host for the stable console, apply the Terraform
security-group change, run Ansible to install the console deployment script,
then let Jenkins perform a forward deployment; that deployment starts the
console independently on the configured port.

Use `terraform -chdir=infra/terraform destroy` to remove the demo
infrastructure. Destroying the instance also deletes its local PostgreSQL
volume, so export any data you want to keep first. AWS charges may apply while
resources are running.