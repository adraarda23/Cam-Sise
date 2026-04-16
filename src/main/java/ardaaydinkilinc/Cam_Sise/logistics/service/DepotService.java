package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for Depot aggregate.
 * Manages depots and their vehicles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepotService {

    private final DepotRepository depotRepository;

    /**
     * Create a new depot.
     */
    public Depot createDepot(
            Long poolOperatorId,
            String name,
            String street,
            String city,
            String province,
            String postalCode,
            String country,
            Double latitude,
            Double longitude
    ) {
        log.info("Creating depot: name={}, poolOperatorId={}", name, poolOperatorId);

        Address address = new Address(street, city, province, postalCode, country);
        GeoCoordinates location = new GeoCoordinates(latitude, longitude);

        Depot depot = Depot.create(poolOperatorId, name, address, location);
        depot = depotRepository.save(depot);

        log.info("Depot created successfully: id={}, name={}", depot.getId(), name);

        return depot;
    }

    /**
     * Add a vehicle to a depot.
     */
    public Depot addVehicle(Long depotId, Long vehicleId) {
        log.info("Adding vehicle to depot: depotId={}, vehicleId={}", depotId, vehicleId);

        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        depot.addVehicle(vehicleId);
        depot = depotRepository.save(depot);

        log.info("Vehicle added to depot successfully: depotId={}, vehicleId={}", depotId, vehicleId);

        return depot;
    }

    /**
     * Remove a vehicle from a depot.
     */
    public Depot removeVehicle(Long depotId, Long vehicleId) {
        log.info("Removing vehicle from depot: depotId={}, vehicleId={}", depotId, vehicleId);

        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));

        depot.removeVehicle(vehicleId);
        depot = depotRepository.save(depot);

        log.info("Vehicle removed from depot successfully: depotId={}, vehicleId={}", depotId, vehicleId);

        return depot;
    }

    /**
     * Find depot by ID.
     */
    @Transactional(readOnly = true)
    public Depot findById(Long depotId) {
        return depotRepository.findById(depotId)
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + depotId));
    }

    /**
     * Find all depots for a pool operator.
     */
    @Transactional(readOnly = true)
    public List<Depot> findByPoolOperator(Long poolOperatorId, Boolean active) {
        if (active != null) {
            return depotRepository.findByPoolOperatorIdAndActive(poolOperatorId, active);
        }
        return depotRepository.findByPoolOperatorId(poolOperatorId);
    }

    /**
     * Find all active depots.
     */
    @Transactional(readOnly = true)
    public List<Depot> findAllActive() {
        return depotRepository.findByActive(true);
    }

    /**
     * Find all depots.
     */
    @Transactional(readOnly = true)
    public List<Depot> findAll() {
        return depotRepository.findAll();
    }
}
