// Authentication API service
import type { LoginRequest, RegisterRequest, AuthResponse } from '@types/auth';
import { API_BASE_URL } from './api';

const AUTH_ENDPOINTS = {
  LOGIN: `${API_BASE_URL}/api/auth/login`,
  REGISTER: `${API_BASE_URL}/api/auth/register`,
} as const;

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(AUTH_ENDPOINTS.LOGIN, {
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
    const response = await fetch(AUTH_ENDPOINTS.REGISTER, {
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

  async loginWithGoogle(idToken: string): Promise<AuthResponse> {
    // TODO: Implement Google login when backend endpoint is ready
    throw new Error('Google login not yet implemented');
  },
};

export default authService;
