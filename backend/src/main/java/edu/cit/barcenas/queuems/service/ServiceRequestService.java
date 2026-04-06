package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.pattern.factory.ServiceRequestFactory;
import edu.cit.barcenas.queuems.pattern.observer.QueueStatusObserver;
import edu.cit.barcenas.queuems.pattern.strategy.QueueNumberStrategy;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final QueueNumberStrategy strategy;
    private final List<QueueStatusObserver> observers;

    public ServiceRequestService(ServiceRequestRepository repository, QueueNumberStrategy strategy, List<QueueStatusObserver> observers) {
        this.repository = repository;
        this.strategy = strategy;
        this.observers = observers;
    }

    public ServiceRequest getRequestById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public ServiceRequest createRequest(String userId, String counterId) throws ExecutionException, InterruptedException {
        ServiceRequest request = ServiceRequestFactory.createRequest(userId, counterId, null);
        request.setQueueNumber(strategy.generateNext(repository));
        repository.save(request);
        return request;
    }

    public List<ServiceRequest> getUserRequests(String userId) throws ExecutionException, InterruptedException {
        return repository.findByUserId(userId);
    }

    public void cancelRequest(String requestId, String userId) throws ExecutionException, InterruptedException {
        ServiceRequest request = repository.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Service request not found: " + requestId);
        }
        
        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("You can only cancel your own requests");
        }

        if (ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
            request.setStatus(ServiceRequest.STATUS_CANCELLED);
            repository.save(request);
            notifyObservers(request);
        } else {
            throw new RuntimeException("Cannot cancel request with status: " + request.getStatus());
        }
    }

    /**
     * Notifies all registered observers about a status change.
     * @param request the request whose status has changed
     */
    private void notifyObservers(ServiceRequest request) {
        observers.forEach(observer -> observer.onStatusChange(request));
    }
}
