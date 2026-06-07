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
                bat 'mvnw.cmd clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def tag        = params.IMAGE_TAG?.trim() ?: env.BUILD_NUMBER
                    def repoPrefix = params.REGISTRY?.trim() ? "${params.REGISTRY}/" : 'bank-app/'
                    env.TAG        = tag
                    env.REPO_PREFIX = repoPrefix

                    def isLocal = !(params.REGISTRY?.trim())

                    def services = [
                        'accounts', 'auth-server', 'bank-web', 'cash',
                        'gateway', 'notifications', 'transfer'
                    ]
                    for (svc in services) {
                        bat "docker build --provenance=false -t ${repoPrefix}${svc}:${tag} .\\${svc}"
                        if (isLocal) {
                            bat "docker save ${repoPrefix}${svc}:${tag} | wsl -d rancher-desktop docker load"
                        }
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
                        'accounts', 'auth-server', 'bank-web', 'cash',
                        'gateway', 'notifications', 'transfer'
                    ]
                    for (svc in services) {
                        bat "docker push ${env.REPO_PREFIX}${svc}:${env.TAG}"
                    }
                }
            }
        }

        stage('Helm Deploy') {
            steps {
                script {
                    def tag    = env.TAG
                    def prefix = env.REPO_PREFIX

                    bat 'helm dependency update helm/bank-app'

                    def setArgs = [
                        "accounts.image.tag=${tag}",
                        "accounts.image.repository=${prefix}accounts",
                        "auth-server.image.tag=${tag}",
                        "auth-server.image.repository=${prefix}auth-server",
                        "bank-web.image.tag=${tag}",
                        "bank-web.image.repository=${prefix}bank-web",
                        "cash.image.tag=${tag}",
                        "cash.image.repository=${prefix}cash",
                        "gateway.image.tag=${tag}",
                        "gateway.image.repository=${prefix}gateway",
                        "notifications.image.tag=${tag}",
                        "notifications.image.repository=${prefix}notifications",
                        "transfer.image.tag=${tag}",
                        "transfer.image.repository=${prefix}transfer",
                    ].collect { "--set ${it}" }.join(' ')

                    bat "helm upgrade --install ${params.HELM_RELEASE} helm/bank-app --namespace ${params.K8S_NAMESPACE} --create-namespace ${setArgs} --wait --timeout 10m"
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