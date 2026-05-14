import React from 'react';
import { Link, NavLink } from 'react-router-dom';
import ThemeToggle from './ThemeToggle';

const navItems = [
  { label: 'Features', to: '/features' },
  { label: 'Pricing', to: '/pricing' },
  { label: 'API Docs', to: '/docs' },
];

export default function MarketingNav() {
  return (
    <header className="marketing-nav">
      <div className="marketing-nav-inner">
        <Link className="brand-link" to="/">
          <span className="brand-mark" aria-hidden="true">
            <img src="/nylepay-mark.svg" alt="" />
          </span>
          <span className="brand-copy">
            <strong>NylePay</strong>
            <span>Business</span>
          </span>
        </Link>

        <nav className="marketing-links" aria-label="Business navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `marketing-link ${isActive ? 'active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="marketing-actions">
          <ThemeToggle />
          <Link className="btn-ghost marketing-signin" to="/">Sign in</Link>
          <Link className="btn-primary" to="/register-business">Get started</Link>
        </div>
      </div>
    </header>
  );
}
