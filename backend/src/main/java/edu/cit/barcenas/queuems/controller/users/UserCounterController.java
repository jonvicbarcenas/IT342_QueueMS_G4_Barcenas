package edu.cit.barcenas.queuems.controller.users;

import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.service.CounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/counters")
public class UserCounterController {

    private final CounterService counterService;

    public UserCounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOpenCounters() {
        try {
            List<Counter> counters = counterService.getOpenCounters();
            return ResponseEntity.ok(counters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
