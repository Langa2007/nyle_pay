# NylePay Consumer Web

This site is the public explainer for NylePay. It does not handle login, signup, wallet access, or account onboarding. Those flows belong inside the mobile app and the NylePay Business console.

## Purpose

The site explains NylePay as a payment routing engine:

- Money can start from M-Pesa, Airtel Money, bank, card, wallet, or supported crypto rails.
- Money can land in M-Pesa, Airtel Money, PesaLink, bank, wallet, Paybill, Till, or future country-specific mobile money rails.
- NylePay quotes the route, checks policy, executes the route, tracks every leg, and reconciles the result.
- Businesses and developers use NylePay Business and the API documentation.
- Individuals use the app, not the marketing website, for account access.

## Main Public Sections

- What NylePay does.
- How funds flow through the routing engine.
- Route examples.
- Developer and business links.
- Account policy.
- Trust, security, and compliance principles.

## Links

The public site links to the business console using:

```bash
VITE_NYLEPAY_BUSINESS_URL=http://localhost:5174
```

If the variable is not set, the site defaults to `http://localhost:5174`.

## Commands

```bash
npm install
npm run dev
npm run build
```

## Product Positioning

NylePay should not be described as only a wallet, crypto product, or merchant gateway. The stronger public position is:

> Payment routing for African commerce. Any supported source, any supported destination, one legal route.
