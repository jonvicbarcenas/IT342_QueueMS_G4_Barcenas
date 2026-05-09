package edu.cit.barcenas.queuems.feature.counters;

import edu.cit.barcenas.queuems.feature.admin.AdminCounterDTO;
import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.Role;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.CounterRepository;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class CounterService {

    private final CounterRepository counterRepository;
    private final UserRepository userRepository;

    public CounterService(CounterRepository counterRepository, UserRepository userRepository) {
        this.counterRepository = counterRepository;
        this.userRepository = userRepository;
    }

    public List<Counter> getAllCounters() throws ExecutionException, InterruptedException {
        return counterRepository.findAll();
    }

    public List<Counter> getOpenCounters() throws ExecutionException, InterruptedException {
        return counterRepository.findByStatus(Counter.STATUS_OPEN);
    }

    public Counter getCounterById(String id) throws ExecutionException, InterruptedException {
        String normalizedId = normalizeRequired(id, "counterId");
        Counter counter = counterRepository.findById(normalizedId);
        if (counter == null) {
            throw new IllegalArgumentException("Counter not found: " + normalizedId);
        }
        return counter;
    }

    public Counter createCounter(AdminCounterDTO dto) throws ExecutionException, InterruptedException {
        Counter counter = new Counter();
        applyCounterFields(counter, dto, true);
        counterRepository.save(counter);
        syncTellerAssignment(counter, null);
        return counter;
    }

    public Counter updateCounter(String id, AdminCounterDTO dto) throws ExecutionException, InterruptedException {
        Counter counter = getCounterById(id);
        String previousTellerId = counter.getAssignedTellerId();
        applyCounterFields(counter, dto, false);
        counterRepository.save(counter);
        syncTellerAssignment(counter, previousTellerId);
        return counter;
    }

    public void deleteCounter(String id) throws ExecutionException, InterruptedException {
        String normalizedId = normalizeRequired(id, "counterId");
        counterRepository.delete(normalizedId);
    }

    private void applyCounterFields(Counter counter, AdminCounterDTO dto, boolean create)
            throws ExecutionException, InterruptedException {
        if (dto == null) {
            throw new IllegalArgumentException("Counter details are required");
        }

        String name = normalizeRequired(dto.getName(), "name");
        String serviceType = normalizeRequired(dto.getServiceType(), "serviceType");
        String status = normalizeOptional(dto.getStatus());
        if (status == null) {
            status = create ? Counter.STATUS_OPEN : counter.getStatus();
        }
        status = status.toUpperCase();
        if (!Counter.isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid counter status: " + status);
        }

        counter.setName(name);
        counter.setServiceType(serviceType);
        counter.setStatus(status);
        applyAssignedTeller(counter, dto.getAssignedTellerId());
        counter.setUpdatedAt(new Date());
        if (counter.getCreatedAt() == null) {
            counter.setCreatedAt(new Date());
        }
    }

    private void applyAssignedTeller(Counter counter, String assignedTellerId)
            throws ExecutionException, InterruptedException {
        String normalizedTellerId = normalizeOptional(assignedTellerId);
        if (normalizedTellerId == null) {
            counter.setAssignedTellerId(null);
            counter.setAssignedTellerName(null);
            return;
        }

        User teller = userRepository.findByUid(normalizedTellerId);
        if (teller == null) {
            throw new IllegalArgumentException("Assigned teller not found: " + normalizedTellerId);
        }
        if (!Role.TELLER.equals(teller.getRole())) {
            throw new IllegalArgumentException("Assigned user must have TELLER role");
        }

        counter.setAssignedTellerId(teller.getUid());
        counter.setAssignedTellerName(fullName(teller));
    }

    private void syncTellerAssignment(Counter counter, String previousTellerId)
            throws ExecutionException, InterruptedException {
        String assignedTellerId = counter.getAssignedTellerId();
        if (previousTellerId != null && !previousTellerId.equals(assignedTellerId)) {
            User previousTeller = userRepository.findByUid(previousTellerId);
            if (previousTeller != null && counter.getId().equals(previousTeller.getCounterId())) {
                previousTeller.setCounterId(null);
                previousTeller.setCounterName(null);
                userRepository.save(previousTeller);
            }
        }

        if (assignedTellerId == null) {
            return;
        }

        User assignedTeller = userRepository.findByUid(assignedTellerId);
        if (assignedTeller != null) {
            assignedTeller.setCounterId(counter.getId());
            assignedTeller.setCounterName(counter.getName());
            userRepository.save(assignedTeller);
        }
    }

    private String fullName(User user) {
        String firstname = normalizeOptional(user.getFirstname());
        String lastname = normalizeOptional(user.getLastname());
        if (firstname == null && lastname == null) {
            return user.getEmail();
        }
        if (firstname == null) {
            return lastname;
        }
        if (lastname == null) {
            return firstname;
        }
        return firstname + " " + lastname;
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
