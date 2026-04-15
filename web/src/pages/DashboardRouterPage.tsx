import { useAuth } from '@/context';
import AdminDashboardPage from './admin/AdminDashboardPage';
import TellerDashboardPage from './teller/TellerDashboardPage';
import UserDashboardPage from './users/UserDashboardPage';

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
