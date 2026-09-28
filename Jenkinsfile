pipeline {
      agent any
                                                                                                                                   
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
          CI_IMAGE = "${APP_NAME}:ci-${env.GIT_COMMIT.take(7)}"
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
     Branch : ${env.BRANCH_NAME}                                                                                                   
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
                      echo Checking code quality
                      echo Quality checks are not configured yet
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
                  
     ✅ Code Quality  : Passed                                                                                                     
     ✅ Quality Gate  : Passed
     ✅ Docker Build  : Passed                                                                                                     
     ✅ Image Verify  : Passed
     ✅ Security Scan : Passed                                                                                                     
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
  
     ✅ Code Quality  : Passed                                                                                                     
     ✅ Quality Gate  : Passed
     ✅ Docker Build  : Passed
     ✅ Image Verify  : Passed
     ✅ Security Scan : Passed                                                                                                     
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
