import { useState, useEffect } from 'react';
import { Shield, User, Lock, Eye, EyeOff, AlertCircle, CheckCircle, ArrowLeft, Key, Unlock, Building } from 'lucide-react';
import PropTypes from 'prop-types';
import { fetchCompanies } from '../services/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function Register({ onBackToLogin, isUnlocked = false, onRegisterSuccess }) {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    role: 'ADMIN',
    secretCode: '',
    companyId: '' // NEW
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [showSecretCode, setShowSecretCode] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasAdmin, setHasAdmin] = useState(true);
  const [adminCount, setAdminCount] = useState(0);
  const [unlocked, setUnlocked] = useState(isUnlocked);
  const [checkingAdmin, setCheckingAdmin] = useState(true);
  
  // NEW: Companies list for dropdown
  const [companies, setCompanies] = useState([]);
  const [loadingCompanies, setLoadingCompanies] = useState(false);

  useEffect(() => {
    const checkAdmin = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/check-admin`);
        if (response.ok) {
          const data = await response.json();
          setHasAdmin(data.hasAdmin);
          setAdminCount(data.adminCount || 0);
          if (!isUnlocked) {
            setUnlocked(data.isUnlocked || false);
          }
        } else {
          setHasAdmin(true);
          setAdminCount(1);
        }
      } catch (error) {
        console.error('Error checking admin status:', error);
        setHasAdmin(true);
        setAdminCount(1);
      } finally {
        setCheckingAdmin(false);
      }
    };
    checkAdmin();
  }, [isUnlocked]);

  // NEW: Load companies when user selects USER role
  useEffect(() => {
    if (formData.role === 'USER') {
      loadCompanies();
    }
  }, [formData.role]);

  const loadCompanies = async () => {
    setLoadingCompanies(true);
    try {
      const data = await fetchCompanies();
      setCompanies(data || []);
    } catch (error) {
      console.error('Error loading companies:', error);
      setCompanies([]);
    } finally {
      setLoadingCompanies(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    const { username, password, confirmPassword, role, secretCode, companyId } = formData;

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

    // NEW: Validate company for USER role
    if (role === 'USER') {
      if (!companyId || companyId === '') {
        setError('Please select a company');
        return;
      }
    }

    if (role === 'ADMIN' && hasAdmin) {
      if (!secretCode.trim()) {
        setError('Secret code is required to register as Admin');
        return;
      }
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
          role,
          secretCode: secretCode.trim(),
          companyId: companyId // NEW
        }),
      });

      const data = await response.text();

      if (!response.ok) {
        try {
          const jsonData = JSON.parse(data);
          if (jsonData.error) {
            throw new Error(jsonData.error);
          }
          throw new Error(data);
        } catch (parseError) {
          throw new Error(parseError.message || data || 'Registration failed');
        }
      }

      setSuccess('✅ Registration successful! Redirecting to login...');
      setFormData({
        username: '',
        password: '',
        confirmPassword: '',
        role: 'ADMIN',
        secretCode: '',
        companyId: ''
      });
      localStorage.setItem('registerSuccess', '✅ Registration successful! You can now login.');

      setTimeout(() => {
        if (onRegisterSuccess) onRegisterSuccess();
        if (onBackToLogin) onBackToLogin();
      }, 2000);
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const isFormLocked = () => {
    if (checkingAdmin) return false;
    if (!hasAdmin) return false;
    if (hasAdmin && !unlocked) return true;
    return false;
  };

  if (isFormLocked()) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 relative overflow-hidden font-sans">
        <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800/80 rounded-2xl shadow-2xl p-8 text-center">
          <div className="bg-yellow-500/10 p-4 rounded-full border border-yellow-500/20 mb-4 mx-auto w-fit">
            <Lock className="w-10 h-10 text-yellow-500" />
          </div>
          <h2 className="text-xl font-bold text-white">🔒 Admin Registration Locked</h2>
          <p className="text-sm text-slate-400 mt-2">
            Admin account already exists. Registration is locked.
          </p>
          <p className="text-xs text-slate-500 font-mono mt-1">
            Go to login page and use the key icon to unlock.
          </p>
          <button
            onClick={onBackToLogin}
            className="mt-4 px-6 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-sm font-mono transition-colors"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4 relative overflow-hidden font-sans">
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-emerald-500/10 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-emerald-500/5 rounded-full blur-[120px]" />

      <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800/80 rounded-2xl shadow-2xl p-8 relative z-10 animate-fade-in">
        <button
          onClick={onBackToLogin}
          className="absolute top-4 left-4 p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white"
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
            {!hasAdmin ? 'Register as System Administrator' : 'Register as User'}
          </p>
          {unlocked && (
            <div className="mt-2 bg-yellow-500/10 border border-yellow-500/30 rounded-lg px-3 py-1 text-xs text-yellow-400 font-mono flex items-center gap-2">
              <Unlock className="w-3 h-3" />
              🔓 Admin Registration Unlocked
            </div>
          )}
          {hasAdmin && (
            <p className="text-xs text-slate-500 font-mono mt-1">
              Total Admins: {adminCount}
            </p>
          )}
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 mb-4 text-sm text-red-400">
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
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* NEW: Company Selection for USER role */}
          {formData.role === 'USER' && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono flex items-center gap-2">
                <Building className="w-3.5 h-3.5 text-blue-400" />
                Company <span className="text-red-400">*</span>
              </label>
              <select
                name="companyId"
                value={formData.companyId}
                onChange={handleChange}
                className="w-full bg-slate-950 border border-slate-800/80 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-emerald-500/50 focus:ring-1 focus:ring-emerald-500/50 transition-all font-mono"
                required
              >
                <option value="">-- Select Company --</option>
                {loadingCompanies ? (
                  <option value="" disabled>Loading companies...</option>
                ) : (
                  companies.map((company) => (
                    <option key={company.id} value={company.id}>
                      {company.companyCode} - {company.companyName}
                    </option>
                  ))
                )}
              </select>
              {companies.length === 0 && !loadingCompanies && (
                <p className="text-[10px] text-yellow-400 font-mono">
                  ⚠️ No companies available. Please contact Admin to create a company.
                </p>
              )}
            </div>
          )}

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
              <option value="USER" disabled={!hasAdmin}>
                👤 USER - Limited Access {!hasAdmin && '(First account must be ADMIN)'}
              </option>
            </select>
            {!hasAdmin && (
              <p className="text-[10px] text-yellow-400 font-mono mt-1">
                ⚠️ First account must be ADMIN. Create admin account first.
              </p>
            )}
            {hasAdmin && unlocked && (
              <p className="text-[10px] text-yellow-400 font-mono mt-1">
                🔓 Admin registration unlocked. You can create additional admin accounts.
              </p>
            )}
          </div>

          {(formData.role === 'ADMIN' && hasAdmin) && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono flex items-center gap-2">
                <Key className="w-3.5 h-3.5 text-yellow-400" />
                Secret Code (Required for Admin)
              </label>
              <div className="relative">
                <input
                  type={showSecretCode ? 'text' : 'password'}
                  name="secretCode"
                  value={formData.secretCode}
                  onChange={handleChange}
                  placeholder="Enter master secret code..."
                  className="w-full bg-slate-950 border border-yellow-500/30 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-yellow-500/50 focus:ring-1 focus:ring-yellow-500/50 transition-all font-mono"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowSecretCode(!showSecretCode)}
                  className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                >
                  {showSecretCode ? <Lock className="w-4 h-4" /> : <Key className="w-4 h-4" />}
                </button>
              </div>
              <p className="text-[10px] text-slate-500 font-mono">
                Contact system administrator to get the registration code.
              </p>
            </div>
          )}

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
  isUnlocked: PropTypes.bool,
  onRegisterSuccess: PropTypes.func,
};