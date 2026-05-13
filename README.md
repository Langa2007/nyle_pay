# NylePay

NylePay is a Kenya-first financial routing engine.

It lets a user or merchant say:

> Here is where the money is. Here is where it should go. Route it legally, quote it clearly, execute it safely, and reconcile it.

The first market is Kenya, with M-Pesa, Kenyan banks, cards, NylePay wallet accounts, and crypto rails. The long-term model is Africa-wide routing, where each country gets its own local mobile-money and bank adapters while the core quote, execution, compliance, and ledger logic stays the same.

## What Makes NylePay Different

NylePay is not only a payment gateway and not only a crypto off-ramp. It is a universal money-routing layer:

- Accept from M-Pesa, bank, card, NylePay wallet, on-chain crypto, or linked CEX liquidity.
- Convert between fiat and crypto where supported.
- Settle to M-Pesa, bank, Till, Paybill, Pochi la Biashara, NylePay account, merchant balance, or on-chain wallet.
- Quote the route before execution: fees, FX, network estimate, legs, and expected speed.
- Execute through provider adapters and finalize by callback, not by optimistic assumptions.
- Keep NylePay account numbers in the existing 11-character format: `NPYXXXXXXXX`.

## Core Product

### 1. Universal Routing API

The new routing API is the center of NylePay:

- `POST /api/routes/quote`
- `POST /api/routes/execute`
- `GET /api/routes/capabilities`

Routes are expressed by source and destination rails:

```json
{
  "sourceRail": "NYLEPAY_WALLET",
  "destinationRail": "MPESA",
  "sourceAsset": "KSH",
  "destinationAsset": "KSH",
  "amount": 2500,
  "country": "KE",
  "purpose": "Supplier payout",
  "destination": {
    "phone": "254712345678"
  }
}
```

The authenticated user owns the route. Execution does not trust a `userId` in the request body.

### 2. Merchant Gateway

Merchants can accept payments from multiple rails and settle to their preferred destination:

- M-Pesa checkout
- Card checkout
- NylePay wallet checkout
- Crypto intake routes
- Settlement to M-Pesa or bank
- Signed merchant webhooks
- Merchant API keys for headless API access

### 3. Wallet And Ledger

NylePay keeps a multi-currency wallet per user:

- KSH, USD, stablecoins, ETH, BTC, and other configured assets
- Pessimistic database locks for balance mutation
- Idempotent callback handling by provider references
- Transaction codes for user-facing receipts

### 4. Crypto Bridge

Crypto remains part of the product, but as a routing rail rather than the whole brand:

- On-chain custody addresses for supported EVM chains
- On-chain deposit webhooks
- On-chain withdrawals
- CEX account linking for advanced users
- Future institutional liquidity adapter for production crypto-to-fiat routing

Recommended production posture: use institutional liquidity/custody partners for high-volume conversion and keep user CEX API linking as an optional advanced feature.

## Kenya-First Rails

| Rail | Source | Destination | Notes |
|---|---:|---:|---|
| NylePay Wallet | Yes | Yes | Core instant ledger rail |
| M-Pesa | Yes | Yes | STK Push, B2C, B2B |
| Bank | Yes | Yes | Flutterwave/local bank adapter |
| Card | Yes | Planned destination refunds | Paystack/Stripe provider adapters |
| Till | No | Yes | Safaricom B2B Buy Goods |
| Paybill | No | Yes | Safaricom B2B Paybill |
| Pochi | No | Yes | Safaricom Pochi via shortcode/account reference |
| On-chain | Yes | Yes | EVM custody and webhooks |
| CEX | Yes | Yes, adapter-dependent | Binance/Bybit provider layer |
| Merchant | Yes | Yes | Checkout sessions and settlement |

## Route Examples

### Wallet To M-Pesa

```json
{
  "sourceRail": "NYLEPAY_WALLET",
  "destinationRail": "MPESA",
  "sourceAsset": "KSH",
  "amount": 1000,
  "destination": {
    "phone": "254712345678"
  }
}
```

### Wallet To Paybill

```json
{
  "sourceRail": "NYLEPAY_WALLET",
  "destinationRail": "PAYBILL",
  "sourceAsset": "KSH",
  "amount": 3500,
  "purpose": "Rent",
  "destination": {
    "paybillNumber": "123456",
    "accountNumber": "HOUSE-A12"
  }
}
```

### On-chain Crypto To Local Settlement

```json
{
  "sourceRail": "ONCHAIN",
  "destinationRail": "MPESA",
  "sourceAsset": "USDT",
  "destinationAsset": "KSH",
  "amount": 50,
  "destination": {
    "phone": "254712345678",
    "chain": "POLYGON"
  }
}
```

Execution returns a NylePay custody address and route instructions. The route continues after on-chain confirmations and provider liquidity settlement.

### Wallet To NylePay Account

```json
{
  "sourceRail": "NYLEPAY_WALLET",
  "destinationRail": "NYLEPAY_WALLET",
  "sourceAsset": "KSH",
  "amount": 500,
  "destination": {
    "accountNumber": "NPYABCD2345"
  }
}
```

