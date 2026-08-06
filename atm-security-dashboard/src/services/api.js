const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// ============================================================
// AUTH & USER
// ============================================================

export const login = async (username, password) => {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Login failed');
  }
  return await response.json();
};

export const getCurrentUser = async (username) => {
  const response = await fetch(`${API_BASE_URL}/admin/me?username=${encodeURIComponent(username)}`);
  if (!response.ok) throw new Error('Failed to get user info');
  return await response.json();
};

// ============================================================
// SYSTEM CODE - NEW
// ============================================================

export const getNextSystemCode = async () => {
  const response = await fetch(`${API_BASE_URL}/admin/systems/next-code`);
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to get next system code');
  }
  return await response.json();
};

// ============================================================
// ALERTS - COMPANY-BASED
// ============================================================

export const fetchAlerts = async (username) => {
  try {
    const url = username 
      ? `${API_BASE_URL}/alerts?username=${encodeURIComponent(username)}` 
      : `${API_BASE_URL}/alerts`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch alerts');
    return await response.json();
  } catch (error) {
    console.error('Error fetching alerts:', error);
    return [];
  }
};

export const resolveAlert = async (alertId, resolvedBy, description, username) => {
  try {
    let url = `${API_BASE_URL}/alerts/${alertId}/resolve?resolvedBy=${encodeURIComponent(resolvedBy)}`;
    if (description) {
      url += `&description=${encodeURIComponent(description)}`;
    }
    if (username) {
      url += `&username=${encodeURIComponent(username)}`;
    }
    
    const response = await fetch(url, { method: 'PUT' });
    if (!response.ok) {
      const errorMsg = await response.text();
      throw new Error(errorMsg || 'Failed to resolve alert');
    }
    return await response.json();
  } catch (error) {
    console.error('Error resolving alert:', error);
    throw error;
  }
};

export const getAlertDetails = async (alertId, username) => {
  try {
    const url = username 
      ? `${API_BASE_URL}/alerts/${alertId}/details?username=${encodeURIComponent(username)}`
      : `${API_BASE_URL}/alerts/${alertId}/details`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch alert details');
    return await response.json();
  } catch (error) {
    console.error('Error fetching alert details:', error);
    return null;
  }
};

export const getPendingCount = async (username) => {
  try {
    const url = username 
      ? `${API_BASE_URL}/alerts/pending/count?username=${encodeURIComponent(username)}`
      : `${API_BASE_URL}/alerts/pending/count`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch counts');
    return await response.json();
  } catch (error) {
    console.error('Error fetching counts:', error);
    return { pending: 0, resolved: 0 };
  }
};

// ============================================================
// USERS - WITH COMPANY
// ============================================================

export const fetchUsers = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/users`;
  const params = new URLSearchParams();
  if (companyId) params.append('companyId', companyId);
  if (username) params.append('username', username);
  if (params.toString()) url += `?${params.toString()}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch users');
  return await response.json();
};

export const createUser = async (userData) => {
  const response = await fetch(`${API_BASE_URL}/admin/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(userData),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to create user');
  }
  return await response.json();
};

export const deleteUser = async (userId) => {
  const response = await fetch(`${API_BASE_URL}/admin/users/${userId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to delete user');
  }
  return true;
};

export const resetUserPassword = async (userId, newPassword) => {
  const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/reset-password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newPassword }),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to reset password');
  }
  return await response.json();
};

export const assignSystems = async (userId, systemIds) => {
  const response = await fetch(`${API_BASE_URL}/admin/users/${userId}/assign`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ systemIds }),
  });
  if (!response.ok) throw new Error('Failed to assign systems');
  return true;
};

// ============================================================
// SYSTEMS - COMPANY-BASED
// ============================================================

