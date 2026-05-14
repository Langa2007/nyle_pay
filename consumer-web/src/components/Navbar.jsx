import React, { useEffect, useState } from 'react';

const BUSINESS_URL = import.meta.env.VITE_NYLEPAY_BUSINESS_URL || 'http://localhost:5174';

const links = [
  ['What it does', '#overview'],
  ['Funds flow', '#funds-flow'],
  ['Developers', '#developers'],
  ['Account policy', '#account-policy'],
];

const Navbar = () => {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav style={{ ...styles.navbar, background: scrolled ? 'rgba(6, 9, 16, 0.94)' : 'rgba(6, 9, 16, 0.74)', borderBottom: scrolled ? '1px solid rgba(255,255,255,0.08)' : '1px solid transparent' }}>
      <a href="#top" style={styles.logoWrap} onClick={() => setMenuOpen(false)}>
        <div style={styles.logoIcon}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="url(#npy-logo)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            <defs>
              <linearGradient id="npy-logo" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#3b82f6" />
                <stop offset="100%" stopColor="#10b981" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span className="text-gradient" style={styles.brand}>NylePay</span>
      </a>

      <div style={{ ...styles.links, display: menuOpen ? 'flex' : undefined }}>
        {links.map(([label, href]) => (
          <a key={href} href={href} style={styles.link} onClick={() => setMenuOpen(false)}>{label}</a>
        ))}
      </div>

      <div style={styles.actions}>
        <a href={`${BUSINESS_URL}/docs`} className="btn-secondary" style={styles.actionButton}>API docs</a>
        <a href={BUSINESS_URL} className="btn-primary" style={styles.actionButton}>NylePay Business</a>
        <button style={styles.hamburger} onClick={() => setMenuOpen((open) => !open)} aria-label="Toggle menu">
          <span style={{ ...styles.bar, transform: menuOpen ? 'rotate(45deg) translate(5px, 5px)' : 'none' }} />
          <span style={{ ...styles.bar, opacity: menuOpen ? 0 : 1 }} />
          <span style={{ ...styles.bar, transform: menuOpen ? 'rotate(-45deg) translate(5px, -5px)' : 'none' }} />
        </button>
      </div>
    </nav>
  );
};

const styles = {
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem 5%',
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 1000,
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
    transition: 'all 0.3s ease',
  },
  logoWrap: { display: 'flex', alignItems: 'center', gap: '0.55rem', textDecoration: 'none' },
  logoIcon: {
    width: 34,
    height: 34,
    background: 'rgba(59,130,246,0.15)',
    border: '1px solid rgba(59,130,246,0.3)',
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  brand: { fontWeight: 800, fontSize: '1.4rem', fontFamily: 'var(--font-heading)' },
  links: { display: 'flex', gap: '2rem', alignItems: 'center' },
  link: { color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9rem' },
  actions: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  actionButton: { padding: '0.55rem 1rem', fontSize: '0.86rem' },
  hamburger: { display: 'none', flexDirection: 'column', gap: 5, background: 'none', border: 'none', cursor: 'pointer', padding: 4 },
  bar: { display: 'block', width: 22, height: 2, background: 'var(--text-primary)', borderRadius: 2, transition: 'all 0.3s ease' },
};

export default Navbar;
