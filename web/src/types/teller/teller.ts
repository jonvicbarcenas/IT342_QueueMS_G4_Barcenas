import type { UserCounter } from '@/types/users/userCounter';
import type { UserServiceRequest, UserServiceRequestStatus } from '@/types/users/userServiceRequest';

export type TellerCounter = UserCounter;
export type TellerServiceRequest = UserServiceRequest;
export type TellerRequestStatus = UserServiceRequestStatus;

export interface TellerCounterStatusPayload {
  status: 'OPEN' | 'CLOSED';
}

export interface TellerRequestStatusPayload {
  status: TellerRequestStatus;
}
