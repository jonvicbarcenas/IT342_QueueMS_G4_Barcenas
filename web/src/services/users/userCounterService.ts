import { api } from '@services/api';
import type { UserCounter } from '@/types/users/userCounter';

const COUNTER_ENDPOINTS = {
  BASE: '/api/counters',
} as const;

const readErrorMessage = async (response: Response, fallback: string): Promise<string> => {
  const message = await response.text();
  return message || fallback;
};

export const userCounterService = {
  async getOpenCounters(): Promise<UserCounter[]> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${COUNTER_ENDPOINTS.BASE}`);

    if (!response.ok) {
      throw new Error(await readErrorMessage(response, 'Failed to load counters'));
    }

    return response.json();
  },
};

export default userCounterService;
