package edu.cit.barcenas.queuems.controller.users;

import edu.cit.barcenas.queuems.dto.CreateServiceRequestDTO;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.service.ServiceRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/requests")
public class UserServiceRequestController {

    private final ServiceRequestService service;

    public UserServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createRequest(@RequestBody CreateServiceRequestDTO body, Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();

            if (body == null || body.getCounterId() == null || body.getCounterId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("counterId is required");
            }

            ServiceRequest request = service.createRequest(
                    uid,
                    body.getCounterId(),
                    body.getServiceType(),
                    body.getNotes());
            return ResponseEntity.status(201).body(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRequests(Authentication authentication) {
        return getMyRequests(authentication);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyRequests(Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();
            List<ServiceRequest> requests = service.getUserRequests(uid);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRequest(@PathVariable String id, Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();
            ServiceRequest request = service.getUserRequestById(id, uid);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("only view your own")) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRequest(@PathVariable String id, Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();
            ServiceRequest request = service.cancelRequest(id, uid);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("only cancel your own")) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
