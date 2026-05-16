package ardaaydinkilinc.Cam_Sise.shared.admin;

import ardaaydinkilinc.Cam_Sise.logistics.service.routing.OsrmDistanceProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Operational diagnostics — used to debug environment problems (OSRM
 * reachability, mail SMTP, etc.) without shipping shell access. Exposed
 * under {@code /api/admin/diagnostics/...}, requires staff role.
 */
@RestController
@RequestMapping("/api/admin/diagnostics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Diagnostics", description = "Operasyonel teşhis araçları")
@SecurityRequirement(name = "bearerAuth")
public class DiagnosticController {

    private final OsrmDistanceProvider osrmDistanceProvider;

    // SMTP enabled değilken JavaMailSender bean'i de gelmeyebilir; bu yüzden Autowired(required=false).
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.notifications.email.from:no-reply@cam-sise.local}")
    private String mailFromAddress;

    @Value("${app.notifications.email.subject-prefix:[Cam-Sise]}")
    private String mailSubjectPrefix;

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

    @Operation(
            summary = "SMTP test maili gönder",
            description = "Verilen adrese basit bir test maili gönderir. SMTP bağlantısı, kimlik doğrulama " +
                    "ve TLS akışını doğrulamak için kullanılır."
    )
    @PostMapping("/mail-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_STAFF')")
    public ResponseEntity<MailTestResponse> sendTestMail(@RequestBody MailTestRequest request) {
        if (mailSender == null) {
            return ResponseEntity.ok(new MailTestResponse(false,
                    "SMTP bean'i yüklenmemiş. app.notifications.email.enabled=true ve spring.mail.* yapılandırılmış mı?"));
        }
        if (request == null || request.to == null || request.to.isBlank()) {
            return ResponseEntity.ok(new MailTestResponse(false, "'to' adresi zorunlu."));
        }
        long start = System.currentTimeMillis();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFromAddress);
            msg.setTo(request.to);
            msg.setSubject(mailSubjectPrefix + " Test Maili");
            msg.setText(
                    "Bu Cam-Sise yönetim panelinden gönderilen bir SMTP test mailidir.\n\n" +
                            "Gönderim zamanı: " + LocalDateTime.now() + "\n" +
                            "Eğer bu maili aldıysanız, bildirim akışı doğru yapılandırılmış demektir.\n\n" +
                            "—\nCam-Sise Bildirim Sistemi"
            );
            mailSender.send(msg);
            long elapsed = System.currentTimeMillis() - start;
            log.info("📧 Test mail başarıyla gönderildi: to={} elapsed={}ms", request.to, elapsed);
            return ResponseEntity.ok(new MailTestResponse(true,
                    String.format("Mail gönderildi (%d ms). Gelen kutusunu ve spam klasörünü kontrol edin.", elapsed)));
        } catch (Exception e) {
            log.error("Test mail gönderilemedi: to={} error={}", request.to, e.getMessage(), e);
            return ResponseEntity.ok(new MailTestResponse(false,
                    "SMTP hatası: " + e.getClass().getSimpleName() + " — " + e.getMessage()));
        }
    }

    public record MailTestRequest(String to) {}
    public record MailTestResponse(boolean ok, String detail) {}
}
