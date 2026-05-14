import React from 'react';
import { Link } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';

const included = [
  'Hosted checkout, payment links, and route intents',
  'M-Pesa, card, wallet, bank, and crypto routing options',
  'Business dashboard with exports and reconciliation',
  'Signed webhook delivery and retry visibility',
  'Sandbox credentials and route simulation',
  'Real-time settlement policy management',
];

const rows = [
  ['M-Pesa and wallet collections', '1.5%', 'Charged on successful routed payments only'],
  ['Card and international payments', 'Provider cost + NylePay margin', 'Shown before production activation'],
  ['Crypto intake and conversion', 'Network/provider cost + spread', 'Depends on asset, chain, and liquidity partner'],
  ['Bank or M-Pesa settlement', 'Rail fee applies', 'Depends on destination and payout method'],
  ['Refunds and reversals', 'No NylePay platform fee', 'Provider fees may be non-refundable'],
];

export default function Pricing() {
  return (
    <div className="marketing-page">
      <MarketingNav />
      <main>
        <section className="marketing-subhero">
          <div className="eyebrow">Pricing</div>
          <h1>Clear pricing for business payment routing</h1>
          <p>
            Start with a simple platform rate for successful local routes. Complex routes disclose provider fees, FX, spread, and settlement cost before money moves.
          </p>
        </section>

        <section className="pricing-layout">
          <article className="pricing-card primary">
            <div className="pricing-kicker">Standard business plan</div>
            <h2>1.5%</h2>
            <p>per successful local NylePay route, excluding third-party provider, FX, network, or payout costs where applicable.</p>
            <Link className="btn-primary" to="/register-business">Create business account</Link>
          </article>

          <article className="pricing-card">
            <h3>Included</h3>
            <ul className="clean-list">
              {included.map((item) => <li key={item}>{item}</li>)}
            </ul>
          </article>
        </section>

        <section className="pricing-table-section">
          <h2>Fee schedule</h2>
          <div className="card">
            <table className="data-table pricing-table">
              <thead>
                <tr><th>Product</th><th>Rate</th><th>Notes</th></tr>
              </thead>
              <tbody>
                {rows.map(([product, rate, note]) => (
                  <tr key={product}>
                    <td>{product}</td>
                    <td>{rate}</td>
                    <td>{note}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="cta-band">
          <div>
            <h2>Need custom routing terms?</h2>
            <p>High-volume businesses can negotiate route pricing by rail, destination, risk profile, and settlement volume.</p>
          </div>
          <Link className="btn-outline" to="/docs">Review integration options</Link>
        </section>
      </main>
    </div>
  );
}
