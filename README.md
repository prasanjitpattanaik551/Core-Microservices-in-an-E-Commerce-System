# E-Commerce Microservices Platform

An event-driven e-commerce backend built with **Spring Boot microservices**, **Apache Kafka**, **Spring Cloud Gateway** and **Eureka**, secured with **JWT**, with real payments through **Razorpay**.

Placing an order never calls the payment service directly. The two services only talk through Kafka events, so each one can fail, restart or scale on its own.

---

## Features

- **4 Spring Boot services** (E-commerce, Payment, API Gateway, Service Registry) plus a shared `kafka-common` module for events and JWT logic
- **Event-driven order and payment flow** over Kafka (3 topics, 3 partitions each, messages keyed by order id)
- **Razorpay payments**: server-side order creation and HMAC-SHA256 signature verification
- **JWT authentication** checked at the gateway and again inside each service, with BCrypt passwords and `USER` / `ADMIN` roles
- **Authorization rules**: users can only read and pay for their own orders; admin-only endpoints for product management, all orders and the user list
- **Server-side price calculation**: the client's `totalAmount` is ignored, so prices cannot be tampered with
- **Idempotent payment processing**: a redelivered Kafka message does not create a second payment, and a successful payment is never processed twice
- **Global exception handling and validation**: one consistent JSON error format for every failure
- **Database per service** (MySQL), with Docker Compose for the whole stack

---

## Architecture

```
                        Client (Postman / Frontend)
                                   |
                                   v
                    +-----------------------------+
                    |         API Gateway         |   :9090
                    |  routing + JWT validation   |
                    +--------------+--------------+
                                   |   (lb:// via Eureka)
              +--------------------+--------------------+
              v                                         v
   +----------------------+                  +----------------------+
   |   E-commerce Service |                  |    Payment Service   |
   |        :8080         |                  |         :8081        |
   |  users, products,    |                  |  Razorpay orders,    |
   |  orders, JWT login   |                  |  signature verify    |
   +----------+-----------+                  +----------+-----------+
              |                                         |
              |  order-created-topic                    |
              +---------------> Kafka <-----------------+
              <-----------------      payment-processed-topic
              |
              +--> order-status-updated-topic (published for other consumers)
              |                                         |
      +-------v-------+                         +-------v-------+
      |  MySQL        |                         |  MySQL        |
      |  "flipkart"   |                         |"payment_gateway"|
      +---------------+                         +---------------+

              Service Registry (Eureka) :8761  - all services register here
```

### Order and payment flow

1. The user logs in and calls `POST /api/orders/place/{userId}`. The service calculates the total from the database prices, saves the order as `PENDING` and publishes `OrderCreatedEvent`.
2. The Payment Service consumes the event and creates a **Razorpay order** (payment status `CREATED`). If this fails, it publishes a `FAILED` payment event.
3. The frontend calls `GET /api/payment/order/{orderId}`, gets the Razorpay order details and opens **Razorpay Checkout**.
4. After the customer pays, the frontend calls `POST /api/payment/verify`. The service verifies the Razorpay signature, marks the payment `SUCCESS` or `FAILED`, sends a confirmation email and publishes `PaymentProcessedEvent`.
5. The E-commerce Service consumes that event, sets the order to `PAID` or `PAYMENT_FAILED` and publishes `OrderStatusUpdatedEvent`.

| Entity | Statuses |
|---|---|
| Order | `PENDING` -> `PAID` or `PAYMENT_FAILED` |
| Payment | `CREATED` -> `SUCCESS` or `FAILED` |

---

## Tech stack

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 3.5, Maven multi-module |
| Microservices | Spring Cloud Gateway, Netflix Eureka |
| Messaging | Apache Kafka + Zookeeper (Confluent 7.5), Kafka UI |
| Security | Spring Security, JWT (jjwt 0.12.6, HS256), BCrypt |
| Persistence | Spring Data JPA / Hibernate, MySQL 8 |
| Payments | Razorpay Java SDK |
| Validation | Jakarta Bean Validation (`@Valid`) |
| Email (optional) | JavaMail (SMTP) |
| DevOps | Docker, Docker Compose (multi-stage Dockerfiles) |

