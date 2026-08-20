// src/components/AdminManagementModal.jsx
import { useState } from 'react';
import PropTypes from 'prop-types';
import { 
  X, Shield, Key, ToggleRight, ToggleLeft, 
  Trash2, AlertCircle, CheckCircle, Lock,
  Eye, EyeOff, Info
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function AdminManagementModal({ 
  isOpen, 
  onClose, 
  admins, 
  currentUsername,
  isSuperAdmin,
  onAdminUpdated 
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [selectedAdmin, setSelectedAdmin] = useState(null);
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [processingId, setProcessingId] = useState(null);

  if (!isOpen) return null;

  // ============================================================
  // TOGGLE ADMIN STATUS
  // ============================================================
  
  const handleToggleStatus = async (adminId, adminUsername, currentStatus) => {
    if (!window.confirm(`Are you sure you want to ${currentStatus ? 'deactivate' : 'activate'} admin "${adminUsername}"?`)) return;
    
    setProcessingId(adminId);
    setError('');
    setSuccess('');
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${adminId}/toggle-status?currentUsername=${encodeURIComponent(currentUsername)}`, {
        method: 'PATCH'
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        if (onAdminUpdated) onAdminUpdated();
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to toggle admin status');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to toggle admin status');
    } finally {
      setProcessingId(null);
    }
  };

  // ============================================================
  // DELETE ADMIN
  // ============================================================
  
  const handleDeleteAdmin = async (adminId, adminUsername) => {
    if (!window.confirm(`⚠️ Are you sure you want to PERMANENTLY delete admin "${adminUsername}"?\n\nThis action CANNOT be undone!`)) return;
    
    setProcessingId(adminId);
    setError('');
    setSuccess('');
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${adminId}?currentUsername=${encodeURIComponent(currentUsername)}`, {
        method: 'DELETE'
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        if (onAdminUpdated) onAdminUpdated();
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to delete admin');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to delete admin');
    } finally {
      setProcessingId(null);
    }
  };

  // ============================================================
  // RESET ADMIN PASSWORD
  // ============================================================
  
  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    
    if (!newPassword.trim() || newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${selectedAdmin.id}/reset-password?currentUsername=${encodeURIComponent(currentUsername)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newPassword: newPassword.trim() })
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        setShowResetPassword(false);
        setSelectedAdmin(null);
        setNewPassword('');
        if (onAdminUpdated) onAdminUpdated();
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to reset password');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  const openResetPassword = (admin) => {
    setSelectedAdmin(admin);
    setNewPassword('');
    setShowResetPassword(true);
    setError('');
    setSuccess('');
  };

  const closeResetPassword = () => {
    setShowResetPassword(false);
    setSelectedAdmin(null);
    setNewPassword('');
    setError('');
    setSuccess('');
  };

  // ============================================================
  // FILTER ADMINS
  // ============================================================
  
  // Current user (always shown)
  const currentUser = admins.find(a => a.username === currentUsername);
  
  // Manageable admins (ADMIN_PANEL)
  const manageableAdmins = admins.filter(a => {
    if (a.username === currentUsername) return false;
    if (a.registrationMethod !== 'ADMIN_PANEL') return false;
    return true;
  });

  // Protected admins (FORM - cannot be managed)
  const protectedAdmins = admins.filter(a => {
    if (a.username === currentUsername) return false;
    if (a.registrationMethod === 'FORM') return true;
    return false;
  });

  // ============================================================
  // RENDER
  // ============================================================
  
  return (
    <div className="fixed inset-0 z-[300] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-purple-500/30 rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-purple-500/10">
        
        {/* Header */}
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center gap-3">
            <div className="bg-purple-500/10 p-2 rounded-lg border border-purple-500/20">
              <Shield className="w-6 h-6 text-purple-400" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">🔑 Admin Management</h2>
              <p className="text-xs text-slate-400 font-mono">
                {manageableAdmins.length} manageable admins • {protectedAdmins.length} protected admins
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 overflow-y-auto max-h-[calc(90vh-120px)]">
          
          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400 mb-4">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}
          
          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400 mb-4">
              <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {/* Security Notice */}
          <div className="bg-purple-500/10 border border-purple-500/30 rounded-xl p-3 mb-4">
            <p className="text-xs text-purple-400 font-mono flex items-center gap-2">
              🔒 <span>You can only manage Operational Admins (ADMIN_PANEL). Super Admins (FORM) are protected.</span>
            </p>
          </div>

          {/* ===== CURRENT USER ===== */}
          {currentUser && (
            <>
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                <span>👤 Current User</span>
              </h3>
              <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-3 mb-4">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="font-mono font-bold text-white">{currentUser.username}</span>
                    <span className={`ml-2 text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                      currentUser.registrationMethod === 'FORM'
                        ? 'bg-purple-500/20 text-purple-400 border-purple-500/30'
                        : 'bg-slate-600/20 text-slate-400 border-slate-600/30'
                    }`}>
                      {currentUser.registrationMethod === 'FORM' ? '🔑 Super Admin' : '🛠️ Ops Admin'}
                    </span>
                  </div>
                  <span className="text-[10px] text-emerald-400">🟢 You</span>
                </div>
              </div>
            </>
          )}

          {/* ===== MANAGEABLE ADMINS (ADMIN_PANEL) ===== */}
          {manageableAdmins.length > 0 && (
            <>
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                <span>🛠️ Operational Admins (Manageable)</span>
                <span className="text-[10px] text-slate-500">({manageableAdmins.length})</span>
              </h3>
              <div className="space-y-3 mb-6">
                {manageableAdmins.map((admin) => {
                  const isAdminActive = admin.isActive !== false;
                  const isProcessing = processingId === admin.id;
                  
                  return (
                    <div 
                      key={admin.id}
                      className={`bg-slate-950 border rounded-xl p-4 transition-all ${
                        isAdminActive 
                          ? 'border-slate-800 hover:border-slate-700' 
                          : 'border-red-500/20 opacity-60'
                      }`}
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-mono font-bold text-sm text-white">
                              {admin.username}
                            </span>
                            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                              isAdminActive 
                                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                                : 'bg-red-500/10 text-red-400 border-red-500/20'
                            }`}>
                              {isAdminActive ? '🟢 Active' : '🔴 Inactive'}
                            </span>
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-600/20 text-slate-400 border border-slate-600/30">
                              🛠️ Ops Admin
                            </span>
                          </div>
                          <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-3 flex-wrap">
                            <span>ID: {admin.id}</span>
                            <span>Registration: {admin.registrationMethod || 'Unknown'}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 flex-shrink-0 flex-wrap">
                          <button
                            onClick={() => openResetPassword(admin)}
                            disabled={isProcessing}
                            className="px-3 py-1.5 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 border border-yellow-500/30 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1.5 disabled:opacity-50"
                            title="Reset password"
                          >
                            <Key className="w-3.5 h-3.5" />
                            Reset
                          </button>
                          <button
                            onClick={() => handleToggleStatus(admin.id, admin.username, isAdminActive)}
                            disabled={isProcessing}
                            className={`px-3 py-1.5 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1.5 border ${
                              isAdminActive
                                ? 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                                : 'bg-red-500/10 hover:bg-red-500/20 text-red-400 border-red-500/30'
                            } disabled:opacity-50`}
                            title={isAdminActive ? 'Deactivate' : 'Activate'}
                          >
                            {isAdminActive ? <ToggleRight className="w-3.5 h-3.5" /> : <ToggleLeft className="w-3.5 h-3.5" />}
                            {isAdminActive ? 'Active' : 'Inactive'}
                          </button>
                          <button
                            onClick={() => handleDeleteAdmin(admin.id, admin.username)}
                            disabled={isProcessing}
                            className="px-3 py-1.5 bg-red-500/10 hover:bg-red-600 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1.5 disabled:opacity-50"
                            title="Delete admin"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            Delete
                          </button>
                          {isProcessing && (
                            <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          )}

          {/* ===== PROTECTED ADMINS (FORM) ===== */}
          {protectedAdmins.length > 0 && (
            <>
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                <span>🔑 Super Admins (Protected)</span>
                <span className="text-[10px] text-slate-500">({protectedAdmins.length})</span>
              </h3>
              <div className="space-y-3">
                {protectedAdmins.map((admin) => (
                  <div 
                    key={admin.id}
                    className="bg-slate-950 border border-purple-500/20 rounded-xl p-4 opacity-70"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-mono font-bold text-sm text-purple-400">
                            {admin.username}
                          </span>
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded-full border bg-purple-500/20 text-purple-400 border-purple-500/30">
                            🔑 Super Admin
                          </span>
                          <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                            admin.isActive !== false
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                              : 'bg-red-500/10 text-red-400 border-red-500/20'
                          }`}>
                            {admin.isActive !== false ? '🟢 Active' : '🔴 Inactive'}
                          </span>
                          {admin.isLastSuperAdmin && (
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-yellow-500/20 text-yellow-400 border border-yellow-500/30">
                              ⭐ Last Super Admin
                            </span>
                          )}
                        </div>
                        <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-3 flex-wrap">
                          <span>ID: {admin.id}</span>
                          <span>Registration: {admin.registrationMethod || 'Unknown'}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 flex-shrink-0">
                        <span className="px-3 py-1.5 bg-slate-800 text-slate-500 rounded-lg text-[10px] font-mono flex items-center gap-1.5 border border-slate-700 cursor-not-allowed" 
                              title="Super Admins cannot be managed">
                          <Lock className="w-3.5 h-3.5" />
                          Protected
                        </span>
                        {admin.isLastSuperAdmin && (
                          <span className="text-[10px] text-yellow-400 font-mono">⚠️ Cannot delete last Super Admin</span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

          {manageableAdmins.length === 0 && protectedAdmins.length === 0 && (
            <div className="text-center py-12">
              <Shield className="w-16 h-16 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400 font-mono text-sm">No other admin accounts found</p>
              <p className="text-xs text-slate-500 mt-1">You are the only admin in the system</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/20 flex justify-between items-center">
          <span className="text-[10px] text-slate-500 font-mono">
            Total Admins: {admins.length} • You: {currentUsername}
          </span>
          <button
            onClick={onClose}
            className="px-6 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg text-sm font-mono transition-colors"
          >
            Close
          </button>
        </div>
      </div>

      {/* ===== RESET PASSWORD MODAL ===== */}
      {showResetPassword && selectedAdmin && (
        <div className="fixed inset-0 z-[400] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
          <div className="bg-slate-900 border border-yellow-500/30 rounded-2xl max-w-md w-full shadow-2xl shadow-yellow-500/10">
            
            <div className="flex justify-between items-center p-5 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <Key className="w-5 h-5 text-yellow-500" />
                <h3 className="text-lg font-bold text-white">Reset Admin Password</h3>
              </div>
              <button 
                onClick={closeResetPassword}
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
              >
                <X className="w-5 h-5 text-slate-400 hover:text-white" />
              </button>
            </div>

            <form onSubmit={handleResetPasswordSubmit} className="p-5 space-y-4">
              <div>
                <p className="text-sm text-slate-400 mb-1">
                  Resetting password for: 
                  <span className="text-white font-bold ml-1">{selectedAdmin.username}</span>
                </p>
                <p className="text-xs text-slate-500">
                  Role: <span className="text-purple-400">ADMIN</span>
                  <span className="ml-2 text-slate-500">(Operational Admin)</span>
                </p>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
                  New Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Min 6 characters"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-11 pr-12 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-yellow-500/50 focus:ring-1 focus:ring-yellow-500/50 transition-all font-mono"
                    required
                    minLength={6}
                    autoFocus
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <p className="text-[10px] text-slate-500 font-mono">
                  Password must be at least 6 characters
                </p>
              </div>

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

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeResetPassword}
                  className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 py-2.5 bg-gradient-to-r from-yellow-600 to-yellow-700 hover:from-yellow-500 hover:to-yellow-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {loading ? (
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Key className="w-4 h-4" />
                      Reset Password
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

AdminManagementModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  admins: PropTypes.array.isRequired,
  currentUsername: PropTypes.string.isRequired,
  isSuperAdmin: PropTypes.bool.isRequired,
  onAdminUpdated: PropTypes.func,
};