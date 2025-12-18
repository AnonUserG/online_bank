# Bank Microservices

Набор микросервисов банка: UI, аккаунты, переводы, кеш и уведомления. Проект построен на Spring Boot 3 / Java 21, использует PostgreSQL, Consul и Keycloak.

## Запуск

```bash
git clone --branch elk --single-branch https://github.com/AnonUserG/online_bank.git
cd online_bank
```

# 1) Запуск кластера и ingress
minikube start --driver=docker --memory=6g --cpus=4 --kubernetes-version=v1.29.4 --base-image=docker.io/kicbase/stable:v0.0.48
minikube addons enable ingress

# 2) Используем Docker демона minikube
minikube -p minikube docker-env --shell=powershell | Invoke-Expression

# 3) Сборка артефактов и образов (skipTests чтобы быстрее)
mvn clean package -DskipTests
docker build --no-cache -t bank/keycloak:23.0-custom                keycloak
docker build --no-cache -t bank/accounts-service:latest             accounts-service
docker build --no-cache -t bank/notifications-service:latest        notifications-service
docker build --no-cache -t bank/cash-service:latest                 cash-service
docker build --no-cache -t bank/transfer-service:latest             transfer-service
docker build --no-cache -t bank/blocker-service:latest              blocker-service
docker build --no-cache -t bank/exchange-service:latest             exchange-service
docker build --no-cache -t bank/exchange-generator-service:latest   exchange-generator-service
docker build --no-cache -t bank/front-ui:latest                     front-ui
kubectl apply -f deploy/k8s/kafka-standalone.yaml


# 4) Разворачиваем Zipkin (один инстанс)
helm upgrade --install bank-zipkin deploy/helm/zipkin --timeout 5m --wait
# (опционально) посмотреть UI локально: kubectl port-forward svc/bank-zipkin 9411:9411

# 5) Разворачиваем Prometheus, Grafana и ELK (по одному инстансу)
helm upgrade --install bank-prometheus deploy/helm/prometheus --timeout 5m --wait
helm upgrade --install bank-grafana deploy/helm/grafana --timeout 5m --wait
helm upgrade --install bank-elk deploy/helm/elk --timeout 5m --wait

# 6) Готовим зависимости чарта и деплоим с nip.io
helm dependency update deploy/helm/umbrella
helm upgrade --install bank deploy/helm/umbrella -f deploy/helm/umbrella/values-nipio-example.yaml --set kafka.enabled=false --timeout 5m --wait

kubectl rollout restart deploy/bank-exchange-generator-service
kubectl rollout restart deploy/bank-exchange-service

# 7) Ждём готовности
kubectl get pods

# 8) Заливаем данные в Postgres (job использует тот же SQL, что и init-db/01-init-schemas.sql)
kubectl apply -f deploy/k8s/init-db-job.yaml

# 9) Тоннель для ingress/LoadBalancer в отдельном окне
minikube tunnel

# 10) Обновить ConfigMap содержимым keycloak/realm-export.json
kubectl create configmap bank-keycloak-realm --from-file=realm-export.json=keycloak/realm-export.json --dry-run=client -o yaml | kubectl apply -f -

# 11) Перезапустить Keycloak, чтобы подтянул новый файл
kubectl delete pod -l app.kubernetes.io/name=keycloak
kubectl rollout status deployment/bank-keycloak --timeout=150s

# 12) Импортировать/обновить realm bank через kcadm
$POD = kubectl get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}'
kubectl exec $POD -- bash -c "/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin123 \
&& /opt/keycloak/bin/kcadm.sh update realms/bank -f /opt/keycloak/data/import/realm-export.json"

Дождитесь загрузки всех сервисов.

Авторизуйтесь на http://bank.127.0.0.1.nip.io/ используя:
логин= bob
пароль= password

