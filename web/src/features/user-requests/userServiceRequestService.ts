import { api } from '@/shared/api/api';
import type {
  CreateUserServiceRequestPayload,
  HolidayStatus,
  UserServiceRequest,
} from '@/features/user-requests/userServiceRequest';

const REQUEST_ENDPOINTS = {
  BASE: '/api/requests',
  MINE: '/api/requests/me',
  HOLIDAY_TODAY: '/api/holidays/today',
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

  async uploadAttachment(id: string, file: File): Promise<UserServiceRequest> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${api.API_BASE_URL}${REQUEST_ENDPOINTS.BASE}/${id}/attachment`, {
      method: 'POST',
      headers: {
        ...(api.getAuthToken() && { Authorization: `Bearer ${api.getAuthToken()}` }),
      },
      body: formData,
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to upload supporting document'));
    }

    return response.json();
  },

  getAttachmentDownloadUrl(id: string): string {
    return `${api.API_BASE_URL}${REQUEST_ENDPOINTS.BASE}/${id}/attachment`;
  },

  async getTodayHolidayStatus(): Promise<HolidayStatus> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${REQUEST_ENDPOINTS.HOLIDAY_TODAY}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load holiday status'));
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
