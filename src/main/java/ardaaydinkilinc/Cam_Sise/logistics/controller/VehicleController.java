package ardaaydinkilinc.Cam_Sise.logistics.controller;

import ardaaydinkilinc.Cam_Sise.logistics.service.VehicleService;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
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
 * REST API for Vehicle management.
 */
@RestController
@RequestMapping("/api/logistics/vehicles")
@RequiredArgsConstructor
@Tag(name = "Logistics - Vehicles", description = "Araç yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;
    private final JwtUtil jwtUtil;

    /**
     * Register a new vehicle
     */
    @Operation(
            summary = "Yeni araç kaydı",
            description = "Sisteme yeni bir araç kaydeder. ADMIN ve COMPANY_STAFF tarafından kullanılabilir."
    )
    @ApiResponse(responseCode = "201", description = "Araç başarıyla kaydedildi")
    @ApiResponse(responseCode = "400", description = "Geçersiz request parametreleri")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> registerVehicle(@RequestBody RegisterVehicleRequest request) {
        Vehicle vehicle = vehicleService.registerVehicle(
                request.depotId,
                request.vehicleTypeId,
                request.plateNumber
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    /**
     * Assign vehicle to collection route
     */
    @Operation(
            summary = "Aracı toplama rotasına ata",
            description = "Bir aracı belirli bir toplama planına (rota) atar ve sürücü bilgilerini kaydeder."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla rotaya atandı")
    @ApiResponse(responseCode = "404", description = "Araç veya plan bulunamadı")
    @PostMapping("/{vehicleId}/assign")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> assignToRoute(
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId,
            @RequestBody AssignToRouteRequest request
    ) {
        Vehicle vehicle = vehicleService.assignToRoute(
                vehicleId,
                request.collectionPlanId,
                request.driverName,
                request.licenseNumber,
                request.phone
        );
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Vehicle departs from depot
     */
    @Operation(
            summary = "Araç depodan ayrıldı",
            description = "Aracın depodan ayrıldığını (yola çıktığını) kaydeder."
    )
    @ApiResponse(responseCode = "200", description = "Araç durumu başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Araç bulunamadı")
    @PostMapping("/{vehicleId}/depart")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> departFromDepot(
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId
    ) {
        Vehicle vehicle = vehicleService.departFromDepot(vehicleId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Vehicle returns to depot
     */
    @Operation(
            summary = "Araç depoya döndü",
            description = "Aracın depoya geri döndüğünü kaydeder."
    )
    @ApiResponse(responseCode = "200", description = "Araç durumu başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Araç bulunamadı")
    @PostMapping("/{vehicleId}/return")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> returnToDepot(
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId
    ) {
        Vehicle vehicle = vehicleService.returnToDepot(vehicleId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Change vehicle status
     */
    @Operation(
            summary = "Araç durumunu değiştir",
            description = "Aracın durumunu manuel olarak günceller (AVAILABLE, IN_TRANSIT, MAINTENANCE, etc.)."
    )
    @ApiResponse(responseCode = "200", description = "Araç durumu başarıyla güncellendi")
    @ApiResponse(responseCode = "404", description = "Araç bulunamadı")
    @PutMapping("/{vehicleId}/status")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> changeStatus(
            @Parameter(description = "Araç ID") @PathVariable Long vehicleId,
            @RequestBody ChangeStatusRequest request
    ) {
        Vehicle vehicle = vehicleService.changeStatus(vehicleId, request.newStatus);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get vehicle by ID
     */
    @Operation(
            summary = "Araç bilgisi getir",
            description = "ID'ye göre araç bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Araç bulunamadı")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> getVehicle(
            @Parameter(description = "Araç ID", example = "1") @PathVariable Long id
    ) {
        Vehicle vehicle = vehicleService.findById(id);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get vehicle by plate number
     */
    @Operation(
            summary = "Plakaya göre araç getir",
            description = "Plaka numarasına göre araç bilgisini getirir."
    )
    @ApiResponse(responseCode = "200", description = "Araç başarıyla getirildi")
    @ApiResponse(responseCode = "404", description = "Araç bulunamadı")
    @GetMapping("/plate/{plateNumber}")
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<Vehicle> getVehicleByPlate(
            @Parameter(description = "Plaka numarası", example = "34ABC123") @PathVariable String plateNumber
    ) {
        Vehicle vehicle = vehicleService.findByPlateNumber(plateNumber);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Get all vehicles (with optional filters)
     */
    @Operation(
            summary = "Tüm araçları listele",
            description = "Sistemdeki tüm araçları listeler. Depo ve durum bilgisine göre filtrelenebilir."
    )
    @ApiResponse(responseCode = "200", description = "Araç listesi başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<PageResponse<Vehicle>> getAllVehicles(
            @Parameter(description = "Araç durumuna göre filtrele") @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest
    ) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(httpRequest.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(vehicleService.findByPoolOperatorIdPaged(poolOperatorId, status, search, page, size));
    }

    // ===== DTOs =====

    @Schema(description = "Araç kayıt request DTO")
    public record RegisterVehicleRequest(
            @Schema(description = "Depo ID", example = "1", required = true)
            Long depotId,

            @Schema(description = "Araç tipi ID", example = "1", required = true)
            Long vehicleTypeId,

            @Schema(description = "Plaka numarası", example = "34ABC123", required = true)
            String plateNumber
    ) {}

    @Schema(description = "Rotaya atama request DTO")
    public record AssignToRouteRequest(
            @Schema(description = "Toplama planı ID", example = "1", required = true)
            Long collectionPlanId,

            @Schema(description = "Sürücü adı", example = "Mehmet Yılmaz", required = true)
            String driverName,

            @Schema(description = "Sürücü belgesi numarası", example = "12345678901", required = true)
            String licenseNumber,

            @Schema(description = "Sürücü telefonu", example = "05551234567", required = true)
            String phone
    ) {}

    @Schema(description = "Araç durumu değiştirme request DTO")
    public record ChangeStatusRequest(
            @Schema(description = "Yeni araç durumu", example = "AVAILABLE", required = true)
            VehicleStatus newStatus
    ) {}
}
