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
                // Routes the execution context into your native WSL Linux Docker Engine
                // We use standard Linux syntax (like '|| true') because it executes inside WSL
                bat 'wsl docker build -t %IMAGE_NAME%:%IMAGE_TAG% .'
                bat 'wsl docker stop lendlogic-running || true'
                bat 'wsl docker rm lendlogic-running || true'
                bat 'wsl docker run -d -p 8081:8081 --name lendlogic-running %IMAGE_NAME%:%IMAGE_TAG%'
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
