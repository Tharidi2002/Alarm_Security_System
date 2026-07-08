import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { 
  X, UserPlus, ShieldAlert, Check, Plus, AlertCircle, Users, Cpu, 
  ToggleLeft, ToggleRight, Edit2, Trash2, Save, Eye, EyeOff,
  RefreshCw, Zap
} from 'lucide-react';
import { 
  fetchUsers, 
  createUser, 
  fetchSystems, 
  assignSystems,
  createSystem,
  updateSystem,
  toggleSystemStatus,
  deleteSystem
} from '../services/api';

export default function AdminPanel({ isOpen, onClose }) {
  const [activeTab, setActiveTab] = useState('USERS');
  const [users, setUsers] = useState([]);
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // User form states
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newRole, setNewRole] = useState('USER');
  const [showPassword, setShowPassword] = useState(false);

  // Assign states
  const [selectedUser, setSelectedUser] = useState(null);
  const [userAssignedIds, setUserAssignedIds] = useState([]);

  // System form states
  const [location, setLocation] = useState('');
  const [simNumber, setSimNumber] = useState('');
  const [editingSystem, setEditingSystem] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const [timeNow, setTimeNow] = useState(new Date());

  useEffect(() => {
    if (isOpen) {
      loadData();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, activeTab]);

  useEffect(() => {
    const interval = setInterval(() => setTimeNow(new Date()), 60000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [usersData, systemsData] = await Promise.all([
        fetchUsers(),
        fetchSystems()
      ]);
      setUsers(usersData);
      setSystems(systemsData);
    } catch {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  // ========== USER MANAGEMENT ==========
  const handleCreateUser = async (e) => {
    e.preventDefault();
    if (!newUsername.trim() || !newPassword.trim()) return;

    setError('');
    setSuccess('');
    try {
      await createUser({
        username: newUsername,
        password: newPassword,
        role: newRole
      });
      setSuccess('User registered successfully');
      setNewUsername('');
      setNewPassword('');
      setNewRole('USER');
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to create user');
    }
  };

  const handleSelectUserToAssign = (user) => {
    setSelectedUser(user);
    const assignedIds = user.assignedSystems.map(sys => sys.id);
    setUserAssignedIds(assignedIds);
  };

  const handleToggleSystem = (systemId) => {
    if (userAssignedIds.includes(systemId)) {
      setUserAssignedIds(userAssignedIds.filter(id => id !== systemId));
    } else {
      setUserAssignedIds([...userAssignedIds, systemId]);
    }
  };

  const handleSaveAssignments = async () => {
    if (!selectedUser) return;
    setError('');
    setSuccess('');
    try {
      await assignSystems(selectedUser.id, userAssignedIds);
      setSuccess(`Updated system access for ${selectedUser.username}`);
      setSelectedUser(null);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to save assignments');
    }
  };

  // ========== SYSTEM MANAGEMENT ==========
  const handleCreateSystem = async (e) => {
    e.preventDefault();
    if (!location.trim() || !simNumber.trim()) {
      setError('Location and SIM number are required');
      return;
    }

    setError('');
    setSuccess('');
    setIsGenerating(true);

    try {
      const systemData = {
        location: location.trim(),
        simNumber: simNumber.trim(),
        status: 'ACTIVE'
      };

      const result = await createSystem(systemData);
      setSuccess(`✅ System created: ${result.systemCode}`);
      setLocation('');
      setSimNumber('');
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to create system');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleStartEditSystem = (system) => {
    setEditingSystem(system);
    setLocation(system.location);
    setSimNumber(system.simNumber);
  };

  const handleUpdateSystem = async (e) => {
    e.preventDefault();
    if (!location.trim() || !simNumber.trim()) {
      setError('Location and SIM number are required');
      return;
    }

    setError('');
    setSuccess('');
    try {
      await updateSystem(editingSystem.id, {
        location: location.trim(),
        simNumber: simNumber.trim(),
        status: editingSystem.status
      });
      setSuccess(`✅ System ${editingSystem.systemCode} updated successfully`);
      setEditingSystem(null);
      setLocation('');
      setSimNumber('');
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to update system');
    }
  };

  const handleCancelEditSystem = () => {
    setEditingSystem(null);
    setLocation('');
    setSimNumber('');
  };

  const handleToggleStatus = async (system) => {
    setError('');
    setSuccess('');
    const newStatus = system.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await toggleSystemStatus(system.id, newStatus);
      setSuccess(`System ${system.systemCode} is now ${newStatus}`);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to change status');
    }
  };

  const handleDeleteSystem = async (systemId, systemCode) => {
    if (!window.confirm(`Are you sure you want to delete "${systemCode}"? This cannot be undone.`)) return;
    setError('');
    setSuccess('');
    try {
      await deleteSystem(systemId);
      setSuccess(`✅ System ${systemCode} deleted successfully`);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to delete system');
    }
  };

  // Format status duration
  const formatDuration = (timestamp) => {
    if (!timestamp) return 'No status changes recorded';
    const start = new Date(timestamp);
    const diffMs = Math.abs(timeNow - start);

    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    const years = Math.floor(diffDays / 365);
    const months = Math.floor((diffDays % 365) / 30);
    const days = diffDays % 30;
    const hours = diffHours % 24;
    const minutes = diffMins % 60;

    const parts = [];
    if (years > 0) parts.push(`${years} year${years > 1 ? 's' : ''}`);
    if (months > 0) parts.push(`${months} month${months > 1 ? 's' : ''}`);
    if (days > 0) parts.push(`${days} day${days > 1 ? 's' : ''}`);
    if (hours > 0) parts.push(`${hours} hour${hours > 1 ? 's' : ''}`);
    if (minutes > 0) parts.push(`${minutes} minute${minutes > 1 ? 's' : ''}`);

    return parts.length > 0 ? parts.join(', ') : 'just now';
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-end font-sans">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Panel Drawer */}
      <div className="w-full max-w-2xl h-full bg-slate-900 border-l border-slate-800 text-slate-100 flex flex-col relative z-10 shadow-2xl animate-slide-left">
        
        {/* Header */}
        <div className="p-6 border-b border-slate-800 bg-slate-950/40">
          <div className="flex justify-between items-center mb-4">
            <div className="flex items-center gap-2.5">
              <ShieldAlert className="w-5 h-5 text-red-500" />
              <h2 className="text-lg font-bold uppercase tracking-wider text-white">System Access Control</h2>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <X className="w-5 h-5 text-slate-400 hover:text-white" />
            </button>
          </div>

          {/* Navigation Tabs */}
          <div className="flex gap-2">
            <button
              onClick={() => { setActiveTab('USERS'); setError(''); setSuccess(''); }}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                activeTab === 'USERS' 
                  ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                  : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
              }`}
            >
              <Users className="w-4 h-4" /> Users Management
            </button>
            <button
              onClick={() => { setActiveTab('SYSTEMS'); setError(''); setSuccess(''); }}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                activeTab === 'SYSTEMS' 
                  ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                  : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
              }`}
            >
              <Cpu className="w-4 h-4" /> Systems / Devices
            </button>
          </div>
        </div>

        {/* Content (Scrollable) */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400">
              <Check className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {/* TAB 1: USERS MANAGEMENT */}
          {activeTab === 'USERS' && (
            <>
              {/* Create User Form */}
              <div className="bg-slate-950/40 border border-slate-800/80 rounded-2xl p-5 space-y-4">
                <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono flex items-center gap-2">
                  <UserPlus className="w-4 h-4 text-red-500" /> Register Security User
                </h3>
                
                <form onSubmit={handleCreateUser} className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-end">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">Username</label>
                    <input 
                      type="text"
                      value={newUsername}
                      onChange={(e) => setNewUsername(e.target.value)}
                      placeholder="user_name"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                      required
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">Password</label>
                    <div className="relative">
                      <input 
                        type={showPassword ? 'text' : 'password'}
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="••••••••"
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50 pr-10"
                        required
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">Role</label>
                    <select
                      value={newRole}
                      onChange={(e) => setNewRole(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-red-500/50"
                    >
                      <option value="USER">USER (Operator)</option>
                      <option value="ADMIN">ADMIN (Full Access)</option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    className="col-span-1 sm:col-span-3 w-full bg-slate-800 hover:bg-red-650 hover:text-white border border-slate-700 hover:border-red-500 font-bold py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all flex items-center justify-center gap-1.5"
                  >
                    <Plus className="w-3.5 h-3.5" /> Save User
                  </button>
                </form>
              </div>

              {/* User List */}
              <div className="space-y-3">
                <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">Security User Directory</h3>
                
                <div className="divide-y divide-slate-800/60 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/20">
                  {loading ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">Loading user directory...</div>
                  ) : users.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">No users registered</div>
                  ) : (
                    users.map((u) => (
                      <div key={u.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-900/10 transition-colors">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-bold text-sm text-white">{u.username}</span>
                            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                              u.role === 'ADMIN' 
                                ? 'bg-red-500/10 text-red-400 border-red-500/20' 
                                : 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                            }`}>
                              {u.role}
                            </span>
                          </div>
                          
                          {u.role === 'USER' && (
                            <div className="mt-1.5 flex flex-wrap gap-1">
                              {u.assignedSystems.length === 0 ? (
                                <span className="text-[10px] text-slate-500 font-mono">No systems assigned</span>
                              ) : (
                                u.assignedSystems.map((sys) => (
                                  <span key={sys.id} className="bg-slate-800 text-slate-300 font-mono text-[10px] px-1.5 py-0.5 rounded-md border border-slate-700">
                                    {sys.systemCode}
                                  </span>
                                ))
                              )}
                            </div>
                          )}
                        </div>

                        {u.role === 'USER' && (
                          <button
                            onClick={() => handleSelectUserToAssign(u)}
                            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-xs font-mono transition-all text-slate-300 hover:text-white flex-shrink-0"
                          >
                            Assign Systems
                          </button>
                        )}
                      </div>
                    ))
                  )}
                </div>
              </div>
            </>
          )}

          {/* TAB 2: SYSTEMS/DEVICES MANAGEMENT */}
          {activeTab === 'SYSTEMS' && (
            <>
              {/* Register / Edit System Form */}
              <div className="bg-slate-950/40 border border-slate-800/80 rounded-2xl p-5 space-y-4">
                <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono flex items-center gap-2">
                  <Cpu className="w-4 h-4 text-red-500" /> 
                  {editingSystem ? 'Modify Alarm System' : 'Register New Alarm System'}
                </h3>
                
                <form onSubmit={editingSystem ? handleUpdateSystem : handleCreateSystem} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">
                      System Code
                    </label>
                    <div className="relative">
                      <input 
                        type="text"
                        value={editingSystem ? editingSystem.systemCode : 'Auto-generated'}
                        disabled
                        className={`w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-slate-400 cursor-not-allowed ${
                          editingSystem ? 'opacity-80' : ''
                        }`}
                      />
                      {!editingSystem && (
                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                          <Zap className="w-4 h-4 text-emerald-500 animate-pulse" />
                        </div>
                      )}
                    </div>
                    <p className="text-[9px] text-slate-500 font-mono">
                      {editingSystem ? 'System code cannot be changed' : 'Auto-generated: ALARM-Z8B-XX'}
                    </p>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">SIM Card Number</label>
                    <input 
                      type="text"
                      value={simNumber}
                      onChange={(e) => setSimNumber(e.target.value)}
                      placeholder="e.g. 0771234567"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                      required
                    />
                  </div>

                  <div className="space-y-1.5 sm:col-span-2">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">Location</label>
                    <input 
                      type="text"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      placeholder="e.g. Colombo 03 - Main Street Branch"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                      required
                    />
                  </div>

                  <div className="sm:col-span-2 flex gap-2">
                    <button
                      type="submit"
                      disabled={isGenerating}
                      className="flex-1 bg-slate-800 hover:bg-red-650 hover:text-white border border-slate-700 hover:border-red-500 font-bold py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
                    >
                      {isGenerating ? (
                        <>
                          <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                          Generating...
                        </>
                      ) : (
                        <>
                          <Save className="w-3.5 h-3.5" /> 
                          {editingSystem ? 'Save Changes' : 'Register System'}
                        </>
                      )}
                    </button>
                    {editingSystem && (
                      <button
                        type="button"
                        onClick={handleCancelEditSystem}
                        className="px-4 py-2 border border-slate-700 hover:bg-slate-800 rounded-lg text-xs font-mono text-slate-400 hover:text-white transition-colors"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </form>
              </div>

              {/* Systems directory */}
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">Alarm Systems Directory</h3>
                  <button
                    onClick={loadData}
                    className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
                    title="Refresh systems"
                  >
                    <RefreshCw className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
                
                <div className="divide-y divide-slate-800/60 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/20">
                  {loading ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">Loading systems directory...</div>
                  ) : systems.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">
                      No systems registered. Create your first system above.
                    </div>
                  ) : (
                    systems.map((sys) => {
                      const isActive = sys.status === 'ACTIVE';
                      return (
                        <div key={sys.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-900/10 transition-colors">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="font-mono font-bold text-sm text-emerald-400">{sys.systemCode}</span>
                              <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                                isActive 
                                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                                  : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                              }`}>
                                {sys.status}
                              </span>
                            </div>
                            <div className="text-xs text-slate-400 mt-1">
                              Location: <span className="text-slate-300 font-medium">{sys.location}</span> • SIM: <span className="font-mono text-slate-300">{sys.simNumber}</span>
                            </div>
                            <div className="text-[10px] text-slate-500 mt-0.5 font-mono">
                              {isActive ? 'Active for: ' : 'Inactive for: '} 
                              <span className="text-red-400/90 font-bold">{formatDuration(sys.lastStatusChangedAt)}</span>
                            </div>
                          </div>

                          <div className="flex items-center gap-2 flex-shrink-0">
                            {/* Toggle Switch */}
                            <button
                              onClick={() => handleToggleStatus(sys)}
                              title={isActive ? 'Deactivate System' : 'Activate System'}
                              className={`p-1.5 rounded-lg border transition-all ${
                                isActive 
                                  ? 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-500 border-emerald-500/25' 
                                  : 'bg-slate-800 hover:bg-slate-700 text-slate-400 border-slate-700'
                              }`}
                            >
                              {isActive ? <ToggleRight className="w-5 h-5" /> : <ToggleLeft className="w-5 h-5" />}
                            </button>

                            {/* Edit Button */}
                            <button
                              onClick={() => handleStartEditSystem(sys)}
                              title="Edit System Info"
                              className="p-1.5 bg-slate-800 hover:bg-slate-750 text-slate-300 hover:text-white border border-slate-700 rounded-lg transition-all"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>

                            {/* Delete Button */}
                            <button
                              onClick={() => handleDeleteSystem(sys.id, sys.systemCode)}
                              title="Delete System"
                              className="p-1.5 bg-red-500/10 hover:bg-red-650 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg transition-all"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Assignment Modal dialog */}
      {selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" onClick={() => setSelectedUser(null)} />
          <div className="bg-slate-900 border border-slate-800 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden relative z-10 animate-scale-in">
            <div className="p-5 border-b border-slate-800 flex justify-between items-center bg-slate-950/40">
              <h3 className="font-bold text-sm uppercase tracking-wider text-white font-mono">
                Assign System Access: {selectedUser.username}
              </h3>
              <button onClick={() => setSelectedUser(null)} className="p-1 hover:bg-slate-800 rounded-lg">
                <X className="w-4 h-4 text-slate-400" />
              </button>
            </div>
            
            <div className="p-5 space-y-4 max-h-[300px] overflow-y-auto">
              <p className="text-xs text-slate-400">Select which alarm systems this user can monitor:</p>
              <div className="space-y-2">
                {systems.map((sys) => {
                  const isChecked = userAssignedIds.includes(sys.id);
                  return (
                    <div 
                      key={sys.id}
                      onClick={() => handleToggleSystem(sys.id)}
                      className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-all ${
                        isChecked 
                          ? 'bg-red-500/10 border-red-500/30 text-white' 
                          : 'bg-slate-950 border-slate-800/80 text-slate-400 hover:border-slate-700'
                      }`}
                    >
                      <div>
                        <div className="font-mono text-sm font-bold">{sys.systemCode}</div>
                        <div className="text-[10px] text-slate-500">{sys.location}</div>
                      </div>
                      <div className={`w-5 h-5 rounded-md border flex items-center justify-center transition-all ${
                        isChecked ? 'bg-red-650 border-red-500 text-white' : 'border-slate-700'
                      }`}>
                        {isChecked && <Check className="w-3.5 h-3.5" />}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="p-5 border-t border-slate-800 bg-slate-950/20 flex gap-3">
              <button
                onClick={() => setSelectedUser(null)}
                className="flex-1 py-2 border border-slate-700 text-slate-400 hover:text-white rounded-lg text-xs font-mono transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveAssignments}
                className="flex-1 py-2 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold rounded-lg text-xs font-mono transition-all uppercase tracking-wide"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ========== PROPTYPES VALIDATION ==========
AdminPanel.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};