package ardaaydinkilinc.Cam_Sise.settings.controller;

import ardaaydinkilinc.Cam_Sise.settings.domain.CompanySettings;
import ardaaydinkilinc.Cam_Sise.settings.service.CompanySettingsService;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF') or hasRole('CUSTOMER')")
    public ResponseEntity<CompanySettings> getSettings(HttpServletRequest request) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(request.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(companySettingsService.getSettings(poolOperatorId));
    }

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
