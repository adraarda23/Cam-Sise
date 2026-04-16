package ardaaydinkilinc.Cam_Sise.auth.api;

import ardaaydinkilinc.Cam_Sise.auth.application.service.UserService;
import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventStore;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller to demonstrate event flow.
 * This controller should be removed or secured in production.
 */
@RestController
@RequestMapping("/api/test/events")
@RequiredArgsConstructor
public class TestEventController {

    private final UserService userService;
    private final DomainEventStoreRepository eventStoreRepository;

    /**
     * Test endpoint to create a user and trigger events
     */
    @PostMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        // Create a test user
        User user = userService.registerUser(
                1L,
                "test-user-" + System.currentTimeMillis(),
                "password123",
                "Test User",
                Role.COMPANY_STAFF,
                null
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("info", "Check logs for event publication and database for stored events");

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to change user role and trigger events
     */
    @PostMapping("/change-role/{userId}")
    public ResponseEntity<Map<String, Object>> changeUserRole(
            @PathVariable Long userId,
            @RequestParam Role newRole
    ) {
        User user = userService.changeUserRole(userId, newRole);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User role changed successfully");
        response.put("userId", user.getId());
        response.put("newRole", user.getRole());
        response.put("info", "Check logs for event publication and database for stored events");

        return ResponseEntity.ok(response);
    }

    /**
     * Get all stored events from the event store
     */
    @GetMapping("/stored-events")
    public ResponseEntity<List<DomainEventStore>> getStoredEvents() {
        List<DomainEventStore> events = eventStoreRepository.findAll();
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by type
     */
    @GetMapping("/stored-events/type/{eventType}")
    public ResponseEntity<List<DomainEventStore>> getEventsByType(@PathVariable String eventType) {
        List<DomainEventStore> events = eventStoreRepository.findByEventType(eventType);
        return ResponseEntity.ok(events);
    }
}
