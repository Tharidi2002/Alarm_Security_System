import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { 
  X, Plus, Edit2, Trash2, Eye, RefreshCw, 
  Building, Users, Cpu, AlertCircle, CheckCircle,
  Search, Mail, Phone, MapPin, FileText,
  Zap, Copy, CheckCircle as CheckCircleIcon
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function CompanyManagement({ isOpen, onClose, username, userRole }) {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [companySystems, setCompanySystems] = useState([]);
  const [companyUsers, setCompanyUsers] = useState([]);
  
  // Form states
  const [formData, setFormData] = useState({
    companyName: '',
    address: '',
    contactPerson: '',
    contactEmail: '',
    contactPhone: '',
    registrationNumber: '',
    taxNumber: '',
    status: 'ACTIVE',
    notes: ''
  });

  // Auto-generated company code
  const [generatedCompanyCode, setGeneratedCompanyCode] = useState('');
  const [codeCopied, setCodeCopied] = useState(false);

  const loadCompanies = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/admin/companies${username ? `?username=${encodeURIComponent(username)}` : ''}`);
      if (response.ok) {
        const data = await response.json();
        setCompanies(data);
      } else {
        setError('Failed to load companies');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  // ===== FETCH NEXT COMPANY CODE =====
  const fetchNextCompanyCode = (companiesList) => {
    try {
      const data = companiesList || companies;
      if (data && data.length > 0) {
        const compCodes = data
          .filter(c => c.companyCode && c.companyCode.startsWith('COMP-'))
          .sort((a, b) => {
            const numA = parseInt(a.companyCode.split('-')[1]);
            const numB = parseInt(b.companyCode.split('-')[1]);
            return numB - numA;
          });
        
        if (compCodes.length > 0) {
          const lastCode = compCodes[0].companyCode;
          const lastNum = parseInt(lastCode.split('-')[1]);
          const nextNum = lastNum + 1;
          setGeneratedCompanyCode(`COMP-${String(nextNum).padStart(3, '0')}`);
        } else {
          setGeneratedCompanyCode('COMP-001');
        }
      } else {
        setGeneratedCompanyCode('COMP-001');
      }
    } catch (error) {
      console.error('Error generating company code:', error);
      setGeneratedCompanyCode('COMP-001');
    }
  };

  const copyCompanyCode = () => {
    if (generatedCompanyCode) {
      navigator.clipboard.writeText(generatedCompanyCode);
      setCodeCopied(true);
      setTimeout(() => setCodeCopied(false), 2000);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadCompanies().then(() => {});
    }
  }, [isOpen]);

  // Recompute company code whenever companies list changes
  useEffect(() => {
    if (companies.length >= 0) {
      fetchNextCompanyCode(companies);
    }
  }, [companies]);

  const handleCreateCompany = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/admin/companies`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        setSuccess('✅ Company created successfully!');
        setShowCreateModal(false);
        setFormData({
          companyName: '',
          address: '',
          contactPerson: '',
          contactEmail: '',
          contactPhone: '',
          registrationNumber: '',
          taxNumber: '',
          status: 'ACTIVE',
          notes: ''
        });
        loadCompanies();
      } else {
        const msg = await response.text();
        setError(msg || 'Failed to create company');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateCompany = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/admin/companies/${selectedCompany.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        setSuccess('✅ Company updated successfully!');
        setShowEditModal(false);
        setSelectedCompany(null);
        loadCompanies();
      } else {
        const msg = await response.text();
        setError(msg || 'Failed to update company');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCompany = async (company) => {
    if (!window.confirm(`Are you sure you want to delete "${company.companyName}"? This cannot be undone.`)) return;

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/admin/companies/${company.id}`, {
        method: 'DELETE'
      });

      if (response.ok) {
        setSuccess('✅ Company deleted successfully!');
        loadCompanies();
      } else {
        const msg = await response.text();
        setError(msg || 'Failed to delete company');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  const viewCompanyDetails = async (company) => {
    setSelectedCompany(company);
    setLoading(true);
    setError('');

    try {
      // Get company details
      const response = await fetch(`${API_BASE_URL}/admin/companies/${company.id}`);
      if (!response.ok) throw new Error('Failed to load company details');
      
      // Get systems
      const systemsRes = await fetch(`${API_BASE_URL}/admin/companies/${company.id}/systems`);
      if (systemsRes.ok) {
        const sysData = await systemsRes.json();
        setCompanySystems(sysData);
      }

      // Get users
      const usersRes = await fetch(`${API_BASE_URL}/admin/companies/${company.id}/users`);
      if (usersRes.ok) {
        const userData = await usersRes.json();
        setCompanyUsers(userData);
      }

      setShowViewModal(true);
    } catch {
      setError('Failed to load company details');
    } finally {
      setLoading(false);
    }
  };

  const openEditModal = (company) => {
    setSelectedCompany(company);
    setFormData({
      companyName: company.companyName || '',
      address: company.address || '',
      contactPerson: company.contactPerson || '',
      contactEmail: company.contactEmail || '',
      contactPhone: company.contactPhone || '',
      registrationNumber: company.registrationNumber || '',
      taxNumber: company.taxNumber || '',
      status: company.status || 'ACTIVE',
      notes: company.notes || ''
    });
    setShowEditModal(true);
  };

  const filteredCompanies = companies.filter(c =>
    c.companyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.companyCode.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!isOpen) return null;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <Building className="w-5 h-5 text-blue-400" />
          <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">Companies Management</h3>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={loadCompanies}
            className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
            title="Refresh"
          >
            <RefreshCw className="w-4 h-4 text-slate-400" />
          </button>
          {userRole === 'ADMIN' && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-mono transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Company
            </button>
          )}
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search companies..."
          className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
        />
      </div>

      {/* Error/Success */}
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

      {/* Companies List */}
      <div className="divide-y divide-slate-800/60 border border-slate-800 rounded-xl overflow-hidden bg-slate-950/20">
        {loading ? (
          <div className="p-6 text-center text-xs text-slate-500 font-mono">Loading companies...</div>
        ) : filteredCompanies.length === 0 ? (
          <div className="p-6 text-center text-xs text-slate-500 font-mono">
            {searchQuery ? 'No companies found' : 'No companies registered. Add your first company!'}
          </div>
        ) : (
          filteredCompanies.map((company) => (
            <div key={company.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-900/10 transition-colors">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-mono font-bold text-sm text-blue-400">{company.companyCode}</span>
                  <span className="font-medium text-white">{company.companyName}</span>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                    company.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                  }`}>
                    {company.status}
                  </span>
                </div>
                {company.contactPerson && (
                  <div className="text-xs text-slate-400 mt-1">
                    Contact: {company.contactPerson} {company.contactEmail && `• ${company.contactEmail}`}
                  </div>
                )}
              </div>

              <div className="flex items-center gap-2 flex-shrink-0">
                <button
                  onClick={() => viewCompanyDetails(company)}
                  className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 rounded-lg transition-all"
                  title="View Details"
                >
                  <Eye className="w-4 h-4" />
                </button>
                {userRole === 'ADMIN' && (
                <>
                  <button
                    onClick={() => openEditModal(company)}
                    className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 rounded-lg transition-all"
                    title="Edit Company"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteCompany(company)}
                    className="p-1.5 bg-red-500/10 hover:bg-red-600 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg transition-all"
                    title="Delete Company"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </>
              )}
            </div>
          </div>
          ))
        )}
      </div>

      {/* ===== CREATE MODAL ===== */}
      {showCreateModal && (
        <CompanyFormModal
          title="Add New Company"
          formData={formData}
          setFormData={setFormData}
          generatedCode={generatedCompanyCode}
          codeCopied={codeCopied}
          onCopyCode={copyCompanyCode}
          onClose={() => {
            setShowCreateModal(false);
            setFormData({
              companyName: '',
              address: '',
              contactPerson: '',
              contactEmail: '',
              contactPhone: '',
              registrationNumber: '',
              taxNumber: '',
              status: 'ACTIVE',
              notes: ''
            });
          }}
          onSubmit={handleCreateCompany}
          loading={loading}
        />
      )}

      {/* ===== EDIT MODAL ===== */}
      {showEditModal && selectedCompany && (
        <CompanyFormModal
          title={`Edit Company: ${selectedCompany.companyCode}`}
          formData={formData}
          setFormData={setFormData}
          onClose={() => {
            setShowEditModal(false);
            setSelectedCompany(null);
          }}
          onSubmit={handleUpdateCompany}
          loading={loading}
          isEdit
        />
      )}

      {/* ===== VIEW MODAL ===== */}
      {showViewModal && selectedCompany && (
        <CompanyViewModal
          company={selectedCompany}
          systems={companySystems}
          users={companyUsers}
          onClose={() => {
            setShowViewModal(false);
            setSelectedCompany(null);
            setCompanySystems([]);
            setCompanyUsers([]);
          }}
        />
      )}
    </div>
  );
}

