package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.dto.QueuePositionDTO;
import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.pattern.factory.ServiceRequestFactory;
import edu.cit.barcenas.queuems.pattern.observer.QueueStatusObserver;
import edu.cit.barcenas.queuems.pattern.strategy.QueueNumberStrategy;
import edu.cit.barcenas.queuems.repository.CounterRepository;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class ServiceRequestService {
    private static final int ESTIMATED_MINUTES_PER_REQUEST = 5;

    private final ServiceRequestRepository repository;
    private final CounterRepository counterRepository;
    private final QueueNumberStrategy strategy;
    private final List<QueueStatusObserver> observers;
    private final HolidayService holidayService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FcmService fcmService;
    private final FileStorageService fileStorageService;

    public ServiceRequestService(
            ServiceRequestRepository repository,
            CounterRepository counterRepository,
            QueueNumberStrategy strategy,
            List<QueueStatusObserver> observers,
            HolidayService holidayService,
            UserRepository userRepository,
            EmailService emailService,
            FcmService fcmService,
            FileStorageService fileStorageService) {
        this.repository = repository;
        this.counterRepository = counterRepository;
        this.strategy = strategy;
        this.observers = observers;
        this.holidayService = holidayService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.fcmService = fcmService;
        this.fileStorageService = fileStorageService;
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

        // Check if today is a public holiday
        if (holidayService.isPublicHoliday(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Today is a public holiday. Bookings are not available.");
        }

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
        sendRequestConfirmation(request);
        notifyObservers(request);
        return request;
    }

    public List<ServiceRequest> getUserRequests(String userId) throws ExecutionException, InterruptedException {
        return repository.findByUserId(userId);
    }

    public QueuePositionDTO getQueuePosition(String requestId, String userId)
            throws ExecutionException, InterruptedException {
        ServiceRequest request = getUserRequestById(requestId, userId);
        if (!ServiceRequest.STATUS_PENDING.equals(request.getStatus())
                && !ServiceRequest.STATUS_SERVING.equals(request.getStatus())) {
            return new QueuePositionDTO(request.getId(), 0, 0, 0, 0);
        }

        List<ServiceRequest> activeRequests = repository.findByCounterId(request.getCounterId()).stream()
                .filter(item -> ServiceRequest.STATUS_PENDING.equals(item.getStatus())
                        || ServiceRequest.STATUS_SERVING.equals(item.getStatus()))
                .sorted(java.util.Comparator.comparing(
                        ServiceRequest::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();

        int index = -1;
        for (int i = 0; i < activeRequests.size(); i++) {
            if (request.getId().equals(activeRequests.get(i).getId())) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            return new QueuePositionDTO(request.getId(), 0, activeRequests.size(), 0, 0);
        }

        int position = index + 1;
        int peopleAhead = index;
        return new QueuePositionDTO(
                request.getId(),
                position,
                activeRequests.size(),
                peopleAhead,
                peopleAhead * ESTIMATED_MINUTES_PER_REQUEST);
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

    public ServiceRequest attachSupportingDocument(
            String requestId,
            String userId,
            org.springframework.web.multipart.MultipartFile file) throws Exception {
        ServiceRequest request = getUserRequestById(requestId, userId);
        if (!ServiceRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("Attachments can only be added to pending requests");
        }

        FileStorageService.StoredFile storedFile = fileStorageService.store(file);
        request.setAttachmentOriginalName(storedFile.originalName());
        request.setAttachmentStoredName(storedFile.storedName());
        request.setAttachmentContentType(storedFile.contentType());
        request.setAttachmentUrl(storedFile.secureUrl());
        request.setUpdatedAt(new java.util.Date());
        repository.save(request);
        return request;
    }

    private void sendRequestConfirmation(ServiceRequest request) {
        try {
            User user = userRepository.findByUid(request.getUserId());
            if (user == null) {
                return;
            }

            String subject = "QueueMS: Queue Request Confirmed";
            String message = String.format(
                    "Hello %s, your queue request %s for %s at %s has been created and is currently %s.",
                    user.getFirstname() != null ? user.getFirstname() : user.getEmail(),
                    request.getQueueNumber(),
                    request.getServiceType(),
                    request.getCounterName(),
                    request.getStatus());

            emailService.sendSimpleMessage(user.getEmail(), subject, message);
            fcmService.sendNotification(user.getFcmToken(), subject, message);
        } catch (Exception e) {
            System.err.println("Failed to send request confirmation: " + e.getMessage());
        }
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
