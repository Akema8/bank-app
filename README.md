# bank-app

Микросервисное приложение "Банк" на Spring Boot 3.5 / Spring Cloud 2025 / Java 21.

Поддерживает два режима развёртывания: **Docker Compose** (локальная разработка) и **Kubernetes + Helm**.

## Состав

| Сервис | Порт (Docker) | Порт (k8s) | Описание |
|---|---|---|---|
| `bank-web` | 8080 | NodePort 30800 | Веб-фронтенд (Thymeleaf). OAuth2 Authorization Code Flow. |
| `gateway` | 8090 | ClusterIP 8090 | Spring Cloud Gateway. Маршрутизирует запросы к accounts/cash/transfer. |
| `auth-server` | 9000 | NodePort 30900 | Spring Authorization Server. JWT по Authorization Code и Client Credentials. |
| `accounts` | 8082 | ClusterIP 8082 | CRUD аккаунтов (логин/ФИО/дата рождения/баланс). |
| `cash` | 8084 | ClusterIP 8084 | Внесение/снятие наличных. Вызывает accounts и notifications. |
| `transfer` | 8085 | ClusterIP 8085 | Переводы между пользователями. Сага с компенсацией. |
| `notifications` | 8083 | ClusterIP 8083 | Эмулирует отправку уведомлений. |
| `mysql` | 3307 | StatefulSet 3306 | MySQL 8.4. Базы `accounts`, `cash`, `transfer`. |
| `eureka-server` | 8761 | — | Service Discovery (только Docker Compose). |
| `zookeeper` | 2181 | — | Distributed Config (только Docker Compose). |

---

## Запуск в Kubernetes (Helm)

### Требования

- **Kubernetes** — Rancher Desktop, Minikube, Kind или Docker Desktop с включённым k8s
- **Helm 3**
- **JDK 21** + **Maven 3.9+**

### 1. Собрать JAR-файлы

```bash
mvn clean package -DskipTests
```

### 2. Собрать Docker-образы

**Rancher Desktop** (containerd + k3s, dockerd-режим):

```bash
# Должен быть активен контекст Rancher Desktop
docker context use default

docker build -t bank-app/auth-server:latest  ./auth-server
docker build -t bank-app/accounts:latest     ./accounts
docker build -t bank-app/cash:latest         ./cash
docker build -t bank-app/transfer:latest     ./transfer
docker build -t bank-app/notifications:latest ./notifications
docker build -t bank-app/gateway:latest      ./gateway
docker build -t bank-app/bank-web:latest     ./bank-web
```

**Minikube:**

```bash
eval $(minikube docker-env)
# затем те же docker build команды
```

**Kind:**

```bash
# docker build команды, затем для каждого образа:
kind load docker-image bank-app/accounts:latest
```

### 3. Упаковать Helm-зависимости

```bash
cd helm/bank-app
helm dependency build
```

### 4. Установить

```bash
# Из корня проекта
helm install bank helm/bank-app

# Или с отдельным namespace
helm install bank helm/bank-app --namespace bank --create-namespace
```

Следить за запуском подов:

```bash
kubectl get pods -w
```

### 5. Запустить Helm-тесты

```bash
helm test bank
```

Ожидаемый результат - все 8 тестов со статусом `Phase: Succeeded`.

### URL (Kubernetes)

| Что | URL |
|---|---|
| Веб-приложение | http://localhost:30800 |
| Страница регистрации | http://localhost:30800/register |
| Auth Server (OAuth2) | http://localhost:30900 |

### Обновление после изменений в коде

```bash
mvn package -pl <модуль> -DskipTests
docker build -t bank-app/<модуль>:latest ./<модуль>
helm upgrade bank helm/bank-app
```

### Удаление

```bash
helm uninstall bank
kubectl delete pvc data-mysql-0
```

### Конфигурация (Kubernetes)

| Что | Как задаётся |
|---|---|
| Service Discovery | Kubernetes Services (ClusterIP / NodePort) |
| Distributed Config | ConfigMaps (env vars) |
| Секреты (пароли БД) | Kubernetes Secrets |
| Порты NodePort | `helm/bank-app/values.yaml` → `auth-server.service.nodePort`, `bank-web.service.nodePort` |
| Пароль MySQL | `helm/bank-app/values.yaml` → `mysql.rootPassword` |

Взаимозависимые параметры в `helm/bank-app/values.yaml`:

```
mysql.rootPassword  - accounts/cash/transfer.db.password
auth-server.service.nodePort  -  auth-server.issuerUri  -  bank-web.authServer.externalUri
bank-web.service.nodePort  - auth-server.redirectUri
```

---

## Запуск в Docker Compose

### Требования

- **JDK 21**
- **Maven 3.9+**
- **Docker Desktop** (Windows / macOS) или Docker Engine (Linux)
  - На Windows для интеграционных тестов с Testcontainers: включите `Expose daemon on tcp://localhost:2375 without TLS` в настройках Docker Desktop.

### Запуск

```bash
mvn -DskipTests package
docker compose up --build
```

После старта откройте http://localhost:8080.  
Пользователи создаются через страницу регистрации.

### Остановка

```bash
docker compose down          # сохранить данные MySQL
docker compose down -v       # удалить данные MySQL
```

### URL (Docker Compose)

| Что | URL |
|---|---|
| Веб-приложение | http://localhost:8080 |
| Auth Server | http://localhost:9000 |
| API Gateway | http://localhost:8090 |
| Eureka Dashboard | http://localhost:8761 |

---

## Сборка

```bash
# Все модули
./mvnw clean package

# Пропустить тесты
./mvnw -DskipTests package

# Один модуль
./mvnw -pl accounts -am clean package
```

## Тесты

```bash
./mvnw test
# Один модуль
./mvnw -pl accounts test
```

Используют **Testcontainers** для MySQL - нужен запущенный Docker.

---

## Архитектура

### Безопасность

- **bank-web → auth-server:** OAuth2 Authorization Code Flow (OIDC).
- **cash/transfer → accounts/notifications:** OAuth2 Client Credentials (сервис-к-сервису).
- **JWT-валидация:** все ресурсные сервисы валидируют токены через JWKS (`/oauth2/jwks`).
- Скоупы: `cash.write`, `transfer.write`, `accounts.write`, `notifications.write`.

### Сервисы

- **bank-web** — Spring MVC + Thymeleaf, три блока: аккаунт / деньги / перевод.
- **accounts** — WebFlux + R2DBC. CRUD аккаунтов, deposit/withdraw, вызовы в notifications.
- **cash** — WebFlux + R2DBC. Журнал транзакций, вызовы accounts и notifications.
- **transfer** — WebFlux + R2DBC. Двухэтапный перевод (withdraw → deposit) с откатом при ошибке.
- **notifications** — Spring MVC. Логирование уведомлений.
- **auth-server** — Spring Authorization Server + UI логина/регистрации.
- **gateway** — Spring Cloud Gateway, маршруты к accounts/cash/transfer, JWT-валидация.