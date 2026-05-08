import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '@/context';

const AccountMenu = () => {
  const { user, updateProfile, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [firstname, setFirstname] = useState(user?.firstname ?? '');
  const [lastname, setLastname] = useState(user?.lastname ?? '');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    setFirstname(user?.firstname ?? '');
    setLastname(user?.lastname ?? '');
  }, [user?.firstname, user?.lastname]);

  const initials = `${user?.firstname?.[0] ?? ''}${user?.lastname?.[0] ?? ''}`.toUpperCase() || 'U';
  const fullName = `${user?.firstname ?? ''} ${user?.lastname ?? ''}`.trim() || user?.email || 'User';

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setSuccessMessage('');
    setIsSaving(true);

    try {
      await updateProfile({
        firstname: firstname.trim(),
        lastname: lastname.trim(),
      });
      setSuccessMessage('Profile updated.');
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to update profile.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen(current => !current)}
        className="flex items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2 text-left transition-colors hover:bg-slate-50"
        aria-expanded={isOpen}
      >
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-700 text-sm font-semibold text-white">
          {initials}
        </span>
        <span className="hidden sm:block">
          <span className="block text-sm font-semibold text-slate-900">{fullName}</span>
          <span className="block text-xs text-slate-500">{user?.role ?? 'USER'}</span>
        </span>
        <span className="text-slate-400">v</span>
      </button>

      {isOpen && (
        <div className="absolute right-0 z-20 mt-2 w-80 rounded-lg border border-slate-200 bg-white p-4 shadow-lg">
          <div className="border-b border-slate-200 pb-3">
            <p className="text-sm font-semibold text-slate-900">{fullName}</p>
            <p className="text-xs text-slate-500">{user?.email}</p>
            <p className="mt-1 text-xs font-medium uppercase text-blue-700">{user?.role ?? 'USER'}</p>
          </div>

          {isEditing ? (
            <form onSubmit={handleSubmit} className="mt-4 space-y-3">
              <div>
                <label htmlFor="profileFirstname" className="mb-1 block text-sm font-medium text-slate-700">
                  First Name
                </label>
                <input
                  id="profileFirstname"
                  value={firstname}
                  onChange={event => setFirstname(event.target.value)}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  required
                />
              </div>
              <div>
                <label htmlFor="profileLastname" className="mb-1 block text-sm font-medium text-slate-700">
                  Last Name
                </label>
                <input
                  id="profileLastname"
                  value={lastname}
                  onChange={event => setLastname(event.target.value)}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  required
                />
              </div>

              {error && <p className="text-sm text-red-600">{error}</p>}

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="flex-1 rounded-md bg-blue-700 px-3 py-2 text-sm font-medium text-white hover:bg-blue-800 disabled:bg-slate-300"
                >
                  {isSaving ? 'Saving...' : 'Save'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsEditing(false);
                    setError('');
                    setFirstname(user?.firstname ?? '');
                    setLastname(user?.lastname ?? '');
                  }}
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div className="mt-3 space-y-2">
              {successMessage && <p className="rounded-md bg-green-50 px-3 py-2 text-sm text-green-700">{successMessage}</p>}
              <button
                type="button"
                onClick={() => {
                  setIsEditing(true);
                  setSuccessMessage('');
                  setError('');
                }}
                className="w-full rounded-md px-3 py-2 text-left text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Edit profile
              </button>
              <button
                type="button"
                onClick={logout}
                className="w-full rounded-md px-3 py-2 text-left text-sm font-medium text-red-700 hover:bg-red-50"
              >
                Sign out
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AccountMenu;
