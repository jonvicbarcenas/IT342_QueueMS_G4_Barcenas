import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context';

const OAuth2CallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token');
      const errorParam = searchParams.get('error');

      if (token) {
        try {
          // Login with the token received from backend
          await loginWithToken(token);
          // Small delay to ensure state is updated before navigation
          setTimeout(() => {
            navigate('/dashboard', { replace: true });
          }, 100);
        } catch {
          setError('Failed to authenticate. Please try again.');
          setTimeout(() => navigate('/login'), 3000);
        }
      } else if (errorParam) {
        setError(decodeURIComponent(errorParam));
        setTimeout(() => navigate('/login'), 3000);
      } else {
        const timer = setTimeout(() => {
          if (!token && !errorParam) {
            setError('Invalid authentication response.');
            setTimeout(() => navigate('/login'), 3000);
          }
        }, 500);
        return () => clearTimeout(timer);
      }
    };

    handleCallback();
  }, [searchParams, navigate, loginWithToken]);

  if (error) {
    return (
      <div className="qm-shell flex min-h-screen items-center justify-center px-4">
        <div className="qm-card w-full max-w-md rounded-[28px] p-8 text-center">
          <div className="mb-4">
            <svg
              className="mx-auto h-16 w-16 text-red-700"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <h2 className="mb-2 text-2xl font-bold text-stone-950">Authentication Failed</h2>
          <p className="mb-4 text-stone-600">{error}</p>
          <p className="text-sm text-stone-500">Redirecting to login page...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="qm-shell flex min-h-screen items-center justify-center px-4">
      <div className="qm-card w-full max-w-md rounded-[28px] p-8 text-center">
        <div className="mb-4">
          <div className="mx-auto h-16 w-16 animate-spin rounded-full border-b-2 border-stone-950"></div>
        </div>
        <h2 className="mb-2 text-2xl font-bold text-stone-950">Authenticating...</h2>
        <p className="text-stone-600">Please wait while we complete your sign-in.</p>
      </div>
    </div>
  );
};

export default OAuth2CallbackPage;