# UI/порт-форварды для наблюдаемости (в отдельных терминалах)
# Zipkin:        kubectl port-forward svc/bank-zipkin 9411:9411       # http://localhost:9411
# Prometheus:    kubectl port-forward svc/bank-prometheus 9090:9090   # http://localhost:9090
# Grafana:       kubectl port-forward svc/bank-grafana 3000:3000      # http://localhost:3000 (admin/admin123)
# Kibana:        kubectl port-forward svc/bank-elk-kibana 5601:5601   # http://localhost:5601
# Логи летят в Kafka topic service-logs и индексируются Logstash в service-logs-*

# 13) Почистить все
minikube stop
minikube delete

# CI/CD с Jenkins

### Предпосылки
- Minikube должен быть запущен и kube‑context доступен Jenkins (даже если поды ещё не созданы — `helm upgrade --install` сам развернёт их, но кластер обязан работать).
- Jenkins нужен с установленными docker, kubectl, helm, Maven/Java 21 (или отдельный агент с этими инструментами).
- Для сборки образов из контейнера Jenkins нужно прокинуть Docker socket.

### Быстрый старт Jenkins в контейнере (порт 8090 -> 8080 Jenkins)
```bash
$kubePath = "//c/Users/$env:USERNAME/.kube"
$miniPath = "//c/Users/$env:USERNAME/.minikube"
docker rm -f jenkins 2>$null
docker run -d --name jenkins `
  -p 8090:8080 -p 50000:50000 `
  --restart=on-failure `
  -v jenkins_home:/var/jenkins_home `
  --mount type=bind,source=$kubePath,target=/var/jenkins_home/.kube `
  --mount type=bind,source=$miniPath,target=/var/jenkins_home/.minikube `
  --mount type=bind,source=//var/run/docker.sock,target=/var/run/docker.sock `
  jenkins/jenkins:lts-jdk17
```
*Если нужно helm/kubectl внутри контейнера — доставьте их через `docker exec -it jenkins bash` (apk/apt) или используйте агент с уже установленными утилитами.*

### Jenkinsfile в репозитории
- Зонтичный: `Jenkinsfile` (корень) — собирает все модули, строит и пушит образы, деплоит Helm umbrella-чарт в namespace `default` (параметризуемо).
- Пер-сервисные: `accounts-service/Jenkinsfile`, `cash-service/Jenkinsfile`, `transfer-service/Jenkinsfile`, `notifications-service/Jenkinsfile`, `exchange-service/Jenkinsfile`, `exchange-generator-service/Jenkinsfile`, `blocker-service/Jenkinsfile`, `front-ui/Jenkinsfile` — тесты модуля, сборка jar, docker build/push, Helm deploy чарта сервиса.

### Основные параметры пайплайнов
- `IMAGE_TAG` — тег образов (по умолчанию short SHA).
- `DOCKER_REGISTRY` — префикс репозитория (пример: `bank` или `registry.example.com/bank`).
- `PUSH_IMAGE` и `DOCKER_CREDENTIALS_ID` — пуш образов и креды реестра.
- `K8S_NAMESPACE_TEST`/`K8S_NAMESPACE_PROD` — target namespace (по умолчанию `default`); `DEPLOY_PROD` включает выкатку в prod после test.

### Завести пайплайн в Jenkins
1. Создать Pipeline job → «Pipeline from SCM» → SCM: Git → URL на репозиторий → путь к нужному `Jenkinsfile`.
2. В параметрах указать при необходимости `DOCKER_REGISTRY`, креды, namespace.
3. Убедиться, что Jenkins видит kube‑context minikube: `kubectl config current-context` из контейнера/агента должен выдавать `minikube`.
4. Запустить билд: в test namespace выполнится `helm upgrade --install`; при включённом `DEPLOY_PROD` будет запрос подтверждения и деплой в prod namespace.

### Проверка
- После деплоя: `kubectl get pods -n <namespace>` и `kubectl get svc -n <namespace>`.
- При проблемах со сборкой образов из Jenkins — проверить доступ к Docker socket и достаточно ли ресурсов в контейнере Jenkins/агенте.