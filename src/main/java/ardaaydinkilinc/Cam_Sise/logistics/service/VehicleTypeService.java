package ardaaydinkilinc.Cam_Sise.logistics.service;

import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for VehicleType aggregate.
 * Manages vehicle type catalog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;

    /**
     * Create a new vehicle type.
     */
    public VehicleType createVehicleType(
            Long poolOperatorId,
            String name,
            String description,
            int palletCapacity,
            int separatorCapacity
    ) {
        log.info("Creating vehicle type: name={}, poolOperatorId={}", name, poolOperatorId);

        Capacity capacity = new Capacity(palletCapacity, separatorCapacity);
        VehicleType vehicleType = VehicleType.create(poolOperatorId, name, description, capacity);
        vehicleType = vehicleTypeRepository.save(vehicleType);

        log.info("Vehicle type created successfully: id={}, name={}", vehicleType.getId(), name);

        return vehicleType;
    }

    /**
     * Update vehicle type capacity.
     */
    public VehicleType updateCapacity(Long vehicleTypeId, int palletCapacity, int separatorCapacity) {
        log.info("Updating vehicle type capacity: vehicleTypeId={}", vehicleTypeId);

        VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + vehicleTypeId));

        Capacity newCapacity = new Capacity(palletCapacity, separatorCapacity);
        vehicleType.updateCapacity(newCapacity);
        vehicleType = vehicleTypeRepository.save(vehicleType);

        log.info("Vehicle type capacity updated: vehicleTypeId={}", vehicleTypeId);

        return vehicleType;
    }

    /**
     * Deactivate a vehicle type.
     */
    public VehicleType deactivateVehicleType(Long vehicleTypeId) {
        log.info("Deactivating vehicle type: vehicleTypeId={}", vehicleTypeId);

        VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + vehicleTypeId));

        vehicleType.deactivate();
        vehicleType = vehicleTypeRepository.save(vehicleType);

        log.info("Vehicle type deactivated: vehicleTypeId={}", vehicleTypeId);

        return vehicleType;
    }

    /**
     * Find vehicle type by ID.
     */
    @Transactional(readOnly = true)
    public VehicleType findById(Long vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle type not found: " + vehicleTypeId));
    }

    /**
     * Find all vehicle types for a pool operator.
     */
    @Transactional(readOnly = true)
    public List<VehicleType> findByPoolOperator(Long poolOperatorId, Boolean active) {
        if (active != null) {
            return vehicleTypeRepository.findByPoolOperatorIdAndActive(poolOperatorId, active);
        }
        return vehicleTypeRepository.findByPoolOperatorId(poolOperatorId);
    }

    /**
     * Find all active vehicle types.
     */
    @Transactional(readOnly = true)
    public List<VehicleType> findAllActive() {
        return vehicleTypeRepository.findByActive(true);
    }

    /**
     * Find all vehicle types.
     */
    @Transactional(readOnly = true)
    public List<VehicleType> findAll() {
        return vehicleTypeRepository.findAll();
    }
}
