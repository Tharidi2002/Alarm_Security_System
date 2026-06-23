import React, { useState, useEffect } from 'react';
import { Server, Plus, Edit, Trash2, RefreshCw, MapPin, Smartphone } from 'lucide-react';
import toast from 'react-hot-toast';
import Layout from '../components/Layout/Layout';

const AtmManagement = () => {
  const [atms, setAtms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingAtm, setEditingAtm] = useState(null);
  const [formData, setFormData] = useState({
    atmCode: '',
    atmName: '',
    location: '',
    simNumber: '',
    bankId: '',
  });

  const fetchATMs = async () => {
    try {
      setLoading(true);
      // TODO: Connect to real API
      // For now, use mock data
      const mockData = [
        { id: 1, atmCode: 'ATM001', atmName: 'Main Branch', location: 'Colombo', simNumber: '0712345678', status: 'ACTIVE' },
        { id: 2, atmCode: 'ATM002', atmName: 'Kandy Branch', location: 'Kandy', simNumber: '0712345679', status: 'ACTIVE' },
        { id: 3, atmCode: 'ATM003', atmName: 'Galle Branch', location: 'Galle', simNumber: '0712345680', status: 'ACTIVE' },
      ];
      setAtms(mockData);
    } catch (error) {
      toast.error('Failed to fetch ATMs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchATMs();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      // TODO: Connect to real API
      toast.success(editingAtm ? 'ATM updated successfully!' : 'ATM registered successfully!');
      setShowForm(false);
      setEditingAtm(null);
      setFormData({ atmCode: '', atmName: '', location: '', simNumber: '', bankId: '' });
      fetchATMs();
    } catch (error) {
      toast.error('Operation failed');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this ATM?')) {
      try {
        // TODO: Connect to real API
        toast.success('ATM deleted successfully!');
        fetchATMs();
      } catch (error) {
        toast.error('Failed to delete ATM');
      }
    }
  };

  const handleEdit = (atm) => {
    setEditingAtm(atm);
    setFormData(atm);
    setShowForm(true);
  };

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">ATM Management</h1>
            <p className="text-slate-400 text-sm">Manage all ATM machines in the system</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={fetchATMs}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-4 py-2 rounded-lg text-sm transition-all border border-slate-700"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <button
              onClick={() => {
                setEditingAtm(null);
                setFormData({ atmCode: '', atmName: '', location: '', simNumber: '', bankId: '' });
                setShowForm(true);
              }}
              className="flex items-center gap-2 bg-red-500 hover:bg-red-600 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            >
              <Plus className="w-4 h-4" />
              Add ATM
            </button>
          </div>
        </div>

        {/* ATM Cards */}
        {loading ? (
          <div className="text-center text-slate-400 py-12">Loading ATMs...</div>
        ) : atms.length === 0 ? (
          <div className="text-center text-slate-400 py-12">No ATMs registered</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {atms.map((atm) => (
              <div key={atm.id} className="bg-slate-950 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-colors">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="bg-blue-500/10 p-2 rounded-lg border border-blue-500/20">
                      <Server className="w-5 h-5 text-blue-400" />
                    </div>
                    <div>
                      <h3 className="text-white font-semibold">{atm.atmName || atm.atmCode}</h3>
                      <p className="text-xs font-mono text-slate-400">{atm.atmCode}</p>
                    </div>
                  </div>
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    atm.status === 'ACTIVE' ? 'bg-emerald-500/20 text-emerald-400' :
                    atm.status === 'INACTIVE' ? 'bg-red-500/20 text-red-400' :
                    'bg-yellow-500/20 text-yellow-400'
                  }`}>
                    {atm.status || 'ACTIVE'}
                  </span>
                </div>

                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex items-center gap-2 text-slate-400">
                    <MapPin className="w-4 h-4" />
                    <span>{atm.location || 'Unknown'}</span>
                  </div>
                  <div className="flex items-center gap-2 text-slate-400">
                    <Smartphone className="w-4 h-4" />
                    <span className="font-mono">{atm.simNumber}</span>
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-slate-800 flex gap-2">
                  <button
                    onClick={() => handleEdit(atm)}
                    className="flex-1 flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 py-1.5 rounded-lg text-sm transition-colors"
                  >
                    <Edit className="w-4 h-4" /> Edit
                  </button>
                  <button
                    onClick={() => handleDelete(atm.id)}
                    className="flex-1 flex items-center justify-center gap-2 bg-red-500/10 hover:bg-red-500/20 py-1.5 rounded-lg text-sm text-red-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" /> Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add/Edit Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
              <h2 className="text-xl font-bold text-white mb-4">
                {editingAtm ? 'Edit ATM' : 'Register New ATM'}
              </h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm text-slate-400 mb-1">ATM Code *</label>
                  <input
                    type="text"
                    value={formData.atmCode}
                    onChange={(e) => setFormData({ ...formData, atmCode: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm text-slate-400 mb-1">ATM Name</label>
                  <input
                    type="text"
                    value={formData.atmName}
                    onChange={(e) => setFormData({ ...formData, atmName: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                  />
                </div>

                <div>
                  <label className="block text-sm text-slate-400 mb-1">Location *</label>
                  <input
                    type="text"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm text-slate-400 mb-1">SIM Number *</label>
                  <input
                    type="text"
                    value={formData.simNumber}
                    onChange={(e) => setFormData({ ...formData, simNumber: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-red-500"
                    required
                  />
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="submit"
                    className="flex-1 bg-red-500 hover:bg-red-600 py-2 rounded-lg font-medium transition-colors"
                  >
                    {editingAtm ? 'Update' : 'Register'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false);
                      setEditingAtm(null);
                    }}
                    className="flex-1 bg-slate-800 hover:bg-slate-700 py-2 rounded-lg font-medium transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default AtmManagement;