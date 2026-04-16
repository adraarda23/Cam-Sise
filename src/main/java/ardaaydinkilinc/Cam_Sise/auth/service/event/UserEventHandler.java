package ardaaydinkilinc.Cam_Sise.auth.service.event;

import ardaaydinkilinc.Cam_Sise.auth.domain.event.UserRegistered;
import ardaaydinkilinc.Cam_Sise.auth.domain.event.UserRoleChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for User domain events.
 * Handles side effects like sending emails, logging, notifications, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventHandler {

    /**
     * Handle UserRegistered event
     * In production: send welcome email, create initial settings, etc.
     */
    @EventListener
    @Async
    public void handleUserRegistered(UserRegistered event) {
        log.info("👤 User registered: username={}, role={}, poolOperatorId={}",
                event.username(),
                event.role(),
                event.poolOperatorId());

        // TODO: Send welcome email
        // TODO: Create user profile
        // TODO: Initialize user settings
        // TODO: Send notification to admin
    }

    /**
     * Handle UserRoleChanged event
     * In production: send notification, update permissions cache, etc.
     */
    @EventListener
    @Async
    public void handleUserRoleChanged(UserRoleChanged event) {
        log.info("🔄 User role changed: userId={}, oldRole={}, newRole={}",
                event.userId(),
                event.oldRole(),
                event.newRole());

        // TODO: Send role change notification email
        // TODO: Clear permission cache
        // TODO: Log security audit trail
        // TODO: Update user access tokens
    }
}
