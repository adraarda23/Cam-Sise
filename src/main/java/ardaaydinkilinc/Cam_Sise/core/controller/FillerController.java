package ardaaydinkilinc.Cam_Sise.core.controller;

import ardaaydinkilinc.Cam_Sise.core.service.FillerService;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Filler management.
 * ADMIN and COMPANY_STAFF can manage fillers.
 */
@RestController
@RequestMapping("/api/fillers")
@RequiredArgsConstructor
public class FillerController {

    private final FillerService fillerService;

    /**
     * Register a new filler
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Filler> registerFiller(@RequestBody RegisterFillerRequest request) {
        Filler filler = fillerService.registerFiller(
                request.poolOperatorId,
                request.name,
                request.street,
                request.city,
                request.province,
                request.postalCode,
                request.country,
                request.latitude,
                request.longitude,
                request.contactPhone,
                request.contactEmail,
                request.contactPersonName,
                request.taxId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(filler);
    }

    /**
     * Get filler by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<Filler> getFiller(@PathVariable Long id) {
        Filler filler = fillerService.findById(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Get all fillers (with optional filters)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<List<Filler>> getAllFillers(
            @RequestParam(required = false) Long poolOperatorId,
            @RequestParam(required = false) Boolean active
    ) {
        List<Filler> fillers;
        if (poolOperatorId != null) {
            fillers = fillerService.findByPoolOperator(poolOperatorId, active);
        } else if (active != null && active) {
            fillers = fillerService.findAllActive();
        } else {
            fillers = fillerService.findAll();
        }
        return ResponseEntity.ok(fillers);
    }

    /**
     * Activate a filler
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Filler> activateFiller(@PathVariable Long id) {
        Filler filler = fillerService.activateFiller(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Deactivate a filler
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Filler> deactivateFiller(@PathVariable Long id) {
        Filler filler = fillerService.deactivateFiller(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Update filler contact information
     */
    @PutMapping("/{id}/contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Filler> updateContactInfo(
            @PathVariable Long id,
            @RequestBody UpdateContactInfoRequest request
    ) {
        Filler filler = fillerService.updateContactInfo(
                id,
                request.contactPhone,
                request.contactEmail,
                request.contactPersonName
        );
        return ResponseEntity.ok(filler);
    }

    /**
     * Update filler location
     */
    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<Filler> updateLocation(
            @PathVariable Long id,
            @RequestBody UpdateLocationRequest request
    ) {
        Filler filler = fillerService.updateLocation(id, request.latitude, request.longitude);
        return ResponseEntity.ok(filler);
    }

    // ===== DTOs =====

    public record RegisterFillerRequest(
            Long poolOperatorId,
            String name,
            String street,
            String city,
            String province,
            String postalCode,
            String country,
            Double latitude,
            Double longitude,
            String contactPhone,
            String contactEmail,
            String contactPersonName,
            String taxId
    ) {}

    public record UpdateContactInfoRequest(
            String contactPhone,
            String contactEmail,
            String contactPersonName
    ) {}

    public record UpdateLocationRequest(
            Double latitude,
            Double longitude
    ) {}
}
