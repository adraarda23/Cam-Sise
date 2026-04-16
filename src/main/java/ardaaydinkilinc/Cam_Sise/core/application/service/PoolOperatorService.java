package ardaaydinkilinc.Cam_Sise.core.application.service;

import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for PoolOperator aggregate.
 * Coordinates domain operations and publishes events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PoolOperatorService {

    private final PoolOperatorRepository poolOperatorRepository;

    /**
     * Register a new pool operator.
     * This method creates a pool operator and automatically publishes PoolOperatorRegistered event.
     */
    public PoolOperator registerPoolOperator(
            String companyName,
            String taxIdValue,
            String phone,
            String email,
            String contactPersonName
    ) {
        log.info("Registering new pool operator: companyName={}, taxId={}", companyName, taxIdValue);

        // Check if tax ID already exists
        if (poolOperatorRepository.existsByTaxId_Value(taxIdValue)) {
            throw new IllegalArgumentException("Tax ID already exists: " + taxIdValue);
        }

        // Create value objects
        TaxId taxId = new TaxId(taxIdValue);
        ContactInfo contactInfo = new ContactInfo(phone, email, contactPersonName);

        // Create pool operator (event is added to aggregate)
        PoolOperator poolOperator = PoolOperator.register(companyName, taxId, contactInfo);

        // Save pool operator (JPA listener will automatically publish events)
        poolOperator = poolOperatorRepository.save(poolOperator);

        log.info("Pool operator registered successfully: id={}, companyName={}", poolOperator.getId(), companyName);

        return poolOperator;
    }

    /**
     * Activate a pool operator.
     * This method activates a pool operator and automatically publishes PoolOperatorActivated event.
     */
    public PoolOperator activatePoolOperator(Long poolOperatorId) {
        log.info("Activating pool operator: id={}", poolOperatorId);

        PoolOperator poolOperator = poolOperatorRepository.findById(poolOperatorId)
                .orElseThrow(() -> new IllegalArgumentException("Pool operator not found: " + poolOperatorId));

        // Activate (event is added to aggregate)
        poolOperator.activate();

        // Save (JPA listener will automatically publish events)
        poolOperator = poolOperatorRepository.save(poolOperator);

        log.info("Pool operator activated successfully: id={}", poolOperatorId);

        return poolOperator;
    }

    /**
     * Deactivate a pool operator.
     * This method deactivates a pool operator and automatically publishes PoolOperatorDeactivated event.
     */
    public PoolOperator deactivatePoolOperator(Long poolOperatorId) {
        log.info("Deactivating pool operator: id={}", poolOperatorId);

        PoolOperator poolOperator = poolOperatorRepository.findById(poolOperatorId)
                .orElseThrow(() -> new IllegalArgumentException("Pool operator not found: " + poolOperatorId));

        // Deactivate (event is added to aggregate)
        poolOperator.deactivate();

        // Save (JPA listener will automatically publish events)
        poolOperator = poolOperatorRepository.save(poolOperator);

        log.info("Pool operator deactivated successfully: id={}", poolOperatorId);

        return poolOperator;
    }

    /**
     * Update pool operator contact information.
     */
    public PoolOperator updateContactInfo(
            Long poolOperatorId,
            String phone,
            String email,
            String contactPersonName
    ) {
        log.info("Updating pool operator contact info: id={}", poolOperatorId);

        PoolOperator poolOperator = poolOperatorRepository.findById(poolOperatorId)
                .orElseThrow(() -> new IllegalArgumentException("Pool operator not found: " + poolOperatorId));

        ContactInfo newContactInfo = new ContactInfo(phone, email, contactPersonName);
        poolOperator.updateContactInfo(newContactInfo);

        poolOperator = poolOperatorRepository.save(poolOperator);

        log.info("Pool operator contact info updated successfully: id={}", poolOperatorId);

        return poolOperator;
    }

    /**
     * Find pool operator by ID.
     */
    @Transactional(readOnly = true)
    public PoolOperator findById(Long poolOperatorId) {
        return poolOperatorRepository.findById(poolOperatorId)
                .orElseThrow(() -> new IllegalArgumentException("Pool operator not found: " + poolOperatorId));
    }

    /**
     * Find pool operator by tax ID.
     */
    @Transactional(readOnly = true)
    public PoolOperator findByTaxId(String taxId) {
        return poolOperatorRepository.findByTaxId_Value(taxId)
                .orElseThrow(() -> new IllegalArgumentException("Pool operator not found with tax ID: " + taxId));
    }

    /**
     * Find all active pool operators.
     */
    @Transactional(readOnly = true)
    public List<PoolOperator> findAllActive() {
        return poolOperatorRepository.findByActive(true);
    }

    /**
     * Find all pool operators.
     */
    @Transactional(readOnly = true)
    public List<PoolOperator> findAll() {
        return poolOperatorRepository.findAll();
    }
}
