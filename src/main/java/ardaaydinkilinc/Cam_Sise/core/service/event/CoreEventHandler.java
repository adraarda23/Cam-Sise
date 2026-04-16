package ardaaydinkilinc.Cam_Sise.core.service.event;

import ardaaydinkilinc.Cam_Sise.core.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for Core module domain events (PoolOperator, Filler).
 * Handles side effects like sending emails, logging, notifications, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreEventHandler {

    // ===== PoolOperator Events =====

    /**
     * Handle PoolOperatorRegistered event
     */
    @EventListener
    @Async
    public void handlePoolOperatorRegistered(PoolOperatorRegistered event) {
        log.info("🏢 Pool Operator registered: companyName={}, taxId={}",
                event.companyName(),
                event.taxId());

        // TODO: Send welcome email
        // TODO: Create initial settings
        // TODO: Notify admins
    }

    /**
     * Handle PoolOperatorActivated event
     */
    @EventListener
    @Async
    public void handlePoolOperatorActivated(PoolOperatorActivated event) {
        log.info("✅ Pool Operator activated: id={}, companyName={}",
                event.operatorId(),
                event.companyName());

        // TODO: Send activation notification
        // TODO: Enable features
    }

    /**
     * Handle PoolOperatorDeactivated event
     */
    @EventListener
    @Async
    public void handlePoolOperatorDeactivated(PoolOperatorDeactivated event) {
        log.info("❌ Pool Operator deactivated: id={}, companyName={}",
                event.operatorId(),
                event.companyName());

        // TODO: Send deactivation notification
        // TODO: Disable features
        // TODO: Archive data
    }

    // ===== Filler Events =====

    /**
     * Handle FillerRegistered event
     */
    @EventListener
    @Async
    public void handleFillerRegistered(FillerRegistered event) {
        log.info("🏭 Filler registered: poolOperatorId={}, name={}, location=({}, {})",
                event.poolOperatorId(),
                event.fillerName(),
                event.location().latitude(),
                event.location().longitude());

        // TODO: Send welcome email to filler
        // TODO: Initialize filler stock (will be done by InventoryEventHandler)
        // TODO: Notify pool operator
    }

    /**
     * Handle FillerActivated event
     */
    @EventListener
    @Async
    public void handleFillerActivated(FillerActivated event) {
        log.info("✅ Filler activated: id={}, poolOperatorId={}, name={}",
                event.fillerId(),
                event.poolOperatorId(),
                event.fillerName());

        // TODO: Send activation notification
        // TODO: Re-enable collection requests
    }

    /**
     * Handle FillerDeactivated event
     */
    @EventListener
    @Async
    public void handleFillerDeactivated(FillerDeactivated event) {
        log.info("❌ Filler deactivated: id={}, poolOperatorId={}, name={}",
                event.fillerId(),
                event.poolOperatorId(),
                event.fillerName());

        // TODO: Send deactivation notification
        // TODO: Cancel pending collection requests
        // TODO: Archive stock data
    }
}
