import React, { useEffect, useState } from 'react';

const BUSINESS_URL = import.meta.env.VITE_NYLEPAY_BUSINESS_URL || 'https://nyle-pay.vercel.app';

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
    <nav style={{ ...styles.navbar, background: scrolled ? 'rgba(255, 255, 255, 0.96)' : 'rgba(255, 255, 255, 0.82)', borderBottom: scrolled ? '1px solid var(--border-color)' : '1px solid transparent', boxShadow: scrolled ? '0 8px 24px rgba(15, 23, 42, 0.06)' : 'none' }}>
      <a href="#top" style={styles.logoWrap} onClick={() => setMenuOpen(false)}>
        <div style={styles.logoIcon}>
          <img src="/nylepay-mark.svg" alt="" style={styles.logoImage} />
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
  logoIcon: { width: 36, height: 36, display: 'flex', alignItems: 'center', justifyContent: 'center' },
  logoImage: { width: 36, height: 36, display: 'block' },
  brand: { fontWeight: 800, fontSize: '1.4rem', fontFamily: 'var(--font-heading)' },
  links: { display: 'flex', gap: '2rem', alignItems: 'center' },
  link: { color: 'var(--text-secondary)', fontWeight: 500, fontSize: '0.9rem' },
  actions: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  actionButton: { padding: '0.55rem 1rem', fontSize: '0.86rem' },
  hamburger: { display: 'none', flexDirection: 'column', gap: 5, background: 'none', border: 'none', cursor: 'pointer', padding: 4 },
  bar: { display: 'block', width: 22, height: 2, background: 'var(--text-primary)', borderRadius: 2, transition: 'all 0.3s ease' },
};

export default Navbar;
