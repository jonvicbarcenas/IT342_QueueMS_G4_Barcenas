package edu.cit.barcenas.queuems.pattern.factory;

import edu.cit.barcenas.queuems.model.ServiceRequest;

public class ServiceRequestFactory {
    public static ServiceRequest createRequest(String userId, String counterId, String type) {
        ServiceRequest request = new ServiceRequest();
        request.setUserId(userId);
        request.setCounterId(counterId);
        request.setServiceType(type);
        return request;
    }

    public static ServiceRequest createRequest(
            String userId,
            String counterId,
            String counterName,
            String serviceType,
            String assignedTellerId,
            String assignedTellerName,
            String notes) {
        ServiceRequest request = createRequest(userId, counterId, serviceType);
        request.setCounterName(counterName);
        request.setAssignedTellerId(assignedTellerId);
        request.setAssignedTellerName(assignedTellerName);
        request.setNotes(notes);
        return request;
    }
}
