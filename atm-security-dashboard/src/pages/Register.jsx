import { useState, useEffect } from 'react';
import { Shield, User, Lock, Eye, EyeOff, AlertCircle, CheckCircle, ArrowLeft } from 'lucide-react';
import PropTypes from 'prop-types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function Register({ onBackToLogin }) {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    role: 'ADMIN'
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasAdmin, setHasAdmin] = useState(true);
  const [checkingAdmin, setCheckingAdmin] = useState(true);

  // ===== CHECK IF ADMIN EXISTS =====
  useEffect(() => {
    const checkAdmin = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/check-admin`);
        if (response.ok) {
          const data = await response.json();
          setHasAdmin(data.hasAdmin);
        } else {
          setHasAdmin(true);
        }
      } catch (error) {
        console.error('Error checking admin status:', error);
        setHasAdmin(true);
      } finally {
        setCheckingAdmin(false);
      }
    };
    checkAdmin();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    const { username, password, confirmPassword, role } = formData;

    if (!username.trim()) {
      setError('Username is required');
      return;
    }
    if (username.length < 3) {
      setError('Username must be at least 3 characters');
      return;
    }
    if (!password.trim()) {
      setError('Password is required');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    if (!confirmPassword.trim()) {
      setError('Please confirm your password');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    // ===== Extra validation for role =====
    if (role === 'USER' && !hasAdmin) {
      setError('First account must be ADMIN. Please select ADMIN role.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username.trim(),
          password,
          confirmPassword,
          role
        }),
      });

      const data = await response.text();

      if (!response.ok) {
        throw new Error(data || 'Registration failed');
      }

      setSuccess('✅ Registration successful! You can now login.');
      
      setFormData({
        username: '',
        password: '',
        confirmPassword: '',
        role: 'ADMIN'
      });

      setTimeout(() => {
        if (onBackToLogin) onBackToLogin();
      }, 2000);

    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 relative overflow-hidden font-sans">
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-emerald-500/10 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-emerald-500/5 rounded-full blur-[120px]" />

      <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800/80 rounded-2xl shadow-2xl p-8 relative z-10 animate-fade-in">
        <button
          onClick={onBackToLogin}
          className="absolute top-4 left-4 p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white"
          aria-label="Back to Login"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>

        <div className="flex flex-col items-center mb-6">
          <div className="bg-emerald-500/10 p-4 rounded-full border border-emerald-500/20 mb-4">
            <Shield className="w-10 h-10 text-emerald-500" />
          </div>
          <h1 className="text-2xl font-bold tracking-wider text-white uppercase text-center">
            Create Account
          </h1>
          <p className="text-sm text-slate-400 mt-1 font-mono text-center">
            {hasAdmin ? 'Admin account already exists. Only USER can be created?' : 'Register as System Administrator'}
          </p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 mb-4 text-sm text-red-400 animate-shake">
            <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 mb-4 text-sm text-emerald-400">
            <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
              Username
            </label>
            <div className="relative">
              <User className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Enter username (min 3 chars)"
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl pl-11 pr-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-emerald-500/50 focus:ring-1 focus:ring-emerald-500/50 transition-all font-mono"
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
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Min 6 characters"
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl pl-11 pr-12 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-emerald-500/50 focus:ring-1 focus:ring-emerald-500/50 transition-all font-mono"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                tabIndex="-1"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
              Confirm Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Re-enter password"
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl pl-11 pr-12 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-emerald-500/50 focus:ring-1 focus:ring-emerald-500/50 transition-all font-mono"
                required
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                tabIndex="-1"
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
              Account Type
            </label>
            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="w-full bg-slate-950 border border-slate-800/80 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-emerald-500/50 focus:ring-1 focus:ring-emerald-500/50 transition-all font-mono"
            >
              <option value="ADMIN">🔑 ADMIN - Full Access</option>
              {/* ===== USER option - DISABLED if no admin exists ===== */}
              <option value="USER" disabled={!hasAdmin}>
                👤 USER - Limited Access {!hasAdmin && '(First account must be ADMIN)'}
              </option>
            </select>
            {!hasAdmin && (
              <p className="text-[10px] text-yellow-400 font-mono mt-1">
                ⚠️ First account must be ADMIN. USER role will be available after admin is created.
              </p>
            )}
            {hasAdmin && (
              <p className="text-[10px] text-slate-500 font-mono mt-1">
                ℹ️ Admin account exists. Only register as USER from this form.
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 active:scale-[0.99] disabled:opacity-50 text-white font-bold py-3 px-4 rounded-xl text-sm transition-all shadow-lg shadow-emerald-500/10 font-mono tracking-wider uppercase mt-2 flex items-center justify-center gap-2"
          >
            {loading ? (
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              'Create Account'
            )}
          </button>
        </form>

        <p className="text-center text-xs text-slate-500 mt-4 font-mono">
          Already have an account?{' '}
          <button
            type="button"
            onClick={onBackToLogin}
            className="text-emerald-400 hover:text-emerald-300 transition-colors font-bold underline-offset-2 hover:underline"
          >
            Sign In
          </button>
        </p>
      </div>
    </div>
  );
}

Register.propTypes = {
  onBackToLogin: PropTypes.func.isRequired,
};