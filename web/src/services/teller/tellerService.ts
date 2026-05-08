import { api } from '@services/api';
import type {
  TellerCounter,
  TellerCounterStatusPayload,
  TellerRequestStatusPayload,
  TellerServiceRequest,
} from '@/types/teller/teller';

const TELLER_ENDPOINTS = {
  COUNTER: '/api/teller/counter',
  COUNTER_STATUS: '/api/teller/counter/status',
  REQUESTS: '/api/teller/requests',
  SERVE_NEXT: '/api/teller/requests/next/serve',
} as const;

const readErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  const message = await response.text();
  return message || fallback;
};

export const tellerService = {
  async getAssignedCounter(): Promise<TellerCounter> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.COUNTER}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load assigned counter'));
    }

    return response.json();
  },

  async updateCounterStatus(payload: TellerCounterStatusPayload): Promise<TellerCounter> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.COUNTER_STATUS}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to update counter status'));
    }

    return response.json();
  },

  async getAssignedRequests(): Promise<TellerServiceRequest[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.REQUESTS}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load teller requests'));
    }

    return response.json();
  },

  async serveNextRequest(): Promise<TellerServiceRequest> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.SERVE_NEXT}`, {
      method: 'PUT',
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to serve next request'));
    }

    return response.json();
  },

  async serveRequest(id: string): Promise<TellerServiceRequest> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.REQUESTS}/${id}/serve`, {
      method: 'PUT',
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to serve request'));
    }

    return response.json();
  },

  async updateRequestStatus(id: string, payload: TellerRequestStatusPayload): Promise<TellerServiceRequest> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${TELLER_ENDPOINTS.REQUESTS}/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to update request status'));
    }

    return response.json();
  },

  getAttachmentUrl(request: TellerServiceRequest): string | null {
    if (request.attachmentUrl) {
      return request.attachmentUrl;
    }

    if (!request.id) {
      return null;
    }

    return `${api.API_BASE_URL}${TELLER_ENDPOINTS.REQUESTS}/${request.id}/attachment`;
  },
};

export default tellerService;
