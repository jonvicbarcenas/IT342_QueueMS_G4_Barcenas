import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import type { User, AuthState, LoginRequest, RegisterRequest } from '@types/auth';
import { authService } from '@services/authService';
import { setAuthToken, removeAuthToken, getAuthToken } from '@services/api';

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
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

  // Check for existing token on mount
  useEffect(() => {
    const token = getAuthToken();
    if (token) {
      // TODO: Validate token with backend or decode JWT
      setAuthState({
        user: null, // Set user data when available
        token,
        isAuthenticated: true,
        isLoading: false,
      });
    } else {
      setAuthState(prev => ({ ...prev, isLoading: false }));
    }
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      setAuthToken(response.backendToken);
      
      // TODO: Decode JWT or fetch user data
      const user: User = {
        id: 'temp-id',
        email: credentials.email,
        firstname: '',
        lastname: '',
      };

      setAuthState({
        user,
        token: response.backendToken,
        isAuthenticated: true,
        isLoading: false,
      });
    } catch (error) {
      throw error;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      await authService.register(data);
      // After successful registration, log the user in
      await login({ email: data.email, password: data.password });
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    removeAuthToken();
    setAuthState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  };

  return (
    <AuthContext.Provider value={{ ...authState, login, register, logout }}>
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
