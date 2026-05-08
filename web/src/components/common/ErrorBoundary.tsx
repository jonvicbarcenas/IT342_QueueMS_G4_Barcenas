import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public state: ErrorBoundaryState = {
    error: null,
  };

  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Application render error:', error, errorInfo);
  }

  public render() {
    if (this.state.error) {
      return (
        <div className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900">
          <div className="mx-auto max-w-2xl rounded-lg border border-red-200 bg-white p-6 shadow-sm">
            <p className="text-sm font-semibold text-red-700">QueueMS could not render this page</p>
            <h1 className="mt-2 text-2xl font-bold">Something crashed in the web app.</h1>
            <p className="mt-3 text-sm text-slate-600">
              Open the browser console for the full stack trace. The first error is:
            </p>
            <pre className="mt-4 overflow-auto rounded-md bg-slate-950 p-4 text-sm text-white">
              {this.state.error.message}
            </pre>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-5 rounded-md bg-blue-700 px-4 py-2 text-sm font-medium text-white hover:bg-blue-800"
            >
              Reload page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
