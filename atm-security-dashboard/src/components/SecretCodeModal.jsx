import { useState } from 'react';
import PropTypes from 'prop-types';
import { X, Key, AlertCircle, CheckCircle, Lock, Clock } from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function SecretCodeModal({ isOpen, onClose, onVerified }) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [remainingAttempts, setRemainingAttempts] = useState(5);
  const [lockedOut, setLockedOut] = useState(false);
  const [remainingMinutes, setRemainingMinutes] = useState(0);
  const [showCode, setShowCode] = useState(false);

  if (!isOpen) return null;

  const handleVerify = async () => {
    if (!code.trim()) {
      setError('Please enter the secret code');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/auth/verify-secret`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ secretCode: code.trim() })
      });

      const data = await response.json();

      if (response.ok && data.valid) {
        setSuccess('✅ Code verified! Registration unlocked.');
        setRemainingAttempts(5);
        
        // Wait and then navigate to register
        setTimeout(() => {
          if (onVerified) onVerified();
          onClose();
        }, 1500);
      } else {
        setError(data.message || 'Invalid code');
        if (data.remainingAttempts !== undefined) {
          setRemainingAttempts(data.remainingAttempts);
        }
        if (data.lockedOut) {
          setLockedOut(true);
          setRemainingMinutes(data.remainingMinutes || 30);
        }
        // Clear code on wrong attempt
        setCode('');
        // Focus input
        setTimeout(() => {
          document.querySelector('input[type="password"]')?.focus();
        }, 100);
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setCode('');
    setError('');
    setSuccess('');
    setLoading(false);
    setRemainingAttempts(5);
    setLockedOut(false);
    setRemainingMinutes(0);
    onClose();
  };

  const getRemainingText = () => {
    if (lockedOut) {
      return `⛔ Too many attempts. Try again in ${remainingMinutes} minutes.`;
    }
    if (remainingAttempts <= 2 && remainingAttempts > 0) {
      return `⚠️ ${remainingAttempts} attempts remaining`;
    }
    if (remainingAttempts === 0) {
      return '⛔ No attempts remaining. Please wait.';
    }
    return null;
  };

  return (
    <div 
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={handleClose}
    >
      <div 
        className="bg-slate-900 border border-yellow-500/30 rounded-2xl max-w-md w-full shadow-2xl shadow-yellow-500/10"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex justify-between items-center p-5 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="bg-yellow-500/10 p-2 rounded-lg border border-yellow-500/20">
              <Key className="w-5 h-5 text-yellow-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">🔑 Developer Access</h3>
              <p className="text-xs text-slate-400 font-mono">Unlock Admin Registration</p>
            </div>
          </div>
          <button 
            onClick={handleClose}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-4">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
            <p className="text-sm text-slate-300">
              Enter the <span className="text-yellow-400 font-bold">Master Secret Code</span> to unlock admin registration.
            </p>
            <p className="text-[10px] text-slate-500 font-mono mt-1">
              Contact system administrator to get the code.
            </p>
          </div>

          {/* Code Input */}
          <div className="space-y-2">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono flex items-center gap-2">
              <Lock className="w-3.5 h-3.5" />
              Secret Code
            </label>
            <div className="relative">
              <input
                type={showCode ? 'text' : 'password'}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !loading && !lockedOut && remainingAttempts > 0) {
                    handleVerify();
                  }
                }}
                placeholder="Enter secret code..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-yellow-500/50 focus:ring-1 focus:ring-yellow-500/50 transition-all font-mono"
                disabled={loading || lockedOut || remainingAttempts <= 0}
                autoFocus
              />
              <button
                type="button"
                onClick={() => setShowCode(!showCode)}
                className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
              >
                {showCode ? (
                  <Lock className="w-4 h-4" />
                ) : (
                  <Key className="w-4 h-4" />
                )}
              </button>
            </div>
          </div>

          {/* Error/Success Messages */}
          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400">
              <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {/* Remaining Attempts Warning */}
          {getRemainingText() && !error && !success && (
            <div className={`rounded-xl p-3 flex items-start gap-2.5 text-sm ${
              lockedOut || remainingAttempts === 0
                ? 'bg-red-500/10 border border-red-500/30 text-red-400'
                : 'bg-yellow-500/10 border border-yellow-500/30 text-yellow-400'
            }`}>
              {lockedOut || remainingAttempts === 0 ? (
                <Clock className="w-4 h-4 mt-0.5 flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              )}
              <span>{getRemainingText()}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2 border-t border-slate-800">
            <button
              onClick={handleClose}
              className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleVerify}
              disabled={loading || lockedOut || remainingAttempts <= 0}
              className="flex-1 py-2.5 bg-gradient-to-r from-yellow-600 to-yellow-700 hover:from-yellow-500 hover:to-yellow-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <Key className="w-4 h-4" />
                  Verify Code
                </>
              )}
            </button>
          </div>

          {/* Hint */}
          <div className="text-center text-[9px] text-slate-600 font-mono">
            ⚠️ This is a secure feature. Unauthorized access is logged.
          </div>
        </div>
      </div>
    </div>
  );
}

SecretCodeModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onVerified: PropTypes.func.isRequired,
};