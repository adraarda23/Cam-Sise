package ardaaydinkilinc.Cam_Sise.shared.domain.base;

import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all aggregate roots.
 * Aggregate roots are the entry point to the aggregate and are responsible for maintaining invariants.
 * They collect domain events that will be published after the aggregate is persisted.
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot() {
        super();
    }

    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Register a domain event to be published after persistence
     */
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Get all domain events and clear the list
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events (usually called after publishing)
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
