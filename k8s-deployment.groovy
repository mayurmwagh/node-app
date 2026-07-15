pipeline{
    agent any
    environment {
        DOCKER_REPO = "mayurwagh"
        DOCKER_USER = "node-app"
        IMAGE_NAME = "node-app"
        CONTAINER_NAME = "node-container"
        AWS_REGION = "eu-north-1"
        CLUSTER_NAME = "demo-mayurekscluster"
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
            stage('Update Manifest') {
                steps {

                    sh """
                    sed -i 's|image: .*|image: ${IMAGE_NAME}:${BUILD_NUMBER}|' k8s/deployment.yaml
                    """

                    sh "cat k8s/deployment.yaml"
                }
            }
            stage('K8s-Deployment'){
                steps {
                    sh 'kubectl apply -f deployment.yaml'
                }
            }
            stage('Configure EKS') {
                steps {

                    withCredentials([
                        [$class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: 'aws-creds']
                    ]) {

                        sh """
                        aws eks update-kubeconfig \
                        --region ${AWS_REGION} \
                        --name ${CLUSTER_NAME}
                        """
                    }
                }
            }
            stage('Deploy to EKS') {
                steps {

                    sh '''
                    kubectl apply -f k8s/*
                
                    '''
                }
            }
            stage('Verify Deployment') {
                steps {

                    sh '''
                    kubectl get nodes

                    kubectl get deployments

                    kubectl get pods

                    kubectl get svc

                    kubectl rollout status deployment/node-app
                    '''
                }
           }
    }
           post {

                success {
                    echo "Application deployed successfully to Amazon EKS."
                }

                failure {
                    echo "Pipeline failed. Please check the logs."
                }

                always {
                    cleanWs()
                }
            }
        
    }






