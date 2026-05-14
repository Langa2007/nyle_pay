import React, { useState, useEffect } from 'react';
import { AuthContext } from './authContextValue';

const API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function AuthProvider({ children }) {
  const [user, setUser]     = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('npy_business_session');
    if (stored) {
      try { setUser(JSON.parse(stored)); } catch { /* corrupt */ }
    }
    setLoading(false);
  }, []);

  const storeSession = (data) => {
    const session = {
      token: data.token,
      userId: data.userId,
      email: data.email,
      fullName: data.fullName,
      accountNumber: data.accountNumber,
      emailVerified: data.emailVerified,
      businessId: null,
      businessName: null,
      apiKeys: null,
    };
    localStorage.setItem('npy_business_session', JSON.stringify(session));
    setUser(session);
    return session;
  };

  /**
   * Sign in with email + password against the real NylePay backend.
   * On success stores the JWT and user profile in localStorage.
   */
  const login = async (email, password) => {
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Invalid credentials');
    }
    return storeSession(json.data);
  };

  const requestBusinessAccess = async ({ fullName, email }) => {
    const res = await fetch(`${API}/api/auth/business-access/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Unable to send confirmation email');
    }
    return json.data;
  };

  const confirmBusinessAccess = async (token) => {
    const res = await fetch(`${API}/api/auth/business-access/confirm?token=${encodeURIComponent(token)}`);
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Email confirmation failed');
    }
    return storeSession(json.data);
  };

  /**
   * Create a new NylePay user account required before business onboarding.
   * mpesaNumber and countryCode are required by the backend.
   */
  const signup = async (email, password, fullName, mpesaNumber = '', countryCode = 'KE') => {
    const res = await fetch(`${API}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, fullName, mpesaNumber, countryCode }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Registration failed');
    }
    // Auto-login after successful registration
    return login(email, password);
  };

  const updateBusinessInfo = (info) => {
    const updated = { ...user, ...info };
    localStorage.setItem('npy_business_session', JSON.stringify(updated));
    setUser(updated);
  };

  const logout = () => {
    localStorage.removeItem('npy_business_session');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, requestBusinessAccess, confirmBusinessAccess, logout, updateBusinessInfo }}>
      {children}
    </AuthContext.Provider>
  );
}
