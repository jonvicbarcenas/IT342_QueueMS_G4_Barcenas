package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.pattern.strategy.QueueNumberStrategy;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final QueueNumberStrategy strategy;

    public ServiceRequestService(ServiceRequestRepository repository, QueueNumberStrategy strategy) {
        this.repository = repository;
        this.strategy = strategy;
    }

    public ServiceRequestRepository getRepository() {
        return repository;
    }

    public ServiceRequest createRequest(String userId, String counterId) throws ExecutionException, InterruptedException {
        ServiceRequest request = new ServiceRequest();
        request.setUserId(userId);
        request.setCounterId(counterId);
        request.setQueueNumber(strategy.generateNext(repository));
        repository.save(request);
        return request;
    }

    public List<ServiceRequest> getUserRequests(String userId) throws ExecutionException, InterruptedException {
        return repository.findByUserId(userId);
    }

    public void cancelRequest(String requestId) throws ExecutionException, InterruptedException {
        ServiceRequest request = repository.findById(requestId);
        if (request != null && ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
            request.setStatus(ServiceRequest.STATUS_CANCELLED);
            repository.save(request);
        } else if (request == null) {
            throw new RuntimeException("Service request not found: " + requestId);
        } else {
            throw new RuntimeException("Cannot cancel request with status: " + request.getStatus());
        }
    }
}
