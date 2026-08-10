// src/components/CompanyProfile.jsx
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Building, Edit2, Save, X, AlertCircle, CheckCircle, Users, Cpu, Mail, Phone, MapPin, User, RefreshCw } from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function CompanyProfile({ companyId, username, userRole, onRefresh }) {
    const [company, setCompany] = useState(null);
    const [systemCount, setSystemCount] = useState(0);
    const [userCount, setUserCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [retryCount, setRetryCount] = useState(0);
    const [formData, setFormData] = useState({
        companyName: '',
        address: '',
        contactPerson: '',
        contactEmail: '',
        contactPhone: '',
        notes: ''
    });

    const loadCompany = async () => {
        if (!companyId) {
            setError('No company assigned to this user');
            setLoading(false);
            return;
        }

        setLoading(true);
        setError('');
        setSuccess('');
        
        try {
            const url = `${API_BASE_URL}/admin/companies/${companyId}?username=${encodeURIComponent(username)}`;
            console.log('📌 Fetching company from:', url);
            
            const response = await fetch(url, {
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                // Add timeout
                signal: AbortSignal.timeout(10000)
            });
            
            if (!response.ok) {
                const text = await response.text();
                console.error('❌ Response error:', response.status, text);
                throw new Error(`Server responded with ${response.status}: ${text || 'Unknown error'}`);
            }
            
            const data = await response.json();
            console.log('📌 Company data received:', data);
            
            const comp = data.company || data;
            setCompany(comp);
            setSystemCount(data.systemCount || 0);
            setUserCount(data.userCount || 0);
            setFormData({
                companyName: comp.companyName || '',
                address: comp.address || '',
                contactPerson: comp.contactPerson || '',
                contactEmail: comp.contactEmail || '',
                contactPhone: comp.contactPhone || '',
                notes: comp.notes || ''
            });
            
        } catch (err) {
            console.error('❌ Error loading company:', err);
            if (err.name === 'AbortError' || err.name === 'TimeoutError') {
                setError('Request timeout. Please try again.');
            } else if (err.message.includes('ERR_INCOMPLETE_CHUNKED_ENCODING')) {
                setError('Server connection issue. Please refresh and try again.');
            } else {
                setError(err.message || 'Network error loading company');
            }
            setRetryCount(prev => prev + 1);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCompany();
    }, [companyId]);

    const handleEdit = () => {
        setEditing(true);
        setError('');
        setSuccess('');
    };

    const handleCancel = () => {
        setEditing(false);
        setFormData({
            companyName: company?.companyName || '',
            address: company?.address || '',
            contactPerson: company?.contactPerson || '',
            contactEmail: company?.contactEmail || '',
            contactPhone: company?.contactPhone || '',
            notes: company?.notes || ''
        });
        setError('');
        setSuccess('');
    };


    const handleSave = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            // Send as Map/JSON object
            const payload = {
                companyName: formData.companyName,
                address: formData.address,
                contactPerson: formData.contactPerson,
                contactEmail: formData.contactEmail,
                contactPhone: formData.contactPhone,
                notes: formData.notes
            };

            console.log('📌 Sending update payload:', payload);

            const response = await fetch(`${API_BASE_URL}/admin/companies/${companyId}?username=${encodeURIComponent(username)}`, {
                method: 'PUT',
                headers: { 
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || 'Failed to update company');
            }

            const data = await response.json();
            console.log('📌 Update response:', data);
            
            setCompany(data);
            setSystemCount(data.systemCount || 0);
            setUserCount(data.userCount || 0);
            setSuccess('✅ Company updated successfully!');
            setEditing(false);
            if (onRefresh) onRefresh();
            setTimeout(() => setSuccess(''), 3000);
            
        } catch (err) {
            console.error('❌ Error updating company:', err);
            setError(err.message || 'Network error updating company');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleRetry = () => {
        loadCompany();
    };

    if (loading && !company) {
        return (
            <div className="text-center py-8 text-slate-400 font-mono text-sm">
                <div className="w-8 h-8 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mx-auto mb-3" />
                Loading company profile...
            </div>
        );
    }

    if (error && !company) {
        return (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 text-center">
                <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-2" />
                <p className="text-red-400 text-sm">{error}</p>
                <p className="text-xs text-slate-500 font-mono mt-1">
                    {retryCount > 3 ? '⚠️ Multiple retry attempts failed. Please check server.' : `Attempt ${retryCount + 1}`}
                </p>
                <button
                    onClick={handleRetry}
                    disabled={loading}
                    className="mt-3 px-4 py-2 bg-slate-800 hover:bg-slate-700 rounded-lg text-sm font-mono text-white transition-colors disabled:opacity-50"
                >
                    {loading ? (
                        <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin inline-block mr-1" />
                    ) : (
                        <RefreshCw className="w-3.5 h-3.5 inline mr-1" />
                    )}
                    Retry
                </button>
            </div>
        );
    }

    if (!company) {
        return (
            <div className="text-center py-12">
                <Building className="w-16 h-16 text-slate-600 mx-auto mb-3" />
                <p className="text-slate-400 font-mono text-sm">No company assigned to this user</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* Header */}
            <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                    <Building className="w-5 h-5 text-blue-400" />
                    <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">
                        {editing ? '✏️ Edit Company Profile' : '🏢 Company Profile'}
                    </h3>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={loadCompany}
                        className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
                        title="Refresh"
                        disabled={loading}
                    >
                        <RefreshCw className={`w-4 h-4 text-slate-400 ${loading ? 'animate-spin' : ''}`} />
                    </button>
                    {!editing && (
                        <button
                            onClick={handleEdit}
                            className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-mono transition-colors"
                        >
                            <Edit2 className="w-3.5 h-3.5" />
                            Edit Profile
                        </button>
                    )}
                </div>
            </div>

            {/* Error/Success */}
            {error && (
                <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400">
                    <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                    <span>{error}</span>
                    <button
                        onClick={handleRetry}
                        className="ml-auto text-xs text-red-400 hover:text-red-300 underline"
                    >
                        Retry
                    </button>
                </div>
            )}
            {success && (
                <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400">
                    <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                    <span>{success}</span>
                </div>
            )}

            {/* Company Stats */}
            <div className="grid grid-cols-2 gap-3">
                <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-3 text-center">
                    <Cpu className="w-5 h-5 text-blue-400 mx-auto mb-1" />
                    <p className="text-2xl font-bold text-white">{systemCount}</p>
                    <p className="text-[10px] text-slate-400">Systems</p>
                </div>
                <div className="bg-purple-500/10 border border-purple-500/20 rounded-xl p-3 text-center">
                    <Users className="w-5 h-5 text-purple-400 mx-auto mb-1" />
                    <p className="text-2xl font-bold text-white">{userCount}</p>
                    <p className="text-[10px] text-slate-400">Users</p>
                </div>
            </div>

            {/* Company Details */}
            <div className="bg-slate-950/40 border border-slate-800/80 rounded-2xl p-5 space-y-4">
                
                {/* Code & Status - Read Only */}
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">
                            Company Code
                        </label>
                        <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-slate-400 cursor-not-allowed">
                            {company.companyCode || 'N/A'}
                        </div>
                        <p className="text-[8px] text-slate-500">Cannot be changed</p>
                    </div>
                    <div className="space-y-1.5">
                        <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">
                            Status
                        </label>
                        <div className={`bg-slate-950/80 border rounded-lg px-3 py-2 text-xs font-mono cursor-not-allowed flex items-center gap-2 ${
                            company.status === 'ACTIVE' 
                                ? 'border-emerald-500/30 text-emerald-400' 
                                : 'border-red-500/30 text-red-400'
                        }`}>
                            <span className={`w-2 h-2 rounded-full ${company.status === 'ACTIVE' ? 'bg-emerald-500' : 'bg-red-500'}`}></span>
                            {company.status || 'N/A'}
                        </div>
                        <p className="text-[8px] text-slate-500">Status can only be changed by Admin</p>
                    </div>
                </div>

                {/* Company Name */}
                <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">
                        Company Name <span className="text-red-400">*</span>
                    </label>
                    {editing ? (
                        <input
                            type="text"
                            name="companyName"
                            value={formData.companyName}
                            onChange={handleChange}
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50"
                            required
                        />
                    ) : (
                        <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white">
                            {company.companyName || 'N/A'}
                        </div>
                    )}
                </div>

                {/* Address */}
                <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                        <MapPin className="w-3.5 h-3.5 text-slate-400" />
                        Address
                    </label>
                    {editing ? (
                        <textarea
                            name="address"
                            value={formData.address}
                            onChange={handleChange}
                            rows="2"
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50 resize-none"
                            placeholder="Enter company address"
                        />
                    ) : (
                        <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-300">
                            {company.address || 'No address provided'}
                        </div>
                    )}
                </div>

                {/* Contact Person */}
                <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                        <User className="w-3.5 h-3.5 text-slate-400" />
                        Contact Person
                    </label>
                    {editing ? (
                        <input
                            type="text"
                            name="contactPerson"
                            value={formData.contactPerson}
                            onChange={handleChange}
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50"
                            placeholder="Full name"
                        />
                    ) : (
                        <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white">
                            {company.contactPerson || 'Not specified'}
                        </div>
                    )}
                </div>

                {/* Contact Email & Phone */}
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                            <Mail className="w-3.5 h-3.5 text-slate-400" />
                            Contact Email
                        </label>
                        {editing ? (
                            <input
                                type="email"
                                name="contactEmail"
                                value={formData.contactEmail}
                                onChange={handleChange}
                                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50"
                                placeholder="email@company.com"
                            />
                        ) : (
                            <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white">
                                {company.contactEmail || 'Not specified'}
                            </div>
                        )}
                    </div>
                    <div className="space-y-1.5">
                        <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono flex items-center gap-2">
                            <Phone className="w-3.5 h-3.5 text-slate-400" />
                            Contact Phone
                        </label>
                        {editing ? (
                            <input
                                type="text"
                                name="contactPhone"
                                value={formData.contactPhone}
                                onChange={handleChange}
                                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50"
                                placeholder="+94 77 123 4567"
                            />
                        ) : (
                            <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white">
                                {company.contactPhone || 'Not specified'}
                            </div>
                        )}
                    </div>
                </div>

                {/* Notes */}
                <div className="space-y-1.5">
                    <label className="text-[10px] font-bold tracking-wider uppercase text-slate-400 font-mono">
                        Notes
                    </label>
                    {editing ? (
                        <textarea
                            name="notes"
                            value={formData.notes}
                            onChange={handleChange}
                            rows="2"
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500/50 resize-none"
                            placeholder="Additional notes..."
                        />
                    ) : (
                        <div className="bg-slate-950/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-300">
                            {company.notes || 'No notes'}
                        </div>
                    )}
                </div>

                {/* Security Notice */}
                <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-3">
                    <p className="text-[10px] text-amber-400 font-mono flex items-center gap-2">
                        🔒 <span>You can update company name, address, contact details and notes. Company code and status cannot be changed by users.</span>
                    </p>
                </div>

                {/* Edit Actions */}
                {editing && (
                    <div className="flex gap-3 pt-2 border-t border-slate-800">
                        <button
                            type="button"
                            onClick={handleCancel}
                            className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-lg text-sm font-mono transition-colors"
                            disabled={loading}
                        >
                            <X className="w-4 h-4 inline mr-1" />
                            Cancel
                        </button>
                        <button
                            type="submit"
                            onClick={handleSave}
                            disabled={loading}
                            className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-lg text-sm font-mono transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                            {loading ? (
                                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            ) : (
                                <>
                                    <Save className="w-4 h-4" />
                                    Save Changes
                                </>
                            )}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

CompanyProfile.propTypes = {
    companyId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    username: PropTypes.string.isRequired,
    userRole: PropTypes.string,
    onRefresh: PropTypes.func,
};