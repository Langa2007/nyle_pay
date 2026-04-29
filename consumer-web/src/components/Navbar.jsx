import React from 'react';
import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <nav style={styles.navbar} className="glass-panel">
      <div style={styles.logo}>
        <span className="text-gradient" style={{ fontWeight: 800, fontSize: '1.5rem', fontFamily: 'var(--font-heading)' }}>
          NylePay
        </span>
      </div>
      <div style={styles.links}>
        <a href="#how-it-works" style={styles.link}>How it Works</a>
        <a href="#features" style={styles.link}>Features</a>
        <a href="#security" style={styles.link}>Security</a>
      </div>
      <div style={styles.actions}>
        <Link to="/login" className="btn-secondary" style={{ padding: '0.5rem 1.5rem', fontSize: '1rem' }}>Log In</Link>
        <Link to="/register" className="btn-primary" style={{ padding: '0.5rem 1.5rem', fontSize: '1rem', marginLeft: '1rem' }}>Get Started</Link>
      </div>
    </nav>
  );
};

const styles = {
  navbar: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem 2rem',
    position: 'fixed',
    top: '1rem',
    left: '2rem',
    right: '2rem',
    zIndex: 100,
    borderRadius: '100px',
  },
  links: {
    display: 'flex',
    gap: '2rem',
  },
  link: {
    color: 'var(--text-primary)',
    fontWeight: 500,
    fontSize: '1rem',
  },
  actions: {
    display: 'flex',
    alignItems: 'center',
  }
};

export default Navbar;
