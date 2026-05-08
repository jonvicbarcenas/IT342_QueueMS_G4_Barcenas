import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '@/context';
import { AccountMenu, Button, Input } from '@components/common';
import { adminService } from '@services/admin/adminService';
import { webSocketService } from '@services/websocketService';
import type {
  AdminCounter,
  AdminCounterPayload,
  AdminRole,
  AdminServiceRequest,
  AdminStaffUserPayload,
  AdminUser,
  CounterStatus,
} from '@/types/admin/admin';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-green-50 text-green-700 border-green-200',
  CLOSED: 'bg-red-50 text-red-700 border-red-200',
  PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  SERVING: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  COMPLETED: 'bg-green-50 text-green-700 border-green-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
};

const emptyCounterForm: AdminCounterPayload = {
  name: '',
  serviceType: '',
  status: 'OPEN',
  assignedTellerId: '',
};

const emptyStaffForm: AdminStaffUserPayload = {
  email: '',
  password: '',
  firstname: '',
  lastname: '',
  role: 'TELLER',
  counterId: '',
};

const formatDate = (value?: string): string => {
  if (!value) {
    return 'Date unavailable';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'Date unavailable';
  }

  return new Intl.DateTimeFormat('en-PH', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
};

const fullName = (user: AdminUser): string => `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim() || user.email;

const countRequests = (requests: AdminServiceRequest[], status: string): number => (
  requests.filter(request => request.status === status).length
);

const AdminDashboardPage = () => {
  const { user } = useAuth();
  const [counters, setCounters] = useState<AdminCounter[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [requests, setRequests] = useState<AdminServiceRequest[]>([]);
  const [counterForm, setCounterForm] = useState<AdminCounterPayload>(emptyCounterForm);
  const [staffForm, setStaffForm] = useState<AdminStaffUserPayload>(emptyStaffForm);
  const [editingCounterId, setEditingCounterId] = useState<string | null>(null);
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSavingCounter, setIsSavingCounter] = useState(false);
  const [isSavingStaff, setIsSavingStaff] = useState(false);
  const [pageError, setPageError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const tellers = useMemo(() => users.filter(currentUser => currentUser.role === 'TELLER'), [users]);
  const stats = useMemo(() => ({
    totalRequests: requests.length,
    pending: countRequests(requests, 'PENDING'),
    serving: countRequests(requests, 'SERVING'),
    openCounters: counters.filter(counter => counter.status === 'OPEN').length,
  }), [counters, requests]);

  const loadAdminData = useCallback(async (silent = false) => {
    if (!silent) {
      setIsLoading(true);
      setPageError('');
    }

    try {
      const [counterData, userData, requestData] = await Promise.all([
        adminService.getCounters(),
        adminService.getUsers(),
        adminService.getRequests(),
      ]);
      setCounters(counterData);
      setUsers(userData);
      setRequests(requestData);
    } catch (error) {
      if (!silent) {
        setPageError(error instanceof Error ? error.message : 'Unable to load admin dashboard data.');
      }
    } finally {
      if (!silent) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    loadAdminData();

    // Connect to WebSocket for real-time updates
    const setupWebSocket = async () => {
      try {
        await webSocketService.connect();
        webSocketService.subscribe('/topic/queue', (updatedRequest: AdminServiceRequest) => {
          setRequests(currentRequests => {
            const index = currentRequests.findIndex(r => r.id === updatedRequest.id);
            if (index !== -1) {
              const newRequests = [...currentRequests];
              newRequests[index] = updatedRequest;
              return newRequests;
            } else {
              // Only add if it doesn't exist (e.g., new request)
              return [updatedRequest, ...currentRequests];
            }
          });

          // Also refresh counters in case a status change affected them
          loadAdminData(true);
        });
      } catch (error) {
        console.error('WebSocket connection failed:', error);
      }
    };

    setupWebSocket();

    return () => {
      webSocketService.disconnect();
    };
  }, [loadAdminData]);

  const resetCounterForm = () => {
    setCounterForm(emptyCounterForm);
    setEditingCounterId(null);
  };

  const resetStaffForm = () => {
    setStaffForm(emptyStaffForm);
    setEditingUserId(null);
  };

  const handleCounterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPageError('');
    setSuccessMessage('');
    setIsSavingCounter(true);

    try {
      const payload: AdminCounterPayload = {
        name: counterForm.name.trim(),
        serviceType: counterForm.serviceType.trim(),
        status: counterForm.status,
        assignedTellerId: counterForm.assignedTellerId || undefined,
      };

      if (editingCounterId) {
        const updatedCounter = await adminService.updateCounter(editingCounterId, payload);
        setCounters(currentCounters => currentCounters.map(counter => (
          counter.id === updatedCounter.id ? updatedCounter : counter
        )));
        setSuccessMessage(`${updatedCounter.name} was updated.`);
      } else {
        const createdCounter = await adminService.createCounter(payload);
        setCounters(currentCounters => [...currentCounters, createdCounter]);
        setSuccessMessage(`${createdCounter.name} was created.`);
      }

      resetCounterForm();
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to save counter.');
    } finally {
      setIsSavingCounter(false);
    }
  };

  const handleEditCounter = (counter: AdminCounter) => {
    setCounterForm({
      name: counter.name,
      serviceType: counter.serviceType,
      status: counter.status,
      assignedTellerId: counter.assignedTellerId ?? '',
    });
    setEditingCounterId(counter.id);
    setSuccessMessage('');
    setPageError('');
  };

  const handleDeleteCounter = async (counter: AdminCounter) => {
    const shouldDelete = window.confirm(`Delete ${counter.name}?`);
    if (!shouldDelete) {
      return;
    }

    setPageError('');
    setSuccessMessage('');

    try {
      await adminService.deleteCounter(counter.id);
      setCounters(currentCounters => currentCounters.filter(currentCounter => currentCounter.id !== counter.id));
      setSuccessMessage(`${counter.name} was deleted.`);
      if (editingCounterId === counter.id) {
        resetCounterForm();
      }
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to delete counter.');
    }
  };

  const handleStaffSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPageError('');
    setSuccessMessage('');
    setIsSavingStaff(true);

    try {
      const payload: AdminStaffUserPayload = {
        email: staffForm.email.trim(),
        ...(staffForm.password ? { password: staffForm.password } : {}),
        firstname: staffForm.firstname.trim(),
        lastname: staffForm.lastname.trim(),
        role: staffForm.role,
        counterId: staffForm.role === 'TELLER' && staffForm.counterId ? staffForm.counterId : undefined,
      };

      if (editingUserId) {
        const updatedUser = await adminService.updateUser(editingUserId, payload);
        setUsers(currentUsers => currentUsers.map(currentUser => (
          currentUser.uid === updatedUser.uid ? updatedUser : currentUser
        )));
        setSuccessMessage(`${fullName(updatedUser)} was updated.`);
      } else {
        if (!staffForm.password) {
          throw new Error('Temporary password is required');
        }

        const createdUser = await adminService.createStaffUser(payload);
        setUsers(currentUsers => [...currentUsers, createdUser]);
        setSuccessMessage(`${fullName(createdUser)} was created.`);
      }

      resetStaffForm();
      await loadAdminData();
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to save staff user.');
    } finally {
      setIsSavingStaff(false);
    }
  };

  const handleEditUser = (selectedUser: AdminUser) => {
    setStaffForm({
      email: selectedUser.email,
      password: '',
      firstname: selectedUser.firstname ?? '',
      lastname: selectedUser.lastname ?? '',
      role: selectedUser.role,
      counterId: selectedUser.counterId ?? '',
    });
    setEditingUserId(selectedUser.uid);
    setSuccessMessage('');
    setPageError('');
  };

  const handleDeleteUser = async (selectedUser: AdminUser) => {
    const shouldDelete = window.confirm(`Delete ${fullName(selectedUser)}?`);
    if (!shouldDelete) {
      return;
    }

    setPageError('');
    setSuccessMessage('');

    try {
      await adminService.deleteUser(selectedUser.uid);
      setUsers(currentUsers => currentUsers.filter(currentUser => currentUser.uid !== selectedUser.uid));
      setSuccessMessage(`${fullName(selectedUser)} was deleted.`);
      if (editingUserId === selectedUser.uid) {
        resetStaffForm();
      }
      await loadAdminData();
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to delete user.');
    }
  };

  const updateCounterForm = (field: keyof AdminCounterPayload, value: string) => {
    setCounterForm(currentForm => ({
      ...currentForm,
      [field]: field === 'status' ? value as CounterStatus : value,
    }));
  };

  const updateStaffForm = (field: keyof AdminStaffUserPayload, value: string) => {
    setStaffForm(currentForm => ({
      ...currentForm,
      [field]: field === 'role' ? value as AdminStaffUserPayload['role'] : value,
    }));
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <nav className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div>
            <p className="text-sm font-semibold text-blue-700">QueueMS Admin</p>
            <h1 className="text-lg font-bold">Superadmin Dashboard</h1>
          </div>
          <AccountMenu />
        </div>
      </nav>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm text-slate-600">
              Signed in as {user?.firstname} {user?.lastname} ({user?.email})
            </p>
            <p className="text-sm text-slate-500">Role: {user?.role ?? 'SUPERADMIN'}</p>
          </div>
          <button
            onClick={() => loadAdminData()}
            disabled={isLoading}
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:text-slate-400"
          >
            {isLoading ? 'Refreshing...' : 'Refresh Dashboard'}
          </button>
        </div>

        {successMessage && (
          <div className="mb-4 rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
            {successMessage}
          </div>
        )}

        {pageError && (
          <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {pageError}
          </div>
        )}

        <section className="mb-6 grid gap-4 md:grid-cols-4">
          <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-slate-500">All Requests</p>
            <p className="mt-2 text-3xl font-bold">{stats.totalRequests}</p>
          </div>
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-yellow-700">Pending</p>
            <p className="mt-2 text-3xl font-bold text-yellow-700">{stats.pending}</p>
          </div>
          <div className="rounded-lg border border-indigo-200 bg-indigo-50 p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-indigo-700">Serving</p>
            <p className="mt-2 text-3xl font-bold text-indigo-700">{stats.serving}</p>
          </div>
          <div className="rounded-lg border border-green-200 bg-green-50 p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-green-700">Open Counters</p>
            <p className="mt-2 text-3xl font-bold text-green-700">{stats.openCounters}</p>
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-2">
          <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5">
              <h2 className="text-xl font-bold">{editingCounterId ? 'Edit Counter' : 'Create Counter'}</h2>
              <p className="mt-1 text-sm text-slate-600">
                Service requests use these counter records and teller assignments.
              </p>
            </div>

            <form onSubmit={handleCounterSubmit} className="space-y-4">
              <Input
                label="Counter Name"
                value={counterForm.name}
                onChange={event => updateCounterForm('name', event.target.value)}
                placeholder="Registrar Window 1"
                required
              />
              <Input
                label="Service Type"
                value={counterForm.serviceType}
                onChange={event => updateCounterForm('serviceType', event.target.value)}
                placeholder="Records and Enrollment"
                required
              />
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="counterStatus" className="mb-1 block text-sm font-medium text-slate-700">
                    Status
                  </label>
                  <select
                    id="counterStatus"
                    value={counterForm.status}
                    onChange={event => updateCounterForm('status', event.target.value)}
                    className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  >
                    <option value="OPEN">Open</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="assignedTellerId" className="mb-1 block text-sm font-medium text-slate-700">
                    Assigned Teller
                  </label>
                  <select
                    id="assignedTellerId"
                    value={counterForm.assignedTellerId ?? ''}
                    onChange={event => updateCounterForm('assignedTellerId', event.target.value)}
                    className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  >
                    <option value="">No teller</option>
                    {tellers.map(teller => (
                      <option key={teller.uid} value={teller.uid}>
                        {fullName(teller)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex gap-3">
                <Button type="submit" isLoading={isSavingCounter} className="bg-blue-700 hover:bg-blue-800">
                  {editingCounterId ? 'Save Counter' : 'Create Counter'}
                </Button>
                {editingCounterId && (
                  <button
                    type="button"
                    onClick={resetCounterForm}
                    className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>

            <div className="mt-6 overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Counter</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Teller</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Status</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-slate-500">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {counters.map(counter => (
                    <tr key={counter.id}>
                      <td className="px-4 py-3 text-sm">
                        <p className="font-semibold text-slate-900">{counter.name}</p>
                        <p className="text-xs text-slate-500">{counter.serviceType}</p>
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700">
                        {counter.assignedTellerName ?? 'Unassigned'}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_STYLES[counter.status]}`}>
                          {counter.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-sm">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleEditCounter(counter)}
                            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteCounter(counter)}
                            className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {counters.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-500">
                        No counters have been created.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5">
              <h2 className="text-xl font-bold">{editingUserId ? 'Edit User Account' : 'Create Staff Account'}</h2>
              <p className="mt-1 text-sm text-slate-600">
                Tellers can be assigned to counters from this form or from counter editing.
              </p>
            </div>

            <form onSubmit={handleStaffSubmit} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="First Name"
                  value={staffForm.firstname}
                  onChange={event => updateStaffForm('firstname', event.target.value)}
                  required
                />
                <Input
                  label="Last Name"
                  value={staffForm.lastname}
                  onChange={event => updateStaffForm('lastname', event.target.value)}
                  required
                />
              </div>
              <Input
                label="Email"
                type="email"
                value={staffForm.email}
                onChange={event => updateStaffForm('email', event.target.value)}
                required
              />
              <Input
                label={editingUserId ? 'New Password' : 'Temporary Password'}
                type="password"
                value={staffForm.password}
                onChange={event => updateStaffForm('password', event.target.value)}
                minLength={6}
                placeholder={editingUserId ? 'Leave blank to keep current password' : undefined}
                required={!editingUserId}
              />
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="staffRole" className="mb-1 block text-sm font-medium text-slate-700">
                    Role
                  </label>
                  <select
                    id="staffRole"
                    value={staffForm.role}
                    onChange={event => updateStaffForm('role', event.target.value)}
                    className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  >
                    {editingUserId && <option value="USER">User</option>}
                    <option value="TELLER">Teller</option>
                    <option value="SUPERADMIN">Superadmin</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="staffCounterId" className="mb-1 block text-sm font-medium text-slate-700">
                    Counter
                  </label>
                  <select
                    id="staffCounterId"
                    value={staffForm.counterId ?? ''}
                    onChange={event => updateStaffForm('counterId', event.target.value)}
                    disabled={staffForm.role !== 'TELLER'}
                    className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600 disabled:bg-slate-100"
                  >
                    <option value="">No counter</option>
                    {counters.map(counter => (
                      <option key={counter.id} value={counter.id}>
                        {counter.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="flex gap-3">
                <Button type="submit" isLoading={isSavingStaff} className="bg-blue-700 hover:bg-blue-800">
                  {editingUserId ? 'Save User' : 'Create Staff Account'}
                </Button>
                {editingUserId && (
                  <button
                    type="button"
                    onClick={resetStaffForm}
                    className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>

            <div className="mt-6 overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">User</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Role</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Counter</th>
                    <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-slate-500">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {users.map(currentUser => (
                    <tr key={currentUser.uid}>
                      <td className="px-4 py-3 text-sm">
                        <p className="font-semibold text-slate-900">{fullName(currentUser)}</p>
                        <p className="text-xs text-slate-500">{currentUser.email}</p>
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700">{currentUser.role as AdminRole}</td>
                      <td className="px-4 py-3 text-sm text-slate-700">{currentUser.counterName ?? 'None'}</td>
                      <td className="px-4 py-3 text-right text-sm">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleEditUser(currentUser)}
                            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteUser(currentUser)}
                            className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {users.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-500">
                        No users found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <section className="mt-6 rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-200 px-6 py-5">
            <h2 className="text-xl font-bold">All Service Requests</h2>
            <p className="text-sm text-slate-600">Requests across every admin-managed counter.</p>
          </div>
          {isLoading ? (
            <div className="px-6 py-10 text-center text-sm text-slate-600">Loading admin records...</div>
          ) : requests.length === 0 ? (
            <div className="px-6 py-10 text-center text-sm text-slate-600">No service requests yet.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Queue No.</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Counter</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Teller</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {requests.map(request => (
                    <tr key={request.id}>
                      <td className="whitespace-nowrap px-6 py-4 text-sm font-bold text-slate-900">
                        {request.queueNumber}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <p className="font-medium text-slate-900">{request.counterName ?? request.counterId}</p>
                        <p className="text-xs text-slate-500">{request.serviceType ?? 'General Service'}</p>
                      </td>
                      <td className="px-6 py-4 text-sm text-slate-700">
                        {request.assignedTellerName ?? 'Unassigned'}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm">
                        <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_STYLES[request.status]}`}>
                          {request.status}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600">
                        {formatDate(request.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default AdminDashboardPage;
