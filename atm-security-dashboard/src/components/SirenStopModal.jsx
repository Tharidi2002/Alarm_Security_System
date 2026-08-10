import { useState } from 'react';
import PropTypes from 'prop-types';
import { X, BellOff, AlertCircle, User, FileText, AlertTriangle, CheckCircle } from 'lucide-react';
import { stopSiren } from '../services/api';

export default function SirenStopModal({ systemCode, location, isOpen, onClose, onSirenStopped, username }) {
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  if (!isOpen || !systemCode) return null;

  const handleStopSirenSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const res = await stopSiren(systemCode, username || 'DASHBOARD', description.trim() || undefined, username);
      if (res.success) {
        setSuccess('✅ Siren stopped successfully. Reason saved.');
        setTimeout(() => {
          if (onSirenStopped) {
            onSirenStopped(res);
          }
          if (onClose) {
            onClose();
          }
        }, 1500);
      } else {
        setError(res.message || 'Failed to stop siren');
      }
    } catch (err) {
      setError(err.message || 'An error occurred while stopping siren');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (onClose) onClose();
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-amber-500/30 rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-amber-500/10">
        
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center gap-3">
            <div className="bg-amber-500/20 p-2.5 rounded-xl border border-amber-500/30">
              <BellOff className="w-6 h-6 text-amber-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">Stop Siren Only</h2>
              <p className="text-xs text-amber-400 font-mono">System: {systemCode}</p>
            </div>
          </div>
          <button 
            onClick={handleClose}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleStopSirenSubmit} className="p-6 space-y-4">
          
          {/* Warning Banner */}
          <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-3.5 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
            <div className="text-xs text-amber-300 font-mono leading-relaxed">
              <span className="font-bold block text-amber-400">Notice:</span>
              This action only turns off the physical siren sound. <strong className="underline">Pending alerts will NOT be resolved</strong> and remain active.
            </div>
          </div>

          {/* System Info Summary */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2 text-xs">
            <div className="flex justify-between items-center">
              <span className="text-slate-400 font-mono">System Code</span>
              <span className="font-mono text-white font-bold">{systemCode}</span>
            </div>
            {location && (
              <div className="flex justify-between items-center">
                <span className="text-slate-400 font-mono">Location</span>
                <span className="text-slate-300">{location}</span>
              </div>
            )}
            <div className="flex justify-between items-center pt-2 border-t border-slate-800/80">
              <span className="text-slate-400 font-mono">Operator</span>
              <span className="text-emerald-400 font-mono font-bold flex items-center gap-1">
                <User className="w-3.5 h-3.5" />
                {username || 'DASHBOARD'}
              </span>
            </div>
          </div>

          {/* Reason / Description Input */}
          <div className="space-y-2">
            <label className="text-xs font-bold tracking-wide uppercase text-slate-300 font-mono flex items-center gap-2">
              <FileText className="w-4 h-4 text-amber-400" />
              Reason for Stopping Siren <span className="text-red-400">*</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Enter reason for stopping the siren (e.g. False alarm verified, Maintenance check)..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-amber-500/50 focus:ring-1 focus:ring-amber-500/50 transition-all font-mono min-h-[90px] resize-none"
              rows={3}
              required
            />
            <p className="text-[10px] text-slate-500 font-mono">
              This description along with your username and timestamp will be saved for admin review.
            </p>
          </div>

          {/* Messages */}
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

          {/* Buttons */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white hover:border-slate-600 rounded-xl text-sm font-mono transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || !description.trim()}
              className="flex-1 py-2.5 bg-amber-600 hover:bg-amber-500 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-amber-600/20"
            >
              {loading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <BellOff className="w-4 h-4" />
                  Stop Siren Only
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

SirenStopModal.propTypes = {
  systemCode: PropTypes.string,
  location: PropTypes.string,
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onSirenStopped: PropTypes.func,
  username: PropTypes.string,
};
