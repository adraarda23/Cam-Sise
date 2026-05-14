package ardaaydinkilinc.Cam_Sise.shared.admin;

import ardaaydinkilinc.Cam_Sise.logistics.service.routing.OsrmDistanceProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operational diagnostics — used to debug environment problems (OSRM
 * reachability, mail SMTP, etc.) without shipping shell access. Exposed
 * under {@code /api/admin/diagnostics/...}, requires staff role.
 */
@RestController
@RequestMapping("/api/admin/diagnostics")
@RequiredArgsConstructor
@Tag(name = "Admin - Diagnostics", description = "Operasyonel teşhis araçları")
@SecurityRequirement(name = "bearerAuth")
public class DiagnosticController {

    private final OsrmDistanceProvider osrmDistanceProvider;

    @Operation(
            summary = "OSRM erişilebilirliği teşhisi",
            description = "Backend'in OSRM endpoint'ine ulaşıp ulaşamadığını test eder. " +
                    "HTTP durum kodu, gecikme ve yanıt önizlemesi döndürür."
    )
    @GetMapping("/osrm-health")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<OsrmHealthResponse> osrmHealth() {
        String result = osrmDistanceProvider.diagnosticPing();
        boolean ok = result.startsWith("status=200");
        return ResponseEntity.ok(new OsrmHealthResponse(ok, result));
    }

    public record OsrmHealthResponse(boolean ok, String detail) {}
}