---

## Project structure

```
.
├── kafka-common/          shared module: Kafka topic names, event classes, JwtUtil
├── service-registry/      Eureka server                          :8761
├── api-gateway/           routing + JWT check + CORS             :9090
├── ecommerce-service/     users, products, orders, login         :8080
│   └── .../ecommerce/{config,controller,service,model,dto,repo,kafka,security,exception}
├── payment-service/       Razorpay + payment records             :8081
│   └── .../payment/{config,controller,service,entity,dto,repo,kafka,security,exception}
├── docker-compose.yml     Kafka, MySQL x2 (+ all services with --profile apps)
└── .env.example           environment variables template
```

---

## Getting started

### Prerequisites

- Docker and Docker Compose
- Java 21 and Maven (only if you run the services outside Docker)
- A Razorpay account (free) for **test** API keys

### 1. Configure environment variables

```bash
cp .env.example .env
```

Open `.env` and set at least `JWT_SECRET` (32+ characters) and your Razorpay **test** keys. Never commit `.env`.

Generate a JWT secret:

```bash
# Linux / macOS / Git Bash
openssl rand -base64 48

# Windows PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | yes | Signs and verifies tokens. **Must be identical** for the gateway, ecommerce-service and payment-service |
| `MYSQL_ROOT_PASSWORD` | yes | Password for both MySQL containers (local dev default: `root`) |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | for payments | Razorpay test keys |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | no | SMTP login for payment emails. If empty, emails are skipped |
| `KAFKA_UI_USER`, `KAFKA_UI_PASSWORD` | no | Login for Kafka UI |
| `CORS_ALLOWED_ORIGINS` | no | Your frontend origin(s), comma separated |

### 2. Run it

**Option A: everything in Docker**

```bash
docker compose --profile apps up -d --build
```

**Option B: infrastructure in Docker, services from your IDE**

```bash
docker compose up -d                 # Kafka, Zookeeper, Kafka UI, 2x MySQL
mvn clean install -DskipTests        # builds kafka-common first
```

Then start the services in this order, with `JWT_SECRET` (and the `RAZORPAY_*` variables for payment-service) set as environment variables:

1. `service-registry`
2. `ecommerce-service`
3. `payment-service`
4. `api-gateway`

### Useful URLs

| What | URL |
|---|---|
| API (everything goes through here) | http://localhost:9090 |
| Eureka dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:8089 |

### 3. Create an admin user

Roles can only be changed in the database. Register a normal user first, then:

```bash
docker exec -it mysql-ecommerce mysql -uroot -p flipkart
```

```sql
UPDATE user SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Log in again afterwards, because the role is stored inside the token.

---

## API

All requests go through the gateway on port **9090**. Send the token as `Authorization: Bearer <token>`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/users/register` | public | Create an account (role is always `USER`) |
| POST | `/api/users/login` | public | Returns `{ token, id, name, email, role }` |
| GET | `/api/users` | ADMIN | List users (passwords are never returned) |
| GET | `/api/products` | public | List products |
| GET | `/api/products/{id}` | public | Get one product (404 if missing) |
| POST | `/api/products` | ADMIN | Add a product |
| DELETE | `/api/products/{id}` | ADMIN | Delete a product |
| POST | `/api/orders/place/{userId}` | owner | Place an order. Body: `{ "productQuantities": { "1": 2, "3": 1 } }` |
| GET | `/api/orders/user/{userId}` | owner or ADMIN | Orders of one user |
| GET | `/api/orders/all-orders` | ADMIN | All orders |
| GET | `/api/payment/order/{orderId}` | owner or ADMIN | Razorpay details for Checkout (404 until the Kafka event has been processed, so retry) |
| POST | `/api/payment/verify` | owner | Verify the Razorpay signature and finish the payment |
| POST | `/api/payment/failed?razorpayOrderId=...` | owner | Mark a payment failed (checkout dismissed) |

`verify` body:

```json
{ "razorpay_order_id": "order_xxx", "razorpay_payment_id": "pay_xxx", "razorpay_signature": "..." }
```

### Quick try

```bash
# 1. register and log in
curl -X POST http://localhost:9090/api/users/register -H "Content-Type: application/json" \
  -d '{"name":"Demo","email":"demo@example.com","password":"Demo@12345"}'

