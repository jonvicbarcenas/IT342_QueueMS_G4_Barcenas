import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import type { User, AuthState, LoginRequest, RegisterRequest } from '@types/auth';
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

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginWithToken: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: true,
  });

  // Restore session from localStorage on mount
  useEffect(() => {
    const token = getAuthToken();
    const user = getStoredUser();
    if (token && user) {
      setAuthState({
        user,
        token,
        isAuthenticated: true,
        isLoading: false,
      });
    } else {
      setAuthState(prev => ({ ...prev, isLoading: false }));
    }
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      setAuthToken(response.backendToken);

      let user: User = {
        id: '',
        email: credentials.email,
        firstname: '',
        lastname: '',
      };
      try {
        const payload = JSON.parse(atob(response.backendToken.split('.')[1]));
        user = {
          id: payload.sub ?? '',
          email: payload.email ?? credentials.email,
          firstname: payload.firstname ?? '',
          lastname: payload.lastname ?? '',
          role: payload.role,
        };
      } catch {
      }

      setStoredUser(user);
      setAuthState({
        user,
        token: response.backendToken,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      throw error;
    }
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    try {
      await authService.register(data);
      await login({ email: data.email, password: data.password });
    } catch (error) {
      throw error;
    }
  }, [login]);

  const loginWithToken = useCallback((token: string) => {
    setAuthToken(token);

    // Decode JWT to get user information
    let user: User = {
      id: '',
      email: '',
      firstname: '',
      lastname: '',
    };
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      user = {
        id: payload.sub ?? '',
        email: payload.email ?? '',
        firstname: payload.firstname ?? '',
        lastname: payload.lastname ?? '',
        role: payload.role,
      };
    } catch (error) {
      console.error('Failed to decode token:', error);
    }

    setStoredUser(user);
    setAuthState({
      user,
      token,
      isAuthenticated: true,
      isLoading: false,
    });
  }, []);

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

  return (
    <AuthContext.Provider value={{ ...authState, login, register, loginWithToken, logout }}>
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
