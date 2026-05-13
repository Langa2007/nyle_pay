import React from 'react';
import { Link } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';

const included = [
  'Hosted checkout and payment links',
  'M-Pesa, card, wallet, and crypto payment options',
  'Merchant dashboard and transaction exports',
  'Signed webhook delivery',
  'Sandbox credentials',
  'Settlement tracking',
];

const rows = [
  ['M-Pesa and wallet payments', '1.5%', 'Charged on successful payments only'],
  ['Card payments', 'Provider cost + NylePay margin', 'Shown in your merchant agreement'],
  ['Bank or M-Pesa settlement', 'Standard payout fee applies', 'Depends on rail and destination'],
  ['Refunds and reversals', 'No NylePay platform fee', 'Provider fees may be non-refundable'],
];

export default function Pricing() {
  return (
    <div className="marketing-page">
      <MarketingNav />
      <main>
        <section className="marketing-subhero">
          <div className="eyebrow">Pricing</div>
          <h1>Clear pricing for merchant payments</h1>
          <p>
            Start with a simple platform rate for successful payments. Custom terms are available for high-volume merchants and regulated payout use cases.
          </p>
        </section>

        <section className="pricing-layout">
          <article className="pricing-card primary">
            <div className="pricing-kicker">Standard merchant plan</div>
            <h2>1.5%</h2>
            <p>per successful NylePay merchant transaction, excluding third-party provider costs where applicable.</p>
            <Link className="btn-primary" to="/register-business">Create merchant account</Link>
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
            <h2>Need volume pricing?</h2>
            <p>Contact NylePay support after onboarding to review settlement volume, risk controls, and custom terms.</p>
          </div>
          <Link className="btn-outline" to="/docs">Review integration options</Link>
        </section>
      </main>
    </div>
  );
}
