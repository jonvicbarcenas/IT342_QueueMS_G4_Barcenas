import { api } from '../api';
import type { CreateUserServiceRequestPayload, UserServiceRequest } from '@/types/users/userServiceRequest';

const REQUEST_ENDPOINTS = {
  BASE: '/api/requests',
  MINE: '/api/requests/me',
} as const;

const readErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  const message = await response.text();
  return message || fallback;
};

export const userServiceRequestService = {
  async getMyRequests(): Promise<UserServiceRequest[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${REQUEST_ENDPOINTS.MINE}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load service requests'));
    }

    return response.json();
  },

  async createRequest(payload: CreateUserServiceRequestPayload): Promise<UserServiceRequest> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${REQUEST_ENDPOINTS.BASE}`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to create service request'));
    }

    return response.json();
  },

  async cancelRequest(id: string): Promise<UserServiceRequest> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${REQUEST_ENDPOINTS.BASE}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to cancel service request'));
    }

    return response.json();
  },
};

export default userServiceRequestService;
