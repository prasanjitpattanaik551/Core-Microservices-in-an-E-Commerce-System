# 🛒 E-Commerce Microservices Platform with Kafka

A microservice architecture integrating the **E-Commerce** and **Payment Gateway** projects with **Apache Kafka** for event-driven asynchronous communication, **Eureka** for service discovery, and **Spring Cloud Gateway** for API routing.

---

## 📐 Architecture Overview

```
                    ┌──────────────────┐
                    │   API Gateway    │
                    │   (port 9090)    │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
   /api/products/**   /api/orders/**  /api/payment/**
   /api/users/**
              │              │              │
              ▼              │              ▼
  ┌───────────────────┐      │    ┌───────────────────┐
  │ E-Commerce Service│◄─────┘    │  Payment Service  │
  │   (port 8080)     │           │   (port 8081)     │
  └────────┬──────────┘           └────────┬──────────┘
           │                               │
           │     ┌───────────────┐         │
           ├────►│  Kafka Broker │◄────────┤
           │     │ (port 9092)   │         │
           │     └───────────────┘         │
           │                               │
           ▼                               ▼
  ┌─────────────────┐           ┌─────────────────┐
  │  MySQL (flipkart)│          │MySQL (payment_gw)│
  │   (port 3306)    │          │   (port 3307)    │
  └──────────────────┘          └──────────────────┘
```

## 🔄 Kafka Event Flow

```
1. User places order via E-Commerce Service
       │
       ▼
2. OrderCreatedEvent ──────► [order-created-topic] ──────► Payment Service
       │                                                        │
       │                                                        ▼
       │                                              3. Processes payment
       │                                                 Saves PaymentOrder
       │                                                 Sends email
       │                                                        │
       │                                                        ▼
4. E-Commerce Service ◄── [payment-processed-topic] ◄── PaymentProcessedEvent
       │
       ▼
5. Updates order status (PENDING → PAID / PAYMENT_FAILED)
       │
       ▼
6. OrderStatusUpdatedEvent ──────► [order-status-updated-topic]
```

## 📦 Module Structure

```
ecommerce-microservices/          ← Parent POM
├── kafka-common/                 ← Shared Kafka event DTOs
│   └── com.microservice.common
│       ├── constants/KafkaTopics.java
│       └── event/
│           ├── OrderCreatedEvent.java
│           ├── PaymentProcessedEvent.java
│           └── OrderStatusUpdatedEvent.java
│
├── ecommerce-service/            ← E-Commerce (port 8080)
│   └── com.microservice.ecommerce
│       ├── controller/           ← REST: /products, /orders, /users
│       ├── model/                ← JPA: Product, User, Orders, OrderItem
│       ├── dto/                  ← OrderDTO, OrderItemDTO
│       ├── repo/                 ← Spring Data JPA repositories
│       ├── service/              ← Business logic
│       └── kafka/
│           ├── OrderEventProducer.java    ← Publishes order events
│           ├── PaymentEventConsumer.java   ← Consumes payment results
│           └── KafkaProducerConfig.java   ← Topic creation
│
├── payment-service/              ← Payment Gateway (port 8081)
│   └── com.microservice.payment
│       ├── controller/           ← REST: /api/payment
│       ├── entity/               ← JPA: PaymentOrder
│       ├── repo/                 ← Spring Data JPA repository
│       ├── service/              ← PaymentService, EmailService
│       └── kafka/
│           ├── OrderEventConsumer.java    ← Consumes order events
│           ├── PaymentEventProducer.java  ← Publishes payment results
│           └── KafkaConsumerConfig.java   ← Topic creation
│
├── api-gateway/                  ← Spring Cloud Gateway (port 9090)
├── service-registry/             ← Eureka Server (port 8761)
├── docker-compose.yml            ← Kafka, Zookeeper, MySQL, Kafka UI
└── pom.xml                       ← Parent POM
```

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

### Step 1: Start Infrastructure (Kafka + MySQL)

```bash
docker-compose up -d
```

This starts:
| Service | Port | Purpose |
|---------|------|---------|
| Zookeeper | 2181 | Kafka coordination |
| Kafka | 9092 | Message broker |
| Kafka UI | 8089 | Web dashboard for Kafka |
| MySQL (E-commerce) | 3306 | Database for flipkart |
| MySQL (Payment) | 3307 | Database for payment_gateway |

### Step 2: Build All Modules

```bash
mvn clean install -DskipTests
```

### Step 3: Start Services (in order)

```bash
# 1. Service Registry (Eureka)
cd service-registry && mvn spring-boot:run

# 2. E-Commerce Service
cd ecommerce-service && mvn spring-boot:run

# 3. Payment Service
cd payment-service && mvn spring-boot:run

# 4. API Gateway
cd api-gateway && mvn spring-boot:run
```

### Step 4: Access

| Service | URL |
|---------|-----|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:9090 |
| E-Commerce (direct) | http://localhost:8080 |
| Payment (direct) | http://localhost:8081 |
| Kafka UI | http://localhost:8089 |

## 📡 API Endpoints (via Gateway :9090)

### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Add a product |
| DELETE | `/api/products/{id}` | Delete a product |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/register` | Register a new user |
| POST | `/api/users/login` | Login |
| GET | `/api/users` | List all users |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders/place/{userId}` | Place order (triggers Kafka flow) |
| GET | `/api/orders/all-orders` | List all orders |
| GET | `/api/orders/user/{userId}` | Orders by user |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payment/create-order` | Create payment manually |
| POST | `/api/payment/update-order` | Update payment status |

## 🔑 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `order-created-topic` | ecommerce-service | payment-service | Notify payment of new order |
| `payment-processed-topic` | payment-service | ecommerce-service | Return payment result |
| `order-status-updated-topic` | ecommerce-service | (any subscriber) | Broadcast status changes |

## ⚙️ Environment Variables (Optional)

```bash
# For Payment Service email notifications
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# For Nvidia AI-powered order confirmations
export NVIDIA_API_KEY=your-nvidia-api-key
```

## 🧪 Test the Kafka Flow

```bash
# 1. Register a user
curl -X POST http://localhost:9090/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Prasanjit","email":"prasanjit@test.com","password":"pass123"}'

# 2. Add a product
curl -X POST http://localhost:9090/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone 15","description":"Latest iPhone","price":79999,"imageUrl":"img.jpg","category":"Electronics"}'

# 3. Place an order (triggers Kafka → payment → status update)
curl -X POST http://localhost:9090/api/orders/place/1 \
  -H "Content-Type: application/json" \
  -d '{"productQuantities":{"1":2},"totalAmount":159998}'

# 4. Check order status (should be "PAID" after Kafka processing)
curl http://localhost:9090/api/orders/all-orders
```

## 🏗️ Original Projects

- [E-Commerce (Flipkart)](./E-commerce/) — Original standalone e-commerce app
- [Payment Gateway](./Payment-Gateway-Project/) — Original standalone payment gateway
