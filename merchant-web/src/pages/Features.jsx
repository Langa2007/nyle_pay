import React from 'react';
import { Link } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';

const features = [
  {
    title: 'Hosted checkout',
    body: 'Send customers to a NylePay checkout page that supports M-Pesa, cards, NylePay wallet payments, and crypto without exposing your team to card data.',
    details: ['Payment links with expiry controls', 'Redirect URLs after payment', 'Customer contact capture for receipts'],
  },
  {
    title: 'Direct API charges',
    body: 'Use server-side API calls to initiate M-Pesa STK pushes, inspect balances, and trigger merchant payouts from your own backend.',
    details: ['Secret-key authentication', 'JSON request and response payloads', 'Works with any HTTP client or language'],
  },
  {
    title: 'Settlement and payouts',
    body: 'Track completed payments and settle funds to M-Pesa or bank accounts with a ledger designed for reconciliation.',
    details: ['Available and pending balances', 'Payout references', 'Webhook events for payout status'],
  },
  {
    title: 'Webhooks and reconciliation',
    body: 'Receive signed webhook events for payment completion, failures, refunds, and payout updates.',
    details: ['HMAC-SHA256 signatures', 'Retry-safe event handling', 'Reference fields for order matching'],
  },
  {
    title: 'Security and compliance',
    body: 'NylePay combines JWT access, secret-key merchant APIs, encrypted credentials, AML checks, and audit trails.',
    details: ['HMAC verified provider webhooks', 'Role-based admin flows', 'KYC and AML guardrails'],
  },
  {
    title: 'Developer sandbox',
    body: 'Test payment flows before production activation using sandbox credentials and predictable responses.',
    details: ['Test key prefixes', 'No real money movement', 'Examples for multiple languages'],
  },
];

export default function Features() {
  return (
    <div className="marketing-page">
      <MarketingNav />
      <main>
        <section className="marketing-subhero">
          <div className="eyebrow">Features</div>
          <h1>Payment infrastructure for serious merchant workflows</h1>
          <p>
            NylePay gives merchants a single operational layer for checkout, API-driven payments, settlement, webhooks, and developer tooling.
          </p>
        </section>

        <section className="feature-detail-grid">
          {features.map((feature) => (
            <article className="feature-detail-card" key={feature.title}>
              <h2>{feature.title}</h2>
              <p>{feature.body}</p>
              <ul>
                {feature.details.map((detail) => <li key={detail}>{detail}</li>)}
              </ul>
            </article>
          ))}
        </section>

        <section className="cta-band">
          <div>
            <h2>Ready to integrate NylePay?</h2>
            <p>Start with hosted checkout, then move to direct API payments as your product grows.</p>
          </div>
          <Link className="btn-primary" to="/docs">Read the API docs</Link>
        </section>
      </main>
    </div>
  );
}
