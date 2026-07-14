pipeline{
    agent any
    environment {
        DOCKER_REPO = "mayurwagh"
        DOCKER_USER = "node-app"
        IMAGE_NAME = "node-app"
        CONTAINER_NAME = "node-container"
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/mayurmwagh/node-app.git'
            }
        }
        stage('Verify Environment') {
            steps {
                sh '''
                echo "Node Version:"
                node -v

                echo "NPM Version:"
                npm -v

                echo "Docker Version:"
                docker --version
                '''
            }
        }
        stage('Install Dependencies') {
            steps {
                sh 'npm install' 
            }
        }
        stage('Run Tests') {
            steps {
                sh 'npm test'
            }
        }
        stage('Build Docker Image') {
            steps {
                sh '''
                docker build -t ${DOCKER_REPO}:${BUILD_NUMBER} .
                ''' 
            }
        }
        stage('Docker login'){
            steps{
               withCredentials([
                        usernamePassword(
                            credentialsId: 'docker-hub-creds',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) 
                    {
                        sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}'
                    }
                }
            }
        stage('Docker push'){
            steps{
               withCredentials([
                        usernamePassword(
                            credentialsId: 'docker-hub-creds',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) 
                    {
                        sh '''
                            docker tag ${DOCKER_REPO}:${BUILD_NUMBER} \
                            ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}

                            docker push ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}
                        '''
                    }
                }
            }
            // stage('Deploy Container'){
            //     steps {
            //             sh '''
            //                 docker rm -f ${CONTAINER_NAME} || true

            //                 docker run -d \
            //                 --name ${CONTAINER_NAME} -p 3000:3000 \
            //                 ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER} 
            //             '''
            //     }
            // }
            stage('K8s-Deployment'){
                steps {
                    sh 'kubectl apply -f deployment.yaml'
                }
            }
        }
    }






