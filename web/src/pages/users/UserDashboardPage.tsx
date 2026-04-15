import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '@/context';
import { Button, Input } from '@components/common';
import { userCounterService } from '@services/users/userCounterService';
import { userServiceRequestService } from '@services/users/userServiceRequestService';
import type { CreateUserServiceRequestPayload, UserServiceRequest, UserServiceRequestStatus } from '@/types/users/userServiceRequest';
import type { UserCounter } from '@/types/users/userCounter';

const STATUS_LABELS: Record<UserServiceRequestStatus, string> = {
  PENDING: 'Pending',
  SERVING: 'Serving',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const STATUS_STYLES: Record<UserServiceRequestStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  SERVING: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  COMPLETED: 'bg-green-100 text-green-700 border-green-200',
  CANCELLED: 'bg-red-100 text-red-700 border-red-200',
};

const AUTO_REFRESH_INTERVAL_MS = 5000;

const getCounterName = (request: UserServiceRequest, counters: UserCounter[]): string => {
  if (request.counterName) {
    return request.counterName;
  }

  return counters.find(counter => counter.id === request.counterId)?.name ?? request.counterId;
};

const formatDate = (value: string): string => {
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

const countByStatus = (requests: UserServiceRequest[], status: UserServiceRequestStatus): number => (
  requests.filter(request => request.status === status).length
);

const UserDashboardPage = () => {
  const { user, logout } = useAuth();
  const [requests, setRequests] = useState<UserServiceRequest[]>([]);
  const [counters, setCounters] = useState<UserCounter[]>([]);
  const [selectedCounterId, setSelectedCounterId] = useState('');
  const [notes, setNotes] = useState('');
  const [isLoadingRequests, setIsLoadingRequests] = useState(true);
  const [isLoadingCounters, setIsLoadingCounters] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [cancellingRequestId, setCancellingRequestId] = useState<string | null>(null);
  const [formError, setFormError] = useState('');
  const [pageError, setPageError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const stats = useMemo(() => ({
    total: requests.length,
    pending: countByStatus(requests, 'PENDING'),
    serving: countByStatus(requests, 'SERVING'),
    completed: countByStatus(requests, 'COMPLETED'),
  }), [requests]);

  const loadRequests = useCallback(async (silent = false) => {
    if (!silent) {
      setIsLoadingRequests(true);
      setPageError('');
    }

    try {
      const data = await userServiceRequestService.getMyRequests();
      setRequests(data);
    } catch (error) {
      if (!silent) {
        setPageError(error instanceof Error ? error.message : 'Unable to load your service requests.');
      }
    } finally {
      if (!silent) {
        setIsLoadingRequests(false);
      }
    }
  }, []);

  const loadCounters = useCallback(async (silent = false) => {
    if (!silent) {
      setIsLoadingCounters(true);
      setPageError('');
    }

    try {
      const data = await userCounterService.getOpenCounters();
      setCounters(data);
      setSelectedCounterId(currentCounterId => {
        if (!currentCounterId || data.some(counter => counter.id === currentCounterId)) {
          return currentCounterId;
        }
        return '';
      });
    } catch (error) {
      if (!silent) {
        setPageError(error instanceof Error ? error.message : 'Unable to load service counters.');
      }
    } finally {
      if (!silent) {
        setIsLoadingCounters(false);
      }
    }
  }, []);

  useEffect(() => {
    loadRequests();
    loadCounters();
  }, [loadRequests, loadCounters]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      if (document.hidden) {
        return;
      }

      loadRequests(true);
      loadCounters(true);
    }, AUTO_REFRESH_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [loadRequests, loadCounters]);

  const handleCreateRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFormError('');
    setPageError('');
    setSuccessMessage('');

    const selectedCounter = counters.find(counter => counter.id === selectedCounterId);
    if (!selectedCounter) {
      setFormError('Please select a service counter.');
      return;
    }

    const payload: CreateUserServiceRequestPayload = {
      counterId: selectedCounter.id,
      notes: notes.trim() || undefined,
    };

    setIsCreating(true);

    try {
      const createdRequest = await userServiceRequestService.createRequest(payload);
      setRequests(currentRequests => [createdRequest, ...currentRequests]);
      setSelectedCounterId('');
      setNotes('');
      setSuccessMessage(`Request ${createdRequest.queueNumber} was created successfully.`);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to create service request.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancelRequest = async (request: UserServiceRequest) => {
    const shouldCancel = window.confirm(`Cancel request ${request.queueNumber}?`);
    if (!shouldCancel) {
      return;
    }

    setCancellingRequestId(request.id);
    setPageError('');
    setSuccessMessage('');

    try {
      const cancelledRequest = await userServiceRequestService.cancelRequest(request.id);
      setRequests(currentRequests => currentRequests.map(currentRequest => (
        currentRequest.id === cancelledRequest.id ? cancelledRequest : currentRequest
      )));
      setSuccessMessage(`Request ${cancelledRequest.queueNumber} was cancelled.`);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to cancel service request.');
    } finally {
      setCancellingRequestId(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <nav className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div>
            <p className="text-sm font-semibold text-blue-700">QueueMS</p>
            <h1 className="text-lg font-bold">Service Request Dashboard</h1>
          </div>
          <button
            onClick={logout}
            className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
          >
            Logout
          </button>
        </div>
      </nav>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6">
          <p className="text-sm text-slate-600">
            Signed in as {user?.firstname} {user?.lastname} ({user?.email})
          </p>
          <p className="text-sm text-slate-500">Role: {user?.role ?? 'USER'}</p>
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

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
          <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5">
              <h2 className="text-2xl font-bold">Create Service Request</h2>
              <p className="mt-1 text-sm text-slate-600">
                Choose the office you need and get your queue number.
              </p>
            </div>

            {formError && (
              <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {formError}
              </div>
            )}

            <form onSubmit={handleCreateRequest} className="space-y-4">
              <div>
                <label htmlFor="counterId" className="mb-1 block text-sm font-medium text-slate-700">
                  Service Counter
                </label>
                <select
                  id="counterId"
                  value={selectedCounterId}
                  onChange={event => setSelectedCounterId(event.target.value)}
                  className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-600"
                  disabled={isCreating || isLoadingCounters || counters.length === 0}
                >
                  <option value="">
                    {isLoadingCounters ? 'Loading counters...' : 'Select a counter'}
                  </option>
                  {counters.map(counter => (
                    <option key={counter.id} value={counter.id}>
                      {counter.name} - {counter.serviceType}
                    </option>
                  ))}
                </select>
                {!isLoadingCounters && counters.length === 0 && (
                  <p className="mt-2 text-sm text-red-600">
                    No open counters are available. Please check again later.
                  </p>
                )}
              </div>

              <Input
                label="Notes"
                type="text"
                placeholder="Optional request notes"
                value={notes}
                maxLength={160}
                onChange={event => setNotes(event.target.value)}
                disabled={isCreating}
              />

              <Button
                type="submit"
                isLoading={isCreating}
                className="bg-blue-700 hover:bg-blue-800"
                disabled={counters.length === 0}
              >
                Create Queue Request
              </Button>
            </form>
          </section>

          <aside className="space-y-4">
            <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold">Request Summary</h2>
              <div className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-md border border-slate-200 p-3">
                  <p className="text-xs font-semibold uppercase text-slate-500">Total</p>
                  <p className="mt-1 text-2xl font-bold">{stats.total}</p>
                </div>
                <div className="rounded-md border border-yellow-200 bg-yellow-50 p-3">
                  <p className="text-xs font-semibold uppercase text-yellow-700">Pending</p>
                  <p className="mt-1 text-2xl font-bold text-yellow-700">{stats.pending}</p>
                </div>
                <div className="rounded-md border border-indigo-200 bg-indigo-50 p-3">
                  <p className="text-xs font-semibold uppercase text-indigo-700">Serving</p>
                  <p className="mt-1 text-2xl font-bold text-indigo-700">{stats.serving}</p>
                </div>
                <div className="rounded-md border border-green-200 bg-green-50 p-3">
                  <p className="text-xs font-semibold uppercase text-green-700">Completed</p>
                  <p className="mt-1 text-2xl font-bold text-green-700">{stats.completed}</p>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold">Next Step</h2>
              <p className="mt-2 text-sm text-slate-600">
                Keep this page open and watch your request status. Cancel is available while the request is still pending.
              </p>
            </div>
          </aside>
        </div>

        <section className="mt-6 rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-3 border-b border-slate-200 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-xl font-bold">My Requests</h2>
              <p className="text-sm text-slate-600">Your queue numbers and current service status.</p>
            </div>
            <button
              onClick={() => loadRequests()}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
              disabled={isLoadingRequests}
            >
              {isLoadingRequests ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          {isLoadingRequests ? (
            <div className="px-6 py-10 text-center text-sm text-slate-600">Loading service requests...</div>
          ) : requests.length === 0 ? (
            <div className="px-6 py-10 text-center">
              <p className="text-base font-semibold text-slate-800">No service requests yet</p>
              <p className="mt-1 text-sm text-slate-600">Create your first request to receive a queue number.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Queue No.</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Counter</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Service</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold uppercase text-slate-500">Created</th>
                    <th className="px-6 py-3 text-right text-xs font-semibold uppercase text-slate-500">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 bg-white">
                  {requests.map(request => (
                    <tr key={request.id}>
                      <td className="whitespace-nowrap px-6 py-4 text-sm font-bold text-slate-900">
                        {request.queueNumber}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-700">
                        {getCounterName(request, counters)}
                      </td>
                      <td className="px-6 py-4 text-sm text-slate-700">
                        {request.serviceType ?? 'General Service'}
                        {request.assignedTellerName && (
                          <p className="mt-1 text-xs text-slate-500">Teller: {request.assignedTellerName}</p>
                        )}
                        {request.notes && (
                          <p className="mt-1 text-xs text-slate-500">Note: {request.notes}</p>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm">
                        <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_STYLES[request.status]}`}>
                          {STATUS_LABELS[request.status]}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600">
                        {formatDate(request.createdAt)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-right text-sm">
                        {request.status === 'PENDING' ? (
                          <button
                            onClick={() => handleCancelRequest(request)}
                            className="rounded-md bg-red-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:bg-slate-300"
                            disabled={cancellingRequestId === request.id}
                          >
                            {cancellingRequestId === request.id ? 'Cancelling...' : 'Cancel'}
                          </button>
                        ) : (
                          <span className="text-slate-400">No action</span>
                        )}
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

export default UserDashboardPage;
