import { useState, useEffect } from 'react';
import { Shield, Lock, User, AlertCircle, Eye, EyeOff, Key } from 'lucide-react';
import PropTypes from 'prop-types';
import SecretCodeModal from '../components/SecretCodeModal';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function Login({ onLoginSuccess, onShowRegister, onUnlockRegister }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasAdmin, setHasAdmin] = useState(true);
  const [hasSuperAdmin, setHasSuperAdmin] = useState(false);
  const [checkingAdmin, setCheckingAdmin] = useState(true);
  const [showSecretModal, setShowSecretModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  // ===== CHECK ADMIN STATUS =====
  useEffect(() => {
    const checkAdmin = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/check-admin`);
        if (response.ok) {
          const data = await response.json();
          setHasAdmin(data.hasAdmin);
          setHasSuperAdmin(data.hasSuperAdmin);
        } else {
          setHasAdmin(true);
          setHasSuperAdmin(false);
        }
      } catch (error) {
        console.error('Error checking admin status:', error);
        setHasAdmin(true);
        setHasSuperAdmin(false);
      } finally {
        setCheckingAdmin(false);
      }
    };
    checkAdmin();
  }, []);

  // ===== Check for success message from register =====
  useEffect(() => {
    const msg = localStorage.getItem('registerSuccess');
    if (msg) {
      setSuccessMessage(msg);
      localStorage.removeItem('registerSuccess');
      setTimeout(() => setSuccessMessage(''), 5000);
    }
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Please fill in all fields');
      return;
    }

    setLoading(true);
    setError('');
    setSuccessMessage('');

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Invalid username or password');
      }

      const userData = await response.json();
      onLoginSuccess(userData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSecretVerified = () => {
    // Code verified - unlock register and navigate
    if (onUnlockRegister) {
      onUnlockRegister();
    }
  };

  // ===== Show Register button if no admin exists =====
  const showRegisterButton = !checkingAdmin && !hasAdmin && !hasSuperAdmin;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 relative overflow-hidden font-sans">
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-red-500/10 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-red-500/5 rounded-full blur-[120px]" />

      <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800/80 rounded-2xl shadow-2xl p-8 relative z-10 animate-fade-in">
        
        {/* ===== SECRET CODE KEY ICON - Bottom Right ===== */}
        {!checkingAdmin && (hasAdmin || hasSuperAdmin) && (
          <button
            onClick={() => setShowSecretModal(true)}
            className="absolute bottom-4 right-4 p-2 text-slate-600 hover:text-yellow-400 hover:bg-yellow-500/10 rounded-lg transition-all duration-300 opacity-30 hover:opacity-100 border border-transparent hover:border-yellow-500/30"
            title="Developer Access - Unlock Admin Registration"
          >
            <Key className="w-4 h-4" />
          </button>
        )}

        <div className="flex flex-col items-center mb-8">
          <div className="bg-red-500/10 p-4 rounded-full border border-red-500/20 mb-4 animate-pulse">
            <Shield className="w-10 h-10 text-red-500" />
          </div>
          <h1 className="text-2xl font-bold tracking-wider text-white uppercase text-center">
            Alarm Security System
          </h1>
          <p className="text-sm text-slate-400 mt-1 font-mono text-center">
            Centralized Live Monitoring Portal
          </p>
        </div>

        {/* ===== SUCCESS MESSAGE ===== */}
        {successMessage && (
          <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 mb-4 text-sm text-emerald-400 animate-in">
            <span>✅</span>
            <span>{successMessage}</span>
          </div>
        )}

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 mb-6 text-sm text-red-400 animate-shake">
            <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
              Username
            </label>
            <div className="relative">
              <User className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl pl-11 pr-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50 focus:ring-1 focus:ring-red-500/50 transition-all font-mono"
                required
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl pl-11 pr-12 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50 focus:ring-1 focus:ring-red-500/50 transition-all font-mono"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                tabIndex="-1"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? (
                  <EyeOff className="w-4 h-4" />
                ) : (
                  <Eye className="w-4 h-4" />
                )}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 active:scale-[0.99] disabled:opacity-50 text-white font-bold py-3 px-4 rounded-xl text-sm transition-all shadow-lg shadow-red-500/10 font-mono tracking-wider uppercase mt-2 flex items-center justify-center gap-2"
          >
            {loading ? (
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              'Sign In to Portal'
            )}
          </button>
        </form>

        {/* ===== REGISTER BUTTON - No admin exists ===== */}
        {showRegisterButton && (
          <p className="text-center text-xs text-slate-500 mt-4 font-mono">
            No admin account found.{' '}
            <button
              type="button"
              onClick={onShowRegister}
              className="text-red-400 hover:text-red-300 transition-colors font-bold underline-offset-2 hover:underline"
            >
              Create Admin Account
            </button>
          </p>
        )}

        {/* ===== SHOW MESSAGE WHEN ADMIN EXISTS ===== */}
        {!checkingAdmin && hasAdmin && !successMessage && (
          <p className="text-center text-xs text-slate-500 mt-4 font-mono">
            Admin account exists. Please login.
          </p>
        )}

        {checkingAdmin && (
          <p className="text-center text-xs text-slate-500 mt-4 font-mono">
            Checking system status...
          </p>
        )}
      </div>

      {/* ===== SECRET CODE MODAL ===== */}
      <SecretCodeModal
        isOpen={showSecretModal}
        onClose={() => setShowSecretModal(false)}
        onVerified={handleSecretVerified}
      />
    </div>
  );
}

Login.propTypes = {
  onLoginSuccess: PropTypes.func.isRequired,
  onShowRegister: PropTypes.func.isRequired,
  onUnlockRegister: PropTypes.func.isRequired,
};