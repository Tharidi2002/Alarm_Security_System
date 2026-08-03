const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

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

export const fetchUsers = async () => {
  const response = await fetch(`${API_BASE_URL}/admin/users`);
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

export const fetchSystems = async () => {
  const response = await fetch(`${API_BASE_URL}/admin/systems`);
  if (!response.ok) throw new Error('Failed to fetch systems');
  return await response.json();
};

export const getSystemById = async (systemId) => {
  const response = await fetch(`${API_BASE_URL}/admin/systems/${systemId}`);
  if (!response.ok) throw new Error('Failed to fetch system');
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

export const fetchCompanies = async () => {
  const response = await fetch(`${API_BASE_URL}/admin/companies`);
  if (!response.ok) throw new Error('Failed to fetch companies');
  return await response.json();
};

export const createSystem = async (systemData, companyId) => {
  const url = companyId 
    ? `${API_BASE_URL}/admin/systems?companyId=${encodeURIComponent(companyId)}` 
    : `${API_BASE_URL}/admin/systems`;
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

export const updateSystem = async (systemId, systemData, companyId) => {
  const url = companyId !== undefined && companyId !== null
    ? `${API_BASE_URL}/admin/systems/${systemId}?companyId=${encodeURIComponent(companyId)}`
    : `${API_BASE_URL}/admin/systems/${systemId}`;
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

export const toggleSystemStatus = async (systemId, status) => {
  const response = await fetch(`${API_BASE_URL}/admin/systems/${systemId}/status`, {
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

export const deleteSystem = async (systemId) => {
  const response = await fetch(`${API_BASE_URL}/admin/systems/${systemId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || 'Failed to delete system');
  }
  return true;
};

// ========== RESOLVE ALERT ==========
export const resolveAlert = async (alertId, resolvedBy, description) => {
  try {
    const url = description 
      ? `${API_BASE_URL}/alerts/${alertId}/resolve?resolvedBy=${encodeURIComponent(resolvedBy)}&description=${encodeURIComponent(description)}`
      : `${API_BASE_URL}/alerts/${alertId}/resolve?resolvedBy=${encodeURIComponent(resolvedBy)}`;
    
    const response = await fetch(url, {
      method: 'PUT',
    });
    
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

export const getAlertDetails = async (alertId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/alerts/${alertId}/details`);
    if (!response.ok) throw new Error('Failed to fetch alert details');
    return await response.json();
  } catch (error) {
    console.error('Error fetching alert details:', error);
    return null;
  }
};

export const getPendingCount = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/alerts/pending/count`);
    if (!response.ok) throw new Error('Failed to fetch counts');
    return await response.json();
  } catch (error) {
    console.error('Error fetching counts:', error);
    return { pending: 0, resolved: 0 };
  }
};

// ========== RESET USER PASSWORD ==========
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

// ===== ZONE MANAGEMENT =====
export const fetchZones = async (systemId) => {
  const response = await fetch(`${API_BASE_URL}/admin/zones/system/${systemId}`);
  if (!response.ok) throw new Error('Failed to fetch zones');
  return await response.json();
};

export const updateZone = async (zoneId, zoneData) => {
  const response = await fetch(`${API_BASE_URL}/admin/zones/${zoneId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(zoneData),
  });
  if (!response.ok) throw new Error('Failed to update zone');
  return await response.json();
};

export const bulkUpdateZones = async (systemId, zoneUpdates) => {
  const response = await fetch(`${API_BASE_URL}/admin/zones/system/${systemId}/bulk`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(zoneUpdates),
  });
  if (!response.ok) throw new Error('Failed to update zones');
  return await response.json();
};

export const resetZones = async (systemId) => {
  const response = await fetch(`${API_BASE_URL}/admin/zones/system/${systemId}/reset`, {
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

// ===== SYSTEM CONTROL COMMANDS =====
export const disarmSystem = async (systemCode, triggeredBy) => {
  const response = await fetch(`${API_BASE_URL}/alerts/disarm`, {
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

export const stopSiren = async (systemCode, triggeredBy) => {
  const response = await fetch(`${API_BASE_URL}/alerts/stop-siren`, {
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

export const sendSystemCommand = async (atmCode, command) => {
  const response = await fetch(`${API_BASE_URL}/alerts/set-command?atmCode=${encodeURIComponent(atmCode)}&command=${encodeURIComponent(command)}`, {
    method: 'POST',
  });
  if (!response.ok) {
    const errorMsg = await response.text();
    throw new Error(errorMsg || `Failed to send command ${command}`);
  }
  return await response.json();
};

// ===== DELETE USER =====
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