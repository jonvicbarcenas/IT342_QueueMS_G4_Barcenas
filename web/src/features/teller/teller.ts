import type { UserCounter } from '@/features/user-requests/userCounter';
import type { UserServiceRequest, UserServiceRequestStatus } from '@/features/user-requests/userServiceRequest';

export type TellerCounter = UserCounter;
export type TellerServiceRequest = UserServiceRequest;
export type TellerRequestStatus = UserServiceRequestStatus;

export interface TellerCounterStatusPayload {
  status: 'OPEN' | 'CLOSED';
}

export interface TellerRequestStatusPayload {
  status: TellerRequestStatus;
}
