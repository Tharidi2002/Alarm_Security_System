import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
  X, Database, Search, Eye,
  Download, RefreshCw,
  Building, AlertCircle
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function ArchivedSystems({ isOpen, onClose, username }) {
  const [archives, setArchives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedArchive, setSelectedArchive] = useState(null);
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [archiveData, setArchiveData] = useState(null);

  const loadArchives = useCallback(async () => {
    if (!isOpen) return;
    
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
  }, [isOpen, username]);

  useEffect(() => {
    if (isOpen) {
      loadArchives();
    }
  }, [isOpen, loadArchives]);

  const viewArchive = async (archiveId) => {
    setSelectedArchive(archives.find(a => a.id === archiveId));
    setLoading(true);
    try {
      const url = `${API_BASE_URL}/admin/archive/${archiveId}/report?username=${encodeURIComponent(username)}`;
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        setArchiveData(data);
        setViewModalOpen(true);
      } else {
        setError('Failed to load archive details');
      }
    } catch {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  const downloadReport = async (archiveId) => {
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
        const archive = archives.find(a => a.id === archiveId);
        link.download = `archive_${archive?.systemCode || 'system'}_${new Date().toISOString().split('T')[0]}.json`;
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
    if (!archive.retentionUntil) return 'Unknown';
    const now = new Date();
    const retention = new Date(archive.retentionUntil);
    const daysLeft = Math.ceil((retention - now) / (1000 * 60 * 60 * 24));
    
    if (daysLeft <= 0) return 'Expired';
    if (daysLeft <= 30) return `Expires in ${daysLeft}d`;
    if (daysLeft <= 90) return `${daysLeft}d remaining`;
    return `${Math.ceil(daysLeft / 30)} months remaining`;
  };

  const getStatusColor = (archive) => {
    const status = getRetentionStatus(archive);
    if (status === 'Expired') return 'text-red-400 bg-red-500/10 border-red-500/20';
    if (status.includes('Expires')) return 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20';
    return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
  };

  const filteredArchives = archives.filter(a => 
    a.systemCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    a.location?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    a.companyName?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[150] flex items-center justify-end p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center gap-3">
            <div className="bg-blue-500/10 p-2 rounded-lg border border-blue-500/20">
              <Database className="w-5 h-5 text-blue-400" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">🗄️ Archived Systems</h2>
              <p className="text-xs text-slate-400 font-mono">
                {archives.length} archived systems • Data retained for 6 months
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={loadArchives}
              className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <RefreshCw className="w-4 h-4 text-slate-400" />
            </button>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
            >
              <X className="w-5 h-5 text-slate-400 hover:text-white" />
            </button>
          </div>
        </div>

        <div className="p-5 overflow-y-auto max-h-[calc(90vh-100px)]">
          
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search archived systems..."
              className="w-full bg-slate-800 border border-slate-700 rounded-xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
            />
          </div>

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400 mb-4">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

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
              {filteredArchives.map((archive) => (
                <div 
                  key={archive.id}
                  className="bg-slate-950 border border-slate-800 rounded-xl p-4 hover:border-slate-700 transition-all"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono font-bold text-sm text-white">{archive.systemCode}</span>
                        <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${getStatusColor(archive)}`}>
                          {getRetentionStatus(archive)}
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
                      <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-4">
                        <span className="flex items-center gap-1">
                          <span>🗓️</span>
                          Archived: {formatDate(archive.archivedAt)}
                        </span>
                        <span className="flex items-center gap-1">
                          <span>⏰</span>
                          Deleted: {formatDate(archive.deletedAt)}
                        </span>
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
                        onClick={() => downloadReport(archive.id)}
                        className="px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-mono transition-all flex items-center gap-1.5"
                      >
                        <Download className="w-3.5 h-3.5" />
                        Report
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <ArchiveViewModal
        isOpen={viewModalOpen}
        onClose={() => {
          setViewModalOpen(false);
          setArchiveData(null);
          setSelectedArchive(null);
        }}
        archive={selectedArchive}
        data={archiveData}
      />
    </div>
  );
}

// ===== ARCHIVE VIEW MODAL =====
function ArchiveViewModal({ isOpen, onClose, archive, data }) {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isOpen && data) {
      setLoading(false);
    }
  }, [isOpen, data]);

  if (!isOpen) return null;

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

  let parsedData = null;
  try {
    if (data?.archiveData) {
      parsedData = typeof data.archiveData === 'string' 
        ? JSON.parse(data.archiveData) 
        : data.archiveData;
    }
  } catch {
    console.error('Failed to parse archive data');
  }

  const alerts = parsedData?.alerts || [];
  const zones = parsedData?.zones || [];

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Database className="w-5 h-5 text-blue-400" />
              {archive?.systemCode || 'Archive Details'}
            </h3>
            <p className="text-xs text-slate-400 font-mono">
              Archived on {formatDate(archive?.archivedAt)}
            </p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        <div className="p-5 overflow-y-auto max-h-[calc(90vh-120px)]">
          {loading ? (
            <div className="text-center py-8 text-slate-400">Loading...</div>
          ) : (
            <div className="space-y-4">
              {parsedData?.system && (
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
                  <h4 className="text-sm font-bold text-white mb-3">📋 System Information</h4>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div>
                      <span className="text-slate-400">Code:</span>
                      <span className="text-white ml-2">{parsedData.system.systemCode}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Location:</span>
                      <span className="text-white ml-2">{parsedData.system.location || 'N/A'}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">SIM:</span>
                      <span className="text-white ml-2 font-mono">{parsedData.system.simNumber}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Status:</span>
                      <span className={`ml-2 ${parsedData.system.status === 'ACTIVE' ? 'text-emerald-400' : 'text-red-400'}`}>
                        {parsedData.system.status}
                      </span>
                    </div>
                  </div>
                </div>
              )}

              {zones.length > 0 && (
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
                  <h4 className="text-sm font-bold text-white mb-3">📡 Zones ({zones.length})</h4>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                    {zones.map((zone) => (
                      <div key={zone.id} className="bg-slate-900/50 border border-slate-800 rounded-lg px-3 py-2 text-sm">
                        <span className="font-mono text-blue-400">Z{String(zone.zoneNumber).padStart(2, '0')}</span>
                        <span className="text-slate-300 ml-2">{zone.zoneName}</span>
                        <span className={`text-[10px] ml-2 px-1.5 py-0.5 rounded ${zone.isActive ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                          {zone.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {alerts.length > 0 && (
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4">
                  <h4 className="text-sm font-bold text-white mb-3">🚨 Alerts ({alerts.length})</h4>
                  <div className="max-h-48 overflow-y-auto space-y-1">
                    {alerts.slice(0, 20).map((alert) => (
                      <div key={alert.id} className="flex items-center justify-between bg-slate-900/30 border border-slate-800 rounded-lg px-3 py-2 text-sm">
                        <div>
                          <span className="font-mono text-slate-400">#{alert.id}</span>
                          <span className="text-slate-300 ml-2">{alert.alertType}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`text-[10px] px-2 py-0.5 rounded ${
                            alert.status === 'PENDING' ? 'bg-red-500/20 text-red-400' :
                            alert.status === 'RESOLVED' ? 'bg-emerald-500/20 text-emerald-400' :
                            'bg-slate-500/20 text-slate-400'
                          }`}>
                            {alert.status}
                          </span>
                          <span className="text-[10px] text-slate-500 font-mono">
                            {formatDate(alert.receivedAt)}
                          </span>
                        </div>
                      </div>
                    ))}
                    {alerts.length > 20 && (
                      <div className="text-center text-xs text-slate-500 font-mono py-2">
                        Showing 20 of {alerts.length} alerts
                      </div>
                    )}
                  </div>
                </div>
              )}

              {!parsedData && (
                <div className="text-center text-slate-400 py-8">
                  <p>No data available for this archive</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

ArchiveViewModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  archive: PropTypes.object,
  data: PropTypes.object,
};

ArchivedSystems.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  username: PropTypes.string.isRequired,
};