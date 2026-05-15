# NylePay

NylePay is a Kenya-first financial routing platform built around one simple promise:

> Tell us where the money is and where it needs to go. NylePay handles the route.

It is designed for a market where money does not live in one place. A Kenyan user may hold value in M-Pesa, Airtel Money, a bank account, a card, a crypto wallet, a stablecoin balance, a merchant till, or an exchange account. A merchant may want to accept payments from all of those places but settle only to M-Pesa, Airtel Money, PesaLink, or a bank account.

NylePay turns that fragmented reality into one routing layer.

## The Gap

Kenya already has strong financial rails, but they are scattered:

- M-Pesa dominates daily payments.
- Airtel Money gives users and businesses another mobile-money path.
- PesaLink connects Kenyan bank accounts through a faster bank-switch route.
- Banks still matter for businesses, salaries, suppliers, and settlement.
- Cards are useful for online and international payments.
- Stablecoins and crypto are increasingly used for cross-border value movement.
- Merchants want simple checkout and predictable settlement, not five integrations.
- Users want to move money without understanding rails, liquidity, provider rules, or settlement delays.

Most products solve only one part:

- A payment gateway helps merchants collect money.
- A crypto off-ramp helps users convert crypto to local currency.
- A wallet helps users hold balances.
- A bank transfer provider moves money between accounts.
- Mobile money integrations handle M-Pesa and Airtel Money.
- PesaLink handles bank-switch settlement where it is the better route.

NylePay's opportunity is to connect these worlds into a single routing experience.

## The Product

NylePay is a universal money router for users, merchants, and developers.

A user or business chooses:

- source: where money is coming from
- destination: where money should arrive
- asset: what value is being moved
- route preference: speed, cost, settlement rail, or reliability

NylePay then quotes, executes, tracks, and reconciles the route.

Example routes:

- M-Pesa to NylePay wallet
- Airtel Money to NylePay wallet
- NylePay wallet to M-Pesa
- NylePay wallet to Airtel Money
- NylePay wallet to PesaLink
- NylePay wallet to Till
- NylePay wallet to Paybill
- NylePay wallet to Pochi la Biashara
- Bank to wallet
- Wallet to bank
- Card to merchant
- Stablecoin to M-Pesa
- Stablecoin to Airtel Money
- Card or M-Pesa collection to PesaLink settlement
- Stablecoin to bank
- Crypto wallet to merchant settlement
- CEX balance to M-Pesa
- Customer payment from any supported rail to merchant bank settlement

The product is not "crypto only" and not "M-Pesa only." Crypto is one rail. M-Pesa is one rail. Airtel Money is one rail. PesaLink is one rail. Banks are one rail. Cards are one rail. NylePay is the routing brain above them.

## Why Kenya First

Kenya is the right first market because it has:

- deep mobile money behavior
- strong merchant use of Till and Paybill
- high digital payment familiarity
- diaspora and cross-border payment demand
- growing stablecoin and crypto utility
- a clear need for business settlement tools
- merchants who want payment simplicity without technical complexity

Winning Kenya first gives NylePay a strong operating template for Africa. The expansion model is not to copy-paste M-Pesa everywhere, but to adapt the destination rail per country:

- Kenya: M-Pesa, Airtel Money, PesaLink, banks, Till, Paybill, Pochi
- Ghana: MTN MoMo, Vodafone Cash, banks
- Uganda: MTN MoMo, Airtel Money, banks
- Tanzania: M-Pesa, Tigo Pesa, Airtel Money, banks
- Nigeria: bank accounts, virtual accounts, cards, mobile wallets
- South Africa: bank EFT, cards, wallets, cash-out partners

The routing engine stays the same. The local rail adapters change.

## Merchant Vision

For merchants, NylePay should feel like:

> One checkout. Many ways to pay. One settlement destination.

A merchant should be able to accept:

- M-Pesa
- Airtel Money
- PesaLink
- card
- NylePay wallet
- bank transfer
- crypto/stablecoins
- future country-specific mobile money rails

Then settle to:

- M-Pesa
- Airtel Money
- PesaLink
- bank account
- NylePay merchant balance
- another supported business rail

The merchant does not need to care how the money arrived. NylePay handles routing, conversion, fees, confirmation, settlement, and webhooks.

This is the gateway angle: NylePay is not merely collecting payments. It is deciding the best path from customer value to merchant settlement.

## User Vision

For users, NylePay should feel like:

> Any balance can become useful money.

A user should be able to:

- receive crypto and cash out to M-Pesa or Airtel Money
- top up from M-Pesa and pay a Paybill
- hold funds in a NylePay wallet
- send to another NylePay account
- move from wallet to bank
- pay merchants without thinking about the merchant's settlement rail
- convert supported assets when needed

The user should not need to know whether a route uses STK, B2C, B2B, bank payout, on-chain confirmation, card settlement, or liquidity conversion. NylePay abstracts that away.

## Developer Vision

For developers, NylePay should be:

> One API for financial routing in Kenya, then Africa.

