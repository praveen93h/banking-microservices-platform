# Banking Microservices Application

A comprehensive microservices-based banking application built with Spring Boot 3.2, Spring Cloud, and various enterprise-grade technologies.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Applications                             │
│                    (Web, Mobile, Third-party Applications)                   │
└─────────────────────────────────────────┬───────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (8080)                              │
│                    (Authentication, Routing, Rate Limiting)                  │
└─────────────────────────────────────────┬───────────────────────────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    ▼                     ▼                     ▼
┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────────┐
│    User Service (8081)  │ │  Account Service (8082) │ │Transaction Svc (8083)   │
│   - Authentication      │ │   - Account CRUD        │ │  - Money Transfers      │
│   - User Management     │ │   - Balance Operations  │ │  - Deposits/Withdrawals │
│   - KYC Verification    │ │   - Account History     │ │  - Transaction History  │
└───────────┬─────────────┘ └───────────┬─────────────┘ └───────────┬─────────────┘
            │                           │                           │
            │      PostgreSQL           │      PostgreSQL           │      PostgreSQL
            ▼      (users_db)           ▼      (accounts_db)        ▼      (transactions_db)
┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────────┐
│        Database         │ │        Database         │ │        Database         │
└─────────────────────────┘ └─────────────────────────┘ └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         Notification Service (8084)                          │
│                    (Email, SMS, Push, In-App Notifications)                  │
└─────────────────────────────────────────┬───────────────────────────────────┘
                                          │
                                          ▼ PostgreSQL (notifications_db)
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  Database                                    │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │         Supporting Services          │
                    │  ┌─────────────────────────────┐    │
                    │  │  Eureka Server (8761)       │    │
                    │  │  (Service Discovery)        │    │
                    │  └─────────────────────────────┘    │
                    │  ┌─────────────────────────────┐    │
                    │  │  Apache Kafka (9092)        │    │
                    │  │  (Event Bus)                │    │
                    │  └─────────────────────────────┘    │
                    │  ┌─────────────────────────────┐    │
                    │  │  Redis (6379)               │    │
                    │  │  (Caching & Locking)        │    │
                    │  └─────────────────────────────┘    │
                    │  ┌─────────────────────────────┐    │
                    │  │  Zipkin (9411)              │    │
                    │  │  (Distributed Tracing)      │    │
                    │  └─────────────────────────────┘    │
                    └─────────────────────────────────────┘
```

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.2.0 |
| **Cloud** | Spring Cloud | 2023.0.0 |
| **Service Discovery** | Netflix Eureka | Latest |
| **API Gateway** | Spring Cloud Gateway | Latest |
| **Database** | PostgreSQL | 15 |
| **Cache** | Redis | 7 |
| **Message Broker** | Apache Kafka | 7.5.0 (Confluent) |
| **Distributed Tracing** | Zipkin | Latest |
| **Authentication** | JWT (jjwt) | 0.11.5 |
| **Resilience** | Resilience4j | Latest |
| **Containerization** | Docker & Docker Compose | Latest |

## 📦 Services

### 1. Eureka Server (Port: 8761)
Service discovery server that enables microservices to find and communicate with each other.

### 2. API Gateway (Port: 8080)
Central entry point for all client requests with:
- JWT-based authentication
- Request routing
- CORS configuration
- Rate limiting (planned)

### 3. User Service (Port: 8081)
Handles user-related operations:
- User registration and authentication
- JWT token generation
- User profile management
- KYC verification status

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Authenticate user |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/username/{username}` | Get user by username |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

### 4. Account Service (Port: 8082)
Manages bank accounts:
- Account creation and management
- Balance inquiries
- Debit/Credit operations
- Account history

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts` | Create new account |
| GET | `/api/accounts` | Get all accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts/number/{accountNumber}` | Get account by number |
| GET | `/api/accounts/user/{userId}` | Get user's accounts |
| GET | `/api/accounts/{id}/balance` | Get account balance |
| POST | `/api/accounts/{id}/debit` | Debit from account |
| POST | `/api/accounts/{id}/credit` | Credit to account |
| PUT | `/api/accounts/{id}/status` | Update account status |
| GET | `/api/accounts/{id}/history` | Get account history |

