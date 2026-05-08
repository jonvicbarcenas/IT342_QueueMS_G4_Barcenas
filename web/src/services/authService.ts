// Authentication API service
import type { LoginRequest, RegisterRequest, UpdateProfileRequest, AuthResponse, User } from '@/types/auth';
import { api } from './api';

const AUTH_ENDPOINTS = {
  LOGIN: `/api/auth/login`,
  REGISTER: `/api/auth/register`,
  GOOGLE_LOGIN: `/api/auth/google/login`,
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

  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await api.authenticatedFetch(`${api.API_BASE_URL}${AUTH_ENDPOINTS.ME}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Failed to update profile');
    }

    return response.json();
  },

  async getGoogleLoginUrl(): Promise<string> {
    const response = await fetch(`${api.API_BASE_URL}${AUTH_ENDPOINTS.GOOGLE_LOGIN}`);

    if (!response.ok) {
      const contentType = response.headers.get('content-type');
      if (contentType?.includes('application/json')) {
        const error = await response.json();
        throw new Error(error.message || 'Google login is not available');
      }

      const error = await response.text();
      throw new Error(error || 'Google login is not available');
    }

    const data = await response.json() as { redirectUrl: string };
    return data.redirectUrl.startsWith('http')
      ? data.redirectUrl
      : `${api.API_BASE_URL}${data.redirectUrl}`;
  },

  async initiateGoogleLogin(): Promise<void> {
    window.location.href = await this.getGoogleLoginUrl();
  },
};

export default authService;