export const fetchSystems = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/systems`;
  const params = new URLSearchParams();
  
  if (companyId) {
    params.append('companyId', companyId);
  }
  
  if (username) {
    params.append('username', username);
  }
  
  if (params.toString()) {
    url += `?${params.toString()}`;
  }
  
  console.log('Fetching systems from:', url);
  
  const response = await fetch(url);
  if (!response.ok) {
    const errorMsg = await response.text();
    console.error('Failed to fetch systems:', errorMsg);
    throw new Error(errorMsg || 'Failed to fetch systems');
  }
  
  const data = await response.json();
  console.log('Systems fetched:', data);
  return data;
};

export const getSystemById = async (systemId) => {
  const response = await fetch(`${API_BASE_URL}/admin/systems/${systemId}`);
  if (!response.ok) throw new Error('Failed to fetch system');
  return await response.json();
};

export const createSystem = async (systemData, companyId, username) => {
  let url = `${API_BASE_URL}/admin/systems`;
  const params = new URLSearchParams();
  
  if (username) {
    params.append('username', username);
  }
  if (companyId) {
    params.append('companyId', companyId);
  }
  
  if (params.toString()) {
    url += `?${params.toString()}`;
  }
  
  console.log('Creating system with URL:', url);
  console.log('System data:', systemData);
  
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(systemData),
  });
  
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to create system');
  }
  return await response.json();
};

export const updateSystem = async (systemId, systemData, companyId, username) => {
  let url = `${API_BASE_URL}/admin/systems/${systemId}`;
  const params = new URLSearchParams();
  if (companyId !== undefined && companyId !== null) params.append('companyId', companyId);
  if (username) params.append('username', username);
  if (params.toString()) url += `?${params.toString()}`;
  
  const response = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(systemData),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to update system');
  }
  return await response.json();
};

export const toggleSystemStatus = async (systemId, status, username) => {
  let url = `${API_BASE_URL}/admin/systems/${systemId}/status`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to change status');
  }
  return await response.json();
};

export const deleteSystem = async (systemId, username) => {
  try {
    let url = `${API_BASE_URL}/admin/systems/${systemId}`;
    if (username) url += `?username=${encodeURIComponent(username)}`;
    
    console.log('Deleting system:', url);
    
    const response = await fetch(url, {
      method: 'DELETE',
    });
    
    if (!response.ok) {
      const errorMsg = await response.text();
      console.error('Delete system error response:', errorMsg);
      throw new Error(errorMsg || 'Failed to delete system');
    }
    
    const result = await response.text();
    console.log('Delete system result:', result);
    return true;
    
  } catch (error) {
    console.error('Error deleting system:', error);
    throw error;
  }
};

// ============================================================
// SYSTEM COMMANDS
// ============================================================

export const disarmSystem = async (systemCode, triggeredBy, username) => {
  let url = `${API_BASE_URL}/alerts/disarm`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ systemCode, triggeredBy }),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to disarm system');
  }
  return await response.json();
};

export const stopSiren = async (systemCode, triggeredBy, username) => {
  let url = `${API_BASE_URL}/alerts/stop-siren`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ systemCode, triggeredBy }),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to stop siren');
  }
  return await response.json();
};

export const sendSystemCommand = async (atmCode, command, username) => {
  let url = `${API_BASE_URL}/alerts/set-command?atmCode=${encodeURIComponent(atmCode)}&command=${encodeURIComponent(command)}`;
  if (username) url += `&username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, { method: 'POST' });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || `Failed to send command ${command}`);
  }
  return await response.json();
};

// ============================================================
// COMPANIES
// ============================================================

export const fetchCompanies = async (username) => {
  let url = `${API_BASE_URL}/admin/companies`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch companies');
  return await response.json();
};

export const getCompanyById = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/companies/${companyId}`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch company');
  return await response.json();
};

export const getCompanySystems = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/companies/${companyId}/systems`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch company systems');
  return await response.json();
};