### 5. Transaction Service (Port: 8083)
Handles money transfers with Saga pattern:
- Money transfers between accounts
- Deposits and withdrawals
- Transaction history
- Saga-based distributed transactions

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/transfer` | Transfer money |
| POST | `/api/transactions/deposit` | Deposit money |
| POST | `/api/transactions/withdraw` | Withdraw money |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| GET | `/api/transactions/reference/{reference}` | Get by reference |
| GET | `/api/transactions/account/{accountId}` | Get account transactions |
| GET | `/api/transactions/user/{userId}` | Get user transactions |

### 6. Notification Service (Port: 8084)
Event-driven notification system:
- Email notifications
- SMS notifications (planned)
- Push notifications (planned)
- In-app notifications
- Kafka event listeners

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications/user/{userId}` | Get user notifications |
| GET | `/api/notifications/pending` | Get pending notifications |

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local development)
- Maven 3.8+ (for local development)

### Running with Docker Compose

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd banking_microservices
   ```

2. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Start in detached mode:**
   ```bash
   docker-compose up -d --build
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f
   ```

5. **Stop all services:**
   ```bash
   docker-compose down
   ```

6. **Stop and remove volumes:**
   ```bash
   docker-compose down -v
   ```

### Service URLs

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| User Service | http://localhost:8081 |
| Account Service | http://localhost:8082 |
| Transaction Service | http://localhost:8083 |
| Notification Service | http://localhost:8084 |
| Kafka UI | http://localhost:8090 |
| Zipkin | http://localhost:9411 |

### Local Development

1. **Start infrastructure services only:**
   ```bash
   docker-compose up postgres-users postgres-accounts postgres-transactions postgres-notifications kafka zookeeper redis zipkin -d
   ```

2. **Run individual services:**
   ```bash
   cd user-service
   ./mvnw spring-boot:run
   ```

## 📋 Kafka Topics

| Topic | Publisher | Subscriber | Description |
|-------|-----------|------------|-------------|
| `user-events` | User Service | Notification Service | User created/updated events |
| `account-events` | Account Service | Notification Service | Account created events |
| `account-balance-events` | Account Service | - | Balance change events |
| `transaction-completed` | Transaction Service | Notification Service | Successful transactions |
| `transaction-failed` | Transaction Service | Notification Service | Failed transactions |

## 🔐 Authentication

The application uses JWT-based authentication:

1. **Register a new user:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "john",
       "email": "john@example.com",
       "password": "password123",
       "firstName": "John",
       "lastName": "Doe",
       "phoneNumber": "+1234567890"
     }'
   ```

2. **Login:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "john",
       "password": "password123"
     }'
   ```

3. **Use the token:**
   ```bash
   curl -X GET http://localhost:8080/api/users/me \
     -H "Authorization: Bearer <your-jwt-token>"
   ```

## 🔄 Saga Pattern Implementation

The Transaction Service implements the Saga pattern for distributed transactions:

### Transfer Flow
1. **Start Transaction** - Create transaction record with PENDING status
2. **Debit Step** - Debit source account
   - On failure: Mark transaction as FAILED
3. **Credit Step** - Credit destination account
   - On failure: Compensate by crediting source account back
4. **Complete** - Mark transaction as COMPLETED

### Compensation
If any step fails, the saga automatically compensates:
- Credit step fails → Refund the debited amount to source account
- Full audit trail maintained in transaction steps

## 🛡️ Circuit Breaker

Resilience4j circuit breaker is configured for inter-service communication:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      accountService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

## 📊 Monitoring

### Zipkin Tracing
Access Zipkin UI at http://localhost:9411 to trace requests across services.

### Kafka UI
Monitor Kafka topics and messages at http://localhost:8090.

### Health Endpoints
Each service exposes health endpoints:
```bash
curl http://localhost:8081/actuator/health
```

## 📁 Project Structure

```
banking_microservices/
├── docker-compose.yml
├── README.md
├── eureka-server/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/banking/eureka/
├── api-gateway/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/banking/gateway/
├── user-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/banking/user/
├── account-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/banking/account/
├── transaction-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/banking/transaction/
└── notification-service/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/com/banking/notification/
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `SPRING_DATASOURCE_URL` | Database URL | Service-specific |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka URL | `http://localhost:8761/eureka/` |

### JWT Configuration

| Property | Default |
|----------|---------|
| `jwt.secret` | (configured in application.yml) |
| `jwt.expiration` | 86400000 (24 hours) |

## 🧪 Testing

### Run Tests
```bash
cd user-service
./mvnw test
```

### Integration Tests
```bash
./mvnw verify
```

## 🚧 Future Enhancements

- [ ] Rate limiting in API Gateway
- [ ] SMS notification integration
- [ ] Push notification support
- [ ] Loan Service
- [ ] Card Service
- [ ] Kubernetes deployment manifests
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] OAuth2/OpenID Connect integration
- [ ] API versioning

## 📝 License

This project is for educational purposes.

## 👥 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request
