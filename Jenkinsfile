pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test --no-daemon'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }
        
        stage('Code Coverage') {
            steps {
                sh './gradlew jacocoTestReport --no-daemon'
            }
            post {
                success {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage Report'
                    ])
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    sh "docker build -t minesweeper:${BUILD_NUMBER} ."
                    sh "docker tag minesweeper:${BUILD_NUMBER} minesweeper:latest"
                }
            }
        }
        
        stage('Docker Push') {
            when {
                expression { 
                    return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main'
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh '''
                            echo $PASS | docker login -u $USER --password-stdin
                            docker tag minesweeper:${BUILD_NUMBER} $USER/minesweeper:latest
                            docker push $USER/minesweeper:latest
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
