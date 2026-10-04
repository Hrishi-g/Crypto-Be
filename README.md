# 🚀 CryptX Backend Service

A high-performance, resilient, enterprise-grade cryptocurrency trading & portfolio management backend built with **Spring Boot 3 (Java 21)**, **Spring Security (JWT & OAuth2)**, **Resilience4j**, **PostgreSQL**, **Razorpay Gateway**, and **Binance / CoinGecko APIs**.

Designed for seamless integration with web and mobile trading platforms, CryptX provides real-time market data streaming via WebSockets, idempotent financial transactions, secure wallet operations, and automated portfolio PnL tracking.

---

## ✨ Features

### 🔑 Authentication & Security
- **Dual Token Auth System**: Access & Refresh JWT tokens stored securely in `HttpOnly` cookies.
- **Google OAuth2 Login**: Seamless social authentication with cookie-based authorization state management.
- **CSRF Protection**: Custom `CsrfCookieFilter` ensuring strict web security compliance.
- **Password Management**: Rate-limited email password reset link generation and token verification.

### 💹 Real-Time Market Data & WebSockets
- **Binance & CoinGecko Integration**: Live price tickers, top coins listings, coin search, and historical chart data.
- **Currency Conversion**: Dynamic USD to INR exchange rate retrieval.
- **Binance WebSocket Streaming**: Low-latency real-time ticker updates relayed to client sockets (`/ws-crypto`).

### 💰 Wallet & Financial Operations
- **Fiat & Crypto Wallet Management**: Balance tracking, deposits, and withdrawal processing.
- **Idempotency Safeguards**: Header-based `Idempotency-Key` checking preventing duplicate financial transactions.
- **Paginated Transaction History**: Comprehensive ledger logs with `SUCCESS`, `PENDING`, and `FAILED` status tracking.

### 📈 Trading & Portfolio Management
- **Instant Buy & Sell Execution**: Real-time market order processing with automatic portfolio balance updates.
- **Portfolio Analytics**: Holdings overview, total asset valuation, and live profit/loss (PnL) metrics.
- **Concurrency Control**: Service bulkheads ensuring transaction consistency under heavy load.

### 💳 Razorpay Payment Gateway
- **Order Creation & Verification**: Native Razorpay order initialization and HMAC signature verification.
- **Automated Ledger Approval**: Secure wallet crediting upon verified payment completion and automated status rollbacks on cancellation/failure.

### 🛡️ Resilience & Performance
- **Resilience4j Governance**: Comprehensive Circuit Breakers, Time Limiters, Retries, Rate Limiters, and Bulkheads guarding external Binance, CoinGecko, and Razorpay calls.
- **Reactive & High-Throughput**: Non-blocking `WebClient` integration via Spring WebFlux.
- **Multi-Stage Docker & APM**: Production-ready Alpine container featuring New Relic Java Agent monitoring and non-root execution.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language & Runtime** | Java 21 (Eclipse Temurin) |
| **Framework** | Spring Boot 3.4.0 (Web, WebFlux, Data JPA, Security, WebSockets, Actuator) |
| **Database** | PostgreSQL |
| **Security** | Spring Security, JJWT (0.11.5), OAuth2 Client, Custom CSRF Cookie Filter |
| **Fault Tolerance** | Resilience4j (CircuitBreaker, TimeLimiter, Retry, RateLimiter, Bulkhead) |
| **Integrations** | Razorpay SDK (1.4.8), Binance API/WS, CoinGecko API, Java Mail |
| **Performance & Tools** | Lombok, Jackson, Spring Dotenv, New Relic APM |
| **Containerization** | Docker (Multi-stage Maven + JRE Alpine) |

---

## 📂 Project Structure

```
cryptx/
├── Dockerfile                      # Multi-stage Docker build with New Relic APM
├── pom.xml                         # Maven dependencies & build configuration
├── .env                            # Environment variables config file
└── src/
    ├── main/
    │   ├── java/com/project/cryptx/
    │   │   ├── config/             # Security, WebSocket, and Application configurations
    │   │   ├── controller/         # REST Controllers (Auth, Wallet, Trade, Portfolio, Payment, Market)
    │   │   ├── dto/                # Request & Response Data Transfer Objects
    │   │   ├── exception/          # Global Exception Handler & Custom Domain Exceptions
    │   │   ├── repo/               # Spring Data JPA Repositories
    │   │   ├── security/           # JWT Utils, Cookie Filters, UserDetails Service
    │   │   ├── service/            # Core Business Logic (Trade, Wallet, Auth, Razorpay, Market Data)
    │   │   ├── vo/                 # JPA Entities & Enums (Users, Wallet, Portfolio, Transaction)
    │   │   └── websocket/          # Binance WebSocket Handlers
    │   └── resources/
    │       ├── application.yaml    # Application & Resilience4j configuration
    │       └── logback-spring.xml  # Logging configuration
    └── test/                       # Unit & Integration Tests
```

