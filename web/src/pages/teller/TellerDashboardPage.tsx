import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/context';
import { AccountMenu } from '@components/common';
import { tellerService } from '@services/teller/tellerService';
import { webSocketService } from '@services/websocketService';
import type { TellerCounter, TellerRequestStatus, TellerServiceRequest } from '@/types/teller/teller';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-green-50 text-green-700 border-green-200',
  CLOSED: 'bg-red-50 text-red-700 border-red-200',
  PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  SERVING: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  COMPLETED: 'bg-green-50 text-green-700 border-green-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
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

const countByStatus = (requests: TellerServiceRequest[], status: TellerRequestStatus): number => (
  requests.filter(request => request.status === status).length
);

const TellerDashboardPage = () => {
  const { user } = useAuth();
  const [counter, setCounter] = useState<TellerCounter | null>(null);
  const [requests, setRequests] = useState<TellerServiceRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdatingCounter, setIsUpdatingCounter] = useState(false);
  const [isServingNext, setIsServingNext] = useState(false);
  const [updatingRequestId, setUpdatingRequestId] = useState<string | null>(null);
  const [pageError, setPageError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const stats = useMemo(() => ({
    total: requests.length,
    pending: countByStatus(requests, 'PENDING'),
    serving: countByStatus(requests, 'SERVING'),
    completed: countByStatus(requests, 'COMPLETED'),
  }), [requests]);

  useEffect(() => {
    if (!successMessage) {
      return;
    }

    const timer = window.setTimeout(() => {
      setSuccessMessage('');
    }, 4000);

    return () => window.clearTimeout(timer);
  }, [successMessage]);

  const loadTellerData = useCallback(async (silent = false) => {
    if (!silent) {
      setIsLoading(true);
      setPageError('');
    }

    try {
      const [counterData, requestData] = await Promise.all([
        tellerService.getAssignedCounter(),
        tellerService.getAssignedRequests(),
      ]);
      setCounter(counterData);
      setRequests(requestData);
    } catch (error) {
      if (!silent) {
        setPageError(error instanceof Error ? error.message : 'Unable to load teller dashboard.');
      }
    } finally {
      if (!silent) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    loadTellerData();
  }, [loadTellerData]);

  useEffect(() => {
    // Only subscribe if we have counter data
    if (!counter?.id) return;

    const setupWebSocket = async () => {
      try {
        await webSocketService.connect();
        webSocketService.subscribe('/topic/queue', (updatedRequest: TellerServiceRequest) => {
          // Only update if it belongs to this counter
          if (updatedRequest.counterId === counter.id) {
            setRequests(currentRequests => {
              const index = currentRequests.findIndex(r => r.id === updatedRequest.id);
              if (index !== -1) {
                const newRequests = [...currentRequests];
                newRequests[index] = updatedRequest;
                return newRequests;
              } else if (updatedRequest.status === 'PENDING') {
                // New pending request for this counter
                return [updatedRequest, ...currentRequests];
              }
              return currentRequests;
            });
          }
        });
      } catch (error) {
        console.error('WebSocket connection failed:', error);
      }
    };

    setupWebSocket();

    return () => {
      webSocketService.disconnect();
    };
  }, [counter?.id]);

  const replaceRequest = (updatedRequest: TellerServiceRequest) => {
    setRequests(currentRequests => currentRequests.map(request => (
      request.id === updatedRequest.id ? updatedRequest : request
    )));
  };

  const handleCounterStatus = async () => {
    if (!counter) {
      return;
    }

    setIsUpdatingCounter(true);
    setPageError('');
    setSuccessMessage('');

    try {
      const nextStatus = counter.status === 'OPEN' ? 'CLOSED' : 'OPEN';
      const updatedCounter = await tellerService.updateCounterStatus({ status: nextStatus });
      setCounter(updatedCounter);
      setSuccessMessage(`${updatedCounter.name} is now ${updatedCounter.status.toLowerCase()}.`);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to update counter status.');
    } finally {
      setIsUpdatingCounter(false);
    }
  };

  const handleServeNext = async () => {
    setIsServingNext(true);
    setPageError('');
    setSuccessMessage('');

    try {
      const updatedRequest = await tellerService.serveNextRequest();
      replaceRequest(updatedRequest);
      setSuccessMessage(`Now serving ${updatedRequest.queueNumber}.`);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to serve next request.');
    } finally {
      setIsServingNext(false);
    }
  };

  const handleServeRequest = async (request: TellerServiceRequest) => {
    setUpdatingRequestId(request.id);
    setPageError('');
    setSuccessMessage('');

    try {
      const updatedRequest = await tellerService.serveRequest(request.id);
      replaceRequest(updatedRequest);
      setSuccessMessage(`Now serving ${updatedRequest.queueNumber}.`);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to serve request.');
    } finally {
      setUpdatingRequestId(null);
    }
  };

  const handleStatusUpdate = async (request: TellerServiceRequest, status: TellerRequestStatus) => {
    setUpdatingRequestId(request.id);
    setPageError('');
    setSuccessMessage('');

    try {
      const updatedRequest = await tellerService.updateRequestStatus(request.id, { status });
      replaceRequest(updatedRequest);
      setSuccessMessage(`${updatedRequest.queueNumber} was marked ${status.toLowerCase()}.`);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : 'Unable to update request status.');
    } finally {
      setUpdatingRequestId(null);
    }
  };

  const handleOpenDocument = (request: TellerServiceRequest) => {
    const attachmentUrl = tellerService.getAttachmentUrl(request);
    if (!attachmentUrl) {
      setPageError('No supporting document is attached to this request.');
      return;
    }

    window.open(attachmentUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="qm-shell min-h-screen text-slate-900">
      <nav className="qm-nav">
        <div className="mx-auto flex h-16 w-full max-w-[1760px] items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <span className="qm-logo h-10 w-10"><span className="qm-logo-mark" /></span>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-stone-500">QueueMS Teller</p>
              <h1 className="text-lg font-bold">Counter Queue Dashboard</h1>
            </div>
          </div>
          <AccountMenu />
        </div>
      </nav>

      <main className="mx-auto w-full max-w-[1760px] px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm text-slate-600">
              Signed in as {user?.firstname} {user?.lastname} ({user?.email})
            </p>
            <p className="qm-dot-text mt-2 text-3xl font-black">SERVE. UPDATE. PREVENT.</p>
            <p className="text-sm text-slate-500">Role: {user?.role ?? 'TELLER'}</p>
          </div>
          <button
            onClick={() => loadTellerData()}
            disabled={isLoading}
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:text-slate-400"
          >
            {isLoading ? 'Refreshing...' : 'Refresh Queue'}
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

        <div className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
          <aside className="space-y-4">
            <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase text-slate-500">Assigned Counter</p>
              {counter ? (
                <>
                  <h2 className="mt-2 text-2xl font-bold">{counter.name}</h2>
                  <p className="mt-1 text-sm text-slate-600">{counter.serviceType}</p>
                  <span className={`mt-4 inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_STYLES[counter.status]}`}>
                    {counter.status}
                  </span>
                  <button
                    onClick={handleCounterStatus}
                    disabled={isUpdatingCounter}
                    className="mt-5 w-full rounded-xl bg-stone-950 px-4 py-3 text-sm font-medium text-[#f7f5ef] transition-colors hover:bg-stone-800 disabled:bg-slate-300"
                  >
                    {isUpdatingCounter
                      ? 'Updating...'
                      : counter.status === 'OPEN' ? 'Close Counter' : 'Open Counter'}
                  </button>
                </>
              ) : (
                <p className="mt-2 text-sm text-slate-600">
                  No counter is assigned to this teller account.
                </p>
              )}
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="text-lg font-semibold">Queue Summary</h2>
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
            </section>
          </aside>

          <section className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-xl font-bold">Assigned Requests</h2>
                <p className="text-sm text-slate-600">Requests submitted to your assigned counter.</p>
              </div>
              <button
                onClick={handleServeNext}
                disabled={isServingNext || stats.pending === 0 || !counter}
                className="rounded-xl bg-stone-950 px-4 py-2 text-sm font-medium text-[#f7f5ef] transition-colors hover:bg-stone-800 disabled:bg-slate-300"
              >
                {isServingNext ? 'Serving...' : 'Serve Next'}
              </button>
            </div>

            {isLoading ? (
              <div className="px-6 py-10 text-center text-sm text-slate-600">Loading assigned requests...</div>
            ) : requests.length === 0 ? (
              <div className="px-6 py-10 text-center text-sm text-slate-600">No requests assigned to this counter.</div>
            ) : (
              <div>
                <div className="hidden grid-cols-[82px_minmax(150px,1fr)_130px_minmax(150px,220px)_170px_minmax(160px,210px)] gap-4 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-semibold uppercase text-slate-500 2xl:grid">
                  <span>Queue</span>
                  <span>Service</span>
                  <span>Status</span>
                  <span>Document</span>
                  <span>Created</span>
                  <span className="text-right">Action</span>
                </div>
                <div className="divide-y divide-slate-200">
                  {requests.map(request => (
                    <article
                      key={request.id}
                      className="grid gap-4 px-6 py-5 2xl:grid-cols-[82px_minmax(150px,1fr)_130px_minmax(150px,220px)_170px_minmax(160px,210px)] 2xl:items-center"
                    >
                      <div>
                        <p className="text-xs font-semibold uppercase text-slate-500 2xl:hidden">Queue</p>
                        <p className="text-base font-bold text-slate-900">{request.queueNumber}</p>
                      </div>

                      <div className="min-w-0">
                        <p className="text-xs font-semibold uppercase text-slate-500 2xl:hidden">Service</p>
                        <p className="text-sm font-medium text-slate-800">
                          {request.serviceType ?? counter?.serviceType ?? 'General Service'}
                        </p>
                        {request.notes && (
                          <p className="mt-1 text-xs text-slate-500">Note: {request.notes}</p>
                        )}
                      </div>

                      <div>
                        <p className="text-xs font-semibold uppercase text-slate-500 2xl:hidden">Status</p>
                        <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_STYLES[request.status]}`}>
                          {request.status}
                        </span>
                      </div>

                      <div className="min-w-0">
                        <p className="text-xs font-semibold uppercase text-slate-500 2xl:hidden">Document</p>
                        {request.attachmentUrl || request.attachmentOriginalName ? (
                          <button
                            onClick={() => handleOpenDocument(request)}
                            className="min-h-10 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
                          >
                            View Document
                          </button>
                        ) : (
                          <span className="text-sm text-slate-400">No document</span>
                        )}
                        {request.attachmentOriginalName && (
                          <p className="mt-1 truncate text-xs text-slate-500" title={request.attachmentOriginalName}>
                            {request.attachmentOriginalName}
                          </p>
                        )}
                      </div>

                      <div>
                        <p className="text-xs font-semibold uppercase text-slate-500 2xl:hidden">Created</p>
                        <p className="text-sm text-slate-600">{formatDate(request.createdAt)}</p>
                      </div>

                      <div className="flex flex-wrap justify-start gap-2 2xl:justify-end">
                        {request.status === 'PENDING' && (
                          <button
                            onClick={() => handleServeRequest(request)}
                            disabled={updatingRequestId === request.id}
                            className="min-h-10 rounded-xl bg-stone-950 px-3 py-2 text-sm font-medium text-[#f7f5ef] transition-colors hover:bg-stone-800 disabled:bg-slate-300"
                          >
                            Serve
                          </button>
                        )}
                        {request.status === 'SERVING' && (
                          <button
                            onClick={() => handleStatusUpdate(request, 'COMPLETED')}
                            disabled={updatingRequestId === request.id}
                            className="min-h-10 rounded-xl bg-green-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-green-700 disabled:bg-slate-300"
                          >
                            Complete
                          </button>
                        )}
                        {(request.status === 'PENDING' || request.status === 'SERVING') && (
                          <button
                            onClick={() => handleStatusUpdate(request, 'CANCELLED')}
                            disabled={updatingRequestId === request.id}
                            className="min-h-10 rounded-xl border border-red-300 px-3 py-2 text-sm font-medium text-red-700 transition-colors hover:bg-red-50 disabled:text-slate-400"
                          >
                            Cancel
                          </button>
                        )}
                        {request.status !== 'PENDING' && request.status !== 'SERVING' && (
                          <span className="self-center text-sm text-slate-400">No action</span>
                        )}
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
};

export default TellerDashboardPage;
