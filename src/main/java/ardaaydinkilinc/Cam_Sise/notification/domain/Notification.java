package ardaaydinkilinc.Cam_Sise.notification.domain;

import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification aggregate.
 *
 * <p>One row per addressed message. {@code recipientUserId} is the target user;
 * {@code poolOperatorId} is the tenant scope. {@code fillerId} is optional
 * and links the notification to a specific filler when applicable (e.g. for
 * anomaly / threshold alerts).
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notif_recipient_unread", columnList = "recipient_user_id, read_flag, created_at"),
                @Index(name = "idx_notif_pool_operator", columnList = "pool_operator_id, created_at")
        }
)
@Getter
@NoArgsConstructor
public class Notification extends AggregateRoot<Long> {

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "pool_operator_id")
    private Long poolOperatorId;

    @Column(name = "filler_id")
    private Long fillerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private NotificationSeverity severity;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "read_flag", nullable = false)
    private boolean read;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create(
            Long recipientUserId,
            Long poolOperatorId,
            Long fillerId,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String body,
            String actionUrl
    ) {
        if (type == null) throw new IllegalArgumentException("type required");
        if (severity == null) throw new IllegalArgumentException("severity required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");

        Notification n = new Notification();
        n.recipientUserId = recipientUserId;
        n.poolOperatorId = poolOperatorId;
        n.fillerId = fillerId;
        n.type = type;
        n.severity = severity;
        n.title = title;
        n.body = body;
        n.actionUrl = actionUrl;
        n.read = false;
        n.emailSent = false;
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public void markRead() {
        if (!read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void markEmailSent() {
        this.emailSent = true;
    }
}
