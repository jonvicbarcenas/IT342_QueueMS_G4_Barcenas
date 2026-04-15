package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.dto.admin.AdminStaffUserDTO;
import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.Role;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.CounterRepository;
import edu.cit.barcenas.queuems.repository.ServiceRequestRepository;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CounterRepository counterRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            CounterRepository counterRepository,
            ServiceRequestRepository serviceRequestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.counterRepository = counterRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() throws ExecutionException, InterruptedException {
        return userRepository.findAll();
    }

    public List<User> getTellers() throws ExecutionException, InterruptedException {
        return userRepository.findByRole(Role.TELLER);
    }

    public User createStaffUser(AdminStaffUserDTO dto) throws ExecutionException, InterruptedException {
        if (dto == null) {
            throw new IllegalArgumentException("Staff user details are required");
        }

        String email = normalizeRequired(dto.getEmail(), "email").toLowerCase();
        String password = normalizeRequired(dto.getPassword(), "password");
        String firstname = normalizeRequired(dto.getFirstname(), "firstname");
        String lastname = normalizeRequired(dto.getLastname(), "lastname");
        String role = normalizeRequired(dto.getRole(), "role").toUpperCase();

        if (!Role.TELLER.equals(role) && !Role.SUPERADMIN.equals(role)) {
            throw new IllegalArgumentException("Admin can only create TELLER or SUPERADMIN accounts");
        }

        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        Counter assignedCounter = null;
        String counterId = normalizeOptional(dto.getCounterId());
        if (Role.TELLER.equals(role)) {
            if (counterId != null) {
                assignedCounter = counterRepository.findById(counterId);
                if (assignedCounter == null) {
                    throw new IllegalArgumentException("Assigned counter not found: " + counterId);
                }
            }
        } else {
            counterId = null;
        }

        String uid = UUID.randomUUID().toString();
        User user = new User(
                uid,
                email,
                passwordEncoder.encode(password),
                firstname,
                lastname,
                role,
                counterId,
                assignedCounter != null ? assignedCounter.getName() : null);
        userRepository.save(user);

        if (assignedCounter != null) {
            String previousTellerId = assignedCounter.getAssignedTellerId();
            if (previousTellerId != null && !previousTellerId.equals(user.getUid())) {
                User previousTeller = userRepository.findByUid(previousTellerId);
                if (previousTeller != null && assignedCounter.getId().equals(previousTeller.getCounterId())) {
                    previousTeller.setCounterId(null);
                    previousTeller.setCounterName(null);
                    userRepository.save(previousTeller);
                }
            }
            assignedCounter.setAssignedTellerId(user.getUid());
            assignedCounter.setAssignedTellerName(firstname + " " + lastname);
            counterRepository.save(assignedCounter);
        }

        return user;
    }

    public List<ServiceRequest> getAllRequests() throws ExecutionException, InterruptedException {
        return serviceRequestRepository.findAll();
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
