import { useState } from 'react';
import PropTypes from 'prop-types';
import Navbar from '../components/Navbar';
import StatsCards from '../components/StatsCards';
import AlertTable from '../components/AlertTable';
import NotificationToast from '../components/NotificationToast';
import AdminPanel from '../components/AdminPanel';
import ReportGenerator from '../components/ReportGenerator';
import { useAlerts } from '../hooks/useAlerts';

export default function Dashboard({ user, onLogout }) {
  const [isAdminPanelOpen, setIsAdminPanelOpen] = useState(false);
  const [isReportOpen, setIsReportOpen] = useState(false);
  
  const { 
    alerts, 
    loading, 
    stats, 
    newAlert,
    clearNewAlert,
    refreshAlerts,
    tableContainerRef 
  } = useAlerts(user.username);

  const handleAlertResolved = () => {
    refreshAlerts();
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans">
      <Navbar 
        user={user} 
        onLogout={onLogout} 
        onOpenAdminPanel={() => setIsAdminPanelOpen(true)}
        onRefresh={refreshAlerts}
        onOpenReport={() => setIsReportOpen(true)}
      />
      
      <main className="p-3 sm:p-4 md:p-6 max-w-7xl mx-auto space-y-4 sm:space-y-6 animate-fade-in">
        
        {/* NEW: Company Scope Alert Notification */}
        {user.role === 'USER' && user.companyName && (
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
            <div className="text-[9px] sm:text-[10px] text-slate-500">
              Only alerts from your company are visible.
            </div>
          </div>
        )}

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
      </main>

      {/* NEW: Pass user to AdminPanel for role-based access */}
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

      {newAlert && (
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