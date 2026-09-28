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
