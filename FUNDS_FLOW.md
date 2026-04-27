# NylePay Funds Flow & Architecture

This document explains the technical and financial architecture of NylePay, detailing how money moves between users, merchants, and global gateways.

---

## 1. The Core: Wallet-Centric Model

NylePay operates on a **virtual ledger system**. 
- Every user and merchant has a multi-currency wallet (KSH, USD, EUR, GBP, BTC, ETH).
- All external movements (deposits/withdrawals) start or end at the wallet.
- All internal movements are instant ledger entries with no external gateway lag.

---

## 2. Inbound Flows (Deposits / Top-ups)

How money enters the NylePay ecosystem:

### A. M-Pesa (C2B / STK Push)
- **Gateway**: Safaricom M-Pesa Daraja API.
- **Process**: User triggers an STK Push on their phone.
- **Settlement**: M-Pesa sends a `HTTP POST` callback to our webhook. Upon successful validation, we credit the user's **KSH Wallet** instantly.

### B. Card Payments (Visa / Mastercard)
- **Gateways**: **Stripe** (International) & **Paystack** (Africa).
- **Process**: User enters card details via secure hosted fields.
- **Settlement**: Stripe/Paystack processes the charge and notifies NylePay via webhook. We credit the user's **USD or KSH Wallet**.

### C. Bank Transfers
- **Gateway**: **Flutterwave** / **Paystack**.
- **Process**: User is given a unique virtual account or initiates a transfer.
- **Settlement**: The gateway notifies us when funds are received.

### D. PayPal Deposits
- **Gateway**: **Flutterwave (PayPal Bridge)**.
- **Process**: User selects PayPal, is redirected to the PayPal login.
- **Settlement**: Handled via the aggregator to ensure compliance with PayPal's high-risk policies. Credit is applied to the **USD Wallet**.

---

## 3. Internal Flows (Intra-Ecosystem)

How money moves within NylePay:

### A. Peer-to-Peer (P2P) Transfers
- **Process**: User A sends KSH to User B using their `NPY-XXXXXX` account number.
- **Mechanism**: Atomic database transaction (`@Transactional`).
- **Result**: User A's wallet is debited; User B's wallet is credited. Total ecosystem balance remains unchanged.

### B. Merchant Checkout
- **Process**: Customer pays a Merchant via a Checkout Session.
- **Mechanism**: Customer's wallet is debited -> NylePay takes a small fee -> Merchant's wallet is credited.
- **Settlement**: Merchant balance is available for withdrawal immediately (Instant Settlement).

---

## 4. The Crypto Bridge (CEX ⇄ NylePay)

This is the "Cutting Edge" layer where crypto meets local fiat.

### A. CEX to NylePay (Deposit & Swap)
- **Model**: **Liquidity Bridge (Auto-Conversion)**.
- **Process**: User sends USDT/BTC from a CEX (Binance, Bybit) to their NylePay address.
- **Conversion**: NylePay detects the deposit -> Calls Exchange API -> Swaps Crypto for KES/USD -> Credits the Fiat Wallet.
- **User Experience**: User sends "Crypto" and receives "KSH" in their wallet within 5-10 minutes.

### B. NylePay to CEX (Exit & Payout)
- **Model**: **Direct Withdrawal**.
- **Process**: User enters their CEX Wallet Address and KSH amount.
- **Conversion**: NylePay debits KSH -> Buys Crypto via Liquidity Provider -> Sends Crypto to the user's CEX address.

---

## 5. Outbound Flows (Withdrawals / Payouts)

How money leaves the NylePay ecosystem:

### A. Send Money (B2C)
- **Gateway**: Safaricom B2C API.
- **Flow**: User's KSH Wallet is debited -> NylePay calls Safaricom B2C -> Funds arrive on the recipient's phone.

### B. Local Payments (Till / Paybill / Pochi)
- **Gateway**: Safaricom B2B API.
- **Flow**: User's KSH Wallet is debited -> NylePay calls Safaricom B2B (Shortcode to Shortcode) -> The Merchant (e.g., a supermarket Till) receives the payment.

### C. Bank Payouts
- **Gateway**: **Flutterwave** (Global/Regional) & **Paystack**.
- **Flow**: User initiates withdrawal -> Wallet debited -> NylePay calls Gateway Payout API -> Funds cleared to the user's commercial bank.

---

## 5. Gateway Connectivity Matrix

| Method | Provider | primary Currency | Target |
|:---|:---|:---|:---|
| **M-Pesa** | Safaricom | KES | Mobile Wallet |
| **Card** | Stripe | USD / EUR / GBP | International Banks |
| **Card / Bank** | Paystack | KES / NGN / GHS | African Banks |
| **Bank / Payout** | Flutterwave | 30+ Currencies | Global Banks |
| **Crypto** | Binance / Bybit | BTC / ETH / USDT | CEX Wallets |
| **PayPal** | PayPal Payouts API| USD / EUR / GBP | Global Users |

---

## 7. ACID Compliance & Safety

NylePay uses a **"Verify-Debit-Dispatch"** pattern to ensure funds are never lost:

1.  **Verify**: Check balances, KYC limits, and Anti-Fraud velocity.
2.  **Debit**: Deduct the amount from the User's Wallet in a `FOR UPDATE` pessimistic lock block.
3.  **Dispatch**: Call the external Gateway API (Safaricom/Stripe/etc).
4.  **Finalize**: 
    *   If Gateway succeeds: Mark transaction `COMPLETED`.
    *   If Gateway fails: **Roll back** the wallet deduction instantly (ACID rollback).

---

## 7. Security Interceptors

Every fund movement passes through three interceptors:
1.  **RateLimitFilter**: Prevents API abuse and brute-force attempts.
2.  **AntiFraudService**: Analyzes velocity (e.g., is this the 10th transfer in 5 minutes?).
3.  **AuditLogService**: Records every single "Who, What, Where, When" in an immutable ledger for compliance.

---
*NylePay: Secure, Instant, Global. 🇰🇪*
