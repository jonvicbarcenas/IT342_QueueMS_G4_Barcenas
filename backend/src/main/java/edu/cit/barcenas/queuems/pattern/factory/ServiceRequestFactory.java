package edu.cit.barcenas.queuems.pattern.factory;

import edu.cit.barcenas.queuems.model.ServiceRequest;

public class ServiceRequestFactory {
    public static ServiceRequest createRequest(String userId, String counterId, String type) {
        ServiceRequest request = new ServiceRequest();
        request.setUserId(userId);
        request.setCounterId(counterId);
        // Add logic for 'type' if needed later (e.g. Regular vs Priority)
        return request;
    }
}
