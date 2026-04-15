// Authentication API service
import type { LoginRequest, RegisterRequest, AuthResponse, User } from '@/types/auth';
import { api } from './api';

const AUTH_ENDPOINTS = {
  LOGIN: `/api/auth/login`,
  REGISTER: `/api/auth/register`,
  GOOGLE_LOGIN: `/oauth2/authorization/google`,
  ME: `/api/auth/me`,
} as const;

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(`${api.API_BASE_URL}${AUTH_ENDPOINTS.LOGIN}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Login failed');
    }

    return response.json();
  },

  async register(data: RegisterRequest): Promise<void> {
    const response = await fetch(`${api.API_BASE_URL}${AUTH_ENDPOINTS.REGISTER}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Registration failed');
    }
  },

  async getMe(): Promise<User> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${AUTH_ENDPOINTS.ME}`);

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Failed to fetch user profile');
    }

    return response.json();
  },

  // Initiate Google OAuth2 login by redirecting to backend OAuth2 endpoint
  initiateGoogleLogin(): void {
    window.location.href = `${api.API_BASE_URL}${AUTH_ENDPOINTS.GOOGLE_LOGIN}`;
  },

  // Get OAuth2 login URL (alternative method if you want to open in popup)
  getGoogleLoginUrl(): string {
    return `${api.API_BASE_URL}${AUTH_ENDPOINTS.GOOGLE_LOGIN}`;
  },
};

export default authService;