export const getCompanyUsers = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/companies/${companyId}/users`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch company users');
  return await response.json();
};

export const createCompany = async (companyData, username) => {
  let url = `${API_BASE_URL}/admin/companies`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(companyData),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to create company');
  }
  return await response.json();
};

export const updateCompany = async (companyId, companyData, username) => {
  let url = `${API_BASE_URL}/admin/companies/${companyId}`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(companyData),
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to update company');
  }
  return await response.json();
};

export const deleteCompany = async (companyId, username) => {
  let url = `${API_BASE_URL}/admin/companies/${companyId}`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, { method: 'DELETE' });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to delete company');
  }
  return true;
};

// ============================================================
// ZONES
// ============================================================

export const fetchZones = async (systemId, username) => {
  let url = `${API_BASE_URL}/admin/zones/system/${systemId}`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch zones');
  return await response.json();
};

export const updateZone = async (zoneId, zoneData, username) => {
  let url = `${API_BASE_URL}/admin/zones/${zoneId}`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(zoneData),
  });
  if (!response.ok) throw new Error('Failed to update zone');
  return await response.json();
};

export const resetZones = async (systemId, username) => {
  let url = `${API_BASE_URL}/admin/zones/system/${systemId}/reset`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!response.ok) throw new Error('Failed to reset zones');
  return await response.json();
};

export const fetchZoneTypes = async () => {
  const response = await fetch(`${API_BASE_URL}/admin/zones/types`);
  if (!response.ok) throw new Error('Failed to fetch zone types');
  return await response.json();
};

// ============================================================
// REPORTS - COMPANY-BASED
// ============================================================

export const getReportSummary = async (from, to, username, systemCode) => {
  const params = new URLSearchParams();
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  if (username) params.append('username', username);
  if (systemCode) params.append('systemCode', systemCode);
  
  const response = await fetch(`${API_BASE_URL}/reports/summary?${params}`);
  if (!response.ok) throw new Error('Failed to get report summary');
  return await response.json();
};

export const getReportDetailed = async (from, to, username, systemCode, status) => {
  const params = new URLSearchParams();
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  if (username) params.append('username', username);
  if (systemCode) params.append('systemCode', systemCode);
  if (status) params.append('status', status);
  
  const response = await fetch(`${API_BASE_URL}/reports/detailed?${params}`);
  if (!response.ok) throw new Error('Failed to get detailed report');
  return await response.json();
};

export const getSystemHealth = async (username) => {
  let url = `${API_BASE_URL}/reports/health`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to get system health');
  return await response.json();
};

export const getPerformanceReport = async (from, to, username) => {
  const params = new URLSearchParams();
  if (from) params.append('from', from);
  if (to) params.append('to', to);
  if (username) params.append('username', username);
  
  const response = await fetch(`${API_BASE_URL}/reports/performance?${params}`);
  if (!response.ok) throw new Error('Failed to get performance report');
  return await response.json();
};

export const getReportSystems = async (username) => {
  let url = `${API_BASE_URL}/reports/systems`;
  if (username) url += `?username=${encodeURIComponent(username)}`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to get report systems');
  return await response.json();
};

// ============================================================
// ARCHIVE SYSTEM
// ============================================================

export const checkDeletionEligibility = async (systemId, username) => {
  const url = `${API_BASE_URL}/admin/archive/systems/${systemId}/check?username=${encodeURIComponent(username)}`;
  const response = await fetch(url);
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to check deletion eligibility');
  }
  return await response.json();
};

export const archiveAndDeleteSystem = async (systemId, username) => {
  const url = `${API_BASE_URL}/admin/archive/systems/${systemId}/archive-delete?username=${encodeURIComponent(username)}&deleteBy=${encodeURIComponent(username)}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to archive and delete system');
  }
  return await response.json();
};

export const getArchivedSystems = async (username) => {
  const url = `${API_BASE_URL}/admin/archive${username ? `?username=${encodeURIComponent(username)}` : ''}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch archived systems');
  return await response.json();
};

export const getArchiveDetails = async (archiveId, username) => {
  const url = `${API_BASE_URL}/admin/archive/${archiveId}/report?username=${encodeURIComponent(username)}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch archive details');
  return await response.json();
};

export const downloadArchiveReport = async (archiveId, username) => {
  const url = `${API_BASE_URL}/admin/archive/${archiveId}/report?username=${encodeURIComponent(username)}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to download archive report');
  return await response.json();
};