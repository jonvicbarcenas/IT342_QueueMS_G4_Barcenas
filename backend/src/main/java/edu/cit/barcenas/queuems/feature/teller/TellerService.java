package edu.cit.barcenas.queuems.feature.teller;

import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.Role;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.pattern.observer.QueueStatusObserver;
import edu.cit.barcenas.queuems.repository.CounterRepository;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class TellerService {

    private final UserRepository userRepository;
    private final CounterRepository counterRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final List<QueueStatusObserver> observers;

    public TellerService(
            UserRepository userRepository,
            CounterRepository counterRepository,
            ServiceRequestRepository serviceRequestRepository,
            List<QueueStatusObserver> observers) {
        this.userRepository = userRepository;
        this.counterRepository = counterRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.observers = observers;
    }

    public Counter getAssignedCounter(String tellerUid) throws ExecutionException, InterruptedException {
        User teller = getTeller(tellerUid);
        String counterId = normalizeRequired(teller.getCounterId(), "Assigned counter");
        Counter counter = counterRepository.findById(counterId);
        if (counter == null) {
            throw new IllegalArgumentException("Assigned counter not found: " + counterId);
        }
        return counter;
    }

    public List<ServiceRequest> getAssignedRequests(String tellerUid) throws ExecutionException, InterruptedException {
        Counter counter = getAssignedCounter(tellerUid);
        return serviceRequestRepository.findByCounterId(counter.getId());
    }

    public ServiceRequest getAssignedRequestById(String tellerUid, String requestId)
            throws ExecutionException, InterruptedException {
        return getAssignedRequest(tellerUid, requestId);
    }

    public ServiceRequest serveNextRequest(String tellerUid) throws ExecutionException, InterruptedException {
        Counter counter = getAssignedCounter(tellerUid);
        List<ServiceRequest> pendingRequests = serviceRequestRepository.findByCounterIdAndStatus(
                counter.getId(),
                ServiceRequest.STATUS_PENDING);

        if (pendingRequests.isEmpty()) {
            throw new IllegalArgumentException("No pending requests for " + counter.getName());
        }

        ServiceRequest request = pendingRequests.get(0);
        return updateOwnedRequestStatus(request, ServiceRequest.STATUS_SERVING);
    }

    public ServiceRequest serveRequest(String tellerUid, String requestId) throws ExecutionException, InterruptedException {
        ServiceRequest request = getAssignedRequest(tellerUid, requestId);
        if (!ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("Only pending requests can be served");
        }
        return updateOwnedRequestStatus(request, ServiceRequest.STATUS_SERVING);
    }

    public ServiceRequest updateRequestStatus(String tellerUid, String requestId, String status)
            throws ExecutionException, InterruptedException {
        String normalizedStatus = normalizeRequired(status, "status").toUpperCase();
        if (!ServiceRequest.isValidStatus(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid request status: " + normalizedStatus);
        }

        ServiceRequest request = getAssignedRequest(tellerUid, requestId);
        return updateOwnedRequestStatus(request, normalizedStatus);
    }

    public Counter updateCounterStatus(String tellerUid, String status) throws ExecutionException, InterruptedException {
        String normalizedStatus = normalizeRequired(status, "status").toUpperCase();
        if (!Counter.isValidStatus(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid counter status: " + normalizedStatus);
        }

        Counter counter = getAssignedCounter(tellerUid);
        counter.setStatus(normalizedStatus);
        counter.setUpdatedAt(new Date());
        counterRepository.save(counter);
        return counter;
    }

    private ServiceRequest getAssignedRequest(String tellerUid, String requestId)
            throws ExecutionException, InterruptedException {
        Counter counter = getAssignedCounter(tellerUid);
        ServiceRequest request = serviceRequestRepository.findById(normalizeRequired(requestId, "requestId"));
        if (request == null) {
            throw new IllegalArgumentException("Service request not found: " + requestId);
        }
        if (!counter.getId().equals(request.getCounterId())) {
            throw new IllegalArgumentException("You can only update requests assigned to your counter");
        }
        return request;
    }

    private ServiceRequest updateOwnedRequestStatus(ServiceRequest request, String status)
            throws ExecutionException, InterruptedException {
        request.setStatus(status);
        request.setUpdatedAt(new Date());
        serviceRequestRepository.save(request);
        observers.forEach(observer -> observer.onStatusChange(request));
        return request;
    }

    private User getTeller(String tellerUid) throws ExecutionException, InterruptedException {
        User teller = userRepository.findByUid(normalizeRequired(tellerUid, "tellerUid"));
        if (teller == null) {
            throw new IllegalArgumentException("Teller not found");
        }
        if (!Role.TELLER.equals(teller.getRole()) && !Role.SUPERADMIN.equals(teller.getRole())) {
            throw new IllegalArgumentException("Teller role is required");
        }
        return teller;
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
