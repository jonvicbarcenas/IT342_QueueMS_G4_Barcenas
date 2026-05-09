import { useAuth } from '@/features/auth';
import AdminDashboardPage from '@/features/admin/AdminDashboardPage';
import TellerDashboardPage from '@/features/teller/TellerDashboardPage';
import UserDashboardPage from '@/features/user-requests/UserDashboardPage';

const DashboardRouterPage = () => {
  const { user } = useAuth();

  if (user?.role === 'SUPERADMIN') {
    return <AdminDashboardPage />;
  }

  if (user?.role === 'TELLER') {
    return <TellerDashboardPage />;
  }

  return <UserDashboardPage />;
};

export default DashboardRouterPage;
