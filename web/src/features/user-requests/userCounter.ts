export type UserCounterStatus = 'OPEN' | 'CLOSED';

export interface UserCounter {
  id: string;
  name: string;
  serviceType: string;
  status: UserCounterStatus;
  assignedTellerId?: string;
  assignedTellerName?: string;
}
