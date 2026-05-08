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
        <div className="qm-shell min-h-screen px-4 py-10 text-stone-950">
          <div className="qm-card mx-auto max-w-2xl rounded-[28px] p-6">
            <p className="text-sm font-semibold text-red-700">QueueMS could not render this page</p>
            <h1 className="mt-2 text-2xl font-bold">Something crashed in the web app.</h1>
            <p className="mt-3 text-sm text-stone-600">
              Open the browser console for the full stack trace. The first error is:
            </p>
            <pre className="mt-4 overflow-auto rounded-xl bg-stone-950 p-4 text-sm text-[#f7f5ef]">
              {this.state.error.message}
            </pre>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-5 rounded-xl bg-stone-950 px-4 py-2 text-sm font-medium text-[#f7f5ef] hover:bg-stone-800"
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
