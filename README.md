# bank-app

Микросервисное приложение "Банк" на Spring Boot 3.5 / Spring Cloud 2025 / Java 21.

## Состав

| Сервис | Порт      | Описание |
|---|-----------|---|
| `bank-web` | 8080      | Веб-фронтенд (Thymeleaf + WebFlux). OAuth2 Authorization Code Flow. |
| `gateway` | 8090      | Spring Cloud Gateway. Маршрутизирует запросы к accounts/cash/transfer. |
| `auth-server` | 9000 / 9001 | Spring Authorization Server. Выдаёт JWT по Authorization Code и Client Credentials. |
| `accounts` | 8082      | CRUD аккаунтов (логин/ФИО/дата рождения/баланс). |
| `cash` | 8084      | Внесение/снятие наличных. Дёргает accounts и notifications. |
| `transfer` | 8085      | Переводы между пользователями. Сага с компенсацией. |
| `notifications` | 8083      | Эмулирует отправку уведомлений. |
| `eureka-server` | 8761      | Service Discovery. |
| `zookeeper` | 2181      | Spring Cloud Config через ZooKeeper. |
| `mysql` | 3307      | MySQL 8.4. Базы `accounts`, `cash`, `transfer`. |

## Требования

- **JDK 21**
- **Maven 3.9+** (или использовать `./mvnw`)
- **Docker Desktop** (Windows / macOS) или Docker Engine (Linux)
  - На Windows для запуска интеграционных тестов с Testcontainers нужно включить `Expose daemon on tcp://localhost:2375 without TLS` в настройках Docker Desktop.

## Запуск в Docker

Поднять весь стек одной командой:

```bash
mvn -DskipTests package
docker compose up --build
```

После старта откройте http://localhost:8080.

Тестовые пользователи создаются через UI на странице регистрации.

Остановка:

```bash
docker compose down
```

С удалением данных MySQL:

```bash
docker compose down -v
```

## Сборка

Из корня проекта:

```bash
./mvnw clean package
```

Будут собраны все 8 модулей. Готовые JAR-файлы лежат в `<module>/target/*.jar`.

Пропустить тесты:

```bash
./mvnw -DskipTests package
```

Собрать один модуль (с его зависимостями):

```bash
./mvnw -pl accounts -am clean package
```

## Запуск тестов

```bash
./mvnw test
```

Тесты используют **Testcontainers** для MySQL — для них нужен запущенный Docker и доступ к нему через `tcp://127.0.0.1:2375` (см. раздел «Требования»).

Запуск тестов одного модуля:

```bash
./mvnw -pl accounts test
```


## Запуск в среде разработки (без Docker для приложений)

Инфраструктуру (MySQL + ZooKeeper) удобно поднять в Docker, а сами сервисы запускать из IDE.

### 1. Поднять инфраструктуру

```bash
docker compose up -d mysql zookeeper zk-init
```

### 2. Запустить сервисы в порядке зависимостей

В IDE запустить main-классы, либо в терминалах:

```bash
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl auth-server   spring-boot:run
./mvnw -pl accounts      spring-boot:run
./mvnw -pl notifications spring-boot:run
./mvnw -pl cash          spring-boot:run
./mvnw -pl transfer      spring-boot:run
./mvnw -pl gateway       spring-boot:run
./mvnw -pl bank-web      spring-boot:run
```

Каждой команде нужен свой терминал. Порядок важен только для первого запуска (auth-server и eureka-server должны быть готовы до старта остальных).

### 3. Переменные окружения для локального запуска

При запуске вне Docker сервисы по умолчанию ходят на `localhost`. Параметры окружения (можно задать в IDE Run Configuration):

```
ZK_HOST=localhost:2181
DB_URL=r2dbc:mysql://localhost:3307/<имя_базы>
DB_USERNAME=root
DB_PASSWORD=secret
```

`<имя_базы>` - `accounts`, `cash` или `transfer` в зависимости от сервиса.

## Конфигурация

- **Application-конфиги:** `<module>/src/main/resources/application.yml`.
- **Распределённая конфигурация:** хранится в ZooKeeper, см. `zk-init/seed.sh`. Этот скрипт автоматически выполняется контейнером `zk-init` при старте `docker compose up`.
- **Инициализация БД:** `mysql-init/init.sql` создаёт три базы. Схема таблиц подтягивается каждым сервисом при старте через `<module>/src/main/resources/schema.sql`.

## URL

| Что | URL |
|---|---|
| Веб-приложение | http://localhost:8080 |
| Авторизация | http://localhost:9000 |
| API Gateway | http://localhost:8090 |
| Eureka Dashboard | http://localhost:8761 |
| Прямой доступ к accounts | http://localhost:8082 |
| Прямой доступ к cash | http://localhost:8084 |
| Прямой доступ к transfer | http://localhost:8085 |
| Прямой доступ к notifications | http://localhost:8083 |

## Безопасность

- **Bank-web <-> auth-server:** OAuth2 Authorization Code Flow.
- **Cash/Transfer <-> Accounts/Notifications:** OAuth2 Client Credentials. Сервис-к-сервису токены получаются у auth-server.
- **JWT-валидация:** все ресурсные сервисы валидируют JWT через JWKS у auth-server (`/oauth2/jwks`).
- Скоупы: `cash.write`, `transfer.write`, `accounts.write`, `notifications.write`.

## Что внутри

Подробнее об архитектуре сервисов:

- `bank-web` — WebFlux + Thymeleaf, три блока на главной (аккаунт / деньги / перевод).
- `accounts` — WebFlux + R2DBC. CRUD аккаунтов, операции deposit/withdraw, исходящие вызовы в notifications.
- `cash` — WebFlux + R2DBC. Журнал транзакций, вызовы accounts и notifications.
- `transfer` — WebFlux + R2DBC. Двухэтажный перевод (withdraw → deposit) с откатом при ошибке.
- `notifications` — WebFlux. Логирование уведомлений.
- `auth-server` — Spring Authorization Server + UI логина/регистрации.
- `gateway` — Spring Cloud Gateway, маршруты к accounts/cash/transfer.
- `eureka-server` — Netflix Eureka.
