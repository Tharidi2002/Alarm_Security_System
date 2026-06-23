import axios from '../api/axiosConfig';

export const atmService = {
  // Get all ATMs
  getATMs: async () => {
    const response = await axios.get('/atm');
    return response.data;
  },

  // Get ATM by ID
  getATMById: async (id) => {
    const response = await axios.get(`/atm/${id}`);
    return response.data;
  },

  // Register new ATM
  registerATM: async (data) => {
    const response = await axios.post('/atm', data);
    return response.data;
  },

  // Update ATM
  updateATM: async (id, data) => {
    const response = await axios.put(`/atm/${id}`, data);
    return response.data;
  },

  // Delete ATM
  deleteATM: async (id) => {
    const response = await axios.delete(`/atm/${id}`);
    return response.data;
  },

  // Get ATM zones
  getATMZones: async (atmId) => {
    const response = await axios.get(`/atm/${atmId}/zones`);
    return response.data;
  },
};
