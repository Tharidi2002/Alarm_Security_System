import { useState } from 'react';
import PropTypes from 'prop-types';
import Navbar from '../components/Navbar';
import StatsCards from '../components/StatsCards';
import AlertTable from '../components/AlertTable';
import NotificationToast from '../components/NotificationToast';
import AdminPanel from '../components/AdminPanel';
import { useAlerts } from '../hooks/useAlerts';

export default function Dashboard({ user, onLogout }) {
  const [isAdminPanelOpen, setIsAdminPanelOpen] = useState(false);
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
      />
      
      <main className="p-4 sm:p-6 max-w-7xl mx-auto space-y-6 animate-fade-in">
        {/* User Scope Alert Notification */}
        {user.role === 'USER' && (
          <div className="bg-slate-950/40 border border-slate-800 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 font-mono text-xs text-slate-400">
            <div>
              <span>Monitoring Scope: </span>
              {user.assignedSystems && user.assignedSystems.length > 0 ? (
                <span className="text-emerald-400 font-bold">
                  {user.assignedSystems.join(', ')}
                </span>
              ) : (
                <span className="text-red-400 font-bold">No systems assigned</span>
              )}
            </div>
            <div className="text-[10px] text-slate-500">
              Only alerts belonging to your scope are visible.
            </div>
          </div>
        )}

        <StatsCards stats={stats} />
        
        {/* Alerts count badge */}
        <div className="flex justify-between items-center">
          <div className="text-sm text-slate-400">
            Showing <span className="text-white font-bold">{alerts.length}</span> alerts
            {stats.pending > 0 && (
              <span className="ml-2 text-red-400">
                • <span className="font-bold">{stats.pending}</span> pending
              </span>
            )}
          </div>
          <div className="text-xs text-slate-500 font-mono">
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

      {/* Admin Panel Drawer */}
      <AdminPanel 
        isOpen={isAdminPanelOpen} 
        onClose={() => setIsAdminPanelOpen(false)} 
      />

      {/* Notification Toast - New alert */}
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
    assignedSystems: PropTypes.array,
  }).isRequired,
  onLogout: PropTypes.func.isRequired,
};