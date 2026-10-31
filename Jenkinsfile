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
                // Compile the production-ready multi-stage image inside your WSL subsystem
                bat "wsl docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."
                
                // Gracefully tear down previous running iterations of your application container
                bat "wsl bash -c 'docker stop lendlogic-running || true'"
                bat "wsl bash -c 'docker rm lendlogic-running || true'"
                
                // Boot your compiled web application onto production-ready port 8082 hands-free
                bat "wsl docker run -d -p 8082:8082 --name lendlogic-running ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
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
