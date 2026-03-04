import { useAuth } from '@context';

const DashboardPage = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <h1 className="text-xl font-bold">QueueMS Dashboard</h1>
            <button
              onClick={logout}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-2xl font-bold mb-4">Welcome to QueueMS!</h2>
          <p className="text-gray-600">
            {user ? `Logged in as: ${user.email}` : 'Loading user information...'}
          </p>
          <p className="text-gray-500 mt-4">
            This is your dashboard. Queue management features coming soon!
          </p>
        </div>
      </main>
    </div>
  );
};

export default DashboardPage;