Instead of integrating M-Pesa, Airtel Money, PesaLink, card providers, bank payout APIs, crypto webhooks, checkout pages, settlement webhooks, fraud rules, and reconciliation separately, developers integrate one routing layer.

The developer asks for a quote, starts a route, listens for status changes, and receives a final event when the money reaches the destination or fails safely.

For NylePay Business, access should stay light until production activation:

- public signup collects full name and email only
- Resend sends the 6-digit verification code
- code verification opens the Business dashboard
- sandbox API keys are available for testing
- the Go Live dashboard section collects business documents, beneficial ownership, settlement details, risk information, and production activation requirements

## Competitive Position

NylePay should not compete by claiming to have every rail first. That is expensive and easy to copy.

NylePay should compete by owning the routing experience:

- clearer quotes before money moves
- faster local settlement where possible
- better merchant settlement workflows
- stronger Kenya-specific payment UX
- support for both fiat and crypto without making crypto the whole identity
- reliable route status tracking
- legal and compliance-aware limits
- support for small merchants, online merchants, and developers
- eventual offline access through USSD/SMS for users without smartphones

The market already has crypto off-ramps, exchanges, payment gateways, and mobile money integrations. The defensible wedge is becoming the layer that coordinates them intelligently.

## Against Crypto Off-Ramps

Crypto off-ramps usually answer one question:

> How do I turn crypto into local money?

NylePay should answer a bigger question:

> How do I move value from anywhere to anywhere useful?

That means crypto-to-M-Pesa is just one route among many. A merchant should be able to receive stablecoin value and settle to bank, PesaLink, M-Pesa, or Airtel Money. A user should be able to move M-Pesa to wallet, Airtel Money to wallet, wallet to Paybill, wallet to PesaLink, crypto to bank, or wallet to another user.

This broader utility makes NylePay less dependent on crypto market cycles.

## Against Traditional Payment Gateways

Traditional gateways focus on collection. NylePay should focus on collection plus routing plus settlement.

A normal gateway may help a merchant accept card or M-Pesa. NylePay should help a merchant accept many value sources and decide whether the resulting value should land in M-Pesa, Airtel Money, PesaLink, bank, wallet, Paybill, Till, or a supported digital asset rail.

That matters because African merchants are practical. They do not only want "payments accepted." They want:

- funds available quickly
- settlement to the account they actually use
- visibility on failed payments
- simple refunds
- low integration complexity
- support for local rails
- a way to accept new forms of value like stablecoins without becoming crypto experts

## Trust Strategy

NylePay should be positioned as a financial routing and settlement company, not as a speculative crypto brand.

The trust message:

- Kenya-first
- compliance-aware
- merchant-friendly
- user-controlled
- transparent fees
- route tracking
- callback-confirmed settlement
- account controls for legal holds and fraud protection
- strong encryption for secrets and private keys

Crypto should be described as supported digital asset routing, not as a casino-like feature.

## Product Principles

1. **Route by intent**
   Users should describe the outcome, not the rails.

2. **Quote before execution**
   Show fees, FX, expected speed, and route legs before money moves.

3. **Confirm by provider callback**
   Initiating a transfer is not the same as completing it.

4. **Keep local rails first-class**
   M-Pesa, Airtel Money, PesaLink, Till, Paybill, Pochi, and Kenyan banks are not side features. They are the foundation.

5. **Make crypto useful**
   Crypto should become spendable, withdrawable, and settleable value.

6. **Serve merchants seriously**
   Merchants need settlement, reconciliation, refunds, payment links, and webhooks.

7. **Design for Africa, launch in Kenya**
   The core engine should support future country adapters without rebuilding the platform.

## Kenya Beachhead

The strongest launch wedge is:

- NylePay wallet
- M-Pesa in and out
- Airtel Money in and out
- PesaLink bank-switch payouts
- Paybill, Till, and Pochi payments
- merchant checkout
- merchant settlement to M-Pesa, Airtel Money, PesaLink, or bank
- crypto/stablecoin to M-Pesa, Airtel Money, PesaLink, or bank for verified users
- developer routing API

This gives NylePay both consumer and merchant value.

Consumers get freedom of movement.

Merchants get broader acceptance and simpler settlement.

Developers get one integration.

## Long-Term Direction

NylePay can grow into:

- a routing API for African fintech developers
- a merchant gateway with multi-rail settlement
- a stablecoin-to-local-money utility layer
- a wallet for users who move across mobile money, bank, and crypto
- a liquidity and settlement orchestration layer for businesses
- a country-by-country mobile money abstraction layer

The ambition is not just to process payments.

The ambition is to become the financial switchboard for practical African money movement.

## One-Line Positioning

**NylePay routes money from wherever it is to wherever it needs to go, starting with Kenya.**

## Short Pitch

NylePay is a Kenya-first money routing platform for users, merchants, and developers. It connects M-Pesa, Airtel Money, PesaLink, banks, cards, wallets, and crypto into one routing layer, allowing value to be collected from many sources and settled to the destination that matters: M-Pesa, Airtel Money, PesaLink, bank, merchant balance, Paybill, Till, Pochi, or supported digital asset rails.

## North Star

One account. Many rails. Any legal destination.