---

## ⚙️ Environment Configuration

Create a `.env` file in the project root directory (`.env`) with the following environment variables:

```env
# Server & Database Configuration
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/cryptx_db
DB_USER=postgres
DB_PASSWORD=your_postgres_password
DDL_STATUS=update

# JWT Security
JWT_SECRET=your_super_secret_jwt_key_at_least_256_bits_long

# Google OAuth2 Credentials
OUTH_GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
OUTH_GOOGLE_CLIENT_SECRET=your_google_client_secret

# Razorpay Payment Gateway
RAYZORPAY_API_KEY=your_razorpay_key_id
RAYZORPAY_KEY_SECRET=your_razorpay_key_secret

# Email Configuration (SMTP)
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
EMAIL_FROM_USERNAME=your_email@gmail.com

# External API Endpoints
BINANCE_BASE_URL=https://api.binance.com
BINANCE_WEBSOCKET_URL=wss://stream.binance.com:9443/ws
COINGECKO_BASE_URL=https://api.coingecko.com/api/v3
EXCHANGE_RATE_BASE_URL=https://api.exchangerate-api.com/v4/latest
CRYPTO_IMAGE_BASE_URL=https://assets.coingecko.com/coins/images
CRYPTO_IMAGE_TOKEN=

# Frontend & CORS
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGIN=http://localhost:5173
COOKIE_SECURE=false
```

---

## 🚦 Getting Started

### Prerequisites

- **Java 21 JDK** installed
- **Maven 3.9+** (or use included `./mvnw` wrapper)
- **PostgreSQL Database** running on port `5432`

---

### 1. Database Setup

Create a PostgreSQL database matching your `.env` configuration:

```sql
CREATE DATABASE cryptx_db;
```

---

### 2. Build & Run Locally

Run the application using the Maven wrapper:

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

---

### 3. Run with Docker

Build and run the Docker image with multi-stage compilation and New Relic APM agent:

```bash
docker build -t cryptx-backend .
docker run -d -p 8080:8080 --env-file .env --name cryptx-service cryptx-backend
```

---

## 📡 API & WebSocket Specification

### 🔐 Authentication Endpoints (`/auth`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/signup` | Register a new user account | ❌ No |
| `POST` | `/auth/login` | Authenticate & set JWT HttpOnly cookies | ❌ No |
| `GET` | `/auth/check` | Verify current session authentication | 🔐 Yes (Cookie/JWT) |
| `POST` | `/auth/refresh` | Issue new access token using refresh token | 🔐 Yes |
| `POST` | `/auth/send-reset-password-link` | Send password reset email link | ❌ No |
| `POST` | `/auth/reset-password` | Reset password using email verification token | ❌ No |

---

### 📊 Market Data Endpoints (`/home/crypto`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/home/crypto/top-crypto` | Fetch top Binance crypto tickers | ❌ No |
| `GET` | `/home/crypto/all-crypto` | Paginated search & list of cryptocurrencies | ❌ No |
| `GET` | `/home/crypto/exchange-rate` | Fetch current USD to INR exchange rate | ❌ No |

---

### 💼 Wallet Endpoints (`/wallet`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/wallet/update` | Deposit or withdraw funds to/from wallet | 🔐 Yes |
| `GET` | `/wallet/history` | Paginated wallet transaction history | 🔐 Yes |

---

### 💳 Payment Endpoints (`/payment`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/payment/razorpay/create-order` | Initialize a Razorpay payment order | 🔐 Yes |
| `POST` | `/payment/razorpay/verify` | Verify payment signature and credit wallet | 🔐 Yes |
| `POST` | `/payment/razorpay/cancel` | Cancel order and record failure state | 🔐 Yes |

---

### 📈 Trading & Portfolio Endpoints (`/trade` & `/portfolio`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/trade/buy-sell` | Execute buy/sell crypto order (`Idempotency-Key` required) | 🔐 Yes |
| `GET` | `/portfolio/get` | Retrieve user crypto holdings & total valuation | 🔐 Yes |

---

### 👤 User Endpoints (`/user`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/user/profile` | Fetch authenticated user profile details | 🔐 Yes |
| `PUT` | `/user/update` | Update user profile information | 🔐 Yes |

---

### ⚡ WebSocket Endpoints

- **WebSocket URL**: `ws://localhost:8080/ws-crypto`
- **Description**: Streams live Binance ticker updates directly to client WebSockets.

---

## 🧪 Testing

To execute unit and integration test suites:

```bash
./mvnw test
```

---

## 📜 License

This project is open-source and available under the [MIT License](LICENSE).