curl -X POST http://localhost:9090/api/users/login -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"Demo@12345"}'      # copy the token

# 2. place an order (use your user id and token)
curl -X POST http://localhost:9090/api/orders/place/1 \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"productQuantities":{"1":2}}'

# 3. get the Razorpay details for checkout
curl http://localhost:9090/api/payment/order/1 -H "Authorization: Bearer <token>"
```

Frontend side (Razorpay Checkout), using the response of step 3:

```js
const rzp = new Razorpay({
  key: data.keyId, amount: data.amount, currency: data.currency, order_id: data.razorpayOrderId,
  handler: (res) => fetch("/api/payment/verify", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: "Bearer " + token },
    body: JSON.stringify(res)   // razorpay_order_id, razorpay_payment_id, razorpay_signature
  })
});
rzp.open();
```

---

## Security

| Concern | How it is handled |
|---|---|
| Authentication | Stateless JWT (HS256, 1 hour expiry). The gateway rejects missing or invalid tokens, and each service validates the token again, so a service called directly is still protected |
| Passwords | BCrypt hashes, never returned in responses; same error for unknown email and wrong password |
| Authorization | Role checks (`USER` / `ADMIN`) plus ownership checks on orders and payments |
| Roles | Cannot be set through the API; registration always creates a `USER` |
| Price tampering | The order total is calculated on the server from database prices |
| Payment forgery | Razorpay signature is verified server-side with the secret key; the secret never leaves the backend |
| Secrets | Read from environment variables; nothing sensitive is committed. The app refuses to start without a valid `JWT_SECRET` |
| Network exposure | Databases, Kafka, Kafka UI and Eureka are bound to `127.0.0.1`; only the gateway is published; CORS is restricted to configured origins |
| Kafka | Consumers only trust the `com.microservice.common.event` package when deserializing |

---

## Error handling and validation

Request bodies are validated with `@Valid` (for example: email format, password length 8-72, non-empty orders, quantity 1-100, positive product price). Every error, including the ones raised by Spring Security and the gateway, uses the same JSON shape:

```json
{
  "timestamp": "2026-11-21T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/users/register",
  "fieldErrors": {
    "email": "Email is not valid",
    "password": "Password must be between 8 and 72 characters"
  }
}
```

`path` is the path as seen by the service (the gateway strips the `/api` prefix). `fieldErrors` only appears for validation errors.

| Status | When |
|---|---|
| 400 | Validation failed, malformed JSON, invalid payment signature |
| 401 | Missing, invalid or expired token; wrong login credentials |
| 403 | Valid token but not allowed (wrong role, or someone else's order or payment) |
| 404 | Product, user, order or payment not found; unknown URL |
| 405 | Wrong HTTP method |
| 409 | Email already registered; database conflict |
| 500 | Unexpected error: a generic message is returned and the details are only logged |

---

## Kafka topics

| Topic | Producer | Consumer | Partitions |
|---|---|---|---|
| `order-created-topic` | ecommerce-service | payment-service | 3 |
| `payment-processed-topic` | payment-service | ecommerce-service | 3 |
| `order-status-updated-topic` | ecommerce-service | none yet (available for notifications, analytics, ...) | 3 |

Topics are declared in code, so broker-side auto-creation is turned off.

---

## Design decisions

- **Database per service.** Services never read each other's tables; they exchange events instead.
- **Asynchronous payments.** Order placement returns immediately; payment completes later through events and the order status follows.
- **Defense in depth.** The gateway checks the JWT, and each service checks it again.
- **Idempotency.** Payment creation is keyed by the e-commerce order id (and is unique in the database), and verification is skipped for payments that already succeeded.
- **Fail closed.** Missing secrets stop the application instead of falling back to an insecure default.

---



## Author

**Prasanjit Pattanaik**: [GitHub](https://github.com/prasanjitpattanaik551)
