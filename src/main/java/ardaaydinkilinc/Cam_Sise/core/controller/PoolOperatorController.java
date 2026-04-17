package ardaaydinkilinc.Cam_Sise.core.controller;

import ardaaydinkilinc.Cam_Sise.core.service.PoolOperatorService;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Pool Operator management.
 * Only ADMIN users can create/manage pool operators.
 * COMPANY_STAFF can view pool operator information.
 */
@RestController
@RequestMapping("/api/pool-operators")
@RequiredArgsConstructor
@Tag(name = "Core - Pool Operators", description = "Havuz operatörü yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class PoolOperatorController {

    private final PoolOperatorService poolOperatorService;

    /**
     * Register a new pool operator
     */
    @Operation(
            summary = "Yeni havuz operatörü kaydı",
            description = "Sisteme yeni bir havuz operatörü kaydeder. Sadece ADMIN yetkisi gerektirir."
    )
    @ApiResponse(responseCode = "201", description = "Havuz operatörü başarıyla kaydedildi")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
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
    @Operation(
            summary = "Havuz operatörü bilgisi getir",
            description = "ID'ye göre havuz operatörü bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Havuz operatörü başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Havuz operatörü bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<PoolOperator> getPoolOperator(
            @Parameter(description = "Havuz operatörü ID", example = "1") @PathVariable Long id
    ) {
        PoolOperator poolOperator = poolOperatorService.findById(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Get all pool operators
     */
    @Operation(
            summary = "Tüm havuz operatörlerini listele",
            description = "Sistemdeki tüm havuz operatörlerini listeler. Aktiflik durumuna göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Havuz operatörü listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PoolOperator>> getAllPoolOperators(
            @Parameter(description = "Aktiflik durumuna göre filtrele") @RequestParam(required = false) Boolean active
    ) {
        List<PoolOperator> poolOperators = active != null && active
                ? poolOperatorService.findAllActive()
                : poolOperatorService.findAll();
        return ResponseEntity.ok(poolOperators);
    }

    /**
     * Activate a pool operator
     */
    @Operation(
            summary = "Havuz operatörünü aktifleştir",
            description = "Devre dışı bırakılmış bir havuz operatörünü tekrar aktif hale getirir."
    )
    @ApiResponse(responseCode = "200", description = "Havuz operatörü başarıyla aktifleştirildi")
    @ApiResponse(responseCode = "404", description = "Havuz operatörü bulunamadı")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PoolOperator> activatePoolOperator(
            @Parameter(description = "Havuz operatörü ID") @PathVariable Long id
    ) {
        PoolOperator poolOperator = poolOperatorService.activatePoolOperator(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Deactivate a pool operator
     */
    @Operation(
            summary = "Havuz operatörünü devre dışı bırak",
            description = "Bir havuz operatörünü devre dışı bırakır (soft delete)."
    )
    @ApiResponse(responseCode = "200", description = "Havuz operatörü başarıyla devre dışı bırakıldı")
    @ApiResponse(responseCode = "404", description = "Havuz operatörü bulunamadı")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PoolOperator> deactivatePoolOperator(
            @Parameter(description = "Havuz operatörü ID") @PathVariable Long id
    ) {
        PoolOperator poolOperator = poolOperatorService.deactivatePoolOperator(id);
        return ResponseEntity.ok(poolOperator);
    }

    /**
     * Update pool operator contact information
     */
    @Operation(
            summary = "Havuz operatörü iletişim bilgilerini güncelle",
            description = "Bir havuz operatörünün telefon, email ve yetkili kişi bilgilerini günceller."
    )
    @ApiResponse(responseCode = "200", description = "İletişim bilgileri başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Havuz operatörü bulunamadı")
    @PutMapping("/{id}/contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<PoolOperator> updateContactInfo(
            @Parameter(description = "Havuz operatörü ID") @PathVariable Long id,
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

    @Schema(description = "Havuz operatörü kayıt request DTO")
    public record RegisterPoolOperatorRequest(
            @Schema(description = "Şirket adı", example = "Cartonplast Havuz A.Ş.", required = true)
            @NotBlank(message = "Company name is required")
            String companyName,

            @Schema(description = "Vergi kimlik numarası", example = "1234567890", required = true)
            @NotBlank(message = "Tax ID is required")
            String taxId,

            @Schema(description = "İletişim telefonu", example = "02241234567", required = true)
            @NotBlank(message = "Contact phone is required")
            String contactPhone,

            @Schema(description = "İletişim email", example = "info@cartonplast.com", required = true)
            @NotBlank(message = "Contact email is required")
            @Email(message = "Email must be valid")
            String contactEmail,

            @Schema(description = "Yetkili kişi adı", example = "Ali Veli", required = true)
            @NotBlank(message = "Contact person name is required")
            String contactPersonName
    ) {}

    @Schema(description = "Havuz operatörü iletişim güncelleme request DTO")
    public record UpdateContactInfoRequest(
            @Schema(description = "Yeni telefon numarası", example = "02241234568", required = true)
            @NotBlank(message = "Contact phone is required")
            String contactPhone,

            @Schema(description = "Yeni email adresi", example = "yeni@cartonplast.com", required = true)
            @NotBlank(message = "Contact email is required")
            @Email(message = "Email must be valid")
            String contactEmail,

            @Schema(description = "Yeni yetkili kişi adı", example = "Mehmet Demir", required = true)
            @NotBlank(message = "Contact person name is required")
            String contactPersonName
    ) {}
}
