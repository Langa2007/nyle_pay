import React from 'react';
import { Link } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';

const features = [
  {
    title: 'Universal collections',
    body: 'Businesses can accept M-Pesa, Airtel Money, cards, wallet balances, bank transfers, and crypto without building a separate integration for every rail.',
    details: ['Hosted checkout and API intents', 'M-Pesa and Airtel Money Kenyan flows', 'Crypto and fiat support without removing either side'],
  },
  {
    title: 'Routing policies',
    body: 'Decide where money should land by default, then define fallback paths for provider downtime, failed payouts, high fees, or customer preference.',
    details: ['Primary and fallback destinations', 'Rail preference rules', 'Real-time route decisions'],
  },
  {
    title: 'Real-time settlement',
    body: 'Move funds to the selected M-Pesa number, Airtel Money number, PesaLink bank account, bank account, or NylePay wallet as soon as the route is confirmed and risk checks pass.',
    details: ['Available and pending balances', 'Settlement references', 'Route-level reconciliation'],
  },
  {
    title: 'Route visibility',
    body: 'Every transaction is shown as a route with source, destination, provider, fees, FX, status, and provider references.',
    details: ['Leg-by-leg status tracking', 'Retryable failures', 'Exports for finance teams'],
  },
  {
    title: 'Secure developer layer',
    body: 'Use server-side secret keys, signed webhooks, sandbox credentials, audit trails, and encrypted provider credentials.',
    details: ['HMAC-SHA256 webhooks', 'Secret keys stay server-side', 'Sandbox route simulation'],
  },
  {
    title: 'Africa-ready expansion',
    body: 'Start with Kenya through M-Pesa, Airtel Money, and PesaLink, then add country-specific mobile money rails without changing the business-facing API model.',
    details: ['Country capability discovery', 'Provider abstraction', 'One API contract for many rails'],
  },
];

export default function Features() {
  return (
    <div className="marketing-page">
      <MarketingNav />
      <main>
        <section className="marketing-subhero">
          <div className="eyebrow">Features</div>
          <h1>Business routing infrastructure for serious money movement</h1>
          <p>
            NylePay Business gives companies one operational layer for collecting, converting, routing, settling, and reconciling value across fiat and crypto rails.
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
            <h2>Build on the routing layer</h2>
            <p>Start with checkout or route intents, then settle value to the destination your business chooses.</p>
          </div>
          <Link className="btn-primary" to="/docs">Read the API docs</Link>
        </section>
      </main>
    </div>
  );
}
