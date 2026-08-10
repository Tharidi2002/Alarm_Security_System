import { useState, useEffect, useCallback, useRef } from 'react';
import { fetchAlerts, getPendingCount } from '../services/api';

export function useAlerts(username) {
  // ============================================================
  // ALL HOOKS MUST BE CALLED IN THE SAME ORDER EVERY TIME
  // ============================================================
  
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newAlert, setNewAlert] = useState(null);
  const [stats, setStats] = useState({ total: 0, pending: 0, resolved: 0 });
  const previousAlertIds = useRef(new Set());
  const scrollPositionRef = useRef(0);
  const tableContainerRef = useRef(null);

  const calculateStats = (data) => {
    const pending = data.filter(a => a.status === 'PENDING').length;
    const resolved = data.filter(a => a.status === 'RESOLVED').length;
    return { total: data.length, pending, resolved };
  };

  const loadAlerts = useCallback(async (showLoading = true) => {
    try {
      if (showLoading) setLoading(true);
      
      if (tableContainerRef.current) {
        scrollPositionRef.current = tableContainerRef.current.scrollTop;
      }

      const data = await fetchAlerts(username);
      const filteredData = (data || []).filter(alert => alert.status !== 'REJECTED');
      
      const currentIds = new Set(filteredData.map(a => a.id));
      const newAlerts = filteredData.filter(a => !previousAlertIds.current.has(a.id));
      
      if (newAlerts.length > 0 && previousAlertIds.current.size > 0) {
        const pendingNewAlert = newAlerts.find(a => a.status === 'PENDING');
        if (pendingNewAlert) {
          setNewAlert(pendingNewAlert);
        }
      }
      
      previousAlertIds.current = currentIds;
      setAlerts(filteredData);
      setStats(calculateStats(filteredData));
      
      setTimeout(() => {
        if (tableContainerRef.current) {
          tableContainerRef.current.scrollTop = scrollPositionRef.current;
        }
      }, 0);
      
    } catch (error) {
      console.error('Error loading alerts:', error);
    } finally {
      if (showLoading) setLoading(false);
    }
  }, [username]);

  const clearNewAlert = useCallback(() => {
    setNewAlert(null);
  }, []);

  // ============================================================
  // FIXED: useEffect for auto-refresh
  // ============================================================
  useEffect(() => {
    loadAlerts(true);
    
    const interval = setInterval(() => loadAlerts(false), 5000);
    return () => clearInterval(interval);
  }, [loadAlerts]); // loadAlerts is already memoized with useCallback

  // ============================================================
  // FIXED: Separate useEffect for stats
  // ============================================================
  useEffect(() => {
    const updateStats = async () => {
      try {
        const data = await getPendingCount(username);
        setStats(prev => ({ ...prev, pending: data.pending || 0, resolved: data.resolved || 0 }));
      } catch (error) {
        console.error('Error updating stats:', error);
      }
    };
    
    updateStats();
    const statsInterval = setInterval(updateStats, 10000);
    return () => clearInterval(statsInterval);
  }, [username]);

  return { 
    alerts, 
    loading, 
    stats, 
    newAlert,
    clearNewAlert,
    refreshAlerts: () => loadAlerts(true),
    tableContainerRef
  };
}