## Routing API

### Capabilities

```http
GET /api/routes/capabilities
Authorization: Bearer <jwt>
```

Returns Kenya-first rails, supported assets, and the account number format.

### Quote A Route

```http
POST /api/routes/quote
Authorization: Bearer <jwt>
Content-Type: application/json
```

Returns:

- `route`
- `sourceRail`
- `destinationRail`
- `amountIn`
- `fxRate`
- `grossAmountOut`
- `nylePayFee`
- `networkFeeEstimate`
- `netAmountOut`
- `estimatedSpeed`
- `settlementMode`
- `legs`

### Execute A Route

```http
POST /api/routes/execute
Authorization: Bearer <jwt>
Content-Type: application/json
```

Possible statuses:

- `INTAKE_REQUIRED`: user/provider must complete the first funding leg.
- `INTAKE_INITIATED`: STK/card/provider collection has started.
- `PROCESSING`: wallet was debited and external dispatch has started.
- `PENDING_APPROVAL`: a reserved route awaits an explicit confirmation step.
- `COMPLETED`: internal NylePay route completed.
- `ROUTE_NOT_AUTOMATED`: quote exists, but execution adapter is not production-wired yet.

## Existing APIs

The original APIs remain available and are now considered rail-specific APIs:

- `/api/payments/**`: deposits, withdrawals, transfer history, M-Pesa/bank webhooks
- `/api/payments/local/**`: Till, Paybill, Pochi, Send Money
- `/api/merchant/**`: merchant onboarding, payment links, refunds
- `/api/v1/merchant/**`: merchant headless API via `Bearer npy_sec_...`
- `/api/card/**`: Paystack/Stripe card flows
- `/api/crypto/**`: wallet creation and on-chain callbacks
- `/api/kyc/**`: KYC status and submission
- `/api/admin/**`: admin/compliance operations

## Architecture

```text
Client / Merchant / Developer
        |
        v
RouteController
        |
        +--> RouteQuoteService
        |       - rail validation
        |       - FX estimate
        |       - fee estimate
        |       - execution legs
        |
        +--> RouteExecutionService
                - authenticated user ownership
                - wallet-funded execution
                - inbound intake instructions
                - provider dispatch
                - existing transaction services

Provider Adapters
        |
        +--> M-Pesa
        +--> Bank / Flutterwave
        +--> Card / Paystack / Stripe
        +--> On-chain EVM
        +--> CEX / Liquidity

Core Data
        |
        +--> Users with NPYXXXXXXXX account numbers
        +--> Wallet balances
        +--> Transactions
        +--> Merchants
        +--> Checkout sessions
        +--> Audit logs
```

## Security And Money Movement Rules

- Route execution resolves the user from the JWT.
- Wallet mutations use pessimistic row locks.
- External payouts should remain `PROCESSING` until callbacks confirm success or failure.
- Webhooks must be verified before they mutate balances.
- Provider references are used for idempotency.
- Crypto private keys and merchant secrets are AES-256-GCM encrypted at rest.
- Merchants receive signed webhooks.
- Admin legal-hold controls can freeze or block outgoing transactions.

## Sandbox Developer Mode

For local development:

```properties
NYLEPAY_SANDBOX_ENABLED=true
cex.live-mode=false
flutterwave.live-mode=false
paystack.live-mode=false
stripe.live-mode=false
kyc.smile.live-mode=false
aml.live-mode=false
mpesa.environment=sandbox
```

Sandbox mode is intended for developers to test the routing contract without moving real money. Some rails still call provider sandbox endpoints, so configure Daraja sandbox keys when testing M-Pesa flows.

## Required Stack

- Java 21+
- Spring Boot 4
- PostgreSQL
- Redis
- Maven wrapper
- Provider sandbox accounts for M-Pesa, card, bank, KYC, and crypto webhooks as needed

## Environment Variables

See [API_KEYS.md](API_KEYS.md) for the complete sandbox and production key checklist.

Minimum local variables:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nylepay
SPRING_DATASOURCE_USERNAME=nylepay
SPRING_DATASOURCE_PASSWORD=nylepay
JWT_SECRET=<32+ chars>
SECURITY_ENCRYPTION_KEY=<32-byte/base64-compatible secret>
ADMIN_EMAIL=admin@nylepay.local
ADMIN_PASSWORD=<strong password>
```

## Build

```bash
./mvnw -DskipTests package
```

Run:

```bash
./mvnw spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## Roadmap

- [x] Kenya-first route quote API
- [x] Kenya-first route execution facade
- [x] Preserve 11-character `NPYXXXXXXXX` account numbers
- [ ] Immutable double-entry ledger table
- [ ] Provider-independent route state machine
- [ ] Callback-confirmed settlement for every external payout
- [ ] Institutional liquidity adapter for crypto-to-fiat routes
- [ ] Merchant routing policies and fallback rails
- [ ] Reconciliation dashboard
- [ ] Country adapter framework for Africa-wide mobile money rails

## License

MIT.
