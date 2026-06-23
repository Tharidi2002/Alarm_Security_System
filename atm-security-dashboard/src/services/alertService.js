import axios from '../api/axiosConfig';

export const alertService = {
  // Get all alerts with pagination and filters
  getAlerts: async (page = 0, size = 20, status = null, atmId = null) => {
    const params = new URLSearchParams({
      page,
      size,
      ...(status && { status }),
      ...(atmId && { atmId }),
    });
    const response = await axios.get(`/alerts?${params}`);
    return response.data;
  },

  // Get single alert by ID
  getAlertById: async (id) => {
    const response = await axios.get(`/alerts/${id}`);
    return response.data;
  },

  // Simulate SMS (for testing)
  simulateSMS: async (simNumber, message) => {
    const response = await axios.post('/alerts/sms-simulate', {
      simNumber,
      message,
    });
    return response.data;
  },

  // Acknowledge alert
  acknowledgeAlert: async (id, username) => {
    const response = await axios.put(`/alerts/${id}/acknowledge?username=${username}`);
    return response.data;
  },

  // Resolve alert
  resolveAlert: async (id, username, notes = '') => {
    const response = await axios.put(`/alerts/${id}/resolve?username=${username}`, { notes });
    return response.data;
  },

  // Ignore alert
  ignoreAlert: async (id) => {
    const response = await axios.put(`/alerts/${id}/ignore`);
    return response.data;
  },

  // Get statistics
  getStats: async () => {
    const response = await axios.get('/alerts/stats');
    return response.data;
  },
};