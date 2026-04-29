<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

# NylePay

**A modular, Kenya-first digital wallet and payment gateway** that unifies M-Pesa, bank transfers, card payments, cryptocurrency, and merchant checkout into a single REST API. Built with production-grade ACID compliance, AES-256-GCM encryption, and CBK regulatory awareness.

---

## Table of Contents

- [What NylePay Does](#what-nylepay-does)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [Roadmap](#roadmap)
- [License](#license)

---

## What NylePay Does

NylePay is a **payment orchestration platform** that lets users:

1. **Move fiat** between M-Pesa ↔ NylePay wallet ↔ Bank accounts
2. **Move crypto** between centralized exchanges (Binance, Bybit) ↔ NylePay wallet ↔ on-chain addresses
3. **Cross-convert** between fiat and crypto rails (CEX → M-Pesa, Crypto → Bank)
4. **Pay locally** in Kenya using Till, Paybill, Pochi la Biashara, and Send Money
5. **Accept card payments** via Visa/Mastercard (Paystack for Africa, Stripe internationally)
6. **Operate as a merchant** with payment links, checkout sessions, and webhook-based settlement
7. **Identity & compliance** — KYC via Smile Identity, AML screening, monthly limit enforcement

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST API (Spring Boot)                    │
├────────┬────────┬─────────┬──────────┬──────────┬───────────────┤
│ Auth   │Payment │  Card   │   CEX    │  Chain   │   Merchant    │
│Contrlr │Contrlr │Contrlr  │ Contrlr  │ Contrlr  │   Contrlr     │
├────────┴────────┴─────────┴──────────┴──────────┴───────────────┤
│                       Service Layer                              │
├──────┬───────┬────────┬───────┬───────┬───────┬────────┬────────┤
│Mpesa │Wallet │Transac │ Card  │  CEX  │ Chain │  KYC   │Merchant│
│Svc   │Svc    │tion Svc│Pay Svc│Routing│Deposit│  Svc   │  Svc   │
├──────┴───────┴────────┴───────┴───────┴───────┴────────┴────────┤
│                   Repository Layer (JPA)                         │
├─────────────────────────────────────────────────────────────────┤
│              PostgreSQL           │           Redis              │
│    (Transactions, Wallets, Users) │  (OTP, Rate Limits, Cache)   │
└───────────────────────────────────┴─────────────────────────────┘
```

### Payment Provider Integrations

| Provider | Purpose | Status |
|----------|---------|--------|
| **Safaricom M-Pesa** | STK Push, B2C, B2B (Till/Paybill/Pochi) | ✅ Implemented |
| **Flutterwave** | Bank transfers, account resolution (Kenya, Nigeria, Ghana) | ✅ Implemented |
| **Paystack** | Card payments — Visa/Mastercard/Verve (Africa) | ✅ Implemented |
| **Stripe** | Card payments — Visa/Mastercard (International) | ✅ Implemented |
| **Binance** | CEX trading, withdrawals, balance aggregation | ✅ Implemented |
| **Bybit** | CEX trading, withdrawals, balance aggregation | ✅ Implemented |
| **Moralis/Alchemy** | On-chain deposit webhooks (EVM chains) | ✅ Implemented |
| **Web3j** | On-chain EVM withdrawals (ETH, USDT, USDC, DAI) | ✅ Implemented |
| **Smile Identity** | KYC verification (National ID, Passport, Biometric) | ✅ Implemented |
| **Resend** | Transactional emails (Welcome, OTP, Transaction alerts) | ✅ Implemented |

---

## Features

### 💰 Wallet & Transfers
- Multi-currency wallet (KSH, USD, EUR, GBP, ETH, BTC, USDT, USDC, DAI)
- ACID-compliant balance operations with `SELECT FOR UPDATE` row locking
- Internal P2P transfers between NylePay users
- Currency conversion with configurable exchange rates and 1% fee

### 📱 M-Pesa Integration
- **STK Push** — Customer-initiated deposits to NylePay wallet
- **B2C** — Wallet withdrawals to M-Pesa phone numbers
- **B2B** — Pay to Till, Paybill, and Pochi la Biashara
- **C2B** — URL registration for receiving payments
- Tiered fee calculation and phone number normalization

### 🏦 Bank Transfers
- Deposit via bank transfer (instructions + settlement account)
- Withdraw to any Kenyan bank via Flutterwave
- Bank account linking with account name resolution
- Bank → M-Pesa routing (multi-leg transaction orchestration)
- Support for 14+ Kenyan banks (KCB, Equity, Co-op, NCBA, etc.)

### 💳 Card Payments
- **Paystack** — Primary acquirer for Africa (Visa, Mastercard, Verve)
- **Stripe** — International payments (USD, EUR)
- PCI DSS SAQ-A compliant (card capture handled by provider SDKs)
- Refund support (full and partial)
- HMAC-SHA512 (Paystack) and Stripe-Signature webhook verification

### 🪙 Cryptocurrency
- **CEX Integration** — Link Binance/Bybit accounts with encrypted API keys
- **Aggregated balances** across all linked exchanges
- **Auto-routing** — CEX → Fiat → M-Pesa in one flow
- **On-chain deposits** — Moralis/Alchemy webhook-driven with configurable confirmations
- **On-chain withdrawals** — EVM transfers via Web3j (Ethereum, Polygon, Arbitrum, Base)
- **Custody wallets** — EVM key pair generation with AES-256-GCM encrypted private keys
- Support for ETH, USDT, USDC, DAI across 4 chains

### 🛒 Merchant Gateway
- Merchant registration with encrypted API keys (`np_pub_*` / `np_sec_*`, shown once)
- Payment link creation with configurable expiry → generates hosted checkout URL
- **Hosted Checkout Page** — premium dark-mode UI served by NylePay at `/checkout/{ref}`
- Customer pays via **M-Pesa**, **Card (Paystack)**, or **NylePay Wallet** on a single page
- Webhook delivery with **HMAC-SHA256 signatures** and 3-retry exponential backoff
- **Automatic Daily Settlement** — pending balances pushed to merchant M-Pesa or bank at 22:00 EAT
- Full and partial **refund** support (ACID-safe)
- Configurable fee percentage (default 1.5%) per merchant

### 🇰🇪 Local Payments
- **Till** — Pay to Buy Goods via Safaricom B2B API
- **Paybill** — Pay to Paybill numbers with account references
- **Pochi la Biashara** — Pay to Pochi wallets via shortcode 440000
- **Send Money** — B2C to M-Pesa phone numbers
- KYC guard + monthly limit enforcement on all local payments

### 🆔 KYC & Compliance
- Smile Identity integration (National ID, Passport, Driver's License, Biometric)
- **NylePay Account Numbers** — `NPYXXXXXXXX` format (11 characters), generated on KYC verification
- CBK-mandated KES 70,000 monthly limit for unverified users
- Real-time monthly transaction sum enforcement
- AML screening with threshold and structuring detection

### 🔐 Security
- JWT authentication (stateless, BCrypt password hashing)
- AES-256-GCM encryption for all secrets at rest (API keys, private keys, merchant secrets)
- HMAC webhook signature verification for all payment providers
- Per-IP rate limiting with Bucket4j (path-specific tiers)
- **2FA / OTP** — Redis-backed, 6-digit, 5-minute TTL, max 5 attempts
- `SELECT FOR UPDATE` pessimistic locking prevents double-spend
- Idempotent webhook processing via unique `externalId` constraints
- CORS whitelist and role-based access control (USER / ADMIN)

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 4.0.2 |
| Language | Java 21 |
| Database | PostgreSQL 15 (Neon Cloud) |
| Cache / OTP | Redis 7 |
| ORM | Hibernate / Spring Data JPA |
| Auth | JWT (jjwt 0.12.5) + Spring Security |
| Crypto | Web3j 4.10 (EVM transactions) |
| Cards | Stripe Java SDK 26.3 |
| Rate Limiting | Bucket4j 8.10 |
| API Docs | SpringDoc OpenAPI 2.8 |
| Email | Resend REST API |
| Build | Maven + Maven Wrapper |
| Containerization | Docker + Docker Compose |
| Checkout UI | Vanilla HTML/CSS/JS (Spring Boot static) |

---

## Merchant Gateway Integration

NylePay can be used as a **payment gateway** for any store or application. Merchants integrate once and accept payments via M-Pesa, card, and crypto without building their own payment infrastructure.

### Step 1 — Register as a Merchant
```bash
POST /api/merchant/register
Authorization: Bearer {your_nylepay_jwt}

{
  "businessName": "Acme Store",
  "businessEmail": "payments@acme.com",
  "webhookUrl": "https://acme.com/nylepay-webhook"
}
```

**Response** (save the `secretKey` — shown only once):
```json
{
  "publicKey":    "np_pub_abc123...",
  "secretKey":    "np_sec_xyz789...",
  "webhookSecret": "hmac-secret-for-verifying-webhooks",
  "status":       "PENDING"
}
```

### Step 2 — Set Your Settlement Account
```bash
POST /api/merchant/settlement-account

{ "type": "MPESA", "phone": "254712345678" }
```

### Step 3 — Create a Payment Link (per order)
```bash
POST /api/merchant/payment-link

{
  "amount": 2500,
  "currency": "KES",
  "description": "Order #1042",
  "redirectUrl": "https://acme.com/thank-you",
  "expiryMinutes": 60
}
```

**Response** — share this URL with your customer:
```json
{
  "paymentUrl": "https://api.yourdomain.com/checkout/NPY-LNK-ABC123XYZ",
  "reference":  "NPY-LNK-ABC123XYZ",
  "expiresAt":  "2026-04-29T23:00:00"
}
```

### Step 4 — Receive Webhook on Payment Completion
NylePay sends a signed POST to your `webhookUrl`:
```json
{
  "event":   "payment.succeeded",
  "eventId": "evt_1714396800000",
  "data": {
    "reference": "NPY-LNK-ABC123XYZ",
    "amount":    2500,
    "currency":  "KES",
    "status":    "COMPLETED"
  }
}
```

Verify the signature before trusting the event:
```javascript
// Node.js example
const crypto = require('crypto');
const sig = req.headers['x-nylepay-signature'];
const expected = crypto.createHmac('sha256', WEBHOOK_SECRET)
                       .update(rawBody).digest('hex');
if (sig !== expected) return res.sendStatus(401); // Reject tampered events
// Mark order as paid ✅
```

### Settlement
NylePay automatically sweeps your pending balance to your registered M-Pesa or bank account **every day at 22:00 EAT**. Your balance after NylePay's 1.5% fee is transferred in full.

---

## Project Structure

```
nylepay/
├── src/main/java/com/nyle/nylepay/
│   ├── NylepayApplication.java           # Entry point
│   │
│   ├── config/
│   │   ├── AdminInitializer.java         # Admin seed (env-var based)
│   │   ├── JwtAuthenticationFilter.java  # JWT request filter
│   │   ├── RateLimitFilter.java          # Bucket4j per-IP rate limiter
│   │   ├── SecurityConfig.java           # Spring Security chain
│   │   ├── SwaggerConfig.java            # OpenAPI config
│   │   └── WebConfig.java               # CORS config
│   │
│   ├── controllers/
│   │   ├── AuthController.java           # Register, Login, OTP, Password Reset
│   │   ├── PaymentController.java        # Deposits, Withdrawals, Transfers, Webhooks
│   │   ├── LocalPaymentController.java   # Till, Paybill, Pochi, Send Money
│   │   ├── CardController.java           # Paystack & Stripe card payments
│   │   ├── CexController.java            # CEX account management
│   │   ├── OnChainController.java        # Crypto deposits & withdrawals
│   │   ├── MerchantController.java       # Merchant registration & payment links
│   │   ├── KycController.java            # KYC submission & status
│   │   ├── UserController.java           # Profile, bank linking, stats
│   │   ├── WalletController.java         # Balance queries
│   │   ├── ExchangeController.java       # Cross-rail routing
│   │   └── AdminController.java          # Admin dashboard APIs
│   │
│   ├── dto/                              # Request/Response DTOs
│   │   ├── ApiResponse.java              # Standardized API envelope
│   │   ├── DepositRequest.java
│   │   ├── WithdrawalRequest.java
│   │   ├── TransferRequest.java
│   │   ├── LocalPaymentRequest.java
│   │   ├── ConversionRequest.java
│   │   ├── BankLinkRequest.java
│   │   ├── RegisterRequest.java
│   │   └── LoginRequest.java
│   │
│   ├── models/                           # JPA Entities
│   │   ├── User.java                     # Users + KYC + OTP + Account Number
│   │   ├── Wallet.java                   # Multi-currency balance ledger
│   │   ├── Transaction.java              # All transaction types
│   │   ├── CryptoWallet.java             # EVM custody wallets per chain
│   │   ├── Merchant.java                 # Merchant accounts
│   │   ├── CheckoutSession.java          # Payment links
│   │   ├── SavedCard.java                # Tokenized card references
│   │   ├── UserBankDetail.java           # Linked bank accounts
│   │   └── UserExchangeKey.java          # Encrypted CEX API keys
│   │
│   ├── repositories/                     # Spring Data JPA repositories
│   │
│   ├── services/
│   │   ├── MpesaService.java             # Safaricom STK/B2C/B2B/C2B APIs
│   │   ├── TransactionService.java       # Transaction orchestration engine
│   │   ├── WalletService.java            # ACID-safe balance operations
│   │   ├── UserService.java              # Registration, profile, password
│   │   ├── OtpService.java               # Redis-backed 2FA
│   │   ├── EmailService.java             # Resend transactional emails
│   │   ├── JwtService.java               # JWT generation & validation
│   │   ├── BankTransferService.java      # Flutterwave bank routing
│   │   ├── card/
│   │   │   ├── CardPaymentService.java   # Paystack + Stripe orchestration
│   │   │   ├── PaystackCardService.java  # Paystack API wrapper
│   │   │   └── StripeCardService.java    # Stripe API wrapper
│   │   ├── cex/
│   │   │   ├── ICexProvider.java         # Plugin interface for exchanges
│   │   │   ├── BinanceProviderImpl.java  # Binance REST integration
│   │   │   ├── BybitProviderImpl.java    # Bybit REST integration
│   │   │   └── CexRoutingService.java    # Auto-routing across exchanges
│   │   ├── chain/
│   │   │   ├── OnChainDepositService.java    # Webhook-driven deposits
│   │   │   └── OnChainWithdrawalService.java # Web3j EVM withdrawals
│   │   ├── kyc/
│   │   │   ├── KycService.java           # Smile Identity + account number gen
│   │   │   └── AmlScreeningService.java  # CBK threshold screening
│   │   ├── merchant/
│   │   │   ├── MerchantService.java      # Merchant accounts & payment links
│   │   │   ├── RefundService.java        # Multi-provider refunds
│   │   │   └── WebhookDeliveryService.java # Outbound merchant webhooks
│   │   └── routing/
│   │       └── ExchangeRoutingService.java # Cross-rail fund movement
│   │
│   ├── utils/
│   │   └── EncryptionUtils.java          # AES-256-GCM encrypt/decrypt
│   │
│   └── exceptions/                       # Custom exception classes
│
├── src/main/resources/
│   └── application.properties            # Config (secrets via env vars)
│
├── docker-compose.yml                    # PostgreSQL + pgAdmin + Redis
├── pom.xml                               # Maven dependencies
├── .env                                  # Local secrets (git-ignored)
├── .gitignore                            # Ignores .env, docker props, etc.
└── README.md                             # This file
```

---

## Getting Started

### Prerequisites

- **Java 21** (or later)
- **PostgreSQL 15** (local or cloud — Neon, Supabase, etc.)
- **Redis 7** (local or cloud)
- **Maven** (or use the included `mvnw` wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/nylepay.git
cd nylepay
```

### 2. Create Your `.env` File

Copy the template and fill in your credentials:

```bash
cp .env.example .env
```

> ⚠️ The `.env` file is git-ignored. Never commit it.

### 3. Start Infrastructure (Docker)

```bash
docker-compose up -d
```

This starts PostgreSQL, pgAdmin, and Redis.

### 4. Build & Run

```bash
# Using Maven Wrapper (no global Maven install needed)
./mvnw spring-boot:run

# Or with Maven
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Access API Documentation

Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Environment Variables

All sensitive values are injected from environment variables. Set these in your `.env` file for local development, or in your hosting dashboard (Render, Railway, etc.) for production.

| Variable | Description | Required |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | Database username | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | ✅ |
| `SECURITY_ENCRYPTION_KEY` | AES-256 key for encrypting secrets at rest | ✅ |
| `ADMIN_EMAIL` | Admin seed account email | ✅ |
| `ADMIN_PASSWORD` | Admin seed account password (min 12 chars) | ✅ |
| `ADMIN_FULL_NAME` | Admin display name | ❌ |
| `MPESA_CONSUMER_KEY` | Safaricom Daraja API consumer key | ✅ |
| `MPESA_CONSUMER_SECRET` | Safaricom Daraja API consumer secret | ✅ |
| `MPESA_PASSKEY` | Safaricom Lipa na M-Pesa passkey | ✅ |
| `FLUTTERWAVE_SECRET_KEY` | Flutterwave API secret key | ✅ |
| `FLUTTERWAVE_PUBLIC_KEY` | Flutterwave API public key | ❌ |
| `FLUTTERWAVE_ENC_KEY` | Flutterwave encryption key | ❌ |
| `FLUTTERWAVE_WEBHOOK_SECRET` | Flutterwave webhook verification hash | ✅ |
| `PAYSTACK_SECRET_KEY` | Paystack API secret key | ✅ |
| `PAYSTACK_PUBLIC_KEY` | Paystack API public key | ❌ |
| `PAYSTACK_WEBHOOK_SECRET` | Paystack webhook secret | ✅ |
| `STRIPE_SECRET_KEY` | Stripe API secret key | ✅ |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | ✅ |
| `SMILE_PARTNER_ID` | Smile Identity partner ID | ❌ |
| `SMILE_API_KEY` | Smile Identity API key | ❌ |
| `COMPLYADVANTAGE_API_KEY` | AML screening API key | ❌ |
| `RESEND_API_KEY` | Resend email API key | ❌ |

---

## API Reference

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login (returns JWT + account number) |
| POST | `/api/auth/refresh-token` | Refresh JWT token |
| POST | `/api/auth/forgot-password` | Request password reset email |
| POST | `/api/auth/reset-password` | Reset password with token |
| POST | `/api/auth/otp/request` | Request 6-digit OTP |
| POST | `/api/auth/otp/verify` | Verify OTP |
| POST | `/api/auth/otp/enable` | Enable 2FA |
| POST | `/api/auth/otp/disable` | Disable 2FA |

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/deposit/mpesa` | M-Pesa STK Push deposit |
| POST | `/api/payments/deposit/bank` | Bank deposit (instructions) |
| POST | `/api/payments/withdraw` | Withdraw to M-Pesa/Bank/Crypto |
| POST | `/api/payments/transfer` | Internal P2P transfer |
| POST | `/api/payments/convert` | Currency conversion |
| GET | `/api/payments/transaction/{id}` | Get transaction details |
| GET | `/api/payments/user/{userId}/transactions` | User transaction history |

### Local Payments (Kenya)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/local/till` | Pay to Till number |
| POST | `/api/payments/local/paybill` | Pay to Paybill |
| POST | `/api/payments/local/pochi` | Pay to Pochi la Biashara |
| POST | `/api/payments/local/send` | Send Money to M-Pesa |

### Card Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/card/pay` | Initialize card payment |
| POST | `/api/card/verify` | Verify card payment |
| POST | `/api/card/refund` | Issue refund |
| GET | `/api/card/user/{userId}/cards` | List saved cards |

### Crypto / CEX

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/cex/connect` | Link CEX account |
| GET | `/api/payments/cex/{userId}/balances` | Aggregated CEX balances |
| POST | `/api/payments/cex/withdraw` | CEX → M-Pesa auto-route |
| POST | `/api/crypto/wallet/create` | Generate EVM custody wallet |
| GET | `/api/crypto/wallet/{userId}` | Get wallet addresses |

### Webhooks (Unauthenticated)

| Method | Endpoint | Provider |
|--------|----------|----------|
| POST | `/api/payments/webhook/mpesa` | Safaricom STK Push |
| POST | `/api/payments/webhook/mpesa/result` | Safaricom B2C result |
| POST | `/api/payments/webhook/mpesa/b2b` | Safaricom B2B result |
| POST | `/api/payments/webhook/bank` | Flutterwave bank |
| POST | `/api/card/webhook/paystack` | Paystack card |
| POST | `/api/card/webhook/stripe` | Stripe card |
| POST | `/api/crypto/webhook/deposit` | Moralis/Alchemy on-chain |
| POST | `/api/kyc/webhook` | Smile Identity KYC |

---

## Security Model

### Encryption

| Data | Algorithm | Key Source |
|------|-----------|------------|
| API keys at rest | AES-256-GCM | `SECURITY_ENCRYPTION_KEY` |
| Passwords | BCrypt (strength 10) | Auto-generated salt |
| JWT tokens | HMAC-SHA256 | `JWT_SECRET` |
| Webhook verification | HMAC-SHA256/512 | Provider-specific secrets |
| Crypto private keys | AES-256-GCM | `SECURITY_ENCRYPTION_KEY` |

### Rate Limiting (per IP)

| Endpoint Category | Limit |
|-------------------|-------|
| `/api/auth/**` | 20 req/min |
| `/api/payments/local/**` | 15 req/min |
| `/api/payments/**` | 30 req/min |
| `/api/kyc/**` | 10 req/min |
| All others | 100 req/min |
| Webhooks | Exempt |

### ACID Transaction Safety

- All balance mutations use `SELECT ... FOR UPDATE` (pessimistic locking)
- Webhook handlers are idempotent via unique `externalId` constraints
- Failed withdrawals are automatically refunded within the same `@Transactional` boundary
- PostgreSQL WAL ensures durability across restarts

---

## Roadmap

- [x] **Phase 1** — Local Payment Rails (Till, Paybill, Pochi, Send Money, ACID Settlement, KYC)
- [x] **Phase 2 — Security & UX Hardening (COMPLETED)**
- **Error Abstraction Layer**: Technical exceptions (e.g., "M-Pesa callback failed") are now caught, logged internally, and hidden from users behind generic, friendly messages.
- **NylePayException**: A new custom exception hierarchy for errors that are safe and helpful to display to the user.
- **Transaction Code System**: Renamed "Claim Codes" to "Transaction Codes" across the entire stack for better clarity (e.g., `MP2NP-XXXXX`).
- **Audit Logs**: All sensitive operations (swaps, withdrawals, logins) are tracked in the audit database.

- [x] **Phase 3 — Crypto Bridge & Global Rails (COMPLETED)**
- **Binance Integration**: Support for automated crypto swaps (Stablecoins -> KES).
- **Golden Flow**: Deposit USDT/ETH/USDC -> Auto-Swap to KES -> Withdraw to M-Pesa.
- **On-Chain Custody**: Dedicated EVM addresses for every user with encrypted key storage.

- [ ] **Phase 4 — Offline & USSD**
  - [ ] Africa's Talking USSD Gateway
  - [ ] SMS Notifications & 2FA
  - [ ] Offline Balance Inquiries

---

## 🔐 Security & Compliance

- **Account & Privacy Policy**: [ACCOUNT_POLICY.md](ACCOUNT_POLICY.md)
- **Funds Flow & Architecture**: [FUNDS_FLOW.md](FUNDS_FLOW.md)

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

<p align="center">
  Built by Langa Fidel in Nairobi 🇰🇪
</p>
