package edu.cit.barcenas.queuems.controller.teller;

import edu.cit.barcenas.queuems.dto.teller.TellerCounterStatusDTO;
import edu.cit.barcenas.queuems.dto.teller.TellerRequestStatusDTO;
import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.service.TellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/teller")
public class TellerController {

    private final TellerService tellerService;

    public TellerController(TellerService tellerService) {
        this.tellerService = tellerService;
    }

    @GetMapping("/counter")
    public ResponseEntity<?> getAssignedCounter(Authentication authentication) {
        try {
            Counter counter = tellerService.getAssignedCounter((String) authentication.getPrincipal());
            return ResponseEntity.ok(counter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/counter/status")
    public ResponseEntity<?> updateCounterStatus(
            @RequestBody TellerCounterStatusDTO body,
            Authentication authentication) {
        try {
            Counter counter = tellerService.updateCounterStatus((String) authentication.getPrincipal(), body.getStatus());
            return ResponseEntity.ok(counter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAssignedRequests(Authentication authentication) {
        try {
            List<ServiceRequest> requests = tellerService.getAssignedRequests((String) authentication.getPrincipal());
            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/requests/next/serve")
    public ResponseEntity<?> serveNextRequest(Authentication authentication) {
        try {
            ServiceRequest request = tellerService.serveNextRequest((String) authentication.getPrincipal());
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/requests/{id}/serve")
    public ResponseEntity<?> serveRequest(@PathVariable String id, Authentication authentication) {
        try {
            ServiceRequest request = tellerService.serveRequest((String) authentication.getPrincipal(), id);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/requests/{id}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable String id,
            @RequestBody TellerRequestStatusDTO body,
            Authentication authentication) {
        try {
            ServiceRequest request = tellerService.updateRequestStatus(
                    (String) authentication.getPrincipal(),
                    id,
                    body.getStatus());
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
