// Authentication DTOs matching backend structure

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstname: string;
  lastname: string;
}

export interface AuthResponse {
  backendToken: string;
  expiresInMs: number;
}

export interface User {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  role?: string;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
