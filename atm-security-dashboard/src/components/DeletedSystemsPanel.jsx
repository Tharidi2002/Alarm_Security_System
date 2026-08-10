import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { X, Trash2, RefreshCw, AlertCircle, CheckCircle, User, Clock, Database } from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function DeletedSystemsPanel({ isOpen, onClose, username, onSystemDeleted }) {
    const [deletedSystems, setDeletedSystems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [deleting, setDeleting] = useState(null);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const loadDeletedSystems = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await fetch(`${API_BASE_URL}/admin/systems/deleted?username=${encodeURIComponent(username)}`);
            if (response.ok) {
                const data = await response.json();
                setDeletedSystems(data);
            } else {
                const msg = await response.text();
                setError(msg || 'Failed to load deleted systems');
            }
        } catch (err) {
            setError('Network error loading deleted systems');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            loadDeletedSystems();
        }
    }, [isOpen]);

    const handlePermanentDelete = async (systemId, systemCode) => {
        if (!window.confirm(`⚠️ Are you sure you want to PERMANENTLY delete system "${systemCode}"?\n\nThis action CANNOT be undone! All data will be permanently removed.`)) {
            return;
        }

        setDeleting(systemId);
        setError('');
        setSuccess('');

        try {
            const response = await fetch(`${API_BASE_URL}/admin/systems/${systemId}/permanent?username=${encodeURIComponent(username)}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                const msg = await response.text();
                setSuccess(`✅ ${systemCode} permanently deleted`);
                setDeletedSystems(prev => prev.filter(s => s.id !== systemId));
                if (onSystemDeleted) onSystemDeleted();
                setTimeout(() => setSuccess(''), 3000);
            } else {
                const msg = await response.text();
                setError(msg || 'Failed to permanently delete system');
            }
        } catch (err) {
            setError('Network error deleting system');
        } finally {
            setDeleting(null);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return 'N/A';
        try {
            const date = new Date(dateStr);
            return date.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
            <div className="bg-slate-900 border border-red-500/30 rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-red-500/10">
                
                {/* Header */}
                <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
                    <div className="flex items-center gap-3">
                        <div className="bg-red-500/10 p-2 rounded-lg border border-red-500/20">
                            <Database className="w-5 h-5 text-red-400" />
                        </div>
                        <div>
                            <h2 className="text-lg font-bold text-white">🗑️ Deleted Systems</h2>
                            <p className="text-xs text-slate-400 font-mono">
                                {deletedSystems.length} systems marked for deletion
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={loadDeletedSystems}
                            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                            disabled={loading}
                        >
                            <RefreshCw className={`w-4 h-4 text-slate-400 ${loading ? 'animate-spin' : ''}`} />
                        </button>
                        <button 
                            onClick={onClose}
                            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                            <X className="w-5 h-5 text-slate-400 hover:text-white" />
                        </button>
                    </div>
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
                    <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-3 mb-4">
                        <p className="text-xs text-amber-400 font-mono flex items-center gap-2">
                            ⚠️ <span>Permanently deleting a system will remove ALL data. This action CANNOT be undone.</span>
                        </p>
                    </div>

                    {loading ? (
                        <div className="text-center py-8 text-slate-400 font-mono text-sm">
                            <div className="w-8 h-8 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mx-auto mb-3" />
                            Loading deleted systems...
                        </div>
                    ) : deletedSystems.length === 0 ? (
                        <div className="text-center py-12">
                            <Database className="w-16 h-16 text-slate-600 mx-auto mb-3" />
                            <p className="text-slate-400 font-mono text-sm">No deleted systems found</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {deletedSystems.map((system) => (
                                <div 
                                    key={system.id}
                                    className="bg-slate-950 border border-slate-800 rounded-xl p-4 hover:border-slate-700 transition-all"
                                >
                                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <span className="font-mono font-bold text-sm text-red-400">
                                                    {system.systemCode}
                                                </span>
                                                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-red-500/10 text-red-400 border border-red-500/20">
                                                    {system.status || 'DELETED'}
                                                </span>
                                                {system.company && (
                                                    <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
                                                        {system.company.companyName}
                                                    </span>
                                                )}
                                            </div>
                                            <div className="text-xs text-slate-400 mt-1">
                                                <span>Location: {system.location || 'N/A'}</span>
                                                <span className="mx-2">•</span>
                                                <span>SIM: {system.simNumber || 'N/A'}</span>
                                            </div>
                                            <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-4 flex-wrap">
                                                <span className="flex items-center gap-1">
                                                    <User className="w-3 h-3" />
                                                    Deleted By: {system.deletedBy || 'Unknown'}
                                                </span>
                                                <span className="flex items-center gap-1">
                                                    <Clock className="w-3 h-3" />
                                                    Deleted At: {formatDate(system.deletedAt)}
                                                </span>
                                            </div>
                                        </div>

                                        <button
                                            onClick={() => handlePermanentDelete(system.id, system.systemCode)}
                                            disabled={deleting === system.id}
                                            className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-mono transition-all flex items-center gap-1.5 disabled:opacity-50"
                                        >
                                            {deleting === system.id ? (
                                                <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                            ) : (
                                                <Trash2 className="w-3.5 h-3.5" />
                                            )}
                                            Permanent Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-slate-800 bg-slate-950/20 flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg text-sm font-mono transition-colors"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}

DeletedSystemsPanel.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    username: PropTypes.string.isRequired,
    onSystemDeleted: PropTypes.func,
};