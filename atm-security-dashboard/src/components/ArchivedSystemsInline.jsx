// components/ArchivedSystemsInline.jsx
import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
  Database, Search, Eye, Download, RefreshCw, 
  Building, AlertCircle, ChevronRight, Clock
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function ArchivedSystemsInline({ username, onRefresh }) {
  const [archives, setArchives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedArchive, setExpandedArchive] = useState(null);

  const loadArchives = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const url = `${API_BASE_URL}/admin/archive${username ? `?username=${encodeURIComponent(username)}` : ''}`;
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        setArchives(data);
      } else {
        setError('Failed to load archived systems');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  }, [username]);

  useEffect(() => {
    loadArchives();
  }, [loadArchives]);

  const viewArchive = async (archiveId) => {
    setExpandedArchive(expandedArchive === archiveId ? null : archiveId);
  };

  const downloadReport = async (archiveId, systemCode) => {
    try {
      const url = `${API_BASE_URL}/admin/archive/${archiveId}/report?username=${encodeURIComponent(username)}`;
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        const blob = new Blob([JSON.stringify(data, null, 2)], { 
          type: 'application/json' 
        });
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `archive_${systemCode}_${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
      }
    } catch {
      setError('Failed to download report');
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

  const getRetentionStatus = (archive) => {
    if (!archive.retentionUntil) return { label: 'Unknown', color: 'text-slate-400' };
    const now = new Date();
    const retention = new Date(archive.retentionUntil);
    const daysLeft = Math.ceil((retention - now) / (1000 * 60 * 60 * 24));
    
    if (daysLeft <= 0) return { label: 'Expired', color: 'text-red-400' };
    if (daysLeft <= 30) return { label: `Expires in ${daysLeft}d`, color: 'text-yellow-400' };
    if (daysLeft <= 90) return { label: `${daysLeft}d remaining`, color: 'text-blue-400' };
    return { label: `${Math.ceil(daysLeft / 30)} months remaining`, color: 'text-emerald-400' };
  };

  const filteredArchives = archives.filter(a => 
    a.systemCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    a.location?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    a.companyName?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <Database className="w-5 h-5 text-blue-400" />
          <h3 className="text-sm font-bold tracking-wide uppercase text-white font-mono">
            Archived Systems
          </h3>
          <span className="text-xs text-slate-400 font-mono">
            ({archives.length} archived systems • Data retained for 6 months)
          </span>
        </div>
        <button
          onClick={() => { loadArchives(); if (onRefresh) onRefresh(); }}
          className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
          title="Refresh"
        >
          <RefreshCw className="w-4 h-4 text-slate-400" />
        </button>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search archived systems..."
          className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
        />
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400">
          <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* List */}
      {loading ? (
        <div className="text-center py-8 text-slate-400 font-mono text-sm">
          Loading archived systems...
        </div>
      ) : filteredArchives.length === 0 ? (
        <div className="text-center py-12">
          <Database className="w-16 h-16 text-slate-600 mx-auto mb-3" />
          <p className="text-slate-400 font-mono text-sm">
            {searchQuery ? 'No archived systems found' : 'No systems have been archived yet'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredArchives.map((archive) => {
            const retention = getRetentionStatus(archive);
            const isExpanded = expandedArchive === archive.id;
            
            return (
              <div 
                key={archive.id}
                className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden hover:border-slate-700 transition-all"
              >
                <div className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono font-bold text-sm text-white">{archive.systemCode}</span>
                      <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${retention.color} bg-opacity-10 border-current/20`}>
                        <Clock className="w-3 h-3 inline mr-1" />
                        {retention.label}
                      </span>
                      {archive.companyName && (
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 flex items-center gap-1">
                          <Building className="w-3 h-3" />
                          {archive.companyName}
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-slate-400 mt-1">
                      {archive.location && <span>{archive.location} • </span>}
                      <span>Alerts: <span className="text-white font-medium">{archive.alertCount || 0}</span></span>
                      <span className="mx-2">•</span>
                      <span>Zones: <span className="text-white font-medium">{archive.zoneCount || 0}</span></span>
                    </div>
                    <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-4 flex-wrap">
                      <span className="flex items-center gap-1">
                        <span>🗓️</span>
                        Archived: {formatDate(archive.archivedAt)}
                      </span>
                      <span className="flex items-center gap-1">
                        <span>⏰</span>
                        Deleted: {formatDate(archive.deletedAt)}
                      </span>
                      {archive.deletedBy && (
                        <span className="flex items-center gap-1">
                          <span>👤</span>
                          By: {archive.deletedBy}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button
                      onClick={() => viewArchive(archive.id)}
                      className="px-3 py-1.5 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded-lg text-xs font-mono transition-all flex items-center gap-1.5"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      View
                    </button>
                    <button
                      onClick={() => downloadReport(archive.id, archive.systemCode)}
                      className="px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-mono transition-all flex items-center gap-1.5"
                    >
                      <Download className="w-3.5 h-3.5" />
                      Report
                    </button>
                    <button
                      onClick={() => viewArchive(archive.id)}
                      className="p-1.5 hover:bg-slate-800 rounded-lg transition-colors"
                    >
                      <ChevronRight className={`w-4 h-4 text-slate-400 transition-transform ${isExpanded ? 'rotate-90' : ''}`} />
                    </button>
                  </div>
                </div>

                {/* Expanded Details */}
                {isExpanded && (
                  <div className="border-t border-slate-800 p-4 bg-slate-900/30">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h4 className="text-xs font-bold text-slate-400 font-mono uppercase tracking-wider mb-2">
                          System Details
                        </h4>
                        <div className="space-y-1 text-sm">
                          <div className="flex justify-between">
                            <span className="text-slate-400">System Code</span>
                            <span className="text-white font-mono">{archive.systemCode}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Location</span>
                            <span className="text-white">{archive.location || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">SIM Number</span>
                            <span className="text-white font-mono">{archive.simNumber || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Panel SIM</span>
                            <span className="text-white font-mono">{archive.panelSimNumber || 'N/A'}</span>
                          </div>
                        </div>
                      </div>
                      <div>
                        <h4 className="text-xs font-bold text-slate-400 font-mono uppercase tracking-wider mb-2">
                          Archive Info
                        </h4>
                        <div className="space-y-1 text-sm">
                          <div className="flex justify-between">
                            <span className="text-slate-400">Total Alerts</span>
                            <span className="text-white font-bold">{archive.alertCount || 0}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Total Zones</span>
                            <span className="text-white font-bold">{archive.zoneCount || 0}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Archived By</span>
                            <span className="text-white">{archive.archivedBy || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Retention</span>
                            <span className={`font-bold ${retention.color}`}>{retention.label}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

ArchivedSystemsInline.propTypes = {
  username: PropTypes.string.isRequired,
  onRefresh: PropTypes.func,
};