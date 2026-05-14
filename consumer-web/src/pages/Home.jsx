import React from 'react';

const BUSINESS_URL = import.meta.env.VITE_NYLEPAY_BUSINESS_URL || 'http://localhost:5174';

const rails = ['M-Pesa', 'Banks', 'Cards', 'NylePay Wallet', 'USDC', 'USDT', 'Paybill', 'Till', 'Future mobile money rails'];

const routeExamples = [
  ['M-Pesa customer', 'Business bank account', 'Collect locally, settle to bank in real time where supported.'],
  ['Card payment', 'Business M-Pesa', 'Use card acceptance while the business receives Kenyan shillings.'],
  ['USDT or USDC', 'M-Pesa or bank', 'Bridge digital assets into local business rails without exposing users to the plumbing.'],
  ['Wallet balance', 'Paybill or Till', 'Move value from NylePay balance into familiar Kenyan payment destinations.'],
];

const policies = [
  ['11-digit NylePay account', 'Every NylePay account keeps the 11-digit account identity as the stable user reference across wallet, routing, and business flows.'],
  ['KYC and limits', 'Account limits, route availability, crypto access, and settlement speed depend on verification level, risk checks, and local regulation.'],
  ['Legal routes only', 'NylePay should only execute routes allowed by licensing, partner rules, AML controls, sanctions screening, and destination rail policies.'],
  ['Transparent execution', 'Quotes should show amount, fees, FX, estimated speed, provider rail, and final destination before a route is executed.'],
];

