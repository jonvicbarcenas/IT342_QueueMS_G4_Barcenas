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

    public User updateUser(String uid, AdminStaffUserDTO dto) throws ExecutionException, InterruptedException {
        if (dto == null) {
            throw new IllegalArgumentException("User details are required");
        }

        User user = userRepository.findByUid(normalizeRequired(uid, "uid"));
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + uid);
        }

        String email = normalizeOptional(dto.getEmail());
        if (email != null) {
            email = email.toLowerCase();
            User existingUser = userRepository.findByEmail(email);
            if (existingUser != null && !existingUser.getUid().equals(user.getUid())) {
                throw new IllegalArgumentException("User with this email already exists");
            }
            user.setEmail(email);
        }

        String firstname = normalizeOptional(dto.getFirstname());
        if (firstname != null) {
            user.setFirstname(firstname);
        }

        String lastname = normalizeOptional(dto.getLastname());
        if (lastname != null) {
            user.setLastname(lastname);
        }

        String password = normalizeOptional(dto.getPassword());
        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }

        String role = normalizeOptional(dto.getRole());
        if (role != null) {
            role = role.toUpperCase();
            if (!Role.USER.equals(role) && !Role.TELLER.equals(role) && !Role.SUPERADMIN.equals(role)) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
            user.setRole(role);
        }

        String previousCounterId = user.getCounterId();
        String counterId = normalizeOptional(dto.getCounterId());
        if (Role.TELLER.equals(user.getRole()) && counterId != null) {
            Counter assignedCounter = counterRepository.findById(counterId);
            if (assignedCounter == null) {
                throw new IllegalArgumentException("Assigned counter not found: " + counterId);
            }
            user.setCounterId(assignedCounter.getId());
            user.setCounterName(assignedCounter.getName());
        } else if (!Role.TELLER.equals(user.getRole()) || dto.getCounterId() != null) {
            user.setCounterId(null);
            user.setCounterName(null);
        }

        userRepository.save(user);
        syncUserCounterAssignment(user, previousCounterId);
        return user;
    }

    public void deleteUser(String uid) throws ExecutionException, InterruptedException {
        String normalizedUid = normalizeRequired(uid, "uid");
        User user = userRepository.findByUid(normalizedUid);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + normalizedUid);
        }

        clearCounterAssignmentsForUser(normalizedUid);
        userRepository.delete(normalizedUid);
    }

    public List<ServiceRequest> getAllRequests() throws ExecutionException, InterruptedException {
        return serviceRequestRepository.findAll();
    }

    private void syncUserCounterAssignment(User user, String previousCounterId)
            throws ExecutionException, InterruptedException {
        if (previousCounterId != null && !previousCounterId.equals(user.getCounterId())) {
            Counter previousCounter = counterRepository.findById(previousCounterId);
            if (previousCounter != null && user.getUid().equals(previousCounter.getAssignedTellerId())) {
                previousCounter.setAssignedTellerId(null);
                previousCounter.setAssignedTellerName(null);
                counterRepository.save(previousCounter);
            }
        }

        if (!Role.TELLER.equals(user.getRole()) || user.getCounterId() == null) {
            clearCounterAssignmentsForUser(user.getUid());
            return;
        }

        Counter counter = counterRepository.findById(user.getCounterId());
        if (counter == null) {
            return;
        }

        String previousTellerId = counter.getAssignedTellerId();
        if (previousTellerId != null && !previousTellerId.equals(user.getUid())) {
            User previousTeller = userRepository.findByUid(previousTellerId);
            if (previousTeller != null && counter.getId().equals(previousTeller.getCounterId())) {
                previousTeller.setCounterId(null);
                previousTeller.setCounterName(null);
                userRepository.save(previousTeller);
            }
        }

        counter.setAssignedTellerId(user.getUid());
        counter.setAssignedTellerName(fullName(user));
        counterRepository.save(counter);
    }

    private void clearCounterAssignmentsForUser(String uid) throws ExecutionException, InterruptedException {
        for (Counter counter : counterRepository.findAll()) {
            if (uid.equals(counter.getAssignedTellerId())) {
                counter.setAssignedTellerId(null);
                counter.setAssignedTellerName(null);
                counterRepository.save(counter);
            }
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
