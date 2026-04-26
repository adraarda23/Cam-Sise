package ardaaydinkilinc.Cam_Sise.core.controller;

import ardaaydinkilinc.Cam_Sise.core.service.FillerService;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;


/**
 * REST API for Filler management.
 * ADMIN and COMPANY_STAFF can manage fillers.
 * CUSTOMER can view their own filler information.
 */
@RestController
@RequestMapping("/api/fillers")
@RequiredArgsConstructor
@Tag(name = "Core - Fillers", description = "Dolumcu yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class FillerController {

    private final FillerService fillerService;
    private final JwtUtil jwtUtil;

    /**
     * Register a new filler
     */
    @Operation(
            summary = "Yeni dolumcu kaydı",
            description = "Sisteme yeni bir dolumcu kaydeder. ADMIN ve COMPANY_STAFF tarafından kullanılabilir."
    )
    @ApiResponse(responseCode = "201", description = "Dolumcu başarıyla kaydedildi")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> registerFiller(@RequestBody RegisterFillerRequest request,
                                                  HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String token = authHeader.substring(7);
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(token);

        Filler filler = fillerService.registerFiller(
                poolOperatorId,
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
    @Operation(
            summary = "Dolumcu bilgisi getir",
            description = "ID'ye göre dolumcu bilgisini getirir. CUSTOMER kendi dolumcusunu görebilir."
    )
    @ApiResponse(responseCode = "200", description = "Dolumcu başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Dolumcu bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF', 'CUSTOMER')")
    public ResponseEntity<Filler> getFiller(
            @Parameter(description = "Dolumcu ID", example = "1") @PathVariable Long id
    ) {
        Filler filler = fillerService.findById(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Get all fillers (with optional filters)
     */
    @Operation(
            summary = "Tüm dolumcuları listele",
            description = "Sisteme kayıtlı tüm dolumcuları listeler. Pool operator ve aktiflik durumuna göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Dolumcu listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<PageResponse<Filler>> getAllFillers(
            @Parameter(description = "Aktiflik durumuna göre filtrele") @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(token);
        return ResponseEntity.ok(fillerService.findByPoolOperatorPaged(poolOperatorId, active, search, page, size));
    }

    /**
     * Full update of a filler
     */
    @Operation(summary = "Dolumcu bilgilerini güncelle", description = "Ad, adres, iletişim ve konum bilgilerini günceller.")
    @ApiResponse(responseCode = "200", description = "Dolumcu başarıyla güncellendi")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> updateFiller(
            @Parameter(description = "Dolumcu ID") @PathVariable Long id,
            @RequestBody UpdateFillerRequest request
    ) {
        Filler filler = fillerService.updateFiller(
                id,
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
                request.contactPersonName
        );
        return ResponseEntity.ok(filler);
    }

    /**
     * Activate a filler
     */
    @Operation(
            summary = "Dolumcuyu aktifleştir",
            description = "Devre dışı bırakılmış bir dolumcuyu tekrar aktif hale getirir."
    )
    @ApiResponse(responseCode = "200", description = "Dolumcu başarıyla aktifleştirildi")
    @ApiResponse(responseCode = "404", description = "Dolumcu bulunamadı")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> activateFiller(
            @Parameter(description = "Dolumcu ID") @PathVariable Long id
    ) {
        Filler filler = fillerService.activateFiller(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Deactivate a filler
     */
    @Operation(
            summary = "Dolumcuyu devre dışı bırak",
            description = "Bir dolumcuyu devre dışı bırakır (soft delete)."
    )
    @ApiResponse(responseCode = "200", description = "Dolumcu başarıyla devre dışı bırakıldı")
    @ApiResponse(responseCode = "404", description = "Dolumcu bulunamadı")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> deactivateFiller(
            @Parameter(description = "Dolumcu ID") @PathVariable Long id
    ) {
        Filler filler = fillerService.deactivateFiller(id);
        return ResponseEntity.ok(filler);
    }

    /**
     * Update filler contact information
     */
    @Operation(
            summary = "Dolumcu iletişim bilgilerini güncelle",
            description = "Bir dolumcunun telefon, email ve yetkili kişi bilgilerini günceller."
    )
    @ApiResponse(responseCode = "200", description = "İletişim bilgileri başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Dolumcu bulunamadı")
    @PutMapping("/{id}/contact")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> updateContactInfo(
            @Parameter(description = "Dolumcu ID") @PathVariable Long id,
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
    @Operation(
            summary = "Dolumcu konum bilgisini güncelle",
            description = "Bir dolumcunun GPS koordinatlarını (enlem/boylam) günceller."
    )
    @ApiResponse(responseCode = "200", description = "Konum bilgisi başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Dolumcu bulunamadı")
    @PutMapping("/{id}/location")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Filler> updateLocation(
            @Parameter(description = "Dolumcu ID") @PathVariable Long id,
            @RequestBody UpdateLocationRequest request
    ) {
        Filler filler = fillerService.updateLocation(id, request.latitude, request.longitude);
        return ResponseEntity.ok(filler);
    }

    // ===== DTOs =====

    @Schema(description = "Dolumcu kayıt request DTO")
    public record RegisterFillerRequest(
            @Schema(description = "Dolumcu adı", example = "Coca-Cola Bursa Dolum", required = true)
            String name,

            @Schema(description = "Sokak adresi", example = "Organize Sanayi Bölgesi 5. Cadde No:12", required = true)
            String street,

            @Schema(description = "İl/Şehir", example = "Bursa", required = true)
            String city,

            @Schema(description = "İlçe", example = "Osmangazi", required = true)
            String province,

            @Schema(description = "Posta kodu", example = "16200", required = true)
            String postalCode,

            @Schema(description = "Ülke", example = "Türkiye", required = true)
            String country,

            @Schema(description = "Enlem (Latitude)", example = "40.2108", required = true)
            @jakarta.validation.constraints.NotNull(message = "Enlem (latitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            Double latitude,

            @Schema(description = "Boylam (Longitude)", example = "29.0138", required = true)
            @jakarta.validation.constraints.NotNull(message = "Boylam (longitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            Double longitude,

            @Schema(description = "İletişim telefonu", example = "02241234567", required = true)
            String contactPhone,

            @Schema(description = "İletişim email", example = "bursa@cocacola.com", required = true)
            String contactEmail,

            @Schema(description = "Yetkili kişi adı", example = "Ahmet Yılmaz", required = true)
            String contactPersonName,

            @Schema(description = "Vergi kimlik numarası", example = "1234567890", required = true)
            String taxId
    ) {}

    @Schema(description = "Dolumcu güncelleme request DTO")
    public record UpdateFillerRequest(
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
            String contactPersonName
    ) {}

    @Schema(description = "Dolumcu iletişim güncelleme request DTO")
    public record UpdateContactInfoRequest(
            @Schema(description = "Yeni telefon numarası", example = "02241234568")
            String contactPhone,

            @Schema(description = "Yeni email adresi", example = "yenimail@cocacola.com")
            String contactEmail,

            @Schema(description = "Yeni yetkili kişi adı", example = "Mehmet Demir")
            String contactPersonName
    ) {}

    @Schema(description = "Dolumcu konum güncelleme request DTO")
    public record UpdateLocationRequest(
            @Schema(description = "Yeni enlem (Latitude)", example = "40.2200", required = true)
            @jakarta.validation.constraints.NotNull(message = "Enlem (latitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Enlem (latitude) -90 ile 90 arasında olmalıdır")
            Double latitude,

            @Schema(description = "Yeni boylam (Longitude)", example = "29.0200", required = true)
            @jakarta.validation.constraints.NotNull(message = "Boylam (longitude) zorunludur")
            @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Boylam (longitude) -180 ile 180 arasında olmalıdır")
            Double longitude
    ) {}
}
