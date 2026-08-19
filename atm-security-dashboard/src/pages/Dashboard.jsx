import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import Navbar from '../components/Navbar';
import StatsCards from '../components/StatsCards';
import AlertTable from '../components/AlertTable';
import NotificationToast from '../components/NotificationToast';
import AdminPanel from '../components/AdminPanel';
import ReportGenerator from '../components/ReportGenerator';
import { useAlerts } from '../hooks/useAlerts';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function Dashboard({ user, onLogout }) {
  const [isAdminPanelOpen, setIsAdminPanelOpen] = useState(false);
  const [isReportOpen, setIsReportOpen] = useState(false);
  const [companyStatus, setCompanyStatus] = useState(null);
  const [checkingCompany, setCheckingCompany] = useState(true);
  
  const { 
    alerts, 
    loading, 
    stats, 
    newAlert,
    clearNewAlert,
    refreshAlerts,
    tableContainerRef 
  } = useAlerts(user.username);

  // ===== COMPANY STATUS CHECK - ONLY FOR USER ROLE =====
  useEffect(() => {
    const checkCompanyStatus = async () => {
      // Only check for USER role
      if (user.role !== 'USER' || !user.companyId) {
        setCheckingCompany(false);
        return;
      }

      try {
        console.log('🔍 Checking company status for:', user.companyId);
        
        const response = await fetch(
          `${API_BASE_URL}/admin/companies/${user.companyId}?username=${encodeURIComponent(user.username)}`
        );
        
        if (response.ok) {
          const data = await response.json();
          console.log('📌 Company data received:', data);
          
          // Handle both response formats (direct or nested)
          const company = data.company || data;
          
          setCompanyStatus({
            isActive: company.status === 'ACTIVE',
            status: company.status || 'UNKNOWN',
            reason: company.inactivationReason || null,
            inactivatedBy: company.inactivatedBy || null,
            inactivatedAt: company.inactivatedAt || null,
            inactivationDescription: company.inactivationDescription || null,
            companyName: company.companyName || user.companyName || 'Unknown',
            companyCode: company.companyCode || user.companyCode || 'N/A'
          });
          
          console.log('✅ Company status set:', company.status);
        } else {
          console.error('❌ Failed to fetch company status:', await response.text());
          // Default to active if can't fetch
          setCompanyStatus({
            isActive: true,
            status: 'ACTIVE',
            reason: null,
            inactivatedBy: null,
            inactivatedAt: null,
            inactivationDescription: null,
            companyName: user.companyName || 'Unknown',
            companyCode: user.companyCode || 'N/A'
          });
        }
      } catch (error) {
        console.error('❌ Error checking company status:', error);
        // Default to active on error
        setCompanyStatus({
          isActive: true,
          status: 'ACTIVE',
          reason: null,
          inactivatedBy: null,
          inactivatedAt: null,
          inactivationDescription: null,
          companyName: user.companyName || 'Unknown',
          companyCode: user.companyCode || 'N/A'
        });
      } finally {
        setCheckingCompany(false);
      }
    };

    checkCompanyStatus();
  }, [user.role, user.companyId, user.username, user.companyName, user.companyCode]);

  const handleAlertResolved = () => {
    refreshAlerts();
  };

  // ===== CHECK IF COMPANY IS INACTIVE =====
  const isCompanyInactive = !checkingCompany && 
    user.role === 'USER' && 
    companyStatus && 
    !companyStatus.isActive;

  // ===== FORMAT DATE =====
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

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans">
      <Navbar 
        user={user} 
        onLogout={onLogout} 
        onOpenAdminPanel={() => setIsAdminPanelOpen(true)}
        onRefresh={refreshAlerts}
        onOpenReport={() => setIsReportOpen(true)}
        onNotificationClick={() => {}}
        isCompanyInactive={isCompanyInactive}  // ← ADD THIS LINE
      />
      
      <main className="p-3 sm:p-4 md:p-6 max-w-7xl mx-auto space-y-4 sm:space-y-6 animate-fade-in">
        
        {/* ===== COMPANY INACTIVE BANNER ===== */}
        {isCompanyInactive && (
          <div className="bg-red-500/10 border-2 border-red-500/50 rounded-xl p-4 sm:p-6 animate-in fade-in duration-300">
            <div className="flex flex-col items-center gap-3 text-center">
              <span className="text-5xl">🚫</span>
              <h2 className="text-2xl font-bold text-red-400">Company is INACTIVE</h2>
              <p className="text-sm text-slate-300 max-w-2xl">
                Your company <span className="font-bold text-white">{companyStatus?.companyName}</span> 
                {companyStatus?.companyCode && (
                  <span className="text-slate-500"> ({companyStatus.companyCode})</span>
                )} has been deactivated.
              </p>
              
              {/* ===== INACTIVE DETAILS ===== */}
              <div className="w-full max-w-md bg-slate-950/60 border border-slate-800 rounded-xl p-4 mt-2 text-left space-y-2">
                {companyStatus?.reason && (
                  <div className="flex flex-col">
                    <span className="text-xs text-yellow-400 font-mono">📌 Reason</span>
                    <span className="text-sm text-white font-medium">{companyStatus.reason}</span>
                  </div>
                )}
                
                {companyStatus?.inactivationDescription && (
                  <div className="flex flex-col">
                    <span className="text-xs text-slate-400 font-mono">📝 Description</span>
                    <span className="text-sm text-slate-300">{companyStatus.inactivationDescription}</span>
                  </div>
                )}
                
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800">
                  {companyStatus?.inactivatedBy && (
                    <div className="flex flex-col">
                      <span className="text-[10px] text-slate-500 font-mono">👤 Deactivated By</span>
                      <span className="text-sm text-white font-medium">{companyStatus.inactivatedBy}</span>
                    </div>
                  )}
                  {companyStatus?.inactivatedAt && (
                    <div className="flex flex-col">
                      <span className="text-[10px] text-slate-500 font-mono">🕐 Deactivated At</span>
                      <span className="text-sm text-white font-medium">{formatDate(companyStatus.inactivatedAt)}</span>
                    </div>
                  )}
                </div>
              </div>
              
              <p className="text-xs text-slate-400 font-mono mt-2">
                ⚠️ Alerts and systems are disabled. Please contact administrator.
              </p>
            </div>
          </div>
        )}

        {/* ===== COMPANY SCOPE ALERT (Only when ACTIVE) ===== */}
        {user.role === 'USER' && user.companyName && !isCompanyInactive && (
          <div className="bg-slate-950/40 border border-slate-800 rounded-xl p-3 sm:p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2 sm:gap-3 font-mono text-[10px] sm:text-xs text-slate-400">
            <div className="flex items-center gap-1 flex-wrap">
              <span>Company Scope:</span>
              <span className="text-blue-400 font-bold text-[10px] sm:text-xs">
                {user.companyName}
              </span>
              {user.companyCode && (
                <span className="text-slate-500">({user.companyCode})</span>
              )}
            </div>
            <div className="text-[9px] sm:text-[10px] text-emerald-400">
              🟢 Active
            </div>
          </div>
        )}

        {/* ===== ONLY SHOW ALERTS IF COMPANY IS ACTIVE ===== */}
        {!isCompanyInactive ? (
          <>
            <StatsCards stats={stats} />
            
            <div className="flex flex-wrap justify-between items-center gap-2">
              <div className="text-xs sm:text-sm text-slate-400">
                Showing <span className="text-white font-bold">{alerts.length}</span> alerts
                {stats.pending > 0 && (
                  <span className="ml-1 sm:ml-2 text-red-400">
                    • <span className="font-bold">{stats.pending}</span> pending
                  </span>
                )}
              </div>
              <div className="text-[10px] sm:text-xs text-slate-500 font-mono">
                Auto-refresh every 5s
              </div>
            </div>

            <AlertTable 
              alerts={alerts} 
              loading={loading} 
              tableContainerRef={tableContainerRef}
              username={user.username}
              onAlertResolved={handleAlertResolved}
            />
          </>
        ) : (
          /* ===== COMPANY INACTIVE - Show locked state ===== */
          <div className="bg-slate-950 border-2 border-red-500/20 rounded-xl p-8 sm:p-12 text-center">
            <div className="text-6xl mb-4">🔒</div>
            <h3 className="text-xl font-bold text-slate-400">Access Restricted</h3>
            <p className="text-sm text-slate-500 mt-2 max-w-md mx-auto">
              Your company is currently inactive. Please contact your administrator to regain access.
            </p>
            {companyStatus?.inactivatedBy && (
              <p className="text-xs text-slate-600 mt-3 font-mono">
                Deactivated by: {companyStatus.inactivatedBy}
                {companyStatus?.inactivatedAt && (
                  <span className="ml-2">• {formatDate(companyStatus.inactivatedAt)}</span>
                )}
              </p>
            )}
          </div>
        )}
      </main>

      <AdminPanel 
        isOpen={isAdminPanelOpen} 
        onClose={() => setIsAdminPanelOpen(false)}
        user={user}
        onSystemChange={refreshAlerts}
      />

      <ReportGenerator
        isOpen={isReportOpen}
        onClose={() => setIsReportOpen(false)}
        user={user}
      />

      {newAlert && !isCompanyInactive && (
        <NotificationToast 
          alert={newAlert} 
          onClose={clearNewAlert} 
        />
      )}
    </div>
  );
}

Dashboard.propTypes = {
  user: PropTypes.shape({
    username: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
    companyId: PropTypes.number,
    companyName: PropTypes.string,
    companyCode: PropTypes.string,
    assignedSystems: PropTypes.array,
  }).isRequired,
  onLogout: PropTypes.func.isRequired,
};