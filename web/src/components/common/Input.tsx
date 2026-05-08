import { forwardRef } from 'react';
import type { InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className = '', ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-stone-500">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={`
            min-h-11 w-full rounded-xl border border-stone-200 bg-[#fffef9] px-4 py-3 text-sm text-stone-950
            shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]
            focus:border-stone-500 focus:outline-none focus:ring-2 focus:ring-stone-950/15
            placeholder:text-stone-400
            ${error ? 'border-red-500 bg-red-50' : ''}
            ${className}
          `}
          {...props}
        />
        {error && (
          <p className="mt-2 text-sm font-medium text-red-700">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
