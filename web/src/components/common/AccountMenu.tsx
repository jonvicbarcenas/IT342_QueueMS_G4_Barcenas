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
        className="flex min-h-11 items-center gap-3 rounded-xl border border-stone-200 bg-[#fffef9] px-3 py-2 text-left transition-colors hover:bg-[#f7f5ef] focus:outline-none focus:ring-2 focus:ring-stone-950/20"
        aria-expanded={isOpen}
      >
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-stone-950 text-sm font-semibold text-[#f7f5ef]">
          {initials}
        </span>
        <span className="hidden sm:block">
          <span className="block text-sm font-semibold text-stone-950">{fullName}</span>
          <span className="block text-xs text-stone-500">{user?.role ?? 'USER'}</span>
        </span>
        <span className="text-stone-400">v</span>
      </button>

      {isOpen && (
        <div className="qm-card absolute right-0 z-20 mt-2 w-80 rounded-2xl p-4">
          <div className="border-b border-stone-200 pb-3">
            <p className="text-sm font-semibold text-stone-950">{fullName}</p>
            <p className="text-xs text-stone-500">{user?.email}</p>
            <p className="mt-1 text-xs font-medium uppercase text-stone-700">{user?.role ?? 'USER'}</p>
          </div>

          {isEditing ? (
            <form onSubmit={handleSubmit} className="mt-4 space-y-3">
              <div>
                <label htmlFor="profileFirstname" className="mb-1 block text-sm font-medium text-stone-700">
                  First Name
                </label>
                <input
                  id="profileFirstname"
                  value={firstname}
                  onChange={event => setFirstname(event.target.value)}
                  className="w-full rounded-xl border border-stone-200 bg-[#fffef9] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-stone-950/20"
                  required
                />
              </div>
              <div>
                <label htmlFor="profileLastname" className="mb-1 block text-sm font-medium text-stone-700">
                  Last Name
                </label>
                <input
                  id="profileLastname"
                  value={lastname}
                  onChange={event => setLastname(event.target.value)}
                  className="w-full rounded-xl border border-stone-200 bg-[#fffef9] px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-stone-950/20"
                  required
                />
              </div>

              {error && <p className="text-sm text-red-600">{error}</p>}

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="flex-1 rounded-xl bg-stone-950 px-3 py-2 text-sm font-medium text-[#f7f5ef] hover:bg-stone-800 disabled:bg-stone-300"
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
                  className="rounded-xl border border-stone-200 px-3 py-2 text-sm font-medium text-stone-700 hover:bg-[#f7f5ef]"
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div className="mt-3 space-y-2">
              {successMessage && <p className="rounded-xl border border-stone-200 bg-[#f7f5ef] px-3 py-2 text-sm text-stone-700">{successMessage}</p>}
              <button
                type="button"
                onClick={() => {
                  setIsEditing(true);
                  setSuccessMessage('');
                  setError('');
                }}
                className="w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-stone-700 hover:bg-[#f7f5ef]"
              >
                Edit profile
              </button>
              <button
                type="button"
                onClick={logout}
                className="w-full rounded-xl px-3 py-2 text-left text-sm font-medium text-red-700 hover:bg-red-50"
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
