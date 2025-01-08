pipeline {
    agent any
    parameters {
        string(name: 'NAMESPACE', defaultValue: 'vault', description: 'Kubernetes namespace to deploy into')
    }
    environment {
        DOCKER_HUB_CREDENTIALS = '6e96ac2b-3cf2-4789-adaf-37786589eb11' // Jenkins Docker Hub credentials ID
        KUBE_CONFIG = '/root/.kube/config' // Path to the kubeconfig file
        IMAGE_NAME = 'sonuverma1505/hello-world-app' // Docker Hub image name
        HELM_CHART_DIR = 'Chart' // Directory containing the Helm chart
        HELM_RELEASE_NAME = 'hello-world-app' // Helm release name
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

        stage('Determine Image Tag') {
            steps {
                script {
                    // Generate a unique image tag using the Jenkins build number
                    env.IMAGE_TAG = "build-${BUILD_NUMBER}"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Build the Docker image with the new tag
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

        stage('Package Helm Chart') {
            steps {
                script {
                    // Package the Helm chart
                    dir("${HELM_CHART_DIR}") {
                        sh "helm package ."
                    }
                }
            }
        }

        stage('Deploy with Helm') {
            steps {
                script {
                    withKubeConfig([credentialsId: 'minikube-kubeconfig']) {
                        // Perform a Helm upgrade or install
                        sh "helm upgrade --install ${HELM_RELEASE_NAME} ${HELM_CHART_DIR} \
                            --set image.repository=${IMAGE_NAME},image.tag=${IMAGE_TAG} \
                            --namespace ${params.NAMESPACE} --create-namespace"

                        // Verify Helm release status
                        sh "helm status ${HELM_RELEASE_NAME} -n ${params.NAMESPACE}"
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
