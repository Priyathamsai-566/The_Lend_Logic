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
                // Communicates directly over the local network port to bypass WSL restriction rules entirely
                bat "docker -H tcp://localhost:2375 build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."
                
                // Safely catches execution stops using native Windows Command Prompt syntax
                bat "docker -H tcp://localhost:2375 stop lendlogic-running || cmd /c exit 0"
                bat "docker -H tcp://localhost:2375 rm lendlogic-running || cmd /c exit 0"
                
                // Boots up your multi-stage build container hands-free onto port 8082
                bat "docker -H tcp://localhost:2375 run -d -p 8082:8082 --name lendlogic-running ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
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
