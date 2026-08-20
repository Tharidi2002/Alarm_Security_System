import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
  X, UserPlus, ShieldAlert, Check, Plus, AlertCircle, Users, Cpu, 
  ToggleLeft, ToggleRight, Edit2, Trash2, Save, Eye, EyeOff,
  RefreshCw, Zap, Copy, CheckCircle as CheckCircleIcon,
  Key, Lock, Layers, Trash, Search, Smartphone, Settings,
  BellOff, ShieldOff, Building, Database, Shield
} from 'lucide-react';
import { 
  fetchUsers, 
  createUser, 
  fetchSystems, 
  fetchCompanies,
  assignSystems,
  createSystem,
  updateSystem,
  toggleSystemStatus,
  resetUserPassword,
  deleteUser,
  sendSystemCommand,
  stopSiren,
  getNextSystemCode
} from '../services/api';
import ZoneManagement from './ZoneManagement';
import CompanyManagement from './CompanyManagement';
import DeleteConfirmationModal from './DeleteConfirmationModal';
import ArchivedSystemsInline from './ArchivedSystemsInline';
import SirenStopModal from './SirenStopModal';
import CompanyProfile from './CompanyProfile';
import DeletedSystemsPanel from './DeletedSystemsPanel';
import AdminManagementModal from './AdminManagementModal';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function AdminPanel({ isOpen, onClose, user, onSystemChange }) {
  const [activeTab, setActiveTab] = useState('USERS');
  const [users, setUsers] = useState([]);
  const [systems, setSystems] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Add new state for admin permissions
  const [adminPermissions, setAdminPermissions] = useState(null);
  const [showAdminManagement, setShowAdminManagement] = useState(false);

  // User form states
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newRole, setNewRole] = useState('USER');
  const [newCompanyId, setNewCompanyId] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // ===== FIX: ADD userSearchQuery STATE =====
  const [userSearchQuery, setUserSearchQuery] = useState('');

  // Assign states
  const [selectedUser, setSelectedUser] = useState(null);
  const [userAssignedIds, setUserAssignedIds] = useState([]);

  // System form states
  const [location, setLocation] = useState('');
  const [description, setDescription] = useState('');
  const [simNumber, setSimNumber] = useState('');
  const [panelSimNumber, setPanelSimNumber] = useState('');
  const [disarmCommand, setDisarmCommand] = useState('8888#2A');
  const [armCommand, setArmCommand] = useState('8888#1A');
  const [selectedCompanyId, setSelectedCompanyId] = useState('');
  const [editingSystem, setEditingSystem] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedCode, setGeneratedCode] = useState('');
  const [copied, setCopied] = useState(false);

  // Reset Password states
  const [resetUser, setResetUser] = useState(null);
  const [resetNewPassword, setResetNewPassword] = useState('');
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);

  // Zone Management states
  const [zoneManagementOpen, setZoneManagementOpen] = useState(false);
  const [selectedSystemId, setSelectedSystemId] = useState(null);
  const [selectedSystemCode, setSelectedSystemCode] = useState('');

  // Delete confirmation / archived view states
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [systemToDelete, setSystemToDelete] = useState(null);

  // Siren stop modal state
  const [sirenStopSystem, setSirenStopSystem] = useState(null);

  // Deleted Systems Panel state
  const [showDeletedSystems, setShowDeletedSystems] = useState(false);

  const [timeNow, setTimeNow] = useState(new Date());
  
  useEffect(() => {
    const interval = setInterval(() => setTimeNow(new Date()), 60000);
    return () => clearInterval(interval);
  }, []);

  
  // Get next system code from backend (Global)
  const fetchLatestSystemCode = useCallback(async () => {
    try {
      const data = await getNextSystemCode();
      setGeneratedCode(data.nextCode);
      console.log('✅ Next system code:', data.nextCode);
    } catch (error) {
      console.error('Error fetching system code:', error);
      setGeneratedCode('ALARM-Z8B-01');
    }
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const userCompanyId = user?.companyId || user?.company?.id || user?.company?.companyId || null;
      
      // ===== FIX: Wrap API calls with try-catch for network errors =====
      let usersData = [];
      let systemsDataRaw = [];
      let companiesDataRaw = [];

      try {
        usersData = await fetchUsers(user?.role === 'USER' ? userCompanyId : null, user?.username);
      } catch (err) {
        console.error('Failed to fetch users:', err);
        // Don't set error here, continue with empty data
      }

      try {
        systemsDataRaw = await fetchSystems(user?.role === 'USER' ? userCompanyId : null, user?.username);
      } catch (err) {
        console.error('Failed to fetch systems:', err);
      }

      try {
        companiesDataRaw = await fetchCompanies(user?.username);
      } catch (err) {
        console.error('Failed to fetch companies:', err);
      }

      // Client-side safeguard: if current user is a USER, only show data for their company
      const systemsData = (() => {
        if (!(systemsDataRaw || []).length) return [];
        if (user?.role === 'USER' && userCompanyId) {
          return (systemsDataRaw || []).filter(s => {
            const systemCompanyId = s?.company?.id || s?.companyId || s?.company?.companyId || null;
            return String(systemCompanyId) === String(userCompanyId);
          });
        }
        return (systemsDataRaw || []);
      })();

      const companiesData = user?.role === 'USER' && userCompanyId
        ? (companiesDataRaw || []).filter(c => String(c.id) === String(userCompanyId))
        : (companiesDataRaw || []);

      const usersDataFiltered = user?.role === 'USER' && userCompanyId
        ? (usersData || []).filter(u => {
            const uCompanyId = u?.company?.id || u?.companyId || u?.company?.companyId || null;
            if (uCompanyId) return String(uCompanyId) === String(userCompanyId);
            if (u?.username && u.username === user?.username) return true;
            return false;
          })
        : (usersData || []);

      setUsers(usersDataFiltered);
      setSystems(systemsData);
      setCompanies(companiesData || []);
      if (activeTab === 'SYSTEMS') {
        await fetchLatestSystemCode();
      }
    } catch (err) {
      setError('Failed to load data. Please refresh.');
      console.error('Load data error:', err);
    } finally {
      setLoading(false);
    }
  }, [user, fetchLatestSystemCode, activeTab]);

  const copyToClipboard = () => {
    if (generatedCode) {
      navigator.clipboard.writeText(generatedCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // run fetchLatestSystemCode when systems tab opens
  useEffect(() => {
    if (isOpen && activeTab === 'SYSTEMS') {
      fetchLatestSystemCode();
    }
  }, [isOpen, activeTab, fetchLatestSystemCode]);

  // Load admin permissions
  const loadAdminPermissions = async () => {
      if (!user?.username) return;
      try {
          const response = await fetch(`${API_BASE_URL}/admin/admin-permissions?username=${encodeURIComponent(user.username)}`);
          if (response.ok) {
              const data = await response.json();
              setAdminPermissions(data);
              console.log('Admin Permissions:', data);
          } else {
              console.error('Failed to load admin permissions:', await response.text());
          }
      } catch (error) {
          console.error('Failed to load admin permissions:', error);
      }
  };

  useEffect(() => {
    if (isOpen && user?.role === 'ADMIN') {
      loadAdminPermissions();
    }
  }, [isOpen, user]);

  // load data when panel opens
  useEffect(() => {
    if (isOpen) {
      loadData();
    }
  }, [isOpen, loadData]);

  // ========== USER MANAGEMENT ==========
  const handleCreateUser = async (e) => {
      e.preventDefault();
      if (!newUsername.trim() || !newPassword.trim()) {
          setError('Username and password are required');
          return;
      }

      if (newRole === 'USER' && !newCompanyId) {
          setError('Please select a company for USER role');
          return;
      }

      setError('');
      setSuccess('');
      try {
          // ============================================================
          // FIX: Pass adminUsername to createUser
          // ============================================================
          await createUser(
              {
                  username: newUsername,
                  password: newPassword,
                  role: newRole,
                  companyId: newCompanyId || null
              },
              user?.username  // ← Add this!
          );
          
          setSuccess('✅ User registered successfully');
          setNewUsername('');
          setNewPassword('');
          setNewRole('USER');
          setNewCompanyId('');
          loadData();
      } catch (errorMsg) {
          setError(errorMsg.message || 'Failed to create user');
      }
  };

  const handleDeleteUser = async (userId, username) => {
    if (!window.confirm(`Are you sure you want to delete user "${username}"? This action cannot be undone.`)) return;
    
    setError('');
    setSuccess('');
    try {
      await deleteUser(userId);
      setSuccess(`✅ User "${username}" deleted successfully`);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to delete user');
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
      setSuccess(`✅ Updated system access for ${selectedUser.username}`);
      setSelectedUser(null);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to save assignments');
    }
  };

  // ========== RESET PASSWORD HANDLER ==========
  const handleResetPassword = async (e) => {
    e.preventDefault();
    
    if (!resetNewPassword.trim()) {
      setError('New password is required');
      return;
    }
    if (resetNewPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    
    setResetLoading(true);
    setError('');
    setSuccess('');
    
    try {
      const result = await resetUserPassword(resetUser.id, resetNewPassword.trim());
      setSuccess(`✅ Password reset successfully for ${result.username}`);
      setShowResetPassword(false);
      setResetUser(null);
      setResetNewPassword('');
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to reset password');
    } finally {
      setResetLoading(false);
    }
  };

  const openResetPasswordModal = (user) => {
    setResetUser(user);
    setResetNewPassword('');
    setShowResetPassword(true);
    setError('');
    setSuccess('');
  };

  const closeResetPasswordModal = () => {
    setShowResetPassword(false);
    setResetUser(null);
    setResetNewPassword('');
    setError('');
    setSuccess('');
  };

  // ========== ADMIN MANAGEMENT FUNCTIONS ==========
  
  // Toggle admin active/inactive status
  const handleToggleAdminStatus = async (adminId, currentStatus, adminUsername) => {
    if (!window.confirm(`Are you sure you want to ${currentStatus ? 'deactivate' : 'activate'} admin "${adminUsername}"?`)) return;
    
    setError('');
    setSuccess('');
    setLoading(true);
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${adminId}/toggle-status?currentUsername=${encodeURIComponent(user.username)}`, {
        method: 'PATCH'
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        loadData();
        loadAdminPermissions();
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to toggle admin status');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to toggle admin status');
    } finally {
      setLoading(false);
    }
  };

  // Delete admin
  const handleDeleteAdmin = async (adminId, adminUsername) => {
    if (!window.confirm(`⚠️ Are you sure you want to delete admin "${adminUsername}"?\n\nThis action CANNOT be undone!`)) return;
    
    setError('');
    setSuccess('');
    setLoading(true);
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${adminId}?currentUsername=${encodeURIComponent(user.username)}`, {
        method: 'DELETE'
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        loadData();
        loadAdminPermissions();
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to delete admin');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to delete admin');
    } finally {
      setLoading(false);
    }
  };

  // Reset admin password
  const handleResetAdminPassword = async (adminId, adminUsername, newPassword) => {
    setError('');
    setSuccess('');
    setLoading(true);
    
    try {
      const response = await fetch(`${API_BASE_URL}/admin/admins/${adminId}/reset-password?currentUsername=${encodeURIComponent(user.username)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newPassword })
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ ${data.message}`);
        loadData();
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to reset admin password');
      }
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to reset admin password');
    } finally {
      setLoading(false);
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
        description: description.trim(),
        simNumber: simNumber.trim(),
        panelSimNumber: panelSimNumber.trim() || simNumber.trim(),
        disarmCommand: disarmCommand.trim() || '8888#2A',
        armCommand: armCommand.trim() || '8888#1A',
        status: 'ACTIVE'
      };

      const userCompanyId = user?.companyId || user?.company?.id || user?.company?.companyId || null;
      const companyIdToSend = user?.role === 'ADMIN'
        ? (selectedCompanyId || userCompanyId || null)
        : userCompanyId;
      
      console.log('Creating system with:', {
        systemData,
        companyId: companyIdToSend,
        username: user?.username,
        role: user?.role
      });

      const result = await createSystem(
        systemData, 
        companyIdToSend, 
        user?.username
      );
      
      setSuccess(`✅ System created: ${result.systemCode}`);
      setLocation('');
      setDescription('');
      setSimNumber('');
      setPanelSimNumber('');
      setDisarmCommand('8888#2A');
      setArmCommand('8888#1A');
      setSelectedCompanyId('');
      await loadData();
      await fetchLatestSystemCode();
      if (onSystemChange) onSystemChange();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to create system');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleStartEditSystem = (system) => {
    setEditingSystem(system);
    setLocation(system.location);
    setDescription(system.description || '');
    setSimNumber(system.simNumber);
    setPanelSimNumber(system.panelSimNumber || system.simNumber);
    setDisarmCommand(system.disarmCommand || '8888#2A');
    setArmCommand(system.armCommand || '8888#1A');
    setSelectedCompanyId(system.company ? String(system.company.id) : '');
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
      const companyIdToSend = user?.role === 'ADMIN' ? (selectedCompanyId || 0) : 0;
      
      await updateSystem(
        editingSystem.id,
        {
          location: location.trim(),
          description: description.trim(),
          simNumber: simNumber.trim(),
          panelSimNumber: panelSimNumber.trim() || simNumber.trim(),
          disarmCommand: disarmCommand.trim() || '8888#2A',
          armCommand: armCommand.trim() || '8888#1A',
          status: editingSystem.status
        },
        companyIdToSend,
        user?.username
      );
      setSuccess(`✅ System ${editingSystem.systemCode} updated successfully`);
      setEditingSystem(null);
      setLocation('');
      setDescription('');
      setSimNumber('');
      setPanelSimNumber('');
      setDisarmCommand('8888#2A');
      setArmCommand('8888#1A');
      setSelectedCompanyId('');
      loadData();
      await fetchLatestSystemCode();
      if (onSystemChange) onSystemChange();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to update system');
    }
  };

  const handleCancelEditSystem = () => {
    setEditingSystem(null);
    setLocation('');
    setDescription('');
    setSimNumber('');
    setPanelSimNumber('');
    setDisarmCommand('8888#2A');
    setArmCommand('8888#1A');
    setSelectedCompanyId('');
    fetchLatestSystemCode();
  };

  const handleToggleStatus = async (system) => {
    setError('');
    setSuccess('');
    const newStatus = system.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await toggleSystemStatus(system.id, newStatus, user?.username);
      setSuccess(`✅ System ${system.systemCode} is now ${newStatus}`);
      loadData();
      if (onSystemChange) onSystemChange();
    } catch (errorMsg) {
      setError(errorMsg.message || 'Failed to change status');
    }
  };

  const handleSendCommand = async (systemCode, command) => {
    setError('');
    setSuccess('');
    try {
      await sendSystemCommand(systemCode, command, user?.username);
      setSuccess(`✅ Command ${command} sent to system ${systemCode} successfully`);
      loadData();
    } catch (errorMsg) {
      setError(errorMsg.message || `Failed to send command ${command}`);
    }
  };

  const handleStopSirenDirect = (system) => {
    setError('');
    setSuccess('');
    setSirenStopSystem(system);
  };

  const openDeleteConfirm = (system) => {
    setSystemToDelete(system);
    setShowDeleteConfirm(true);
    setError('');
    setSuccess('');
  };

  const closeDeleteConfirm = () => {
    setShowDeleteConfirm(false);
    setSystemToDelete(null);
  };

  const handleDeleteConfirmed = () => {
    setShowDeleteConfirm(false);
    setSystemToDelete(null);
    loadData();
    fetchLatestSystemCode();
    if (onSystemChange) onSystemChange();
    setSuccess('✅ System archived and deleted');
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

  // ===== FIX: Filter users by search query (with safe check) =====
  const filteredUsers = (users || []).filter(u => 
    u.username?.toLowerCase().includes((userSearchQuery || '').toLowerCase())
  );

  if (!isOpen) return null;

  const isAdmin = user?.role === 'ADMIN';
  const isUserRole = user?.role === 'USER';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-end font-sans">
      <div 
        className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
        onClick={onClose}
      />

      <div className="w-full max-w-2xl h-full bg-slate-900 border-l border-slate-800 text-slate-100 flex flex-col relative z-10 shadow-2xl animate-slide-left">
        
        <div className="p-6 border-b border-slate-800 bg-slate-950/40">
          <div className="flex justify-between items-center mb-4">
            <div className="flex items-center gap-2.5">
              <ShieldAlert className="w-5 h-5 text-red-500" />
              <h2 className="text-lg font-bold uppercase tracking-wider text-white">
                {isAdmin ? 'System Access Control' : 'Company Access Control'}
              </h2>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <X className="w-5 h-5 text-slate-400 hover:text-white" />
            </button>
          </div>

          <div className="flex gap-2 flex-wrap">
            {/* Users tab - Only for ADMIN */}
            {isAdmin && (
              <button
                onClick={() => { setActiveTab('USERS'); setError(''); setSuccess(''); }}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                  activeTab === 'USERS' 
                    ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                    : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
                }`}
              >
                <Users className="w-4 h-4" /> Users
              </button>
            )}
            
            {/* Systems tab - For BOTH Admin and User */}
            <button
              onClick={() => { setActiveTab('SYSTEMS'); setError(''); setSuccess(''); }}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                activeTab === 'SYSTEMS' 
                  ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                  : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
              }`}
            >
              <Cpu className="w-4 h-4" /> Systems
            </button>
            
            {/* Archived tab - NOW AS A TAB (NOT POPUP) */}
            <button
              onClick={() => { setActiveTab('ARCHIVED'); setError(''); setSuccess(''); }}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                activeTab === 'ARCHIVED' 
                  ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                  : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
              }`}
            >
              <Database className="w-4 h-4" /> Archived
            </button>
            
            {/* COMPANY tab - For USER only (own company profile) */}
            {isUserRole && (
              <button
                onClick={() => { setActiveTab('COMPANY'); setError(''); setSuccess(''); }}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                  activeTab === 'COMPANY' 
                    ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                    : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
                }`}
              >
                <Building className="w-4 h-4" /> Company
              </button>
            )}
            
            {/* Companies tab - Only for ADMIN */}
            {isAdmin && (
              <button
                onClick={() => { setActiveTab('COMPANIES'); setError(''); setSuccess(''); }}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all ${
                  activeTab === 'COMPANIES' 
                    ? 'bg-red-650 text-white border border-red-500 shadow-md shadow-red-500/10' 
                    : 'bg-slate-950/50 hover:bg-slate-800 text-slate-400 border border-slate-800'
                }`}
              >
                <Building className="w-4 h-4" /> Companies
              </button>
            )}
          </div>
          
          {/* Show company name for USER */}
          {isUserRole && user?.companyName && (
            <div className="mt-3 text-xs text-emerald-400 font-mono flex items-center gap-2">
              <Building className="w-3.5 h-3.5" />
              Managing Company: {user.companyName} {user.companyCode && `(${user.companyCode})`}
            </div>
          )}

          {/* Show admin type for ADMIN */}
          {/* {isAdmin && adminPermissions && (
            <div className="mt-3 text-xs font-mono flex items-center gap-2">
              <Shield className="w-3.5 h-3.5" />
              <span className={adminPermissions.isSuperAdmin ? 'text-purple-400' : 'text-slate-400'}>
                {adminPermissions.adminType === 'SUPER_ADMIN' ? '🔑 Super Admin (Full Access)' : '🛠️ Operational Admin (Limited)'}
              </span>
            </div>
          )} */}

          {/* ============================================================
              ADMIN MANAGEMENT BUTTON - ONLY FOR SUPER ADMIN (FORM)
              ============================================================ */}
          {/* {isAdmin && adminPermissions?.isSuperAdmin && (
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <button
                onClick={() => setShowAdminManagement(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-purple-500/10 hover:bg-purple-500/20 text-purple-400 border border-purple-500/30 rounded-lg text-[10px] font-mono transition-all"
              >
                <Shield className="w-3.5 h-3.5" />
                Admin Management
              </button>
            </div>
          )} */}
        </div>

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

          {/* ========== TAB 1: USERS MANAGEMENT (ADMIN ONLY) ========== */}
          {activeTab === 'USERS' && isAdmin && (
            <>
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
                          onChange={(e) => {
                              const selectedRole = e.target.value;
                              if (selectedRole === 'ADMIN' && !adminPermissions?.isSuperAdmin) {
                                  setError('Only Super Admins (FORM) can create other admin accounts');
                                  return;
                              }
                              setNewRole(selectedRole);
                              setError('');
                          }}
                          className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-red-500/50"
                      >
                          <option value="USER">USER (Operator)</option>
                          <option value="ADMIN" disabled={!adminPermissions?.isSuperAdmin}>
                              ADMIN (Full Access) {!adminPermissions?.isSuperAdmin && '🔒 Super Admin only'}
                          </option>
                      </select>
                      {newRole === 'ADMIN' && !adminPermissions?.isSuperAdmin && (
                          <p className="text-[8px] text-red-400 font-mono">
                              ⚠️ Only Super Admins can create other admin accounts
                          </p>
                      )}
                  </div>

                  {newRole === 'USER' && (
                    <div className="col-span-1 sm:col-span-3 space-y-1.5">
                      <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                        <Building className="w-3.5 h-3.5 text-blue-400" />
                        Company <span className="text-red-400">*</span>
                      </label>
                      <select
                        value={newCompanyId}
                        onChange={(e) => setNewCompanyId(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-red-500/50"
                        required={newRole === 'USER'}
                      >
                        <option value="">-- Select Company --</option>
                        {companies.map((comp) => (
                          <option key={comp.id} value={comp.id}>
                            {comp.companyCode} - {comp.companyName}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}

                  <button
                    type="submit"
                    className="col-span-1 sm:col-span-3 w-full bg-slate-800 hover:bg-red-650 hover:text-white border border-slate-700 hover:border-red-500 font-bold py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all flex items-center justify-center gap-1.5"
                  >
                    <Plus className="w-3.5 h-3.5" /> Save User
                  </button>
                </form>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">Security User Directory</h3>
                  <div className="flex items-center gap-2">
                    <div className="relative">
                      <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
                      <input
                        type="text"
                        value={userSearchQuery}
                        onChange={(e) => setUserSearchQuery(e.target.value)}
                        placeholder="Search users..."
                        className="bg-slate-800 border border-slate-700 rounded-lg pl-8 pr-3 py-1.5 text-xs font-mono text-white placeholder-slate-500 focus:outline-none focus:border-red-500/50 w-40"
                      />
                    </div>
                    <button
                      onClick={loadData}
                      className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
                      title="Refresh users"
                    >
                      <RefreshCw className="w-4 h-4 text-slate-400" />
                    </button>
                  </div>
                </div>
                
                <div className="divide-y divide-slate-800/60 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/20">
                  {loading ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">Loading user directory...</div>
                  ) : filteredUsers.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">
                      {userSearchQuery ? 'No users found matching your search' : 'No users registered'}
                    </div>
                  ) : (
                    // ============================================================
                    // UPDATED: Users List Rendering with Admin Hierarchy
                    // ============================================================
                    filteredUsers.map((u) => {
                      const isAdminUser = u.role === 'ADMIN';
                      const isCurrentUser = u.username === user?.username;
                      
                      // Check permissions for admin management
                      const isCurrentFormAdmin = adminPermissions?.isSuperAdmin === true;
                      const isTargetFormAdmin = u.registrationMethod === 'FORM';
                      const isTargetAdminPanel = u.registrationMethod === 'ADMIN_PANEL';
                      
                      // Can manage if:
                      // 1. Current user is Super Admin (FORM)
                      // 2. Target is Ops Admin (ADMIN_PANEL)
                      // 3. Target is not self
                      const showAdminButtons = isCurrentFormAdmin && isAdminUser && !isTargetFormAdmin && !isCurrentUser;
                      
                      // Show lock icon for FORM Admins (protected)
                      const showProtectedLock = isCurrentFormAdmin && isTargetFormAdmin && !isCurrentUser;
                      
                      return (
                        <div key={u.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-900/10 transition-colors">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-mono font-bold text-sm text-white">{u.username}</span>
                              <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                                u.role === 'ADMIN' 
                                  ? 'bg-red-500/10 text-red-400 border-red-500/20' 
                                  : 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                              }`}>
                                {u.role}
                              </span>
                              {/* {isAdminUser && (
                                <span className={`text-[8px] font-mono px-1.5 py-0.5 rounded-full border ${
                                  isTargetFormAdmin
                                    ? 'bg-purple-500/20 text-purple-400 border-purple-500/30'
                                    : 'bg-slate-600/20 text-slate-400 border-slate-600/30'
                                }`}>
                                  {isTargetFormAdmin ? '🔑 Super Admin' : '🛠️ Ops Admin'}
                                </span>
                              )} */}
                              {u.companyName && (
                                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-green-500/10 text-green-400 border border-green-500/20">
                                  {u.companyName}
                                </span>
                              )}
                              {u.isActive === false && (
                                <span className="text-[8px] font-mono px-1.5 py-0.5 rounded-full bg-red-500/20 text-red-400 border border-red-500/30">
                                  ⚠️ INACTIVE
                                </span>
                              )}
                              {u.isLastSuperAdmin && (
                                <span className="text-[8px] font-mono px-1.5 py-0.5 rounded-full bg-yellow-500/20 text-yellow-400 border border-yellow-500/30">
                                  ⭐ Last Super Admin
                                </span>
                              )}
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

                          <div className="flex items-center gap-2 flex-shrink-0 flex-wrap">
                            {/* ============================================================
                                Admin Management Buttons - Only for Super Admin -> ADMIN_PANEL
                                ============================================================ */}
                            {showAdminButtons && (
                              <>
                                <button
                                  onClick={() => {
                                    const newPass = prompt(`Enter new password for ${u.username}:`);
                                    if (newPass && newPass.length >= 6) {
                                      handleResetAdminPassword(u.id, u.username, newPass);
                                    } else if (newPass !== null) {
                                      setError('Password must be at least 6 characters');
                                    }
                                  }}
                                  className="px-2 py-1.5 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 border border-yellow-500/30 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1"
                                  title="Reset admin password"
                                >
                                  <Key className="w-3.5 h-3.5" /> Reset
                                </button>
                                <button
                                  onClick={() => handleToggleAdminStatus(u.id, u.isActive !== false, u.username)}
                                  className={`px-2 py-1.5 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1 border ${
                                    u.isActive !== false
                                      ? 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                                      : 'bg-red-500/10 hover:bg-red-500/20 text-red-400 border-red-500/30'
                                  }`}
                                  title={u.isActive !== false ? 'Deactivate admin' : 'Activate admin'}
                                  disabled={u.isLastSuperAdmin}
                                >
                                  {u.isActive !== false ? <ToggleRight className="w-3.5 h-3.5" /> : <ToggleLeft className="w-3.5 h-3.5" />}
                                  {u.isActive !== false ? 'Active' : 'Inactive'}
                                </button>
                                <button
                                  onClick={() => handleDeleteAdmin(u.id, u.username)}
                                  className="px-2 py-1.5 bg-red-500/10 hover:bg-red-600 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1"
                                  title="Delete admin"
                                  disabled={u.isLastSuperAdmin}
                                >
                                  <Trash className="w-3.5 h-3.5" /> Delete
                                </button>
                              </>
                            )}

                            {/* ============================================================
                                Protected Lock for FORM Admins (Cannot be managed)
                                ============================================================ */}
                            {showProtectedLock && (
                              <span className="px-2 py-1.5 bg-slate-800 text-slate-500 rounded-lg text-[10px] font-mono flex items-center gap-1 border border-slate-700 cursor-not-allowed" 
                                    title="Super Admins cannot be managed by other Super Admins">
                                <Lock className="w-3.5 h-3.5" /> Protected
                              </span>
                            )}

                            {/* ============================================================
                                Normal user operations (USER role only)
                                ============================================================ */}
                            {u.role === 'USER' && (
                              <>
                                <button
                                  onClick={() => handleDeleteUser(u.id, u.username)}
                                  className="px-2 py-1.5 bg-red-500/10 hover:bg-red-650 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1"
                                >
                                  <Trash className="w-3.5 h-3.5" /> Delete
                                </button>
                                <button
                                  onClick={() => openResetPasswordModal(u)}
                                  className="px-2 py-1.5 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 hover:text-yellow-300 border border-yellow-500/30 hover:border-yellow-500/50 rounded-lg text-[10px] font-mono transition-all flex items-center gap-1"
                                >
                                  <Key className="w-3.5 h-3.5" /> Reset
                                </button>
                                <button
                                  onClick={() => handleSelectUserToAssign(u)}
                                  className="px-2 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-[10px] font-mono transition-all text-slate-300 hover:text-white"
                                >
                                  Assign
                                </button>
                              </>
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </>
          )}

          {/* ========== TAB 2: SYSTEMS/DEVICES MANAGEMENT ========== */}
          {activeTab === 'SYSTEMS' && (
            <>
              <div className="bg-slate-950/40 border border-slate-800/80 rounded-2xl p-5 space-y-4">
                <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono flex items-center gap-2">
                  <Cpu className="w-4 h-4 text-red-500" /> 
                  {editingSystem ? 'Modify Alarm System' : 'Register New Alarm System'}
                </h3>
                
                <form onSubmit={editingSystem ? handleUpdateSystem : handleCreateSystem} className="grid grid-cols-1 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                      System Code
                      {!editingSystem && generatedCode && (
                        <button
                          type="button"
                          onClick={copyToClipboard}
                          className="p-1 hover:bg-slate-700 rounded-lg transition-colors"
                          title="Copy system code"
                        >
                          {copied ? (
                            <CheckCircleIcon className="w-3.5 h-3.5 text-emerald-400" />
                          ) : (
                            <Copy className="w-3.5 h-3.5 text-slate-400 hover:text-white" />
                          )}
                        </button>
                      )}
                    </label>
                    <div className="relative">
                      <input 
                        type="text"
                        value={editingSystem ? editingSystem.systemCode : generatedCode || 'Loading...'}
                        disabled
                        className={`w-full bg-slate-950 border rounded-lg px-3 py-2 text-xs font-mono cursor-not-allowed ${
                          editingSystem 
                            ? 'border-slate-700 text-slate-400' 
                            : 'border-emerald-500/50 text-emerald-400 bg-emerald-500/5'
                        }`}
                      />
                      {!editingSystem && generatedCode && (
                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                          <Zap className="w-4 h-4 text-emerald-500 animate-pulse" />
                        </div>
                      )}
                    </div>
                    <p className="text-[9px] text-slate-500 font-mono">
                      {editingSystem 
                        ? 'System code cannot be changed' 
                        : 'Auto-generated: Next available code'
                      }
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

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                      <Smartphone className="w-3.5 h-3.5" />
                      Panel SIM Number (Z8B)
                    </label>
                    <input 
                      type="text"
                      value={panelSimNumber}
                      onChange={(e) => setPanelSimNumber(e.target.value)}
                      placeholder="Panel SIM (e.g., 0714868100)"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                    />
                    <p className="text-[8px] text-slate-500">If empty, system SIM will be used</p>
                  </div>

                  <div className="space-y-1.5">
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

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                      <Building className="w-3.5 h-3.5 text-blue-400" />
                      Company
                      {isUserRole && (
                        <span className="text-emerald-400 text-[8px] font-normal">(Auto-set to your company)</span>
                      )}
                      {isAdmin && (
                        <span className="text-slate-500 text-[8px] font-normal">(Optional)</span>
                      )}
                    </label>
                    
                    {isAdmin ? (
                      <select
                        value={selectedCompanyId}
                        onChange={(e) => setSelectedCompanyId(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-red-500/50"
                      >
                        <option value="">-- No Company Assigned --</option>
                        {companies.map((comp) => (
                          <option key={comp.id} value={comp.id}>
                            {comp.companyCode} - {comp.companyName}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <div className="w-full bg-slate-950/50 border border-emerald-500/30 rounded-lg px-3 py-2 text-xs font-mono text-emerald-400 cursor-not-allowed">
                        {user?.companyName || 'No company assigned'}
                        {user?.companyCode && (
                          <span className="text-slate-500 ml-2">({user.companyCode})</span>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">Description</label>
                    <input
                      type="text"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Short description (e.g., Bank: XYZ - Branch: ABC)"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                      <Settings className="w-3.5 h-3.5" />
                      Panel Commands
                    </label>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="text-[8px] text-slate-500 font-mono">Disarm Command</label>
                        <input 
                          type="text"
                          value={disarmCommand}
                          onChange={(e) => setDisarmCommand(e.target.value)}
                          placeholder="8888#2A"
                          className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                        />
                      </div>
                      <div>
                        <label className="text-[8px] text-slate-500 font-mono">Arm Command</label>
                        <input 
                          type="text"
                          value={armCommand}
                          onChange={(e) => setArmCommand(e.target.value)}
                          placeholder="8888#1A"
                          className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-white placeholder-slate-600 focus:outline-none focus:border-red-500/50"
                        />
                      </div>
                    </div>
                    <p className="text-[8px] text-slate-500">Default: 8888#2A (Disarm) | 8888#1A (Arm)</p>
                  </div>

                  <div className="flex gap-2 pt-2">
                    {editingSystem && (
                      <button
                        type="button"
                        onClick={handleCancelEditSystem}
                        className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all"
                      >
                        Cancel
                      </button>
                    )}
                    <button
                      type="submit"
                      disabled={isGenerating}
                      className="flex-1 bg-slate-800 hover:bg-red-650 hover:text-white border border-slate-700 hover:border-red-500 font-bold py-2 rounded-lg text-xs font-mono tracking-wider uppercase transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
                    >
                      {editingSystem ? (
                        <>
                          <Save className="w-3.5 h-3.5" /> Update System
                        </>
                      ) : (
                        <>
                          <Plus className="w-3.5 h-3.5" /> Register System
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">
                    {isUserRole ? 'Your Company Systems' : 'Alarm Systems Directory'}
                  </h3>
                  <div className="flex items-center gap-2">
                    {/* 🆕 Delete Systems Button - Admin Only */}
                    {isAdmin && (
                      <button
                        onClick={() => setShowDeletedSystems(true)}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 hover:border-red-500 rounded-lg text-xs font-mono transition-all"
                        title="View and permanently delete systems"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Delete Systems
                      </button>
                    )}
                    <button
                      onClick={loadData}
                      className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
                      title="Refresh systems"
                    >
                      <RefreshCw className="w-4 h-4 text-slate-400" />
                    </button>
                  </div>
                </div>
                
                <div className="divide-y divide-slate-800/60 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/20">
                  {loading ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">Loading systems directory...</div>
                  ) : systems.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-500 font-mono">
                      {isUserRole 
                        ? 'No systems registered for your company. Create your first system above.'
                        : 'No systems registered. Create your first system above.'
                      }
                    </div>
                  ) : (
                    systems.map((sys) => {
                      const isActive = sys.status === 'ACTIVE';
                      return (
                        <div 
                          key={sys.id} 
                          className={`p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-900/10 transition-colors ${
                            !isActive ? 'opacity-60' : ''
                          }`}
                        >
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className={`font-mono font-bold text-sm ${
                                isActive ? 'text-emerald-400' : 'text-red-400'
                              }`}>
                                {sys.systemCode}
                              </span>
                              <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                                isActive 
                                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                                  : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                              }`}>
                                {sys.status}
                              </span>
                              {sys.company && (
                                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 flex items-center gap-1">
                                  <Building className="w-3 h-3" />
                                  {sys.company.companyCode || sys.company.companyName}
                                </span>
                              )}
                              {sys.sirenStatus && (
                                <span className={`text-[8px] font-mono px-1.5 py-0.5 rounded-full border ${
                                  sys.sirenStatus === 'ON' 
                                    ? 'bg-red-500/20 text-red-400 border-red-500/30 animate-pulse' 
                                    : 'bg-slate-600/20 text-slate-400 border-slate-600/30'
                                }`}>
                                  {sys.sirenStatus === 'ON' ? '🔔 SIREN ON' : '🔕 SIREN OFF'}
                                </span>
                              )}
                              {!isActive && (
                                <span className="text-[8px] font-mono px-1.5 py-0.5 rounded-full bg-yellow-500/20 text-yellow-400 border border-yellow-500/30">
                                  ⚠️ DISABLED
                                </span>
                              )}
                            </div>
                            <div className="text-xs text-slate-400 mt-1">
                              Location: <span className="text-slate-300 font-medium">{sys.location}</span> • SIM: <span className="font-mono text-slate-300">{sys.simNumber}</span>
                              {sys.panelSimNumber && (
                                <span className="ml-2 text-slate-500">• Panel: <span className="font-mono">{sys.panelSimNumber}</span></span>
                              )}
                            </div>
                            {sys.description && (
                              <div className="text-[11px] text-slate-500 mt-1 truncate">{sys.description}</div>
                            )}
                            <div className="text-[10px] text-slate-500 mt-0.5 font-mono">
                              {isActive ? 'Active for: ' : 'Inactive for: '} 
                              <span className={isActive ? 'text-emerald-400/90 font-bold' : 'text-red-400/90 font-bold'}>
                                {formatDuration(sys.lastStatusChangedAt)}
                              </span>
                              {sys.disarmCommand && (
                                <span className="ml-3 text-cyan-400">Disarm: <span className="font-mono">{sys.disarmCommand}</span></span>
                              )}
                            </div>
                          </div>

                          <div className="flex items-center gap-2 flex-shrink-0">
                            {sys.sirenStatus === 'ON' && isActive && (
                              <button
                                onClick={() => handleStopSirenDirect(sys)}
                                title="Stop Siren Only"
                                className="p-1.5 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-lg transition-all"
                              >
                                <BellOff className="w-4 h-4" />
                              </button>
                            )}

                            <button
                              onClick={() => handleSendCommand(sys.systemCode, 'ARM')}
                              disabled={!isActive}
                              title={isActive ? "Arm System" : "System is INACTIVE"}
                              className={`p-1.5 border rounded-lg transition-all ${
                                isActive
                                  ? 'bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 border-yellow-500/30'
                                  : 'bg-slate-800 text-slate-500 border-slate-700 cursor-not-allowed opacity-50'
                              }`}
                            >
                              <Zap className="w-4 h-4" />
                            </button>

                            <button
                              onClick={() => handleSendCommand(sys.systemCode, 'DISARM')}
                              disabled={!isActive}
                              title={isActive ? "Disarm System & Resolve Alerts" : "System is INACTIVE"}
                              className={`p-1.5 border rounded-lg transition-all ${
                                isActive
                                  ? 'bg-red-500/10 hover:bg-red-500/20 text-red-400 border-red-500/30'
                                  : 'bg-slate-800 text-slate-500 border-slate-700 cursor-not-allowed opacity-50'
                              }`}
                            >
                              <ShieldOff className="w-4 h-4" />
                            </button>

                            <button
                              onClick={() => handleToggleStatus(sys)}
                              title={isActive ? 'Deactivate System' : 'Activate System'}
                              className={`p-1.5 rounded-lg border transition-all ${
                                isActive 
                                  ? 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-500 border-emerald-500/25' 
                                  : 'bg-red-500/10 hover:bg-red-500/20 text-red-400 border-red-500/25'
                              }`}
                            >
                              {isActive ? <ToggleRight className="w-5 h-5" /> : <ToggleLeft className="w-5 h-5" />}
                            </button>

                            <button
                              onClick={() => handleStartEditSystem(sys)}
                              title="Edit System Info"
                              className="p-1.5 bg-slate-800 hover:bg-slate-750 text-slate-300 hover:text-white border border-slate-700 rounded-lg transition-all"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>

                            <button
                              onClick={() => {
                                setSelectedSystemId(sys.id);
                                setSelectedSystemCode(sys.systemCode);
                                setZoneManagementOpen(true);
                              }}
                              title="Manage Zones"
                              className={`p-1.5 border rounded-lg transition-all ${
                                isActive
                                  ? 'bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border-blue-500/30'
                                  : 'bg-slate-800 text-slate-500 border-slate-700 cursor-not-allowed opacity-50'
                              }`}
                              disabled={!isActive}
                            >
                              <Layers className="w-4 h-4" />
                            </button>

                            <button
                              onClick={() => openDeleteConfirm(sys)}
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

          {/* ========== TAB 3: ARCHIVED SYSTEMS ========== */}
          {activeTab === 'ARCHIVED' && (
            <ArchivedSystemsInline 
              username={user?.username}
              onRefresh={loadData}
            />
          )}

          {/* ========== TAB: COMPANY PROFILE (USER ONLY) ========== */}
          {activeTab === 'COMPANY' && isUserRole && (
            <CompanyProfile
              companyId={user?.companyId}
              username={user?.username}
              userRole={user?.role}
              onRefresh={loadData}
            />
          )}

          {/* ========== TAB 4: COMPANIES MANAGEMENT (ADMIN ONLY) ========== */}
          {activeTab === 'COMPANIES' && isAdmin && (
            <CompanyManagement
              isOpen={true}
              onClose={() => {}}
              username={user?.username}
              userRole={user?.role}
              companyId={user?.companyId}
            />
          )}
          
          {/* ========== USER sees message if Companies tab is not available ========== */}
          {activeTab === 'COMPANIES' && !isAdmin && (
            <div className="text-center py-12">
              <Building className="w-16 h-16 text-slate-600 mx-auto mb-4" />
              <h3 className="text-lg font-bold text-white">Access Restricted</h3>
              <p className="text-sm text-slate-400 mt-2">
                Companies can only be managed by Administrators.
              </p>
              <p className="text-xs text-slate-500 mt-1 font-mono">
                Your company: {user?.companyName || 'Not assigned'}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* ========== ASSIGNMENT MODAL ========== */}
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

      {/* ========== RESET PASSWORD MODAL ========== */}
      {showResetPassword && resetUser && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-md w-full shadow-2xl shadow-yellow-500/10 animate-in fade-in duration-200">
            <div className="flex justify-between items-center p-5 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <Key className="w-5 h-5 text-yellow-500" />
                <h3 className="text-lg font-bold text-white">Reset Password</h3>
              </div>
              <button 
                onClick={closeResetPasswordModal}
                className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
              >
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>

            <form onSubmit={handleResetPassword} className="p-5 space-y-4">
              <div>
                <p className="text-sm text-slate-400 mb-1">
                  Resetting password for: 
                  <span className="text-white font-bold ml-1">{resetUser.username}</span>
                </p>
                <p className="text-xs text-slate-500">
                  Role: <span className="text-blue-400">{resetUser.role}</span>
                </p>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
                  New Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={resetNewPassword}
                    onChange={(e) => setResetNewPassword(e.target.value)}
                    placeholder="Min 6 characters"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-11 pr-12 py-3 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-yellow-500/50 focus:ring-1 focus:ring-yellow-500/50 transition-all font-mono"
                    required
                    minLength={6}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3.5 top-3.5 text-slate-500 hover:text-slate-300 transition-colors"
                  >
                    {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
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
                  <Check className="w-4 h-4 mt-0.5 flex-shrink-0" />
                  <span>{success}</span>
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeResetPasswordModal}
                  className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={resetLoading}
                  className="flex-1 py-2.5 bg-gradient-to-r from-yellow-600 to-yellow-700 hover:from-yellow-500 hover:to-yellow-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {resetLoading ? (
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

      {/* ========== ZONE MANAGEMENT MODAL ========== */}
      <ZoneManagement
        systemId={selectedSystemId}
        systemCode={selectedSystemCode}
        isOpen={zoneManagementOpen}
        onClose={() => {
          setZoneManagementOpen(false);
          setSelectedSystemId(null);
          setSelectedSystemCode('');
          loadData();
        }}
      />

      {/* ========== DELETE CONFIRMATION MODAL ========== */}
      {showDeleteConfirm && systemToDelete && (
        <DeleteConfirmationModal
          isOpen={showDeleteConfirm}
          onClose={closeDeleteConfirm}
          system={systemToDelete}
          username={user?.username}
          onConfirm={handleDeleteConfirmed}
        />
      )}

      {/* ========== SIREN STOP MODAL ========== */}
      {sirenStopSystem && (
        <SirenStopModal
          systemCode={sirenStopSystem.systemCode}
          location={sirenStopSystem.location}
          isOpen={!!sirenStopSystem}
          onClose={() => setSirenStopSystem(null)}
          onSirenStopped={() => {
            setSirenStopSystem(null);
            loadData();
            if (onSystemChange) onSystemChange();
          }}
          username={user?.username}
        />
      )}

      {/* ========== DELETED SYSTEMS PANEL ========== */}
      {showDeletedSystems && (
        <DeletedSystemsPanel
          isOpen={showDeletedSystems}
          onClose={() => setShowDeletedSystems(false)}
          username={user?.username}
          onSystemDeleted={() => {
            loadData();
            if (onSystemChange) onSystemChange();
          }}
        />
      )}

      {/* ========== ADMIN MANAGEMENT MODAL ========== */}
      {showAdminManagement && adminPermissions && (
        <AdminManagementModal
          isOpen={showAdminManagement}
          onClose={() => setShowAdminManagement(false)}
          admins={adminPermissions.admins || []}
          currentUsername={user?.username}
          isSuperAdmin={adminPermissions.isSuperAdmin}
          onAdminUpdated={() => {
            loadData();
            loadAdminPermissions();
          }}
        />
      )}
    </div>
  );
}

AdminPanel.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  user: PropTypes.shape({
    username: PropTypes.string,
    role: PropTypes.string,
    companyId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    companyName: PropTypes.string,
    companyCode: PropTypes.string,
    assignedSystems: PropTypes.array,
    company: PropTypes.shape({
      id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      companyId: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    })
  }),
  onSystemChange: PropTypes.func,
};