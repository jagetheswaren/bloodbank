/**
 * Blood Bank Centralized API Client
 * Clean Fetch wrapper with unified error handling
 */

const API_BASE = '';

class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  };

  try {
    const response = await fetch(url, config);

    // 204 No Content
    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      let errorMessage = `HTTP Error ${response.status}`;
      if (isJson && data) {
        if (data.message) {
          errorMessage = data.message;
        } else if (data.errors && Array.isArray(data.errors)) {
          errorMessage = data.errors.map(e => e.message || e.defaultMessage || e).join(', ');
        } else if (data.error) {
          errorMessage = data.error;
        }
      }
      throw new ApiError(errorMessage, response.status, data);
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(error.message || 'Network connection failed. Please check backend status.', 0, null);
  }
}

export const BloodBankApi = {
  // Donor APIs
  getDonors: (page = 0, size = 20, active = null) => {
    let query = `?page=${page}&size=${size}`;
    if (active !== null) query += `&active=${active}`;
    return request(`/api/donors${query}`);
  },

  getDonor: (id) => request(`/api/donors/${id}`),

  createDonor: (donorData) => request('/api/donors', {
    method: 'POST',
    body: JSON.stringify(donorData)
  }),

  updateDonor: (id, donorData) => request(`/api/donors/${id}`, {
    method: 'PUT',
    body: JSON.stringify(donorData)
  }),

  deactivateDonor: (id) => request(`/api/donors/${id}`, {
    method: 'DELETE'
  }),

  checkEligibility: (id) => request(`/api/donors/${id}/eligibility`),

  // Donation APIs
  getDonations: (page = 0, size = 20) => request(`/api/donations?page=${page}&size=${size}`),

  getDonation: (id) => request(`/api/donations/${id}`),

  getDonationsByDonor: (donorId) => request(`/api/donations/donor/${donorId}`),

  createDonation: (donationData) => request('/api/donations', {
    method: 'POST',
    body: JSON.stringify(donationData)
  }),

  // Inventory APIs
  getInventory: (page = 0, size = 20) => request(`/api/inventory?page=${page}&size=${size}`),

  getStock: () => request('/api/inventory/stock'),

  getNearExpiry: (page = 0, size = 20) => request(`/api/inventory/near-expiry?page=${page}&size=${size}`),

  getExpired: (page = 0, size = 20) => request(`/api/inventory/expired?page=${page}&size=${size}`),

  getInventoryByBloodGroup: (bloodGroup, page = 0, size = 20) =>
    request(`/api/inventory/blood-group/${encodeURIComponent(bloodGroup)}?page=${page}&size=${size}`),

  getUnitByCode: (unitCode) => request(`/api/inventory/unit/${encodeURIComponent(unitCode)}`),

  // Issue APIs
  getIssues: (page = 0, size = 20) => request(`/api/issues?page=${page}&size=${size}`),

  getIssue: (id) => request(`/api/issues/${id}`),

  createIssue: (issueData) => request('/api/issues', {
    method: 'POST',
    body: JSON.stringify(issueData)
  })
};
