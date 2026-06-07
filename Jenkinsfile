pipeline {
    agent any

    parameters {
        string(
            name: 'REGISTRY',
            defaultValue: '',
            description: 'Docker registry prefix including namespace, e.g. registry.example.com/bank-app. Leave empty for local builds (Rancher Desktop / Minikube).'
        )
        string(
            name: 'IMAGE_TAG',
            defaultValue: '',
            description: 'Docker image tag. Defaults to BUILD_NUMBER when left empty.'
        )
        string(
            name: 'HELM_RELEASE',
            defaultValue: 'bank',
            description: 'Helm release name'
        )
        string(
            name: 'K8S_NAMESPACE',
            defaultValue: 'default',
            description: 'Kubernetes namespace'
        )
    }

    stages {

        stage('Build') {
            steps {
                sh 'chmod +x mvnw && ./mvnw clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def tag        = params.IMAGE_TAG?.trim() ?: env.BUILD_NUMBER
                    def repoPrefix = params.REGISTRY?.trim() ? "${params.REGISTRY}/" : 'bank-app/'
                    env.TAG        = tag
                    env.REPO_PREFIX = repoPrefix

                    def services = [
                        'accounts',
                        'auth-server',
                        'bank-web',
                        'cash',
                        'gateway',
                        'notifications',
                        'transfer'
                    ]
                    for (svc in services) {
                        sh "docker build -t '${repoPrefix}${svc}:${tag}' './${svc}'"
                    }
                }
            }
        }

        stage('Docker Push') {
            when {
                expression { params.REGISTRY?.trim() }
            }
            steps {
                script {
                    def services = [
                        'accounts',
                        'auth-server',
                        'bank-web',
                        'cash',
                        'gateway',
                        'notifications',
                        'transfer'
                    ]
                    for (svc in services) {
                        sh "docker push '${env.REPO_PREFIX}${svc}:${env.TAG}'"
                    }
                }
            }
        }

        stage('Helm Deploy') {
            steps {
                script {
                    def tag    = env.TAG
                    def prefix = env.REPO_PREFIX
                    sh """
                        helm dependency update helm/bank-app
                        helm upgrade --install '${params.HELM_RELEASE}' helm/bank-app \
                            --namespace '${params.K8S_NAMESPACE}' \
                            --create-namespace \
                            --set "accounts.image.tag=${tag}" \
                            --set "accounts.image.repository=${prefix}accounts" \
                            --set "auth-server.image.tag=${tag}" \
                            --set "auth-server.image.repository=${prefix}auth-server" \
                            --set "bank-web.image.tag=${tag}" \
                            --set "bank-web.image.repository=${prefix}bank-web" \
                            --set "cash.image.tag=${tag}" \
                            --set "cash.image.repository=${prefix}cash" \
                            --set "gateway.image.tag=${tag}" \
                            --set "gateway.image.repository=${prefix}gateway" \
                            --set "notifications.image.tag=${tag}" \
                            --set "notifications.image.repository=${prefix}notifications" \
                            --set "transfer.image.tag=${tag}" \
                            --set "transfer.image.repository=${prefix}transfer" \
                            --wait --timeout 5m
                    """
                }
            }
        }

    }

    post {
        success {
            script {
                echo "Deployed '${params.HELM_RELEASE}' with tag '${env.TAG}'"
            }
        }
        failure {
            echo 'Pipeline failed — check logs above'
        }
    }
}