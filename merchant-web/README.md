# NylePay Business

NylePay Business is the business-facing web console for NylePay's routing engine. It is designed for Kenyan businesses first: collect money from the rail a customer wants to use, then route and settle it to the account the business chooses in real time.

The product direction is simple: one business account, many financial rails. A business should be able to accept M-Pesa, Airtel Money, cards, wallets, bank transfers, and crypto, then settle to M-Pesa, Airtel Money, PesaLink, bank, wallet, Paybill, or future country-specific mobile money rails as NylePay expands across Africa.

## Gap It Fills

Businesses in Kenya often deal with fragmented payment operations:

- M-Pesa and Airtel Money collections live in one workflow.
- PesaLink bank-switch settlement lives in another provider workflow.
- Bank settlement and reconciliation live somewhere else.
- Card payments need another provider.
- Crypto liquidity and fiat conversion are disconnected from ordinary business accounts.
- Developers must integrate each provider separately.

NylePay Business turns that into a routing problem. The business tells NylePay where value is coming from, where it should go, and which preference matters most: fastest, lowest cost, M-Pesa first, bank first, wallet reserve, or fallback-enabled.

## Core Promise

- Collect from any supported source rail.
- Quote fees, FX, speed, and destination before execution.
- Execute the route with idempotency.
- Track every leg until completion.
- Settle to the configured business destination in real time where the rail allows.
- Send signed webhooks for route and settlement events.
- Give developers a sandbox that behaves like the real routing engine without moving real money.

## Business Console Scope

The web app is meant to provide:

- Email-confirmed Business access using Resend.
- Sandbox API keys immediately after 6-digit email code verification.
- A Go Live workflow for production activation, business documents, settlement rails, and live API keys.
- API key management with production secret keys shown once after approval.
- Route creation and quote testing.
- Route history with source, destination, fee, FX, provider reference, and status.
- Settlement policy management.
- Webhook configuration and delivery visibility.
- Developer docs and sandbox route simulation.
- Exportable reconciliation views for finance teams.

## Market Direction

NylePay should not compete only as another checkout provider. The stronger position is business money routing:

- For small businesses: make it simple to receive money and settle where they want.
- For developers: one API instead of many payment integrations.
- For marketplaces: collect from many rails and settle many destinations.
- For crypto-aware businesses: bridge crypto value into Kenyan business rails legally and auditable.
- For African expansion: keep one NylePay API while adding each country's dominant mobile money rails behind it.

## Developer Environment

Set the backend base URL with:

```bash
VITE_API_BASE_URL=https://nyle-pay.onrender.com
```

Common commands:

```bash
npm install
npm run dev
npm run build
```

Business access flow:

- Public signup collects only full name and email.
- NylePay sends a 6-digit Resend verification code.
- Confirming the email opens the Business dashboard.
- Sandbox API keys are available in the dashboard for developer testing.
- The dashboard sidebar contains Go Live for the full production requirements flow.

The sandbox tester in the docs is intended to cover:

- Authentication.
- Business registration.
- Route capabilities.
- M-Pesa to bank quotes.
- Airtel Money to PesaLink quotes.
- Wallet to Airtel Money quotes.
- Crypto to M-Pesa or Airtel Money quotes.
- Route execution.
- Route status inspection.

## Security Direction

Secret credentials must never be stored in browser local storage. The console should show them once during registration, then rely on the backend for key rotation, audit logs, and status display. Public keys can be shown in the browser; secret keys and webhook secrets belong only in server-side environment variables.