const Home = () => (
  <div id="top">
    <section style={styles.hero}>
      <div style={styles.heroInner}>
        <div style={styles.heroCopy}>
          <div className="pill pill-blue">Payment routing engine for Kenya first</div>
          <h1 style={styles.heroTitle}>Move value from where it is to where it needs to be.</h1>
          <p style={styles.heroText}>
            NylePay connects M-Pesa, banks, cards, wallets, and crypto rails into one routing layer. Individuals use the app. Businesses and developers use NylePay Business and the routing API.
          </p>
          <div style={styles.heroActions}>
            <a href={`${BUSINESS_URL}`} className="btn-primary">Explore NylePay Business</a>
            <a href={`${BUSINESS_URL}/docs`} className="btn-secondary">Read API docs</a>
          </div>
          <div style={styles.heroStats}>
            <Stat value="Any source" label="Supported rails become route inputs" />
            <Stat value="Any destination" label="M-Pesa, bank, wallet, Paybill, and more" />
            <Stat value="One action" label="Quote, execute, track, reconcile" />
          </div>
        </div>

        <div className="glass-panel" style={styles.routePanel}>
          <div style={styles.routeHeader}>
            <span>NylePay route</span>
            <strong>Quote before execution</strong>
          </div>
          <RouteLine label="Source" value="USDC on supported network" />
          <RouteLine label="Bridge" value="Stablecoin to KES liquidity" />
          <RouteLine label="Destination" value="Business M-Pesa account" />
          <div style={styles.quoteBox}>
            <div><span>Estimated speed</span><strong>Real time where rail allows</strong></div>
            <div><span>Controls</span><strong>KYC, AML, limits, audit log</strong></div>
            <div><span>Webhook</span><strong>route.completed</strong></div>
          </div>
        </div>
      </div>
    </section>

    <section id="overview" style={styles.section}>
      <SectionHeader eyebrow="What NylePay is" title="Not a wallet. Not a crypto app. A routing layer." body="Wallets, M-Pesa, cards, banks, and stablecoins are rails. NylePay's job is to choose, price, execute, and reconcile the best legal route between them." />
      <div style={styles.cardGrid}>
        <InfoCard title="For individuals" body="The consumer experience lives in the app. Users can hold a NylePay account, fund it, and route money to supported destinations without needing to understand every provider behind the scenes." />
        <InfoCard title="For businesses" body="Businesses use NylePay Business to accept from many rails and settle to the account they choose: M-Pesa, bank, wallet, Paybill, or future mobile money destinations." />
        <InfoCard title="For developers" body="Developers integrate once, quote routes, execute routes idempotently, receive signed webhooks, and reconcile every movement through a single API contract." />
      </div>
    </section>

    <section id="funds-flow" style={styles.sectionAlt}>
      <SectionHeader eyebrow="Funds flow" title="How a route moves through NylePay" body="Every movement should be quoted, authorized, executed, tracked by leg, and settled with a verifiable record." />
      <div style={styles.flowGrid}>
        {[
          ['1', 'Source selected', 'The user or business chooses where value starts: M-Pesa, card, bank, wallet, or supported crypto.'],
          ['2', 'Route quoted', 'NylePay returns available rails, fees, FX, estimated speed, limits, and fallback options.'],
          ['3', 'Compliance checked', 'KYC level, AML screening, account limits, provider rules, and licensing scope are checked before execution.'],
          ['4', 'Route executed', 'NylePay sends the instruction to the selected provider path and records each leg in the ledger.'],
          ['5', 'Destination settled', 'Funds arrive in the chosen destination account, with signed webhooks and reconciliation data.'],
        ].map(([num, title, body]) => <FlowStep key={num} num={num} title={title} body={body} />)}
      </div>
    </section>

    <section style={styles.section}>
      <SectionHeader eyebrow="Route examples" title="The product is the path, not the rail" body="These are examples of the kind of routes NylePay should make feel like one action." />
      <div style={styles.tableWrap}>
        <table style={styles.table}>
          <thead>
            <tr><th>Source</th><th>Destination</th><th>Use case</th></tr>
          </thead>
          <tbody>
            {routeExamples.map(([source, destination, use]) => (
              <tr key={source}><td>{source}</td><td>{destination}</td><td>{use}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
      <div style={styles.railsBar}>
        <span style={styles.railsLabel}>Rails in scope</span>
        <div style={styles.rails}>{rails.map((rail) => <span key={rail} style={styles.rail}>{rail}</span>)}</div>
      </div>
    </section>

    <section id="developers" style={styles.sectionAlt}>
      <SectionHeader eyebrow="Developers and businesses" title="Integrate once. Route across many rails." body="NylePay Business is where businesses configure settlement, create route-aware checkout, manage keys, and test sandbox routes." />
      <div style={styles.split}>
        <div className="card-elevated" style={styles.block}>
          <h3 style={styles.blockTitle}>Business console</h3>
          <p style={styles.blockText}>Set primary and fallback destinations, view routes, export reconciliation reports, manage webhooks, and monitor settlement status.</p>
          <a href={BUSINESS_URL} className="btn-primary" style={styles.blockButton}>Open NylePay Business</a>
        </div>
        <div className="card-elevated" style={styles.block}>
          <h3 style={styles.blockTitle}>Routing API</h3>
          <p style={styles.blockText}>Use capabilities, quote, execute, status, settlement policy, and signed webhook endpoints to build financial movement into your product.</p>
          <a href={`${BUSINESS_URL}/docs`} className="btn-secondary" style={styles.blockButton}>View API documentation</a>
        </div>
      </div>
    </section>

    <section id="account-policy" style={styles.section}>
      <SectionHeader eyebrow="Account policy" title="The account model must be simple, but strict." body="The public website should explain the operating principles clearly while actual onboarding remains inside the app and business console." />
      <div style={styles.policyGrid}>
        {policies.map(([title, body]) => <InfoCard key={title} title={title} body={body} />)}
      </div>
    </section>

    <section id="security" style={styles.sectionAlt}>
      <SectionHeader eyebrow="Trust and compliance" title="Routing only works if trust is engineered in." body="NylePay needs strong controls around licensing, KYC, AML, provider credentials, ledger integrity, webhook signatures, and operational auditability." />
      <div style={styles.cardGrid}>
        <InfoCard title="Signed events" body="Route, payment, settlement, and failure events should be delivered through HMAC-signed webhooks." />
        <InfoCard title="Ledger integrity" body="Every balance movement should be idempotent, traceable, and tied to a source, destination, provider, and route reference." />
        <InfoCard title="Regulatory boundary" body="Crypto, fiat, remittance, merchant settlement, and mobile money flows must stay inside the correct licensing and partner framework." />
      </div>
    </section>

    <footer style={styles.footer}>
      <div style={styles.silhouette} aria-hidden="true">
        <div style={styles.silhouetteRail} />
        <div style={{ ...styles.tower, left: '6%', height: 80, width: 34 }} />
        <div style={{ ...styles.tower, left: '12%', height: 112, width: 54 }} />
        <div style={{ ...styles.tower, left: '20%', height: 64, width: 42 }} />
        <div style={{ ...styles.tower, left: '30%', height: 96, width: 68 }} />
        <div style={{ ...styles.tower, left: '43%', height: 72, width: 48 }} />
        <div style={{ ...styles.tower, left: '54%', height: 128, width: 58 }} />
        <div style={{ ...styles.tower, left: '64%', height: 76, width: 44 }} />
        <div style={{ ...styles.tower, left: '73%', height: 104, width: 72 }} />
        <div style={{ ...styles.tower, left: '86%', height: 68, width: 50 }} />
        <div style={{ ...styles.signalLine, left: '11%', width: '34%', transform: 'rotate(-5deg)' }} />
        <div style={{ ...styles.signalLine, right: '12%', width: '36%', transform: 'rotate(4deg)' }} />
      </div>

      <div style={styles.footerInner}>
        <div style={styles.footerBrand}>
          <strong>NylePay</strong>
          <span>Payment routing for African commerce.</span>
        </div>
        <div style={styles.footerLinks}>
          <a href={`${BUSINESS_URL}`}>NylePay Business</a>
          <a href={`${BUSINESS_URL}/docs`}>API docs</a>
          <a href="#funds-flow">Funds flow</a>
          <a href="#account-policy">Account policy</a>
        </div>
        <div style={styles.footerMeta}>
          <span>Kenya first. Africa next.</span>
          <span>Routes execute only inside legal, verified, and auditable rails.</span>
        </div>
      </div>
    </footer>
  </div>
);

function SectionHeader({ eyebrow, title, body }) {
  return (
    <div style={styles.sectionHeader}>
      <div className="section-eyebrow">{eyebrow}</div>
      <h2 className="section-title">{title}</h2>
      <p className="section-subtitle">{body}</p>
    </div>
  );
}

function InfoCard({ title, body }) {
  return (
    <article className="card-elevated" style={styles.infoCard}>
      <h3 style={styles.cardTitle}>{title}</h3>
      <p style={styles.cardText}>{body}</p>
    </article>
  );
}

function FlowStep({ num, title, body }) {
  return (
    <article className="card-elevated" style={styles.flowStep}>
      <div className="step-number">{num}</div>
      <h3 style={styles.cardTitle}>{title}</h3>
      <p style={styles.cardText}>{body}</p>
    </article>
  );
}

function Stat({ value, label }) {
  return (
    <div style={styles.stat}>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function RouteLine({ label, value }) {
  return (
    <div style={styles.routeLine}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

const styles = {
  hero: { minHeight: '100vh', display: 'flex', alignItems: 'center', padding: '8rem 5% 4rem', position: 'relative' },
  heroInner: { maxWidth: 1280, margin: '0 auto', width: '100%', display: 'grid', gridTemplateColumns: 'minmax(0, 1.1fr) 440px', gap: '4rem', alignItems: 'center' },
  heroCopy: { maxWidth: 720 },
  heroTitle: { fontSize: 'clamp(3rem, 6vw, 5.8rem)', fontWeight: 900, letterSpacing: '-0.03em', lineHeight: 1.02, margin: '1.25rem 0' },
  heroText: { fontSize: '1.16rem', color: 'var(--text-secondary)', lineHeight: 1.75, maxWidth: 690 },
  heroActions: { display: 'flex', gap: '1rem', flexWrap: 'wrap', marginTop: '2rem' },
  heroStats: { display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '1rem', marginTop: '3rem' },
  stat: { borderTop: '1px solid var(--border-color)', paddingTop: '1rem' },
  routePanel: { padding: '1.5rem', borderRadius: 16 },
  routeHeader: { display: 'flex', justifyContent: 'space-between', gap: '1rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.86rem' },
  routeLine: { display: 'grid', gridTemplateColumns: '110px 1fr', gap: '1rem', padding: '1rem 0', borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.9rem' },
  quoteBox: { display: 'grid', gap: '0.75rem', marginTop: '1rem', padding: '1rem', borderRadius: 12, background: 'rgba(59,130,246,0.08)', border: '1px solid rgba(59,130,246,0.14)' },
  section: { padding: '6rem 5%', position: 'relative', zIndex: 10 },
  sectionAlt: { padding: '6rem 5%', position: 'relative', zIndex: 10, background: 'linear-gradient(180deg, #ffffff 0%, #f4f9ff 45%, #ffffff 100%)' },
  sectionHeader: { maxWidth: 780, margin: '0 auto 3rem', textAlign: 'center' },
  cardGrid: { maxWidth: 1280, margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: '1.25rem' },
  policyGrid: { maxWidth: 1280, margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: '1.25rem' },
  infoCard: { padding: '1.5rem' },
  cardTitle: { fontSize: '1.05rem', marginBottom: '0.7rem' },
  cardText: { color: 'var(--text-secondary)', fontSize: '0.92rem', lineHeight: 1.7 },
  flowGrid: { maxWidth: 1280, margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(5, minmax(0, 1fr))', gap: '1rem' },
  flowStep: { padding: '1.25rem' },
  tableWrap: { maxWidth: 1080, margin: '0 auto', overflowX: 'auto', border: '1px solid var(--border-color)', borderRadius: 16, background: 'var(--card-bg)' },
  table: { width: '100%', borderCollapse: 'collapse' },
  railsBar: { maxWidth: 1080, margin: '1.25rem auto 0', background: '#ffffff', border: '1px solid var(--border-color)', borderRadius: 16, padding: '1.25rem', display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center', boxShadow: 'var(--shadow-sm)' },
  railsLabel: { color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', fontSize: '0.76rem', fontWeight: 700 },
  rails: { display: 'flex', flexWrap: 'wrap', gap: '0.5rem' },
  rail: { padding: '0.3rem 0.8rem', background: '#f1f7ff', border: '1px solid rgba(23,105,224,0.16)', borderRadius: 999, color: 'var(--brand-blue)', fontSize: '0.82rem', fontWeight: 600 },
  split: { maxWidth: 1080, margin: '0 auto', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' },
  block: { padding: '2rem' },
  blockTitle: { fontSize: '1.3rem', marginBottom: '0.8rem' },
  blockText: { color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: '1.5rem' },
  blockButton: { width: 'fit-content' },
  footer: { position: 'relative', marginTop: '2rem', overflow: 'hidden', borderTop: '1px solid var(--border-color)', background: 'linear-gradient(180deg, #ffffff 0%, #edf6ff 100%)' },
  silhouette: { position: 'relative', height: 180, maxWidth: 1280, margin: '0 auto', opacity: 0.95 },
  silhouetteRail: { position: 'absolute', left: '4%', right: '4%', bottom: 0, height: 18, borderRadius: '14px 14px 0 0', background: '#d9eaff', boxShadow: '0 -18px 60px rgba(23,105,224,0.14)' },
  tower: { position: 'absolute', bottom: 0, borderRadius: '8px 8px 0 0', background: 'linear-gradient(180deg, #c9e0ff 0%, #b8d6fb 100%)', border: '1px solid rgba(23,105,224,0.10)', boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.8)' },
  signalLine: { position: 'absolute', top: 66, height: 1, background: 'linear-gradient(90deg, transparent, rgba(23,105,224,0.42), rgba(47,128,237,0.28), transparent)' },
  footerInner: { maxWidth: 1280, margin: '0 auto', padding: '2rem 5% 2.4rem', display: 'grid', gridTemplateColumns: '1.2fr auto', gap: '1.5rem', alignItems: 'start', borderTop: '1px solid rgba(23,105,224,0.12)' },
  footerBrand: { display: 'grid', gap: '0.3rem', color: 'var(--text-secondary)' },
  footerLinks: { display: 'flex', gap: '1rem', flexWrap: 'wrap' },
  footerMeta: { gridColumn: '1 / -1', display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap', color: 'var(--text-muted)', fontSize: '0.82rem', paddingTop: '1rem', borderTop: '1px solid rgba(23,105,224,0.12)' },
};

export default Home;
