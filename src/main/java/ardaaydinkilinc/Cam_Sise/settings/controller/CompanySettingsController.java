package ardaaydinkilinc.Cam_Sise.settings.controller;

import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Şirket ayarları yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Şirket ayarlarını getir", description = "Tenant'a ait minimum talep miktarı eşiklerini döndürür. COMPANY_STAFF ve CUSTOMER erişebilir.")
    @ApiResponse(responseCode = "200", description = "Ayarlar başarıyla döndürüldü")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF') or hasRole('CUSTOMER')")
    public ResponseEntity<CompanySettings> getSettings(HttpServletRequest request) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(request.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(companySettingsService.getSettings(poolOperatorId));
    }

    @Operation(summary = "Şirket ayarlarını güncelle", description = "Minimum palet ve ayırıcı talep miktarı eşiklerini günceller. Sadece COMPANY_STAFF.")
    @ApiResponse(responseCode = "200", description = "Ayarlar başarıyla güncellendi")
    @ApiResponse(responseCode = "400", description = "Geçersiz parametreler")
    @PutMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<CompanySettings> updateSettings(
            @RequestBody UpdateSettingsRequest body,
            HttpServletRequest request) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(request.getHeader("Authorization").substring(7));
        CompanySettings updated = companySettingsService.updateSettings(
                poolOperatorId,
                body.minPalletRequestQty(),
                body.minSeparatorRequestQty()
        );
        return ResponseEntity.ok(updated);
    }

    record UpdateSettingsRequest(int minPalletRequestQty, int minSeparatorRequestQty) {}
}
