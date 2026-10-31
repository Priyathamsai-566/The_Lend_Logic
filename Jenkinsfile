pipeline {
    agent any

    tools {
        jdk 'Java21'
        maven 'Maven3'
    }

    environment {
        IMAGE_NAME = 'lendlogic'
        IMAGE_TAG = "v${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                bat 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    bat '''
                        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                        -Dsonar.projectKey=lendlogic ^
                        -Dsonar.projectName=LendLogic ^
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Deploy') {
            steps {
                // Route execution through PowerShell specifying your active WSL user profile context
                powershell "wsl --user asus1234 docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."
                
                // Route commands safely into your Ubuntu shell environment to handle the fallback catch
                powershell "wsl --user asus1234 bash -c 'docker stop lendlogic-running || true'"
                powershell "wsl --user asus1234 bash -c 'docker rm lendlogic-running || true'"
                
                // Spin up the finalized production-ready application layer container mapped onto port 8082
                powershell "wsl --user asus1234 docker run -d -p 8082:8082 --name lendlogic-running ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: 'target/surefire-reports/*.xml'

            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true
        }
    }
}
