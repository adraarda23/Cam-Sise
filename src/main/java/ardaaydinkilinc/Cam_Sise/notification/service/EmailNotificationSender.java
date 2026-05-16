package ardaaydinkilinc.Cam_Sise.notification.service;

import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends critical/warning notifications via SMTP.
 *
 * <p>Enabled with {@code app.notifications.email.enabled=true} (off by default).
 * Requires Spring Boot mail starter properties (host, port, username, password).
 * On disabled / mis-configured, falls back to a no-op log so callers can still
 * call {@link #send} unconditionally.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailNotificationSender {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.from:no-reply@cam-sise.local}")
    private String fromAddress;

    @Value("${app.notifications.email.subject-prefix:[Cam-Sise]}")
    private String subjectPrefix;

    @Value("${app.notifications.email.base-url:}")
    private String baseUrl;

    public void send(Notification notification, String toAddress) {
        if (toAddress == null || toAddress.isBlank()) {
            log.debug("Skipping email — no recipient address for notification {}", notification.getId());
            return;
        }
        // Only email warning + critical; informational stays in-app
        if (notification.getSeverity() == NotificationSeverity.INFO) {
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toAddress);
            msg.setSubject(buildSubject(notification));
            msg.setText(buildBody(notification));
            mailSender.send(msg);
            log.info("📧 Sent email for notification {} to {}", notification.getId(), toAddress);
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", toAddress, e.getMessage(), e);
        }
    }

    private String buildSubject(Notification n) {
        return String.format("%s [%s] %s", subjectPrefix, n.getSeverity(), n.getTitle());
    }

    private String buildBody(Notification n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getTitle()).append("\n\n");
        if (n.getBody() != null && !n.getBody().isBlank()) {
            sb.append(n.getBody()).append("\n\n");
        }
        if (n.getActionUrl() != null && !n.getActionUrl().isBlank()) {
            sb.append("Detay için: ").append(buildAbsoluteUrl(n.getActionUrl())).append("\n\n");
        }
        sb.append("—\nCam-Sise Bildirim Sistemi");
        return sb.toString();
    }

    /**
     * actionUrl ham bir path ("/requests") ise yapılandırılmış base URL ile birleştir.
     * Zaten http(s):// ile başlıyorsa olduğu gibi bırak.
     */
    private String buildAbsoluteUrl(String actionUrl) {
        if (actionUrl.startsWith("http://") || actionUrl.startsWith("https://")) {
            return actionUrl;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return actionUrl;
        }
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmedPath = actionUrl.startsWith("/") ? actionUrl : "/" + actionUrl;
        return trimmedBase + trimmedPath;
    }
}
