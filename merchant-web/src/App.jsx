import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Landing from './pages/Landing';
import Features from './pages/Features';
import Pricing from './pages/Pricing';
import ApiDocs from './pages/ApiDocs';
import Requirements from './pages/Requirements';
import ConfirmEmail from './pages/ConfirmEmail';
import Registration from './pages/Registration';
import Dashboard from './pages/Dashboard';
import './index.css';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/features" element={<Features />} />
          <Route path="/pricing" element={<Pricing />} />
          <Route path="/docs" element={<ApiDocs />} />
          <Route path="/requirements" element={<Requirements />} />
          <Route path="/confirm-email" element={<ConfirmEmail />} />
          <Route path="/register-business" element={
            <ProtectedRoute>
              <Registration />
            </ProtectedRoute>
          } />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
