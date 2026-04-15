export type UserServiceRequestStatus = 'PENDING' | 'SERVING' | 'COMPLETED' | 'CANCELLED';

export interface UserServiceRequest {
  id: string;
  userId: string;
  counterId: string;
  counterName?: string;
  serviceType?: string;
  assignedTellerId?: string;
  assignedTellerName?: string;
  notes?: string;
  status: UserServiceRequestStatus;
  queueNumber: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateUserServiceRequestPayload {
  counterId: string;
  notes?: string;
}
