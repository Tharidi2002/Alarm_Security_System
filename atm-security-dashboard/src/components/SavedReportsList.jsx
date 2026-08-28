import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { 
  X, FileText, Download, Eye, Trash2, 
  RefreshCw, Search, Calendar, User,
  FileSpreadsheet, Clock, AlertCircle, Loader2
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function SavedReportsList({ isOpen, onClose, user }) {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);  // ← ADD THIS
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedReport, setSelectedReport] = useState(null);
  const [viewModalOpen, setViewModalOpen] = useState(false);

  const loadReports = async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      params.append('username', user.username);
      if (filterType !== 'ALL') {
        params.append('reportType', filterType);
      }
      
      const response = await fetch(`${API_BASE_URL}/reports/saved?${params}`);
      if (response.ok) {
        const data = await response.json();
        setReports(data);
      } else {
        setError('Failed to load saved reports');
      }
    } catch (err) {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadReports();
    }
  }, [isOpen, filterType]);

  const viewReport = async (report) => {
    setSelectedReport(report);
    setViewModalOpen(true);
    
    try {
      await fetch(`${API_BASE_URL}/reports/saved/${report.id}/view?username=${user.username}`, {
        method: 'POST'
      });
    } catch (err) {
      console.error('Failed to track view:', err);
    }
  };

  // ===== DOWNLOAD REPORT - Fixed with setDownloading =====
  const downloadReport = async (report, format) => {
    setDownloading(true);  // ← Now defined
    setError('');
    setSuccess('');

    try {
      const response = await fetch(
        `${API_BASE_URL}/reports/saved/${report.id}/download?username=${user.username}&format=${format}`
      );
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to download ${format.toUpperCase()}`);
      }
      
      const blob = await response.blob();
      
      if (!blob || blob.size === 0) {
        throw new Error('Downloaded file is empty');
      }
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = report.reportName + (format === 'pdf' ? '.pdf' : '.xlsx');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      setSuccess(`✅ ${format.toUpperCase()} downloaded successfully!`);
      setTimeout(() => setSuccess(''), 3000);
      loadReports();
    } catch (err) {
      setError(err.message || 'Failed to download report');
      console.error('Download error:', err);
    } finally {
      setDownloading(false);
    }
  };

  const deleteReport = async (reportId) => {
    if (!window.confirm('Are you sure you want to delete this report?')) return;
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/reports/saved/${reportId}?username=${user.username}`,
        { method: 'DELETE' }
      );
      if (response.ok) {
        setSuccess('✅ Report deleted successfully');
        loadReports();
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch (err) {
      setError('Failed to delete report');
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
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

  const formatFileSize = (bytes) => {
    if (!bytes) return 'N/A';
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i];
  };

  const reportTypes = [
    { value: 'ALL', label: 'All Reports' },
    { value: 'SUMMARY', label: '📊 Summary' },
    { value: 'DETAILED', label: '📋 Detailed' },
    { value: 'HEALTH', label: '💚 Health' },
    { value: 'PERFORMANCE', label: '👤 Performance' },
    { value: 'ALERT_LOGS', label: '📋 Alert Logs' }
  ];

  const filteredReports = reports.filter(report => 
    report.reportName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    report.reportType.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (report.systemCode && report.systemCode.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[170] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-6xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
        {/* HEADER */}
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40 sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <FileText className="w-6 h-6 text-blue-400" />
            <h2 className="text-xl font-bold text-white">📁 Saved Reports</h2>
            <span className="text-xs text-slate-400 font-mono">
              ({reports.length} reports)
            </span>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        {/* CONTENT */}
        <div className="p-5 overflow-y-auto max-h-[calc(90vh-80px)]">
          
          {success && (
            <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-emerald-400 mb-4">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}
          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-3 flex items-start gap-2.5 text-sm text-red-400 mb-4">
              <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* FILTERS */}
          <div className="flex flex-wrap items-center gap-3 mb-4">
            <div className="flex items-center gap-2">
              <label className="text-xs font-bold tracking-wide uppercase text-slate-400 font-mono">Filter:</label>
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-white focus:outline-none focus:border-blue-500/50"
              >
                {reportTypes.map((type) => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>
            
            <div className="relative flex-1 min-w-[150px]">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search reports..."
                className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-8 pr-3 py-1.5 text-xs font-mono text-white placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
              />
            </div>
            
            <button
              onClick={loadReports}
              disabled={loading}
              className="p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
            >
              <RefreshCw className={`w-4 h-4 text-slate-400 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          {/* REPORTS LIST */}
          {loading ? (
            <div className="text-center py-8 text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin mx-auto mb-2 text-slate-600" />
              Loading reports...
            </div>
          ) : filteredReports.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="w-16 h-16 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400 font-mono text-sm">No saved reports found</p>
              <p className="text-xs text-slate-500 mt-1">Generate reports to save them here</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredReports.map((report) => (
                <div 
                  key={report.id} 
                  className="bg-slate-950 border border-slate-800 rounded-xl p-4 hover:border-slate-700 transition-all"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono font-bold text-sm text-white">{report.reportName}</span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
                          {report.reportType}
                        </span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-700 text-slate-300">
                          {report.recordCount || 0} records
                        </span>
                        {report.fileFormat && (
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            {report.fileFormat.toUpperCase()}
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-slate-400 mt-1 flex flex-wrap gap-3">
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3 h-3" />
                          {formatDate(report.generatedAt)}
                        </span>
                        <span className="flex items-center gap-1">
                          <User className="w-3 h-3" />
                          {report.generatedBy}
                        </span>
                        {report.systemCode && (
                          <span>• System: {report.systemCode}</span>
                        )}
                        {report.downloadCount > 0 && (
                          <span>• 📥 {report.downloadCount} downloads</span>
                        )}
                        {report.viewCount > 0 && (
                          <span>• 👁️ {report.viewCount} views</span>
                        )}
                        {report.fileSize && (
                          <span>• {formatFileSize(report.fileSize)}</span>
                        )}
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <button
                        onClick={() => viewReport(report)}
                        className="p-2 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded-lg transition-all"
                        title="View Report"
                        disabled={downloading}
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => downloadReport(report, 'pdf')}
                        disabled={downloading}
                        className="p-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 rounded-lg transition-all disabled:opacity-50"
                        title="Download PDF"
                      >
                        {downloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={() => downloadReport(report, 'excel')}
                        disabled={downloading}
                        className="p-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg transition-all disabled:opacity-50"
                        title="Download Excel"
                      >
                        <FileSpreadsheet className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => deleteReport(report.id)}
                        className="p-2 bg-red-500/10 hover:bg-red-600 text-red-400 hover:text-white border border-red-500/20 hover:border-red-500 rounded-lg transition-all"
                        title="Delete Report"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* VIEW MODAL */}
      {viewModalOpen && selectedReport && (
        <ViewReportModal
          report={selectedReport}
          onClose={() => {
            setViewModalOpen(false);
            setSelectedReport(null);
          }}
          user={user}
          onDownload={(format) => {
            downloadReport(selectedReport, format);
          }}
          downloading={downloading}
        />
      )}
    </div>
  );
}

// ===== VIEW REPORT MODAL =====
function ViewReportModal({ report, onClose, user, onDownload, downloading }) {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const response = await fetch(
          `${API_BASE_URL}/reports/saved/${report.id}/data?username=${user.username}`
        );
        if (response.ok) {
          const reportData = await response.json();
          setData(reportData);
        }
      } catch (err) {
        setError('Failed to load report data');
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [report.id, user.username]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
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

  return (
    <div className="fixed inset-0 z-[180] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl shadow-blue-500/10">
        
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
          <div>
            <h3 className="text-lg font-bold text-white">{report.reportName}</h3>
            <p className="text-xs text-slate-400 font-mono">
              {report.reportType} • Generated: {formatDate(report.generatedAt)} • By: {report.generatedBy}
            </p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors">
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        <div className="p-5 overflow-y-auto max-h-[calc(90vh-120px)]">
          {loading ? (
            <div className="text-center py-8 text-slate-400">
              <Loader2 className="w-8 h-8 animate-spin mx-auto mb-2 text-slate-600" />
              Loading report data...
            </div>
          ) : error ? (
            <div className="text-center py-8 text-red-400">{error}</div>
          ) : (
            <div className="space-y-4">
              {/* Report Summary */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-center">
                  <p className="text-2xl font-bold text-white">{data?.totalRecords || 0}</p>
                  <p className="text-[10px] text-slate-400">Total Records</p>
                </div>
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-center">
                  <p className="text-2xl font-bold text-red-400">{data?.pending || 0}</p>
                  <p className="text-[10px] text-slate-400">Pending</p>
                </div>
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-center">
                  <p className="text-2xl font-bold text-emerald-400">{data?.resolved || 0}</p>
                  <p className="text-[10px] text-slate-400">Resolved</p>
                </div>
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-center">
                  <p className="text-2xl font-bold text-yellow-400">{data?.call || 0}</p>
                  <p className="text-[10px] text-slate-400">CALL/ARMED</p>
                </div>
              </div>

              {/* By System */}
              {data?.bySystem && Object.keys(data.bySystem).length > 0 && (
                <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
                  <h4 className="text-sm font-bold text-white mb-3">📊 Alerts by System</h4>
                  <div className="space-y-2">
                    {Object.entries(data.bySystem).map(([system, count]) => {
                      const total = data.totalRecords || 1;
                      const pct = (count / total) * 100;
                      return (
                        <div key={system} className="flex items-center gap-2">
                          <span className="text-xs text-slate-300 font-mono w-32 truncate">{system}</span>
                          <div className="flex-1 bg-slate-800 rounded-full h-2 overflow-hidden">
                            <div className="h-full bg-blue-500 rounded-full" style={{ width: `${Math.min(pct, 100)}%` }} />
                          </div>
                          <span className="text-xs text-slate-400 font-mono w-12 text-right">{count}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* By Zone */}
              {data?.byZone && Object.keys(data.byZone).length > 0 && (
                <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4">
                  <h4 className="text-sm font-bold text-white mb-3">📍 Alerts by Zone</h4>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {Object.entries(data.byZone)
                      .sort((a, b) => b[1] - a[1])
                      .slice(0, 8)
                      .map(([zone, count]) => (
                        <div key={zone} className="flex justify-between items-center bg-slate-800/50 rounded-lg px-3 py-2">
                          <span className="text-xs text-slate-300 truncate">{zone}</span>
                          <span className="text-xs font-bold text-amber-400">{count}</span>
                        </div>
                      ))}
                  </div>
                </div>
              )}

              {/* Download buttons */}
              <div className="flex gap-3 pt-4 border-t border-slate-800">
                <button
                  onClick={() => onDownload('pdf')}
                  disabled={downloading}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-500 text-white font-bold rounded-xl text-sm transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {downloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                  Download PDF
                </button>
                <button
                  onClick={() => onDownload('excel')}
                  disabled={downloading}
                  className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-xl text-sm transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <FileSpreadsheet className="w-4 h-4" />
                  Download Excel
                </button>
                <button
                  onClick={onClose}
                  className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm transition-all"
                >
                  Close
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

ViewReportModal.propTypes = {
  report: PropTypes.object.isRequired,
  onClose: PropTypes.func.isRequired,
  user: PropTypes.object.isRequired,
  onDownload: PropTypes.func.isRequired,
  downloading: PropTypes.bool,
};

SavedReportsList.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  user: PropTypes.shape({
    username: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
  }).isRequired,
};