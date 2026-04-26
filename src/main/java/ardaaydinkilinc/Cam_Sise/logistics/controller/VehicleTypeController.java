package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleTypeService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

/**
 * REST API for VehicleType management.
 */
@RestController
@RequestMapping("/api/logistics/vehicle-types")
@RequiredArgsConstructor
@Tag(name = "Logistics - Vehicle Types", description = "Araç tipi yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;
    private final JwtUtil jwtUtil;

    /**
     * Create a new vehicle type
     */
    @Operation(
            summary = "Yeni araç tipi oluştur",
            description = "Sisteme yeni bir araç tipi tanımlar. ADMIN ve COMPANY_STAFF tarafından kullanılabilir."
    )
    @ApiResponse(responseCode = "201", description = "Araç tipi başarıyla oluşturuldu")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<VehicleType> createVehicleType(@RequestBody CreateVehicleTypeRequest request, HttpServletRequest httpRequest) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        VehicleType vehicleType = vehicleTypeService.createVehicleType(
                poolOperatorId,
                request.name,
                request.description,
                request.palletCapacity,
                request.separatorCapacity
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleType);
    }

    /**
     * Update vehicle type capacity
     */
    @Operation(
            summary = "Araç tipi kapasitesini güncelle",
            description = "Bir araç tipinin palet ve seperatör kapasitelerini günceller."
    )
    @ApiResponse(responseCode = "200", description = "Kapasite başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Araç tipi bulunamadı")
    @PutMapping("/{id}/capacity")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<VehicleType> updateCapacity(
            @Parameter(description = "Araç tipi ID") @PathVariable Long id,
            @RequestBody UpdateCapacityRequest request
    ) {
        VehicleType vehicleType = vehicleTypeService.updateCapacity(
                id,
                request.palletCapacity,
                request.separatorCapacity
        );
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Deactivate vehicle type
     */
    @Operation(
            summary = "Araç tipini devre dışı bırak",
            description = "Bir araç tipini devre dışı bırakır (soft delete)."
    )
    @ApiResponse(responseCode = "200", description = "Araç tipi başarıyla devre dışı bırakıldı")
    @ApiResponse(responseCode = "404", description = "Araç tipi bulunamadı")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<VehicleType> deactivateVehicleType(
            @Parameter(description = "Araç tipi ID") @PathVariable Long id
    ) {
        VehicleType vehicleType = vehicleTypeService.deactivateVehicleType(id);
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Get vehicle type by ID
     */
    @Operation(
            summary = "Araç tipi bilgisi getir",
            description = "ID'ye göre araç tipi bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Araç tipi başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Araç tipi bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<VehicleType> getVehicleType(
            @Parameter(description = "Araç tipi ID", example = "1") @PathVariable Long id
    ) {
        VehicleType vehicleType = vehicleTypeService.findById(id);
        return ResponseEntity.ok(vehicleType);
    }

    /**
     * Get all vehicle types (with optional filters)
     */
    @Operation(
            summary = "Tüm araç tiplerini listele",
            description = "Sistemdeki tüm araç tiplerini listeler. Pool operator ve aktiflik durumuna göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Araç tipi listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<List<VehicleType>> getAllVehicleTypes(
            @Parameter(description = "Aktiflik durumuna göre filtrele") @RequestParam(required = false) Boolean active,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        List<VehicleType> vehicleTypes = vehicleTypeService.findByPoolOperator(poolOperatorId, active);
        return ResponseEntity.ok(vehicleTypes);
    }

    // ===== DTOs =====

    @Schema(description = "Araç tipi oluşturma request DTO")
    public record CreateVehicleTypeRequest(
            @Schema(description = "Araç tipi adı", example = "Kamyon (12 ton)", required = true)
            String name,

            @Schema(description = "Araç tipi açıklaması", example = "Büyük kapasiteli kamyon", required = true)
            String description,

            @Schema(description = "Palet kapasitesi", example = "120", required = true)
            int palletCapacity,

            @Schema(description = "Seperatör kapasitesi", example = "80", required = true)
            int separatorCapacity
    ) {}

    @Schema(description = "Kapasite güncelleme request DTO")
    public record UpdateCapacityRequest(
            @Schema(description = "Yeni palet kapasitesi", example = "150", required = true)
            int palletCapacity,

            @Schema(description = "Yeni seperatör kapasitesi", example = "100", required = true)
            int separatorCapacity
    ) {}
}
