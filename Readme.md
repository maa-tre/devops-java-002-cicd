Nginx homebrew command
`sudo brew services start nginx`

Starting the java application
`SERVER_PORT=8090 ./mvnw spring-boot:run`

## Jenkins CI

Create a Pipeline job using this repository's `main` branch and `Jenkinsfile`
as its script path. The CI pipeline uses a Windows Jenkins executor with
`agent any` and calls the Docker CLI with Windows batch steps; Docker Desktop
must be running and available to the Windows account running the Jenkins service.
Enable **GitHub hook trigger for GITScm polling** to build on pushes.

The `deploy.Jenkinsfile` is a separate CD pipeline and still uses Linux shell
steps; it needs a compatible Linux Jenkins agent and deployment credentials.
The CI code-quality stage is currently a placeholder and does not run a
quality tool.

## Automated AWS environment

The `infra` directory provisions a fresh Ubuntu EC2 instance in `us-east-1`
with Terraform and configures Docker plus a private PostgreSQL container with
Ansible. PostgreSQL has a persistent Docker volume, but the Spring Boot app
currently uses its default in-memory H2 database; no PostgreSQL integration is
configured yet. The app is intended to be exposed on port `8080`. Jenkins is
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
PostgreSQL container has no published host port; only the app port is open to
the internet. Terraform outputs the instance public IP and app URL.

Use `terraform -chdir=infra/terraform destroy` to remove the demo
infrastructure. Destroying the instance also deletes its local PostgreSQL
volume, so export any data you want to keep first. AWS charges may apply while
resources are running.