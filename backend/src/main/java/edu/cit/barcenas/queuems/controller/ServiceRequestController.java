package edu.cit.barcenas.queuems.controller;

import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.service.ServiceRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();
            String counterId = body.get("counterId");
            
            if (counterId == null || counterId.isEmpty()) {
                return ResponseEntity.badRequest().body("counterId is required");
            }

            ServiceRequest request = service.createRequest(uid, counterId);
            return ResponseEntity.status(201).body(request);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
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

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelRequest(@PathVariable String id, Authentication authentication) {
        try {
            String uid = (String) authentication.getPrincipal();
            service.cancelRequest(id, uid);
            return ResponseEntity.ok("Request cancelled successfully");
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
