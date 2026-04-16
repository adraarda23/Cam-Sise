package ardaaydinkilinc.Cam_Sise.core.api;

import ardaaydinkilinc.Cam_Sise.core.application.service.PoolOperatorService;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Pool Operator management.
 * Only ADMIN users can manage pool operators.
 */
@RestController
@RequestMapping("/api/pool-operators")
@RequiredArgsConstructor
public class PoolOperatorController {

    private final PoolOperatorService poolOperatorService;

    /**
     * Register a new pool operator
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PoolOperator> registerPoolOperator(@Valid @RequestBody RegisterPoolOperatorRequest request) {
        PoolOperator poolOperator = poolOperatorService.registerPoolOperator(
                request.companyName,
                request.taxId,
                request.contactPhone,
                request.contactEmail,
                request.contactPersonName
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(poolOperator);
    }

    /**
     * Get pool operator by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<PoolOperator> getPoolOperator(@PathVariable Long id) {
        PoolOperator poolOperator = poolOperatorService.findById(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Get all pool operators
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PoolOperator>> getAllPoolOperators(
            @RequestParam(required = false) Boolean active
    ) {
        List<PoolOperator> poolOperators = active != null && active
                ? poolOperatorService.findAllActive()
                : poolOperatorService.findAll();
        return ResponseEntity.ok(poolOperators);
    }

    /**
     * Activate a pool operator
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PoolOperator> activatePoolOperator(@PathVariable Long id) {
        PoolOperator poolOperator = poolOperatorService.activatePoolOperator(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Deactivate a pool operator
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PoolOperator> deactivatePoolOperator(@PathVariable Long id) {
        PoolOperator poolOperator = poolOperatorService.deactivatePoolOperator(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Update pool operator contact information
     */
    @PutMapping("/{id}/contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<PoolOperator> updateContactInfo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactInfoRequest request
    ) {
        PoolOperator poolOperator = poolOperatorService.updateContactInfo(
                id,
                request.contactPhone,
                request.contactEmail,
                request.contactPersonName
        );
        return ResponseEntity.ok(poolOperator);
    }

    // ===== DTOs =====

    public record RegisterPoolOperatorRequest(
            @NotBlank(message = "Company name is required")
            String companyName,

            @NotBlank(message = "Tax ID is required")
            String taxId,

            @NotBlank(message = "Contact phone is required")
            String contactPhone,

            @NotBlank(message = "Contact email is required")
            @Email(message = "Email must be valid")
            String contactEmail,

            @NotBlank(message = "Contact person name is required")
            String contactPersonName
    ) {}

    public record UpdateContactInfoRequest(
            @NotBlank(message = "Contact phone is required")
            String contactPhone,

            @NotBlank(message = "Contact email is required")
            @Email(message = "Email must be valid")
            String contactEmail,

            @NotBlank(message = "Contact person name is required")
            String contactPersonName
    ) {}
}
