import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button, Input } from '@components/common';
import { authService } from '@services/authService';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(formData);
      navigate('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setError('');
    setIsLoading(true);

    try {
      await authService.initiateGoogleLogin();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Google login is not available.');
      setIsLoading(false);
    }
  };

  return (
    <div className="qm-shell flex min-h-screen items-center justify-center px-4 py-8">
      <main className="grid w-full max-w-6xl overflow-hidden rounded-[28px] border border-stone-200 bg-[#faf9f4]/80 shadow-[0_24px_80px_rgba(17,16,14,0.08)] lg:grid-cols-[minmax(0,1fr)_440px]">
        <section className="qm-dotted-field hidden min-h-[720px] flex-col justify-between border-r border-stone-200 p-10 lg:flex">
          <nav className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="qm-logo"><span className="qm-logo-mark" /></span>
              <span className="text-sm font-semibold">QueueMS</span>
            </div>
            <div className="flex gap-6 text-sm text-stone-500">
              <span>Request</span>
              <span>Queue</span>
              <span>Notify</span>
            </div>
          </nav>

          <div className="mx-auto max-w-2xl text-center">
            <p className="qm-dot-text text-6xl font-black leading-tight">QUEUE.</p>
            <div className="my-12 flex items-center justify-center gap-6">
              <span className="h-px w-36 bg-stone-200" />
              <span className="qm-logo h-24 w-24 rounded-[26px]"><span className="qm-logo-mark scale-150" /></span>
              <span className="h-px w-36 bg-stone-200" />
            </div>
            <p className="qm-dot-text text-6xl font-black leading-tight">PREVENT.</p>
            <p className="mx-auto mt-8 max-w-sm text-sm leading-6 text-stone-500">
              Real-time campus service queues with documents, teller routing, and status alerts.
            </p>
          </div>

          <div className="grid grid-cols-3 rounded-3xl border border-stone-200 bg-[#fffef9]/70">
            <div className="p-6">
              <p className="qm-dot-text text-4xl font-black">3x</p>
              <p className="mt-2 text-sm text-stone-600">Faster request flow</p>
            </div>
            <div className="border-x border-stone-200 p-6">
              <p className="qm-dot-text text-4xl font-black">24h</p>
              <p className="mt-2 text-sm text-stone-600">Session window</p>
            </div>
            <div className="p-6">
              <p className="text-sm leading-6 text-stone-600">Connect users, tellers, and admins in one queue surface.</p>
            </div>
          </div>
        </section>

        <section className="p-6 sm:p-10">
          <div className="mb-10 flex items-center justify-between lg:hidden">
            <div className="flex items-center gap-3">
              <span className="qm-logo"><span className="qm-logo-mark" /></span>
              <span className="text-sm font-semibold">QueueMS</span>
            </div>
          </div>

          <div className="mb-8">
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-stone-500">Welcome Back</p>
            <h1 className="mt-3 text-4xl font-semibold tracking-tight text-stone-950">Sign in to QueueMS</h1>
            <p className="mt-3 text-sm leading-6 text-stone-500">
              Access your queue dashboard, attached documents, and live request status.
            </p>
          </div>

          {error && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
              <p className="text-sm font-medium text-red-700">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email Address"
              type="email"
              placeholder="user@example.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />

            <Input
              label="Password"
              type="password"
              placeholder="Password"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
            />

            <Button type="submit" variant="primary" isLoading={isLoading}>
              Sign In
            </Button>
          </form>

          <div className="my-6 flex items-center">
            <div className="flex-1 border-t border-stone-200"></div>
            <span className="px-3 text-xs font-semibold uppercase tracking-[0.16em] text-stone-400">or</span>
            <div className="flex-1 border-t border-stone-200"></div>
          </div>

          <Button variant="outline" onClick={handleGoogleSignIn} type="button">
            <svg className="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Sign in with Google
          </Button>

          <p className="mt-6 text-center text-sm text-stone-600">
            Don't have an account?{' '}
            <Link to="/register" className="font-semibold text-stone-950 underline underline-offset-4">
              Register here
            </Link>
          </p>

          <p className="mt-10 text-center text-xs text-stone-400">
            QueueMS Smart Queue Management System
          </p>
        </section>
      </main>
    </div>
  );
};

export default LoginPage;
