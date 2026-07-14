pipeline {
    agent none

    stages {

        stage('Checkout') {
            agent any
            steps {
                git branch: 'master',
                    url: 'https://github.com/mayurmwagh/onlinebookstore.git'
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'maven:3.9.11-eclipse-temurin-17'
                    reuseNode true
                }
            }

            steps {
                sh 'mvn clean package'
            }
        }
    }
}

