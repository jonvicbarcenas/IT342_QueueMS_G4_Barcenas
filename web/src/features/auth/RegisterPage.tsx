import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/features/auth';
import { Button, Input } from '@components/common';
import { authService } from '@/features/auth/authService';

const RegisterPage = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    firstname: '',
    lastname: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.firstname.trim()) newErrors.firstname = 'First name is required';
    if (!formData.lastname.trim()) newErrors.lastname = 'Last name is required';
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Email is invalid';
    }
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsLoading(true);
    setErrors({});
    setSuccessMessage('');

    try {
      await register({
        email: formData.email,
        password: formData.password,
        firstname: formData.firstname,
        lastname: formData.lastname,
      });
      setSuccessMessage('Registration successful. Redirecting to dashboard...');
      setTimeout(() => navigate('/dashboard'), 2000);
    } catch (err) {
      setErrors({
        general: err instanceof Error ? err.message : 'Registration failed. Please try again.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setErrors({});
    setIsLoading(true);

    try {
      await authService.initiateGoogleLogin();
    } catch (err) {
      setErrors({
        general: err instanceof Error ? err.message : 'Google login is not available.',
      });
      setIsLoading(false);
    }
  };

  return (
    <div className="qm-shell flex min-h-screen items-center justify-center px-4 py-8">
      <main className="grid w-full max-w-6xl overflow-hidden rounded-[28px] border border-stone-200 bg-[#faf9f4]/80 shadow-[0_24px_80px_rgba(17,16,14,0.08)] lg:grid-cols-[440px_minmax(0,1fr)]">
        <section className="p-6 sm:p-10">
          <div className="mb-8 flex items-center gap-3">
            <span className="qm-logo"><span className="qm-logo-mark" /></span>
            <span className="text-sm font-semibold">QueueMS</span>
          </div>

          <div className="mb-8">
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-stone-500">New Student User</p>
            <h1 className="mt-3 text-4xl font-semibold tracking-tight text-stone-950">Create account</h1>
            <p className="mt-3 text-sm leading-6 text-stone-500">
              Register once, request services faster, and track your queue from anywhere.
            </p>
          </div>

          {successMessage && (
            <div className="mb-4 rounded-xl border border-stone-200 bg-[#f7f5ef] p-3">
              <div className="flex items-center gap-2">
                <svg className="h-5 w-5 text-stone-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <p className="text-sm font-medium text-stone-700">{successMessage}</p>
              </div>
            </div>
          )}

          {errors.general && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3">
              <p className="text-sm font-medium text-red-700">{errors.general}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="First Name"
                type="text"
                placeholder="John"
                value={formData.firstname}
                onChange={(e) => setFormData({ ...formData, firstname: e.target.value })}
                error={errors.firstname}
                required
              />
              <Input
                label="Last Name"
                type="text"
                placeholder="Doe"
                value={formData.lastname}
                onChange={(e) => setFormData({ ...formData, lastname: e.target.value })}
                error={errors.lastname}
                required
              />
            </div>

            <Input
              label="Email Address"
              type="email"
              placeholder="user@example.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              error={errors.email}
              required
            />
            <Input
              label="Password"
              type="password"
              placeholder="Password"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              error={errors.password}
              required
            />
            <Input
              label="Confirm Password"
              type="password"
              placeholder="Password"
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              error={errors.confirmPassword}
              required
            />

            <Button type="submit" variant="primary" isLoading={isLoading}>
              Create Account
            </Button>
          </form>

          <div className="my-6 flex items-center">
            <div className="flex-1 border-t border-stone-200"></div>
            <span className="px-3 text-xs font-semibold uppercase tracking-[0.16em] text-stone-400">or</span>
            <div className="flex-1 border-t border-stone-200"></div>
          </div>

          <Button variant="outline" onClick={handleGoogleSignIn} type="button">
            Sign up with Google
          </Button>

          <p className="mt-6 text-center text-sm text-stone-600">
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-stone-950 underline underline-offset-4">
              Sign in here
            </Link>
          </p>
        </section>

        <section className="qm-dotted-field hidden min-h-[760px] flex-col justify-between border-l border-stone-200 p-10 lg:flex">
          <div className="flex justify-between text-sm text-stone-500">
            <span>Counter</span>
            <span>Ticket</span>
            <span>Status</span>
          </div>
          <div className="text-center">
            <p className="qm-dot-text text-6xl font-black leading-tight">REQUEST.</p>
            <div className="my-12 flex items-center justify-center gap-6">
              <span className="h-px w-32 bg-stone-200" />
              <span className="qm-logo h-24 w-24 rounded-[26px]"><span className="qm-logo-mark scale-150" /></span>
              <span className="h-px w-32 bg-stone-200" />
            </div>
            <p className="qm-dot-text text-6xl font-black leading-tight">TRACK.</p>
          </div>
          <div className="rounded-3xl border border-stone-200 bg-[#fffef9]/70 p-6">
            <p className="text-sm leading-6 text-stone-600">
              Submit documents, receive confirmation, and follow your position without standing in line.
            </p>
          </div>
        </section>
      </main>
    </div>
  );
};

export default RegisterPage;
