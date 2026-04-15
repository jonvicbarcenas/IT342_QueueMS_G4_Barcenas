package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.pattern.factory.ServiceRequestFactory;
import edu.cit.barcenas.queuems.pattern.observer.QueueStatusObserver;
import edu.cit.barcenas.queuems.pattern.strategy.QueueNumberStrategy;
import edu.cit.barcenas.queuems.repository.CounterRepository;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final CounterRepository counterRepository;
    private final QueueNumberStrategy strategy;
    private final List<QueueStatusObserver> observers;

    public ServiceRequestService(
            ServiceRequestRepository repository,
            CounterRepository counterRepository,
            QueueNumberStrategy strategy,
            List<QueueStatusObserver> observers) {
        this.repository = repository;
        this.counterRepository = counterRepository;
        this.strategy = strategy;
        this.observers = observers;
    }

    public ServiceRequest getRequestById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public ServiceRequest getUserRequestById(String requestId, String userId) throws ExecutionException, InterruptedException {
        ServiceRequest request = repository.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Service request not found: " + requestId);
        }

        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("You can only view your own requests");
        }

        return request;
    }

    public ServiceRequest createRequest(String userId, String counterId, String serviceType, String notes)
            throws ExecutionException, InterruptedException {
        String normalizedCounterId = normalizeRequired(counterId, "counterId");

        Counter counter = counterRepository.findById(normalizedCounterId);
        if (counter == null) {
            throw new IllegalArgumentException("Counter not found: " + normalizedCounterId);
        }
        if (!Counter.STATUS_OPEN.equals(counter.getStatus())) {
            throw new IllegalArgumentException("Counter is closed: " + counter.getName());
        }

        ServiceRequest request = ServiceRequestFactory.createRequest(
                userId,
                counter.getId(),
                counter.getName(),
                counter.getServiceType(),
                counter.getAssignedTellerId(),
                counter.getAssignedTellerName(),
                normalizeOptional(notes));
        request.setQueueNumber(strategy.generateNext(repository));
        repository.save(request);
        return request;
    }

    public List<ServiceRequest> getUserRequests(String userId) throws ExecutionException, InterruptedException {
        return repository.findByUserId(userId);
    }

    public ServiceRequest cancelRequest(String requestId, String userId) throws ExecutionException, InterruptedException {
        ServiceRequest request = repository.findById(requestId);
        if (request == null) {
            throw new RuntimeException("Service request not found: " + requestId);
        }
        
        if (!request.getUserId().equals(userId)) {
            throw new RuntimeException("You can only cancel your own requests");
        }

        if (ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
            request.setStatus(ServiceRequest.STATUS_CANCELLED);
            request.setUpdatedAt(new java.util.Date());
            repository.save(request);
            notifyObservers(request);
            return request;
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

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

}
