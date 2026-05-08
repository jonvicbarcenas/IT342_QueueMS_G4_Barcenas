import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline';
  isLoading?: boolean;
  children: ReactNode;
}

const Button = ({
  variant = 'primary',
  isLoading = false,
  children,
  className = '',
  disabled,
  ...props
}: ButtonProps) => {
  const baseStyles = 'min-h-11 w-full rounded-xl px-4 py-3 text-sm font-semibold transition-all duration-200 flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-stone-950 focus:ring-offset-2 focus:ring-offset-[#f4f2ed] disabled:opacity-50';
  
  const variants = {
    primary: 'bg-[#11100e] text-[#f7f5ef] shadow-[inset_0_1px_0_rgba(255,255,255,0.18)] hover:bg-[#24211d]',
    secondary: 'border border-stone-200 bg-[#efede7] text-stone-950 hover:bg-[#e4e1d8]',
    outline: 'border border-stone-200 bg-[#fffef9] text-stone-900 hover:border-stone-400 hover:bg-[#f7f5ef]',
  };

  return (
    <button
      className={`${baseStyles} ${variants[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <>
          <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Loading...
        </>
      ) : (
        children
      )}
    </button>
  );
};

export default Button;
