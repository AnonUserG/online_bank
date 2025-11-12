# Bank Microservices

Набор микросервисов банка: UI, аккаунты, переводы, кеш и уведомления. Проект построен на Spring Boot 3 / Java 21, использует PostgreSQL, Consul и Keycloak.

## Запуск

```bash
git clone https://github.com/AnonUserG/online_bank.git
cd online_bank
mvn clean package
docker compose -f docker-compose.yml -f docker-compose.services.yml up -d --build
```
Дождитесь загрузки всех сервисов.

Авторизуйтесь используя:
логин= bob
пароль= password

Изначально у 'bob' на балансе 1500р