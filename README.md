Микросервисная платформа для автоматизации СТО

```mermaid
graph TD
    Client[Client] -->|HTTP| Gateway[API Gateway :8080]
    
    subgraph Infrastructure
        Eureka[Discovery Server]
        Redis[(Shared Redis Cache)]
        RabbitMQ[[RabbitMQ Message Broker]]
    end

    subgraph Services
        Security[Security Service]
        User[User Service]
        Order[Order Service]
        Station[Station Service]
    end

    Security --> DB1[(PostgreSQL)]
    Security --> BL[("Blacklist (Redis/DB)")]
    User --> DB2[(PostgreSQL)]
    Order --> DB3[(PostgreSQL)]
    Station --> DB4[(PostgreSQL)]

    Gateway -->|lb://| Security
    Gateway -->|lb://| User
    Gateway -->|lb://| Order
    Gateway -->|lb://| Station

    Gateway <-->|Rate Limit| Redis
    Order -.->|Feign + CB| User
    Order -.->|Feign + CB| Station
    
    Security -->|Cache/Check| Redis
    User -->|Cache| Redis
    Order -->|Cache| Redis
    Station -->|Cache| Redis
    
    Security -->|Event: User Registered| RabbitMQ
    User -->|Events: user.deleted/updated| RabbitMQ
    Station -->|Events: station.deleted/updated| RabbitMQ
    RabbitMQ -->|Consumers + DLQ| Order
```
3. Технологический стек (Tech Stack)

* **Core:** Java 21+, Spring Boot 3.x, Spring Data JPA
* **Microservices & Infrastructure:** Spring Cloud Gateway, Netflix Eureka (Service Discovery), Spring Cloud OpenFeign
* **Fault Tolerance & Resilience:** Resilience4j (Circuit Breaker, Time Limiter), Spring Cloud LoadBalancer
* **Databases & Caching:** PostgreSQL / MySQL, Redis (Distributed Cache & Rate Limiting)
* **Messaging (Async):** RabbitMQ (AMQP, Dead Letter Queues architecture)
* **API Documentation:** Springdoc OpenAPI / Swagger UI (агрегированный на шлюзе)
* **Containerization:** Docker, Docker Compose
* * **Observability & Tracing:** Micrometer Tracing, Zipkin (распределенная трассировка), Prometheus (сбор метрик), Grafana (дашборды)
* **Testing:** Testcontainers (PostgreSQL, Redis, RabbitMQ), JUnit 5, Mockito
* **External Integrations:** ЮKassa API (эквайринг и обработка платежей)

4. Реализованные паттерны и паттерны отказоустойчивости

* **API Gateway & Centralized Security:** Маршрутизация всех запросов через единую точку входа с централизованной валидацией токенов и ограничением частоты запросов (**Redis Rate Limiter**).
* **Circuit Breaker & Fallbacks (Resilience4j):** Защита каскадных падений в `order-service` при вызовах `user-service` и `station-service`. Настроены изолированные окна падения и фолбэк-методы.
* **Distributed Caching & Eviction:** Оптимизация производительности за счет кэширования запросов в Redis. Реализован механизм инвалидации кэша (`evictCache`) при изменении данных.
* **Reliable Messaging:** Асинхронная синхронизация сущностей (удаление/обновление пользователей и станций) через RabbitMQ. Использование **Dead Letter Queues (DLQ)** для изоляции проблемных сообщений.
* **Transactional Outbox Pattern:** Гарантия надежной доставки сообщений (At-Least-Once) без потери данных при сетевых сбоях. События сохраняются в БД в рамках единой транзакции с бизнес-сущностью и асинхронно перекладываются в RabbitMQ.
* **Idempotent Consumers:** Защита от дублирования сообщений при повторной доставке из брокера.

5. Быстрый запуск (Quick Start)

```markdown

1. Клонируйте репозиторий
2.Соберите jar-файлы всех микросервисов:
mvn clean package
3.Запустите всю инфраструктуру и сервисы одной командой:
docker-compose up --build -d
### Доступные сервисы и панели управления:
* **API Gateway (Единая точка входа):** `http://localhost:8080`
* **Swagger UI (Агрегированная документация):** `http://localhost:8080/swagger-ui.html`
* **Eureka Dashboard:** `http://localhost:8761`
* **RabbitMQ Management:** `http://localhost:15672` (guest / guest)
* **Grafana (Метрики):** `http://localhost:3000`
* **Zipkin (Трассировка запросов):** `http://localhost:9411`
