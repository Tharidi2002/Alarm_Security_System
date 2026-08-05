import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { 
  X, AlertTriangle, FileText,
  Trash2, Clock,
  CheckCircle, AlertCircle, Loader2
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function DeleteConfirmationModal({ 
  isOpen, 
  onClose, 
  system, 
  username,
  onConfirm 
}) {
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(true);
  const [checkResult, setCheckResult] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [step, setStep] = useState('check');

  const checkDeletionEligibility = useCallback(async () => {
    if (!isOpen || !system) return;
    
    setChecking(true);
    setError('');
    setCheckResult(null);

    try {
      const url = `${API_BASE_URL}/admin/archive/systems/${system.id}/check?username=${encodeURIComponent(username)}`;
      const response = await fetch(url);
      
      if (response.ok) {
        const data = await response.json();
        setCheckResult(data);
        setStep('confirm');
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to check deletion eligibility');
        setStep('error');
      }
    } catch (err) {
      setError(err.message || 'Network error');
      setStep('error');
    } finally {
      setChecking(false);
    }
  }, [isOpen, system, username]);

  useEffect(() => {
    if (isOpen && system) {
      checkDeletionEligibility();
    }
  }, [isOpen, system, checkDeletionEligibility]);

  const handleArchiveAndDelete = async () => {
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const url = `${API_BASE_URL}/admin/archive/systems/${system.id}/archive-delete?username=${encodeURIComponent(username)}&deleteBy=${encodeURIComponent(username)}`;
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (response.ok) {
        const data = await response.json();
        setSuccess(`✅ System archived and deleted successfully! Archive ID: ${data.archiveId}`);
        setStep('done');
        if (onConfirm) {
          setTimeout(() => {
            onConfirm(data.archiveId);
            onClose();
          }, 2000);
        }
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to delete system');
      }
    } catch (err) {
      setError(err.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = async () => {
    setGenerating(true);
    setError('');

    try {
      const archiveUrl = `${API_BASE_URL}/admin/archive/systems/${system.id}/archive-delete?username=${encodeURIComponent(username)}&deleteBy=${encodeURIComponent(username)}`;
      const archiveResponse = await fetch(archiveUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!archiveResponse.ok) {
        const errorMsg = await archiveResponse.text();
        throw new Error(errorMsg || 'Failed to archive system');
      }

      const archiveData = await archiveResponse.json();
      const archiveId = archiveData.archiveId;

      const reportUrl = `${API_BASE_URL}/admin/archive/${archiveId}/report?username=${encodeURIComponent(username)}`;
      const reportResponse = await fetch(reportUrl);

      if (reportResponse.ok) {
        const reportData = await reportResponse.json();
        
        const blob = new Blob([JSON.stringify(reportData, null, 2)], { 
          type: 'application/json' 
        });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `system_${system.systemCode}_archive_${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        setSuccess(`✅ Report generated and system archived! Archive ID: ${archiveId}`);
        setStep('done');
        
        if (onConfirm) {
          setTimeout(() => {
            onConfirm(archiveId);
            onClose();
          }, 2000);
        }
      } else {
        throw new Error('Failed to generate report');
      }
    } catch (err) {
      setError(err.message || 'Failed to generate report');
    } finally {
      setGenerating(false);
    }
  };

  if (!isOpen || !system) return null;

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-red-500/30 rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-red-500/10">
        
        <div className="flex justify-between items-center p-5 border-b border-slate-800 bg-slate-950/40">
          <div className="flex items-center gap-3">
            <div className="bg-red-500/10 p-2 rounded-lg border border-red-500/20">
              <AlertTriangle className="w-6 h-6 text-red-500" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">⚠️ Delete System</h2>
              <p className="text-xs text-slate-400 font-mono">{system.systemCode}</p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-slate-400 hover:text-white" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          
          {checking && (
            <div className="text-center py-6">
              <div className="w-8 h-8 border-2 border-red-500/30 border-t-red-500 rounded-full animate-spin mx-auto mb-3" />
              <p className="text-slate-400 text-sm font-mono">Checking system data...</p>
            </div>
          )}

          {!checking && checkResult && step === 'confirm' && (
            <>
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                <p className="text-sm text-slate-300">
                  You are about to delete <span className="text-white font-bold">{system.systemCode}</span>.
                  This action will archive all related data.
                </p>

                <div className="grid grid-cols-2 gap-2">
                  <div className="bg-red-500/5 border border-red-500/20 rounded-lg p-3 text-center">
                    <div className="text-2xl font-bold text-red-400">
                      {checkResult.dataSummary?.totalAlerts || 0}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">Total Alerts</div>
                  </div>
                  <div className="bg-yellow-500/5 border border-yellow-500/20 rounded-lg p-3 text-center">
                    <div className="text-2xl font-bold text-yellow-400">
                      {checkResult.dataSummary?.pendingAlerts || 0}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">Pending Alerts</div>
                  </div>
                  <div className="bg-blue-500/5 border border-blue-500/20 rounded-lg p-3 text-center">
                    <div className="text-2xl font-bold text-blue-400">
                      {checkResult.dataSummary?.zones || 0}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">Zones</div>
                  </div>
                  <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-lg p-3 text-center">
                    <div className="text-2xl font-bold text-emerald-400">
                      {system.company?.companyName || 'None'}
                    </div>
                    <div className="text-[10px] text-slate-400 font-mono">Company</div>
                  </div>
                </div>
              </div>

              <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-3">
                <p className="text-amber-400 text-sm font-bold flex items-center gap-2">
                  <Clock className="w-4 h-4" />
                  Data Retention: 6 Months
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  Archived data will be kept for <span className="text-white font-bold">6 months</span> before permanent deletion.
                  You can view archived data anytime during this period.
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
            </>
          )}

          {step === 'done' && (
            <div className="text-center py-6">
              <div className="bg-emerald-500/10 p-4 rounded-full border border-emerald-500/20 mx-auto w-fit mb-3">
                <CheckCircle className="w-10 h-10 text-emerald-500" />
              </div>
              <p className="text-white font-bold text-lg">✅ System Deleted</p>
              <p className="text-slate-400 text-sm font-mono mt-1">
                All data has been archived successfully
              </p>
            </div>
          )}

          {!checking && step === 'confirm' && (
            <div className="space-y-3">
              <div className="flex gap-3">
                <button
                  onClick={handleGenerateReport}
                  disabled={generating}
                  className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-sm font-mono transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {generating ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <FileText className="w-4 h-4" />
                  )}
                  Generate & Archive
                </button>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={handleArchiveAndDelete}
                  disabled={loading}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-500 text-white font-bold rounded-xl text-sm font-mono transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Trash2 className="w-4 h-4" />
                  )}
                  Confirm Delete
                </button>
              </div>

              <button
                onClick={onClose}
                className="w-full py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
              >
                Cancel
              </button>
            </div>
          )}

          {step === 'done' && (
            <button
              onClick={onClose}
              className="w-full py-2.5 bg-slate-700 hover:bg-slate-600 text-white rounded-xl text-sm font-mono transition-colors"
            >
              Close
            </button>
          )}

          {step === 'error' && (
            <div className="text-center">
              <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 mb-3">
                <p className="text-red-400 text-sm">{error}</p>
              </div>
              <div className="flex gap-3">
                <button
                  onClick={onClose}
                  className="flex-1 py-2.5 border border-slate-700 text-slate-400 hover:text-white rounded-xl text-sm font-mono transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={checkDeletionEligibility}
                  className="flex-1 py-2.5 bg-slate-700 hover:bg-slate-600 text-white rounded-xl text-sm font-mono transition-colors"
                >
                  Retry
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

DeleteConfirmationModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  system: PropTypes.shape({
    id: PropTypes.number,
    systemCode: PropTypes.string,
    company: PropTypes.shape({
      companyName: PropTypes.string
    })
  }),
  username: PropTypes.string.isRequired,
  onConfirm: PropTypes.func,
};