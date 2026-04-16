package ardaaydinkilinc.Cam_Sise.core.service;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for Filler aggregate.
 * Coordinates domain operations and publishes events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FillerService {

    private final FillerRepository fillerRepository;

    /**
     * Register a new filler.
     * This method creates a filler and automatically publishes FillerRegistered event.
     */
    public Filler registerFiller(
            Long poolOperatorId,
            String name,
            String street,
            String city,
            String province,
            String postalCode,
            String country,
            Double latitude,
            Double longitude,
            String phone,
            String email,
            String contactPersonName,
            String taxIdValue
    ) {
        log.info("Registering new filler: name={}, poolOperatorId={}", name, poolOperatorId);

        // Check if tax ID already exists (if provided)
        if (taxIdValue != null && fillerRepository.existsByTaxId_Value(taxIdValue)) {
            throw new IllegalArgumentException("Tax ID already exists: " + taxIdValue);
        }

        // Create value objects
        Address address = new Address(street, city, province, postalCode, country);
        GeoCoordinates location = new GeoCoordinates(latitude, longitude);
        ContactInfo contactInfo = new ContactInfo(phone, email, contactPersonName);
        TaxId taxId = taxIdValue != null ? new TaxId(taxIdValue) : null;

        // Create filler (event is added to aggregate)
        Filler filler = Filler.register(poolOperatorId, name, address, location, contactInfo, taxId);

        // Save filler (JPA listener will automatically publish events)
        filler = fillerRepository.save(filler);

        log.info("Filler registered successfully: id={}, name={}", filler.getId(), name);

        return filler;
    }

    /**
     * Activate a filler.
     */
    public Filler activateFiller(Long fillerId) {
        log.info("Activating filler: id={}", fillerId);

        Filler filler = fillerRepository.findById(fillerId)
                .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));

        filler.activate();
        filler = fillerRepository.save(filler);

        log.info("Filler activated successfully: id={}", fillerId);

        return filler;
    }

    /**
     * Deactivate a filler.
     */
    public Filler deactivateFiller(Long fillerId) {
        log.info("Deactivating filler: id={}", fillerId);

        Filler filler = fillerRepository.findById(fillerId)
                .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));

        filler.deactivate();
        filler = fillerRepository.save(filler);

        log.info("Filler deactivated successfully: id={}", fillerId);

        return filler;
    }

    /**
     * Update filler contact information.
     */
    public Filler updateContactInfo(
            Long fillerId,
            String phone,
            String email,
            String contactPersonName
    ) {
        log.info("Updating filler contact info: id={}", fillerId);

        Filler filler = fillerRepository.findById(fillerId)
                .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));

        ContactInfo newContactInfo = new ContactInfo(phone, email, contactPersonName);
        filler.updateContactInfo(newContactInfo);

        filler = fillerRepository.save(filler);

        log.info("Filler contact info updated successfully: id={}", fillerId);

        return filler;
    }

    /**
     * Update filler location.
     */
    public Filler updateLocation(Long fillerId, Double latitude, Double longitude) {
        log.info("Updating filler location: id={}", fillerId);

        Filler filler = fillerRepository.findById(fillerId)
                .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));

        GeoCoordinates newLocation = new GeoCoordinates(latitude, longitude);
        filler.updateLocation(newLocation);

        filler = fillerRepository.save(filler);

        log.info("Filler location updated successfully: id={}", fillerId);

        return filler;
    }

    /**
     * Find filler by ID.
     */
    @Transactional(readOnly = true)
    public Filler findById(Long fillerId) {
        return fillerRepository.findById(fillerId)
                .orElseThrow(() -> new IllegalArgumentException("Filler not found: " + fillerId));
    }

    /**
     * Find all fillers for a pool operator.
     */
    @Transactional(readOnly = true)
    public List<Filler> findByPoolOperator(Long poolOperatorId, Boolean active) {
        if (active != null) {
            return fillerRepository.findByPoolOperatorIdAndActive(poolOperatorId, active);
        }
        return fillerRepository.findByPoolOperatorId(poolOperatorId);
    }

    /**
     * Find all active fillers.
     */
    @Transactional(readOnly = true)
    public List<Filler> findAllActive() {
        return fillerRepository.findByActive(true);
    }

    /**
     * Find all fillers.
     */
    @Transactional(readOnly = true)
    public List<Filler> findAll() {
        return fillerRepository.findAll();
    }
}
