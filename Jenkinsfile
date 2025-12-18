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
        booleanParam(name: 'ENABLE_KAFKA', defaultValue: true, description: 'Deploy Kafka dependency from umbrella chart')
        string(name: 'KAFKA_VALUES_FILE', defaultValue: 'deploy/helm/kafka/values-single-node.yaml', description: 'Optional values file applied when ENABLE_KAFKA=true')
        booleanParam(name: 'DEPLOY_ZIPKIN', defaultValue: true, description: 'Deploy Zipkin')
        booleanParam(name: 'DEPLOY_PROMETHEUS', defaultValue: true, description: 'Deploy Prometheus')
        booleanParam(name: 'DEPLOY_GRAFANA', defaultValue: true, description: 'Deploy Grafana')
        booleanParam(name: 'DEPLOY_ELK', defaultValue: true, description: 'Deploy Elasticsearch/Logstash/Kibana')
    }
    environment {
        RELEASE_NAME = 'bank'
        UMBRELLA_CHART = 'deploy/helm/umbrella'
        ZIPKIN_CHART = 'deploy/helm/zipkin'
        PROM_CHART = 'deploy/helm/prometheus'
        GRAFANA_CHART = 'deploy/helm/grafana'
        ELK_CHART = 'deploy/helm/elk'
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
        stage('Deploy Observability') {
            steps {
                script {
                    if (params.DEPLOY_ZIPKIN) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-zipkin ${env.ZIPKIN_CHART} --namespace ${params.K8S_NAMESPACE_TEST} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_PROMETHEUS) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-prometheus ${env.PROM_CHART} --namespace ${params.K8S_NAMESPACE_TEST} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_GRAFANA) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-grafana ${env.GRAFANA_CHART} --namespace ${params.K8S_NAMESPACE_TEST} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_ELK) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-elk ${env.ELK_CHART} --namespace ${params.K8S_NAMESPACE_TEST} --wait --timeout 5m"
                    }
                }
            }
        }
        stage('Deploy Test') {
            steps {
                script {
                    def services = new groovy.json.JsonSlurper().parseText(env.SERVICES_JSON)
                    def setFlags = services.collect { svc ->
                        "--set ${svc.name}.image.repository=${params.DOCKER_REGISTRY}/${svc.name} --set ${svc.name}.image.tag=${env.EFFECTIVE_IMAGE_TAG}"
                    }.join(" \\\n  ")
                    def kafkaSet = "--set kafka.enabled=${params.ENABLE_KAFKA}"
                    def kafkaValues = (params.ENABLE_KAFKA && params.KAFKA_VALUES_FILE?.trim()) ? "-f ${params.KAFKA_VALUES_FILE.trim()}" : ""
                    sh """
                      helm upgrade --install ${env.RELEASE_NAME} ${env.UMBRELLA_CHART} \
                        --namespace ${params.K8S_NAMESPACE_TEST} \
                        ${kafkaValues} \
                        ${kafkaSet} \
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
                    def kafkaSet = "--set kafka.enabled=${params.ENABLE_KAFKA}"
                    def kafkaValues = (params.ENABLE_KAFKA && params.KAFKA_VALUES_FILE?.trim()) ? "-f ${params.KAFKA_VALUES_FILE.trim()}" : ""
                    if (params.DEPLOY_ZIPKIN) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-zipkin ${env.ZIPKIN_CHART} --namespace ${params.K8S_NAMESPACE_PROD} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_PROMETHEUS) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-prometheus ${env.PROM_CHART} --namespace ${params.K8S_NAMESPACE_PROD} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_GRAFANA) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-grafana ${env.GRAFANA_CHART} --namespace ${params.K8S_NAMESPACE_PROD} --wait --timeout 3m"
                    }
                    if (params.DEPLOY_ELK) {
                        sh "helm upgrade --install ${env.RELEASE_NAME}-elk ${env.ELK_CHART} --namespace ${params.K8S_NAMESPACE_PROD} --wait --timeout 5m"
                    }
                    sh """
                      helm upgrade --install ${env.RELEASE_NAME} ${env.UMBRELLA_CHART} \
                        --namespace ${params.K8S_NAMESPACE_PROD} \
                        ${kafkaValues} \
                        ${kafkaSet} \
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
