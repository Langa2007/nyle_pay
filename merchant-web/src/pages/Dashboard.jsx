import React, { useState } from 'react';

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');

  return (
    <div className="dashboard-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div style={{ marginBottom: '3rem' }}>
          <h2 style={{ color: 'var(--brand-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '1.5rem' }}>⚡</span> NylePay Business
          </h2>
        </div>
        
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
          <button 
            style={activeTab === 'overview' ? styles.navItemActive : styles.navItem}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button 
            style={activeTab === 'payments' ? styles.navItemActive : styles.navItem}
            onClick={() => setActiveTab('payments')}
          >
            Payments
          </button>
          <button 
            style={activeTab === 'api' ? styles.navItemActive : styles.navItem}
            onClick={() => setActiveTab('api')}
          >
            API & Webhooks
          </button>
        </nav>
        
        <div style={{ marginTop: 'auto', paddingTop: '2rem', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={styles.avatar}>A</div>
            <div>
              <p style={{ fontWeight: 600, fontSize: '0.875rem' }}>Acme Store Ltd</p>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>Merchant ID: 1042</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
          <div>
            <h1>Dashboard Overview</h1>
            <p style={{ color: 'var(--text-secondary)' }}>Welcome back to your NylePay command center.</p>
          </div>
          <button className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span>+</span> Create Payment Link
          </button>
        </header>

        {/* Metrics Row */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '2rem', marginBottom: '3rem' }}>
          <div className="card metric-card">
            <span className="metric-label">Pending Settlement</span>
            <span className="metric-value">KES 14,500</span>
            <span style={{ color: 'var(--brand-success)', fontSize: '0.875rem', fontWeight: 500 }}>
              ↑ Sweeping at 22:00 EAT
            </span>
          </div>
          
          <div className="card metric-card">
            <span className="metric-label">Today's Volume</span>
            <span className="metric-value">KES 42,000</span>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              48 Transactions
            </span>
          </div>

          <div className="card metric-card">
            <span className="metric-label">Settlement Account</span>
            <span className="metric-value" style={{ fontSize: '1.5rem', marginTop: '1rem' }}>M-Pesa (254712***78)</span>
            <span style={{ color: 'var(--brand-success)', fontSize: '0.875rem' }}>
              Status: Verified
            </span>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="card">
          <h3 style={{ marginBottom: '1.5rem' }}>Recent Payments</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)' }}>
                <th style={styles.th}>Reference</th>
                <th style={styles.th}>Amount</th>
                <th style={styles.th}>Method</th>
                <th style={styles.th}>Date</th>
                <th style={styles.th}>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr style={styles.tr}>
                <td style={styles.td}>NPY-LNK-89X2...</td>
                <td style={styles.td}>KES 2,500</td>
                <td style={styles.td}>M-Pesa</td>
                <td style={styles.td}>2 mins ago</td>
                <td style={styles.td}><span style={styles.badgeSuccess}>Completed</span></td>
              </tr>
              <tr style={styles.tr}>
                <td style={styles.td}>NPY-LNK-41B9...</td>
                <td style={styles.td}>KES 12,000</td>
                <td style={styles.td}>Card (Visa)</td>
                <td style={styles.td}>1 hour ago</td>
                <td style={styles.td}><span style={styles.badgeSuccess}>Completed</span></td>
              </tr>
              <tr style={styles.tr}>
                <td style={styles.td}>NPY-LNK-99Z1...</td>
                <td style={styles.td}>KES 850</td>
                <td style={styles.td}>NylePay Wallet</td>
                <td style={styles.td}>3 hours ago</td>
                <td style={styles.td}><span style={styles.badgeSuccess}>Completed</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
};

const styles = {
  navItem: {
    background: 'transparent',
    border: 'none',
    color: 'var(--text-secondary)',
    padding: '0.75rem 1rem',
    textAlign: 'left',
    borderRadius: 'var(--radius-md)',
    fontFamily: 'var(--font-heading)',
    fontSize: '1rem',
    fontWeight: 500,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  navItemActive: {
    background: 'rgba(59, 130, 246, 0.1)',
    border: 'none',
    color: 'var(--brand-primary)',
    padding: '0.75rem 1rem',
    textAlign: 'left',
    borderRadius: 'var(--radius-md)',
    fontFamily: 'var(--font-heading)',
    fontSize: '1rem',
    fontWeight: 600,
    cursor: 'pointer',
  },
  avatar: {
    width: '40px',
    height: '40px',
    borderRadius: '8px',
    background: 'var(--brand-primary)',
    color: 'white',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 'bold',
    fontFamily: 'var(--font-heading)',
  },
  th: {
    padding: '1rem 0',
    fontWeight: 500,
    fontSize: '0.875rem',
  },
  td: {
    padding: '1rem 0',
    borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
  },
  tr: {
    transition: 'background-color 0.2s',
  },
  badgeSuccess: {
    background: 'rgba(16, 185, 129, 0.1)',
    color: 'var(--brand-success)',
    padding: '0.25rem 0.75rem',
    borderRadius: '100px',
    fontSize: '0.75rem',
    fontWeight: 600,
  }
};

export default Dashboard;
