import React from 'react';
import { Link } from 'react-router-dom';

const Footer = () => (
  <footer style={styles.footer}>
    <div className="gradient-divider" />
    <div style={styles.inner}>
      <div style={styles.top}>
        <div style={styles.brand}>
          <div style={styles.logoWrap}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <defs>
                <linearGradient id="f-logo" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stopColor="#3b82f6"/>
                  <stop offset="100%" stopColor="#10b981"/>
                </linearGradient>
              </defs>
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="url(#f-logo)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            <span className="text-gradient" style={{ fontWeight: 800, fontSize: '1.2rem', fontFamily: 'var(--font-heading)' }}>NylePay</span>
          </div>
          <p style={styles.tagline}>Africa's unified payment identity. One account, every rail.</p>
        </div>

        <div style={styles.links}>
          <div>
            <div style={styles.linkGroup}>Account</div>
            <Link to="/register" style={styles.link}>Create Account</Link>
            <Link to="/login" style={styles.link}>Sign In</Link>
            <a href="#ecosystem" style={styles.link}>How It Works</a>
          </div>
          <div>
            <div style={styles.linkGroup}>Businesses</div>
            <a href="http://localhost:5174" style={styles.link}>Merchant Portal</a>
          </div>
          <div>
            <div style={styles.linkGroup}>Legal</div>
            <a href="#security" style={styles.link}>Security</a>
            <a href="#" style={styles.link}>Privacy Policy</a>
            <a href="#" style={styles.link}>Terms of Service</a>
          </div>
        </div>
      </div>

      <div style={styles.bottom}>
        <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
          © {new Date().getFullYear()} NylePay Limited. All rights reserved. Regulated by the Central Bank of Kenya.
        </span>
        <div style={styles.badges}>
          {['PCI DSS', 'CBK Licensed', 'AML Compliant'].map(b => (
            <span key={b} style={styles.badge}>{b}</span>
          ))}
        </div>
      </div>
    </div>
  </footer>
);

const styles = {
  footer: {
    position: 'relative',
    zIndex: 10,
    background: 'var(--bg-secondary)',
    borderTop: '1px solid var(--border-color)',
  },
  inner: {
    maxWidth: '1280px',
    margin: '0 auto',
    padding: '4rem 5% 2rem',
  },
  top: {
    display: 'flex',
    gap: '4rem',
    marginBottom: '3rem',
    flexWrap: 'wrap',
  },
  brand: { flex: '1 1 280px' },
  logoWrap: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    marginBottom: '1rem',
  },
  tagline: {
    color: 'var(--text-secondary)',
    fontSize: '0.875rem',
    lineHeight: 1.6,
    maxWidth: '260px',
  },
  links: {
    flex: '1',
    display: 'flex',
    gap: '3rem',
    flexWrap: 'wrap',
  },
  linkGroup: {
    fontSize: '0.75rem',
    fontWeight: 700,
    letterSpacing: '0.07em',
    textTransform: 'uppercase',
    color: 'var(--text-muted)',
    marginBottom: '1rem',
  },
  link: {
    display: 'block',
    color: 'var(--text-secondary)',
    fontSize: '0.875rem',
    marginBottom: '0.6rem',
    transition: 'color 0.2s',
  },
  bottom: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: '1.5rem',
    borderTop: '1px solid var(--border-color)',
    flexWrap: 'wrap',
    gap: '1rem',
  },
  badges: { display: 'flex', gap: '0.5rem' },
  badge: {
    padding: '0.2rem 0.7rem',
    background: 'rgba(255,255,255,0.05)',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--radius-full)',
    fontSize: '0.7rem',
    color: 'var(--text-muted)',
    fontWeight: 600,
    letterSpacing: '0.04em',
  },
};

export default Footer;
