package ardaaydinkilinc.Cam_Sise.analytics;

import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Raporlama ve analitik API'leri")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY_STAFF')")
    public ResponseEntity<AnalyticsSummary> getSummary(HttpServletRequest request) {
        Long poolOperatorId = jwtUtil.extractPoolOperatorId(
                request.getHeader("Authorization").substring(7));
        return ResponseEntity.ok(analyticsService.getSummary(poolOperatorId));
    }
}
