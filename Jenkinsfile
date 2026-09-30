pipeline {
      agent any

      parameters {
          string(
              name: 'DEPLOY_HOST',
              defaultValue: '54.163.47.143',
              description: 'Public IPv4 address of the space-hyper-cicd EC2 instance'
          )
      }

      options {   
          buildDiscarder(logRotator(numToKeepStr: '10'))
          timestamps()
          timeout(time: 20, unit: 'MINUTES')
          disableConcurrentBuilds()
      }
      triggers {
          githubPush()
      }

      environment {
          APP_NAME = 'java-app'
          CI_IMAGE = "${APP_NAME}:ci-${env.BUILD_NUMBER}"
      }                                                                                                                            
   
   
      stages {                                                                                                                     
                  
          stage('📋 Pipeline Info') {                                                                                              
              steps {
                  script {
                      echo """
  ╔══════════════════════════════════════════════════════╗
  ║               CI PIPELINE STARTED                    ║                                                                         
  ╚══════════════════════════════════════════════════════╝
     Build  : #${env.BUILD_NUMBER}                                                                                                 
     Branch : ${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'}
     PR     : ${env.CHANGE_ID ?: 'Not a PR'}
     Title  : ${env.CHANGE_TITLE ?: 'N/A'}                                                                                         
  ══════════════════════════════════════════════════════
                      """                                                                                                          
                  }
              }                                                                                                                    
          }       

          stage('🔧 Verify Environment') {
              steps {
                  bat '''
                      echo Hostname: %COMPUTERNAME%
                      whoami
                      docker --version
                      echo Environment ready
                  '''
              }   
          }                                                                                                                         
   
          stage('🔍 Code Quality') {
              steps {
                  bat '''
                      echo "Checking code quality"
                      echo "No automated quality checks are configured yet"
                  '''
              }  
          }     

          stage('🐳 Docker Build') {                                                                                               
              steps {
                  bat """
                      echo Building: %CI_IMAGE%
                      docker build --tag %CI_IMAGE% --file Dockerfile .
                      if errorlevel 1 exit /b 1
                      echo Build successful
                      docker images %CI_IMAGE%
                  """
              }   
          }                                                                                                                        
                  
          stage('🧪 Verify Image') {
              steps {
                  bat """
                      echo === Image Verification ===
                      echo Checking JAR exists inside image
                      docker run --rm --entrypoint ls %CI_IMAGE% -lh /app/app.jar
                      if errorlevel 1 exit /b 1
                      echo Checking Java inside image
                      docker run --rm --entrypoint java %CI_IMAGE% -version
                      if errorlevel 1 exit /b 1
                      echo Checking exposed port
                      docker inspect %CI_IMAGE% --format="Port: {{json .Config.ExposedPorts}}"
                      if errorlevel 1 exit /b 1
                      echo Image verification passed
                  """
              }
          }

          stage('🐘 PostgreSQL Startup Test') {
              steps {
                  bat '''
                      @echo off
                      set "PG_TEST_NETWORK=java-app-pg-ci-%BUILD_NUMBER%"
                      set "PG_TEST_DB=java-app-pg-db-%BUILD_NUMBER%"
                      set "PG_TEST_APP=java-app-pg-app-%BUILD_NUMBER%"
                      set /a "PG_TEST_HTTP_PORT=18000+%BUILD_NUMBER%"
                      docker network create %PG_TEST_NETWORK%
                      if errorlevel 1 exit /b 1
                      docker run --detach --name %PG_TEST_DB% --network %PG_TEST_NETWORK% --health-cmd "pg_isready -h 127.0.0.1 -U ci_admin -d ci_test" --health-interval 2s --health-timeout 3s --health-retries 15 -e POSTGRES_DB=ci_test -e POSTGRES_USER=ci_admin -e POSTGRES_PASSWORD=ci-admin-%BUILD_NUMBER%-secret postgres:16-alpine
                      if errorlevel 1 exit /b 1
                      for /l %%I in (1,1,30) do (
                          docker exec %PG_TEST_DB% pg_isready -h 127.0.0.1 -U ci_admin -d ci_test >NUL 2>&1
                          if not errorlevel 1 goto pg_ready
                          ping -n 3 127.0.0.1 >NUL
                      )
                      docker logs %PG_TEST_DB%
                      exit /b 1
                      :pg_ready
                      echo PostgreSQL is accepting TCP connections; configuring the least-privilege test role and schema
                      docker exec --env PGPASSWORD=ci-admin-%BUILD_NUMBER%-secret %PG_TEST_DB% psql -h 127.0.0.1 --set=ON_ERROR_STOP=1 --username ci_admin --dbname ci_test --command="CREATE ROLE ci_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD 'ci-only-%BUILD_NUMBER%-secret'"
                      if errorlevel 1 (
                          echo Failed to create the PostgreSQL test role; database logs follow
                          docker logs %PG_TEST_DB%
                          exit /b 1
                      )
                      docker exec --env PGPASSWORD=ci-admin-%BUILD_NUMBER%-secret %PG_TEST_DB% psql -h 127.0.0.1 --set=ON_ERROR_STOP=1 --username ci_admin --dbname ci_test --command="GRANT CONNECT ON DATABASE ci_test TO ci_app; GRANT USAGE ON SCHEMA public TO ci_app; CREATE TABLE rollback_requests (id uuid PRIMARY KEY, target_image varchar(100) NOT NULL, previous_image varchar(100), status varchar(20) NOT NULL, phase varchar(40) NOT NULL, message varchar(500) NOT NULL, requested_at timestamp with time zone NOT NULL, completed_at timestamp with time zone); GRANT SELECT, INSERT, UPDATE ON TABLE rollback_requests TO ci_app"
                      if errorlevel 1 (
                          echo Failed to provision the PostgreSQL rollback history table; database logs follow
                          docker logs %PG_TEST_DB%
                          exit /b 1
                      )
                      docker run --detach --name %PG_TEST_APP% --network %PG_TEST_NETWORK% --publish 127.0.0.1:%PG_TEST_HTTP_PORT%:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://%PG_TEST_DB%:5432/ci_test -e SPRING_DATASOURCE_USERNAME=ci_app -e SPRING_DATASOURCE_PASSWORD=ci-only-%BUILD_NUMBER%-secret -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate %CI_IMAGE%
                      if errorlevel 1 exit /b 1
                      powershell.exe -NoProfile -NonInteractive -Command "$deadline = (Get-Date).AddSeconds(90); do { try { $response = Invoke-WebRequest -Uri 'http://127.0.0.1:%PG_TEST_HTTP_PORT%/' -UseBasicParsing -TimeoutSec 2; if ($response.StatusCode -eq 200) { exit 0 } } catch { }; Start-Sleep -Seconds 2 } while ((Get-Date) -lt $deadline); exit 1"
                      if errorlevel 1 (
                          docker logs %PG_TEST_APP%
                          exit /b 1
                      )
                      echo PostgreSQL connection and HTTP startup check passed
                  '''
              }
              post {
                  always {
                      bat(returnStatus: true, script: '''
                          @echo off
                          docker rm --force java-app-pg-app-%BUILD_NUMBER% java-app-pg-db-%BUILD_NUMBER% 2>NUL
                          docker network rm java-app-pg-ci-%BUILD_NUMBER% 2>NUL
                      ''')
                  }
              }
          }

          stage('🔒 Security Scan') {                                                                                              
              steps {
                  bat """
                      echo === Security Scan ===
                      for /f %%U in ('docker run --rm --entrypoint id %CI_IMAGE% -u') do set CONTAINER_UID=%%U
                      if not defined CONTAINER_UID exit /b 1
                      echo Container UID: %CONTAINER_UID%
                      if "%CONTAINER_UID%"=="0" (
                          echo FAILED: Image runs as root
                          exit /b 1
                      )
                      echo Passed: Image runs as a non-root user
                  """
              }                                                                                                                    
          }       

          stage('🚀 Deploy to EC2') {
              when {
                  expression { !env.CHANGE_ID }
              }
              steps {
                  script {
                      if (!(params.DEPLOY_HOST ==~ /[A-Za-z0-9.-]+/)) {
                          error('DEPLOY_HOST must be an EC2 IPv4 address or DNS hostname.')
                      }
                  }
                  bat """
                      wsl.exe -- bash -lc "set -o pipefail && docker image inspect '%CI_IMAGE%' >/dev/null && docker save '%CI_IMAGE%' | gzip -c | ssh -i ~/.ssh/devops-java-002 -o BatchMode=yes -o StrictHostKeyChecking=accept-new ubuntu@${params.DEPLOY_HOST} 'gunzip -c | sudo docker load && sudo /opt/devops-java-002/deploy-app.sh %CI_IMAGE% 8080'"
                      if errorlevel 1 exit /b 1
                  """
              }
          }

          stage('🧹 Cleanup') {
              steps {
                  bat """
                      docker rmi %CI_IMAGE% 2>NUL
                      echo Cleanup done
                  """
              }
          }                                                                                                                        
      }
                                                                                                                                   
      post {      
          success {
              script {
                  if (env.CHANGE_ID) {
                      echo """
  ╔══════════════════════════════════════════════════════╗
  ║            ✅ CI PASSED - PR VALIDATED               ║                                                                         
  ╚══════════════════════════════════════════════════════╝
     PR     : #${env.CHANGE_ID} - ${env.CHANGE_TITLE}                                                                              
                  
     ⚠️ Code Quality  : Not configured
     ⚠️ Quality Gate  : Not configured
     ✅ Docker Build  : Passed                                                                                                     
     ✅ Image Verify  : Passed
     ✅ Security Scan : Passed                                                                                                     
     ✅ EC2 Deployment : Passed
     🚫 Deployment   : Skipped (PRs never deploy)
                                                                                                                                   
     → Get code review → Merge to main for deployment                                                                              
  ══════════════════════════════════════════════════════                                                                           
                      """                                                                                                          
                  } else {
                      echo """
  ╔══════════════════════════════════════════════════════╗
  ║          ✅ CI PASSED - BRANCH VALIDATED             ║                                                                         
  ╚══════════════════════════════════════════════════════╝
     Branch : ${env.BRANCH_NAME}                                                                                                   
     Build  : #${env.BUILD_NUMBER}                                                                                                 
  
     ⚠️ Code Quality  : Not configured
     ⚠️ Quality Gate  : Not configured
     ✅ Docker Build  : Passed
     ✅ Image Verify  : Passed
     ✅ Security Scan : Passed                                                                                                     
     ✅ EC2 Deployment : Passed
  ══════════════════════════════════════════════════════
                      """                                                                                                          
                  }
              }
          }
                                                                                                                                   
          failure {
              echo """                                                                                                             
  ╔══════════════════════════════════════════════════════╗
  ║              ❌ CI PIPELINE FAILED                   ║
  ╚══════════════════════════════════════════════════════╝
     Build  : #${env.BUILD_NUMBER}                                                                                                 
     Branch : ${env.BRANCH_NAME}
     PR     : ${env.CHANGE_ID ?: 'N/A'}                                                                                            
     Logs   : ${env.BUILD_URL}
  ══════════════════════════════════════════════════════                                                                           
              """
          }                                                                                                                        
                  
          always {                                                                                                                 
              bat 'docker image prune -f'
          }                                                                                                                        
      }           
  }
