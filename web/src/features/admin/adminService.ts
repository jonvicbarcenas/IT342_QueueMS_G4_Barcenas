import { api } from '@/shared/api/api';
import type {
  AdminCounter,
  AdminCounterPayload,
  AdminServiceRequest,
  AdminStaffUserPayload,
  AdminUser,
} from '@/features/admin/admin';

const ADMIN_ENDPOINTS = {
  COUNTERS: '/api/admin/counters',
  USERS: '/api/admin/users',
  TELLERS: '/api/admin/tellers',
  REQUESTS: '/api/admin/requests',
} as const;

const readErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  const message = await response.text();
  return message || fallback;
};

export const adminService = {
  async getCounters(): Promise<AdminCounter[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.COUNTERS}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load counters'));
    }

    return response.json();
  },

  async createCounter(payload: AdminCounterPayload): Promise<AdminCounter> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.COUNTERS}`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to create counter'));
    }

    return response.json();
  },

  async updateCounter(id: string, payload: AdminCounterPayload): Promise<AdminCounter> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.COUNTERS}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to update counter'));
    }

    return response.json();
  },

  async deleteCounter(id: string): Promise<void> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.COUNTERS}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to delete counter'));
    }
  },

  async getUsers(): Promise<AdminUser[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.USERS}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load users'));
    }

    return response.json();
  },

  async getTellers(): Promise<AdminUser[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.TELLERS}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load tellers'));
    }

    return response.json();
  },

  async createStaffUser(payload: AdminStaffUserPayload): Promise<AdminUser> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.USERS}`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to create staff user'));
    }

    return response.json();
  },

  async updateUser(id: string, payload: AdminStaffUserPayload): Promise<AdminUser> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.USERS}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to update user'));
    }

    return response.json();
  },

  async deleteUser(id: string): Promise<void> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.USERS}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to delete user'));
    }
  },

  async getRequests(): Promise<AdminServiceRequest[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${ADMIN_ENDPOINTS.REQUESTS}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load service requests'));
    }

    return response.json();
  },
};

export default adminService;
