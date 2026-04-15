/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { User, AuthState, LoginRequest, RegisterRequest } from '@/types/auth';
import { authService } from '@services/authService';
import { setAuthToken, removeAuthToken, getAuthToken } from '@services/api';

const USER_KEY = 'authUser';

const getStoredUser = (): User | null => {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
};

const setStoredUser = (user: User): void => {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

const removeStoredUser = (): void => {
  localStorage.removeItem(USER_KEY);
};

const getInitialAuthState = (): AuthState => {
  const token = getAuthToken();
  const user = getStoredUser();

  if (token && user) {
    return {
      user,
      token,
      isAuthenticated: true,
      isLoading: false,
    };
  }

  return {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: Boolean(token),
  };
};

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginWithToken: (token: string) => Promise<void>;
  logout: () => void;
  loadUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [authState, setAuthState] = useState<AuthState>(getInitialAuthState);

  const logout = useCallback(() => {
    removeAuthToken();
    removeStoredUser();
    setAuthState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  }, []);

  const loadUser = useCallback(async () => {
    const token = getAuthToken();
    if (!token) {
      setAuthState(prev => ({ ...prev, isLoading: false }));
      return;
    }

    try {
      const user = await authService.getMe();
      setStoredUser(user);
      setAuthState({
        user,
        token,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to load user:', error);
      logout();
    }
  }, [logout]);

  // Restore session on mount
  useEffect(() => {
    const token = getAuthToken();
    if (token) {
      const timer = window.setTimeout(() => {
        loadUser();
      }, 0);

      return () => window.clearTimeout(timer);
    }
  }, [loadUser]);

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authService.login(credentials);
    setAuthToken(response.backendToken);
    await loadUser();
  }, [loadUser]);

  const register = useCallback(async (data: RegisterRequest) => {
    await authService.register(data);
    await login({ email: data.email, password: data.password });
  }, [login]);

  const loginWithToken = useCallback(async (token: string) => {
    setAuthToken(token);
    await loadUser();
  }, [loadUser]);

  return (
    <AuthContext.Provider value={{ ...authState, login, register, loginWithToken, logout, loadUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
