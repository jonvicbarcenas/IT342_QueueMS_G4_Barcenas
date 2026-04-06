// API service configuration and utilities

class ApiClient {
  private static instance: ApiClient;
  public readonly API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  private constructor() {}

  public static getInstance(): ApiClient {
    if (!ApiClient.instance) {
      ApiClient.instance = new ApiClient();
    }
    return ApiClient.instance;
  }

  // Get auth token from localStorage
  public getAuthToken(): string | null {
    return localStorage.getItem('authToken');
  }

  // Set auth token in localStorage
  public setAuthToken(token: string): void {
    localStorage.setItem('authToken', token);
  }

  // Remove auth token from localStorage
  public removeAuthToken(): void {
    localStorage.removeItem('authToken');
  }

  // Create authenticated fetch wrapper
  public async authenticatedFetch(
    url: string,
    options: RequestInit = {}
  ): Promise<Response> {
    const token = this.getAuthToken();
    const headers = {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    };

    return fetch(url, {
      ...options,
      headers,
    });
  }
}

export const api = ApiClient.getInstance();

// Legacy exports for compatibility
export const getAuthToken = () => api.getAuthToken();
export const setAuthToken = (token: string) => api.setAuthToken(token);
export const removeAuthToken = () => api.removeAuthToken();
export const authenticatedFetch = (url: string, options?: RequestInit) => api.authenticatedFetch(url, options);
export const API_BASE_URL = api.API_BASE_URL;
