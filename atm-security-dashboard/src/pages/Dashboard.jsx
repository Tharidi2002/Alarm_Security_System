import React, { useState, useEffect } from 'react';
import { 
  Shield, AlertTriangle, CheckCircle2, Clock, 
  MapPin, Radio, RefreshCw, Activity, 
  TrendingUp, Zap, Bell, Menu, X 
} from 'lucide-react';
import toast from 'react-hot-toast';
import { alertService } from '../services/alertService';
import Layout from '../components/Layout/Layout';
import StatsCard from '../components/Dashboard/StatsCard';
import AlertList from '../components/Dashboard/AlertList';
import AlertFilters from '../components/Dashboard/AlertFilters';

const Dashboard = () => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, pending: 0, unresolved: 0 });
  const [filters, setFilters] = useState({ status: '', atmId: '' });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [alertsData, statsData] = await Promise.all([
        alertService.getAlerts(page, 20, filters.status || null, filters.atmId || null),
        alertService.getStats(),
      ]);
      
      setAlerts(alertsData.content || []);
      setTotalPages(alertsData.totalPages || 0);
      setStats(statsData);
    } catch (error) {
      console.error('Error fetching data:', error);
      toast.error('Failed to fetch data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 15000);
    return () => clearInterval(interval);
  }, [page, filters]);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    setPage(0);
  };

  return (
    <Layout>
      <div className="space-y-4 sm:space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white">Security Dashboard</h1>
            <p className="text-slate-400 text-xs sm:text-sm">Real-time ATM threat monitoring system</p>
          </div>
          <button
            onClick={fetchData}
            className="flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm transition-all border border-slate-700"
          >
            <RefreshCw className={`w-3 h-3 sm:w-4 sm:h-4 ${loading ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">Refresh</span>
          </button>
        </div>

        {/* Stats - Responsive Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
          <StatsCard
            icon={<Activity className="w-4 h-4 sm:w-5 sm:h-5" />}
            label="Total Alerts"
            value={stats.total}
            color="blue"
          />
          <StatsCard
            icon={<AlertTriangle className="w-4 h-4 sm:w-5 sm:h-5" />}
            label="Active Threats"
            value={stats.pending}
            color="red"
          />
          <StatsCard
            icon={<CheckCircle2 className="w-4 h-4 sm:w-5 sm:h-5" />}
            label="Resolved"
            value={stats.total - stats.pending}
            color="green"
          />
          <StatsCard
            icon={<Bell className="w-4 h-4 sm:w-5 sm:h-5" />}
            label="Unresolved"
            value={stats.unresolved || 0}
            color="yellow"
          />
        </div>

        {/* Filters */}
        <AlertFilters onFilterChange={handleFilterChange} />

        {/* Alerts Table */}
        <AlertList
          alerts={alerts}
          loading={loading}
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
          onAlertUpdate={fetchData}
        />
      </div>
    </Layout>
  );
};

export default Dashboard;