package edu.cit.barcenas.queuems.feature.admin;

import edu.cit.barcenas.queuems.model.Counter;
import edu.cit.barcenas.queuems.model.ServiceRequest;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.feature.counters.CounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CounterService counterService;

    public AdminController(AdminService adminService, CounterService counterService) {
        this.adminService = adminService;
        this.counterService = counterService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/tellers")
    public ResponseEntity<?> getTellers() {
        try {
            List<User> tellers = adminService.getTellers();
            return ResponseEntity.ok(tellers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createStaffUser(@RequestBody AdminStaffUserDTO body) {
        try {
            User user = adminService.createStaffUser(body);
            return ResponseEntity.status(201).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody AdminStaffUserDTO body) {
        try {
            User user = adminService.updateUser(id, body);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        try {
            List<ServiceRequest> requests = adminService.getAllRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/counters")
    public ResponseEntity<?> getCounters() {
        try {
            List<Counter> counters = counterService.getAllCounters();
            return ResponseEntity.ok(counters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/counters")
    public ResponseEntity<?> createCounter(@RequestBody AdminCounterDTO body) {
        try {
            Counter counter = counterService.createCounter(body);
            return ResponseEntity.status(201).body(counter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/counters/{id}")
    public ResponseEntity<?> updateCounter(@PathVariable String id, @RequestBody AdminCounterDTO body) {
        try {
            Counter counter = counterService.updateCounter(id, body);
            return ResponseEntity.ok(counter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/counters/{id}")
    public ResponseEntity<?> deleteCounter(@PathVariable String id) {
        try {
            counterService.deleteCounter(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
