# Bank Microservices

Набор микросервисов банка: UI, аккаунты, переводы, кеш и уведомления. Проект построен на Spring Boot 3 / Java 21, использует PostgreSQL, Consul и Keycloak.

## Запуск

```bash
git clone --branch kuber --single-branch https://github.com/AnonUserG/online_bank.git
cd online_bank
```

# 1) Запуск кластера и ingress
minikube start --driver=docker --memory=6g --cpus=4 --kubernetes-version=v1.29.4 --base-image=docker.io/kicbase/stable:v0.0.48
minikube addons enable ingress

# 2) Используем Docker демона minikube
minikube -p minikube docker-env --shell=powershell | Invoke-Expression

# 3) Сборка артефактов и образов (skipTests чтобы быстрее)
mvn clean package -DskipTests
docker build -t bank/keycloak:23.0-custom                keycloak
docker build -t bank/accounts-service:latest             accounts-service
docker build -t bank/notifications-service:latest        notifications-service
docker build -t bank/cash-service:latest                 cash-service
docker build -t bank/transfer-service:latest             transfer-service
docker build -t bank/blocker-service:latest              blocker-service
docker build -t bank/exchange-service:latest             exchange-service
docker build -t bank/exchange-generator-service:latest   exchange-generator-service
docker build -t bank/front-ui:latest                     front-ui

# 4) Готовим зависимости чарта и деплоим с nip.io
helm dependency update deploy/helm/umbrella
helm upgrade --install bank deploy/helm/umbrella -f deploy/helm/umbrella/values-nipio-example.yaml

# 5) Ждём готовности
kubectl get pods (опционально -w)

# 6) Заливаем данные в Postgres (job использует тот же SQL, что и init-db/01-init-schemas.sql)
kubectl apply -f deploy/k8s/init-db-job.yaml
kubectl logs job/bank-init-db

# 7) Тоннель для ingress/LoadBalancer в отдельном окне
minikube tunnel

# 8) Обновить ConfigMap содержимым keycloak/realm-export.json
kubectl create configmap bank-keycloak-realm --from-file=realm-export.json=keycloak/realm-export.json --dry-run=client -o yaml | kubectl apply -f -

# 9) Перезапустить Keycloak, чтобы подтянул новый файл
kubectl delete pod -l app.kubernetes.io/name=keycloak
kubectl rollout status deployment/bank-keycloak --timeout=120s

# 10) Импортировать/обновить realm bank через kcadm
$POD = kubectl get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}'
kubectl exec $POD -- bash -c "/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin123 \
&& /opt/keycloak/bin/kcadm.sh update realms/bank -f /opt/keycloak/data/import/realm-export.json"


Дождитесь загрузки всех сервисов.

Авторизуйтесь на http://bank.127.0.0.1.nip.io/ используя:
логин= bob
пароль= password