import type { UserServiceRequest } from '@/types/users/userServiceRequest';

export type CounterStatus = 'OPEN' | 'CLOSED';
export type AdminRole = 'USER' | 'TELLER' | 'SUPERADMIN';

export interface AdminCounter {
  id: string;
  name: string;
  serviceType: string;
  status: CounterStatus;
  assignedTellerId?: string;
  assignedTellerName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminCounterPayload {
  name: string;
  serviceType: string;
  status: CounterStatus;
  assignedTellerId?: string;
}

export interface AdminUser {
  uid: string;
  email: string;
  firstname: string;
  lastname: string;
  role: AdminRole;
  counterId?: string;
  counterName?: string;
}

export interface AdminStaffUserPayload {
  email: string;
  password?: string;
  firstname: string;
  lastname: string;
  role: AdminRole;
  counterId?: string;
}

export type AdminServiceRequest = UserServiceRequest;
