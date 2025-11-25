pipeline {
    agent any
    options {
        timestamps()
        disableConcurrentBuilds()
    }
    parameters {
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Override image tag (default: short commit hash)')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'bank', description: 'Image repository prefix (e.g. bank or registry.example.com/bank)')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Push built images to registry')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: '', description: 'Jenkins credentials id for docker registry (optional)')
        booleanParam(name: 'DEPLOY_PROD', defaultValue: false, description: 'Also deploy to prod namespace after test')
        string(name: 'K8S_NAMESPACE_TEST', defaultValue: 'default', description: 'Namespace for test deploy')
        string(name: 'K8S_NAMESPACE_PROD', defaultValue: 'default', description: 'Namespace for prod deploy')
    }
    environment {
        RELEASE_NAME = 'bank'
        UMBRELLA_CHART = 'deploy/helm/umbrella'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Set Variables') {
            steps {
                script {
                    env.EFFECTIVE_IMAGE_TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : (env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : env.BUILD_NUMBER)
                    env.SERVICES_JSON = groovy.json.JsonOutput.toJson([
                        [name: 'accounts-service', module: 'accounts-service'],
                        [name: 'cash-service', module: 'cash-service'],
                        [name: 'transfer-service', module: 'transfer-service'],
                        [name: 'notifications-service', module: 'notifications-service'],
                        [name: 'exchange-service', module: 'exchange-service'],
                        [name: 'exchange-generator-service', module: 'exchange-generator-service'],
                        [name: 'blocker-service', module: 'blocker-service'],
                        [name: 'front-ui', module: 'front-ui'],
                        [name: 'keycloak', module: 'keycloak', context: 'keycloak']
                    ])
                }
            }
        }
        stage('Validate') {
            steps {
                sh "mvn -B test"
            }
        }
        stage('Build Jars') {
            steps {
                sh "mvn -B clean package -DskipTests"
            }
        }
        stage('Build Images') {
            steps {
                script {
                    def services = new groovy.json.JsonSlurper().parseText(env.SERVICES_JSON)
                    services.each { svc ->
                        def contextDir = svc.context ?: svc.module
                        def image = "${params.DOCKER_REGISTRY}/${svc.name}:${env.EFFECTIVE_IMAGE_TAG}"
                        sh "docker build -t ${image} ${contextDir}"
                    }
                }
            }
        }
        stage('Push Images') {
            when { expression { params.PUSH_IMAGE } }
            steps {
                script {
                    def services = new groovy.json.JsonSlurper().parseText(env.SERVICES_JSON)
                    def pushClosure = {
                        services.each { svc ->
                            def image = "${params.DOCKER_REGISTRY}/${svc.name}:${env.EFFECTIVE_IMAGE_TAG}"
                            sh "docker push ${image}"
                        }
                    }
                    if (params.DOCKER_CREDENTIALS_ID?.trim()) {
                        docker.withRegistry('', params.DOCKER_CREDENTIALS_ID) {
                            pushClosure()
                        }
                    } else {
                        pushClosure()
                    }
                }
            }
        }
        stage('Helm Dependencies') {
            steps {
                sh "helm dependency update ${env.UMBRELLA_CHART}"
            }
        }
        stage('Deploy Test') {
            steps {
                script {
                    def services = new groovy.json.JsonSlurper().parseText(env.SERVICES_JSON)
                    def setFlags = services.collect { svc ->
                        "--set ${svc.name}.image.repository=${params.DOCKER_REGISTRY}/${svc.name} --set ${svc.name}.image.tag=${env.EFFECTIVE_IMAGE_TAG}"
                    }.join(" \\\n  ")
                    sh """
                      helm upgrade --install ${env.RELEASE_NAME} ${env.UMBRELLA_CHART} \
                        --namespace ${params.K8S_NAMESPACE_TEST} \
                        ${setFlags} \
                        --wait --atomic
                    """
                }
            }
        }
        stage('Deploy Prod') {
            when { expression { params.DEPLOY_PROD } }
            steps {
                input message: 'Deploy umbrella chart to PROD namespace?', ok: 'Deploy'
                script {
                    def services = new groovy.json.JsonSlurper().parseText(env.SERVICES_JSON)
                    def setFlags = services.collect { svc ->
                        "--set ${svc.name}.image.repository=${params.DOCKER_REGISTRY}/${svc.name} --set ${svc.name}.image.tag=${env.EFFECTIVE_IMAGE_TAG}"
                    }.join(" \\\n  ")
                    sh """
                      helm upgrade --install ${env.RELEASE_NAME} ${env.UMBRELLA_CHART} \
                        --namespace ${params.K8S_NAMESPACE_PROD} \
                        ${setFlags} \
                        --wait --atomic
                    """
                }
            }
        }
    }
    post {
        always {
            sh 'kubectl get pods -A --no-headers || true'
        }
    }
}
