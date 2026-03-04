# React Project Folder Structure

This project follows React best practices for 2025 with a scalable and maintainable folder structure.

## Directory Structure

```
src/
├── assets/              # Static assets (images, fonts, etc.)
├── components/          # Reusable React components
│   ├── common/         # Generic reusable components (Button, Input, Modal, etc.)
│   └── layout/         # Layout components (Header, Footer, Sidebar, etc.)
├── pages/              # Page components (route-level components)
├── hooks/              # Custom React hooks
├── context/            # React Context providers and consumers
├── services/           # API calls and external service integrations
├── utils/              # Utility functions and helpers
├── constants/          # Application constants and configuration
├── types/              # TypeScript type definitions and interfaces
├── routes/             # Route configuration and guards
├── styles/             # Global styles and theme configuration
├── App.tsx             # Main App component
├── main.tsx            # Application entry point
└── index.css           # Global CSS with Tailwind directives
```

## Folder Descriptions

### `/components`
- **common/**: Reusable UI components that can be used across the application
  - Examples: Button, Input, Card, Modal, Spinner, etc.
- **layout/**: Components that define the app's layout structure
  - Examples: Header, Footer, Sidebar, Navigation, etc.

### `/pages`
- Page-level components that represent different routes
- Each page should be in its own file or folder
- Examples: HomePage, LoginPage, DashboardPage, etc.

### `/hooks`
- Custom React hooks for reusable logic
- Examples: useAuth, useFetch, useLocalStorage, useDebounce, etc.

### `/context`
- React Context API providers and consumers
- State management using Context
- Examples: AuthContext, ThemeContext, UserContext, etc.

### `/services`
- API service functions
- External service integrations
- HTTP client configuration
- Examples: authService, userService, queueService, etc.

### `/utils`
- Pure utility functions
- Helper functions
- Examples: formatters, validators, converters, etc.

### `/constants`
- Application-wide constants
- Configuration values
- Enums and static data
- Examples: API endpoints, app config, route paths, etc.

### `/types`
- TypeScript type definitions
- Interfaces
- Shared types across the application

### `/routes`
- Route configuration
- Protected route wrappers
- Route guards and navigation logic

### `/styles`
- Global styles
- Theme configuration
- CSS modules or styled-components if used

## Best Practices

1. **Barrel Exports**: Use `index.ts` files for cleaner imports
2. **Separation of Concerns**: Keep components, logic, and data separate
3. **Reusability**: Build components that can be reused
4. **Type Safety**: Use TypeScript for all files
5. **Naming Conventions**: 
   - Components: PascalCase (e.g., `UserProfile.tsx`)
   - Hooks: camelCase with 'use' prefix (e.g., `useAuth.ts`)
   - Utils: camelCase (e.g., `formatDate.ts`)
   - Constants: UPPER_SNAKE_CASE (e.g., `API_BASE_URL`)

## Import Examples

```typescript
// Good: Using barrel exports
import { Button, Input, Modal } from '@/components/common';
import { Header, Footer } from '@/components/layout';
import { useAuth, useFetch } from '@/hooks';

// Component structure
import { UserList } from './UserList';
import { useUsers } from '@/hooks/useUsers';
import { API_ROUTES } from '@/constants';
import type { User } from '@/types';
```