// ===== COMPANY FORM MODAL =====
function CompanyFormModal({ title, formData, setFormData, onClose, onSubmit, loading, isEdit, generatedCode, codeCopied, onCopyCode }) {
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-blue-500/10">
        <div className="flex justify-between items-center p-5 border-b border-slate-800">
          <h3 className="text-lg font-bold text-white">{title}</h3>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        <form onSubmit={onSubmit} className="p-5 space-y-4">
          {/* ===== Company Code (Auto-Generated) ===== */}
          {!isEdit && generatedCode && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono flex items-center gap-2">
                Company Code
                <button
                  type="button"
                  onClick={onCopyCode}
                  className="p-1 hover:bg-slate-700 rounded-lg transition-colors"
                  title="Copy company code"
                >
                  {codeCopied ? (
                    <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="w-3.5 h-3.5 text-slate-400 hover:text-white" />
                  )}
                </button>
              </label>
              <div className="relative">
                <input 
                  type="text"
                  value={generatedCode}
                  disabled
                  className="w-full bg-slate-950 border border-emerald-500/50 rounded-xl px-4 py-2.5 text-sm font-mono text-emerald-400 bg-emerald-500/5 cursor-not-allowed"
                />
                <div className="absolute right-3 top-1/2 -translate-y-1/2">
                  <Zap className="w-4 h-4 text-emerald-500 animate-pulse" />
                </div>
              </div>
              <p className="text-[9px] text-slate-500 font-mono">Auto-generated: Next available company code</p>
            </div>
          )}
          {isEdit && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Company Code</label>
              <input 
                type="text"
                value={title.split(': ')[1] || ''}
                disabled
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-sm font-mono text-slate-400 cursor-not-allowed"
              />
              <p className="text-[9px] text-slate-500 font-mono">Company code cannot be changed</p>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Company Name */}
            <div className="md:col-span-2 space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">
                Company Name <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                name="companyName"
                value={formData.companyName}
                onChange={handleChange}
                placeholder="Enter company name"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
                required
              />
            </div>

            {/* Address */}
            <div className="md:col-span-2 space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Address</label>
              <textarea
                name="address"
                value={formData.address}
                onChange={handleChange}
                placeholder="Enter address"
                rows="2"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50 resize-none"
              />
            </div>

            {/* Contact Person */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Contact Person</label>
              <input
                type="text"
                name="contactPerson"
                value={formData.contactPerson}
                onChange={handleChange}
                placeholder="Full name"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Contact Phone */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Contact Phone</label>
              <input
                type="text"
                name="contactPhone"
                value={formData.contactPhone}
                onChange={handleChange}
                placeholder="+94 77 123 4567"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Contact Email */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Contact Email</label>
              <input
                type="email"
                name="contactEmail"
                value={formData.contactEmail}
                onChange={handleChange}
                placeholder="contact@company.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Registration Number */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Registration Number</label>
              <input
                type="text"
                name="registrationNumber"
                value={formData.registrationNumber}
                onChange={handleChange}
                placeholder="REG-2024-001"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Tax Number */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Tax Number</label>
              <input
                type="text"
                name="taxNumber"
                value={formData.taxNumber}
                onChange={handleChange}
                placeholder="TAX-001"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50"
              />
            </div>

            {/* Status */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Status</label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500/50"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </div>

            {/* Notes */}
            <div className="md:col-span-2 space-y-1.5">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Notes</label>
              <textarea
                name="notes"
                value={formData.notes}
                onChange={handleChange}
                placeholder="Additional notes..."
                rows="2"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-blue-500/50 resize-none"
              />
            </div>
          </div>

          <div className="flex gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-500 hover:to-blue-600 text-white font-bold rounded-xl text-sm font-mono transition-all uppercase tracking-wide flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {loading ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                isEdit ? 'Update Company' : 'Create Company'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

CompanyFormModal.propTypes = {
  title: PropTypes.string.isRequired,
  formData: PropTypes.object.isRequired,
  setFormData: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  isEdit: PropTypes.bool,
  generatedCode: PropTypes.string,
  codeCopied: PropTypes.bool,
  onCopyCode: PropTypes.func
};

// ===== COMPANY VIEW MODAL =====
function CompanyViewModal({ company, systems, users, onClose }) {
  const activeSystems = systems.filter(s => s.status === 'ACTIVE').length;
  const adminUsers = users.filter(u => u.role === 'ADMIN').length;

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-blue-500/10">
        <div className="flex justify-between items-center p-5 border-b border-slate-800 sticky top-0 bg-slate-900 z-10">
          <div className="flex items-center gap-3">
            <Building className="w-6 h-6 text-blue-400" />
            <div>
              <h3 className="text-lg font-bold text-white">{company.companyName}</h3>
              <p className="text-xs text-slate-400 font-mono">{company.companyCode}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        <div className="p-5 space-y-6">
          {/* Company Info */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Contact Person</p>
              <p className="text-white font-medium">{company.contactPerson || 'N/A'}</p>
            </div>
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Contact Email</p>
              <p className="text-white font-medium">{company.contactEmail || 'N/A'}</p>
            </div>
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Contact Phone</p>
              <p className="text-white font-medium">{company.contactPhone || 'N/A'}</p>
            </div>
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Status</p>
              <span className={`text-sm font-bold ${company.status === 'ACTIVE' ? 'text-emerald-400' : 'text-red-400'}`}>
                {company.status}
              </span>
            </div>
          </div>

          {company.address && (
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Address</p>
              <p className="text-white text-sm">{company.address}</p>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-3 text-center">
              <Cpu className="w-5 h-5 text-blue-400 mx-auto mb-1" />
              <p className="text-2xl font-bold text-white">{systems.length}</p>
              <p className="text-[10px] text-slate-400">Total Systems</p>
            </div>
            <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-3 text-center">
              <Cpu className="w-5 h-5 text-emerald-400 mx-auto mb-1" />
              <p className="text-2xl font-bold text-white">{activeSystems}</p>
              <p className="text-[10px] text-slate-400">Active Systems</p>
            </div>
            <div className="bg-purple-500/10 border border-purple-500/20 rounded-xl p-3 text-center">
              <Users className="w-5 h-5 text-purple-400 mx-auto mb-1" />
              <p className="text-2xl font-bold text-white">{users.length}</p>
              <p className="text-[10px] text-slate-400">Total Users</p>
            </div>
            <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-3 text-center">
              <Users className="w-5 h-5 text-yellow-400 mx-auto mb-1" />
              <p className="text-2xl font-bold text-white">{adminUsers}</p>
              <p className="text-[10px] text-slate-400">Admin Users</p>
            </div>
          </div>

          {/* Systems List */}
          {systems.length > 0 && (
            <div>
              <h4 className="text-sm font-bold text-white mb-2 flex items-center gap-2">
                <Cpu className="w-4 h-4 text-blue-400" />
                Systems ({systems.length})
              </h4>
              <div className="space-y-1 max-h-32 overflow-y-auto">
                {systems.map(sys => (
                  <div key={sys.id} className="flex justify-between items-center bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                    <span className="font-mono text-sm text-white">{sys.systemCode}</span>
                    <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                      sys.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                        : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                    }`}>
                      {sys.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Users List */}
          {users.length > 0 && (
            <div>
              <h4 className="text-sm font-bold text-white mb-2 flex items-center gap-2">
                <Users className="w-4 h-4 text-purple-400" />
                Users ({users.length})
              </h4>
              <div className="space-y-1 max-h-32 overflow-y-auto">
                {users.map(user => (
                  <div key={user.id} className="flex justify-between items-center bg-slate-950 border border-slate-800 rounded-lg px-3 py-2">
                    <span className="text-sm text-white">{user.username}</span>
                    <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                      user.role === 'ADMIN'
                        ? 'bg-red-500/10 text-red-400 border-red-500/20'
                        : 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                    }`}>
                      {user.role}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {company.notes && (
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3">
              <p className="text-[10px] text-slate-500 font-mono uppercase tracking-wider">Notes</p>
              <p className="text-white text-sm">{company.notes}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

CompanyViewModal.propTypes = {
  company: PropTypes.object.isRequired,
  systems: PropTypes.array,
  users: PropTypes.array,
  onClose: PropTypes.func.isRequired
};

CompanyManagement.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  username: PropTypes.string,
  userRole: PropTypes.string,
};