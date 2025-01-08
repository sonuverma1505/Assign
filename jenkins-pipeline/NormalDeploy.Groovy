pipeline {
    agent any
    environment {
        DOCKER_HUB_CREDENTIALS = '6e96ac2b-3cf2-4789-adaf-37786589eb11' // Jenkins Docker Hub credentials ID
        KUBE_CONFIG = '/root/.kube/config' // Path to the kubeconfig file
        IMAGE_NAME = 'sonuverma1505/hello-world-app' // Docker Hub image name
        IMAGE_TAG = 'latest' // Docker image tag
    }

    stages {
        stage('Clean Environment') {
            steps {
                script {
                    // Ensure a clean environment
                    sh 'git config --global --unset-all credential.helper || true'
                    sh 'git config --global --unset-all user.name || true'
                    sh 'git config --global --unset-all user.password || true'
                    sh 'unset GIT_USERNAME || true'
                    sh 'unset GIT_TOKEN || true'
                }
            }
        }

        stage('Print Environment Variables') {
            steps {
                script {
                    // Print all environment variables
                    sh 'printenv'
                }
            }
        }

        stage('Checkout') {
            steps {
                // Checkout the repository using scmGit
                checkout scmGit(
                    branches: [[name: 'main']],
                    userRemoteConfigs: [[url: 'https://github.com/sonuverma1505/Assign.git']]
                )
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Build the Docker image from the Frontend directory
                    dir('Frontend') {
                        docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                    }
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    // Login to Docker Hub with credentials
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDENTIALS}", usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh 'echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin'
                    }

                    // Push the Docker image to Docker Hub
                    sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: 'minikube-kubeconfig']) {
                        // Apply the Kubernetes deployment manifest
                        sh "kubectl apply -f k8s/app-deploy.yaml -n vault"
                        
                        // Set the image for the deployment
                        sh "kubectl set image deployment/hello-world-app hello-world-app=${IMAGE_NAME}:${IMAGE_TAG} -n vault"
                        
                        // Wait for the deployment to roll out
                        sh "kubectl rollout status deployment/hello-world-app -n vault"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs() // Clean workspace after pipeline runs
        }
    }
}