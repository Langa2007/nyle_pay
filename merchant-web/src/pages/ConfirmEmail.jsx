import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import MarketingNav from '../components/MarketingNav';
import { useAuth } from '../context/useAuth';

export default function ConfirmEmail() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { confirmBusinessAccess } = useAuth();
  const [status, setStatus] = useState('checking');
  const [message, setMessage] = useState('Confirming your email address.');

  useEffect(() => {
    const token = params.get('token');
    if (!token) {
      setStatus('error');
      setMessage('The confirmation link is missing a token.');
      return;
    }

    confirmBusinessAccess(token)
      .then(() => {
        setStatus('success');
        setMessage('Email confirmed. Opening your business dashboard.');
        setTimeout(() => navigate('/dashboard'), 900);
      })
      .catch((err) => {
        setStatus('error');
        setMessage(err.message || 'Email confirmation failed.');
      });
  }, [params, navigate]);

  return (
    <div className="marketing-page">
      <MarketingNav />
      <main className="confirm-shell">
        <div className={`confirm-card ${status}`}>
          <span className="eyebrow">Email confirmation</span>
          <h1>{status === 'success' ? 'Access confirmed' : status === 'error' ? 'Link could not be confirmed' : 'Confirming access'}</h1>
          <p>{message}</p>
          {status === 'error' && <Link className="btn-primary" to="/">Request a new link</Link>}
        </div>
      </main>
    </div>
  );
}
