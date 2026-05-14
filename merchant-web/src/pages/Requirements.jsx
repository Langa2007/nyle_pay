import React from 'react';
import { Link } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';

const requirementGroups = [
  {
    title: 'Business identity',
    items: [
      'Certificate of business registration or certificate of incorporation',
      'Current company search or CR12 for limited companies',
      'KRA PIN certificate for the business',
      'County business permit or sector licence where required',
      'Registered office, trading address, and contact details',
      'Business activity description, website or sales channel, and products sold',
    ],
  },
  {
    title: 'Owners and operators',
    items: [
      'National ID, passport, or alien ID for directors, proprietors, partners, and signatories',
      'KRA PIN certificates for directors, proprietors, partners, and account signatories',
      'Passport photos where a provider or bank requests them',
      'Proof of physical address for at least one director, owner, or signatory',
      'Board resolution, partners resolution, or authority letter naming approved operators',
      'Mobile number and email for each person allowed to operate the NylePay Business account',
    ],
  },
  {
    title: 'Beneficial ownership',
    items: [
      'Beneficial owner names, dates of birth, nationalities, ID or passport copies',
      'BRS beneficial ownership filing or BO form where applicable',
      'Ownership percentages, voting rights, and control structure',
      'Share certificates, register of members, or ownership chart for layered entities',
      'Trust deed, partnership deed, or LLP deed where relevant',
      'Declaration for any person who ultimately owns, controls, or benefits from the business',
    ],
  },
  {
    title: 'Settlement and rail setup',
    items: [
      'Business bank account details, bank letter, or cancelled cheque',
      'M-PESA Till or Paybill application documents and nominated mobile number',
      'Airtel Money settlement number or provider-approved merchant details when enabled',
      'PesaLink bank destination details: bank code, account number, and account name',
      'Expected transaction volumes, source of funds, customer type, and route purpose',
      'Webhook URL, settlement preference, refund contact, and reconciliation email',
    ],
  },
  {
    title: 'Risk and compliance',
    items: [
      'Source of funds and source of wealth for higher-risk or high-volume businesses',
      'Expected monthly value, average ticket size, largest expected transaction, and countries served',
      'PEP, sanctions, adverse media, and AML screening consent for owners and operators',
      'Evidence of licensing or regulator approval for regulated sectors',
      'Prohibited goods, restricted activity, and acceptable-use declaration',
    ],
  },
  {
    title: 'Business type notes',
    items: [
      'Sole proprietors provide business registration, owner ID, owner KRA PIN, and settlement account proof',
      'Limited companies provide incorporation documents, CR12, directors KYC, BO details, and board authority',
      'Partnerships or LLPs provide deed or registration documents, partner KYC, and authority to operate',
      'NGOs, schools, churches, SACCOs, and regulated entities provide registration plus governing approvals',
      'Marketplaces provide seller onboarding controls, dispute process, and settlement responsibility model',
    ],
  },
];

export default function Requirements() {
  return (
    <div className="marketing-page">
      <MarketingNav />
      <main>
        <section className="marketing-subhero">
          <div className="eyebrow">Business requirements</div>
          <h1>NylePay live account requirements</h1>
          <p>
            Documents and business information required before a business can use live collections,
            payouts, settlement rails, production API keys, or higher operational limits.
          </p>
          <div className="hero-actions">
            <Link className="btn-primary" to="/register-business">Start onboarding</Link>
            <Link className="btn-outline" to="/docs">View API docs</Link>
          </div>
        </section>

        <section className="requirements-shell">
          {requirementGroups.map((group) => (
            <article className="requirements-card" key={group.title}>
              <h2>{group.title}</h2>
              <ul>
                {group.items.map((item) => <li key={item}>{item}</li>)}
              </ul>
            </article>
          ))}
        </section>
      </main>
      <BusinessFooter />
    </div>
  );
}

function BusinessFooter() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="business-footer">
      <div>
        <strong>NylePay Business</strong>
        <span>Payment routing infrastructure for African commerce.</span>
      </div>
      <nav aria-label="Footer navigation">
        <Link to="/">Home</Link>
        <Link to="/requirements">Requirements</Link>
        <Link to="/docs">API Docs</Link>
      </nav>
      <span>Copyright © {currentYear} NylePay Business. All rights reserved.</span>
    </footer>
  );
}

