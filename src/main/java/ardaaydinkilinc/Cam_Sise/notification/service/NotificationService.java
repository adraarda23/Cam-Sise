package ardaaydinkilinc.Cam_Sise.notification.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for the notification module.
 *
 * <p>Two main responsibilities:
 * <ul>
 *   <li>Create persistent notifications targeted at users (and optionally
 *       fan out to all staff of a pool operator).</li>
 *   <li>Read APIs used by the bell/panel UI (unread count, list, mark-read).</li>
 * </ul>
 *
 * <p>The email sender is optional ({@link ObjectProvider}) so the module
 * works even when {@code app.notifications.email.enabled=false}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<EmailNotificationSender> emailSender;

    /**
     * Push a notification to a single recipient user. Emails them if the
     * severity warrants and email is enabled.
     */
    public Notification notifyUser(
            Long recipientUserId,
            Long poolOperatorId,
            Long fillerId,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String body,
            String actionUrl
    ) {
        Notification n = Notification.create(
                recipientUserId, poolOperatorId, fillerId,
                type, severity, title, body, actionUrl);
        n = notificationRepository.save(n);
        sendEmailIfPossible(n);
        return n;
    }

    /**
     * Fan-out to every staff user (COMPANY_STAFF role) belonging to a pool operator.
     */
    public int notifyAllStaff(
            Long poolOperatorId,
            Long fillerId,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String body,
            String actionUrl
    ) {
        List<User> staff = userRepository.findByPoolOperatorId(poolOperatorId).stream()
                .filter(u -> u.getRole() == Role.COMPANY_STAFF && Boolean.TRUE.equals(u.getActive()))
                .toList();

        for (User u : staff) {
            notifyUser(u.getId(), poolOperatorId, fillerId, type, severity, title, body, actionUrl);
        }
        return staff.size();
    }

    /**
     * Notify the CUSTOMER user associated with a filler (if any).
     */
    public Optional<Notification> notifyFillerCustomer(
            Long fillerId,
            Long poolOperatorId,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String body,
            String actionUrl
    ) {
        return userRepository.findByFillerIdAndActiveTrue(fillerId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .map(user -> notifyUser(user.getId(), poolOperatorId, fillerId,
                        type, severity, title, body, actionUrl));
    }

    @Transactional(readOnly = true)
    public Page<Notification> listForUser(Long userId, Boolean unreadOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByRecipientUserIdAndReadOrderByCreatedAtDesc(
                    userId, false, pageable);
        }
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndRead(userId, false);
    }

    public Notification markRead(Long notificationId, Long requestingUserId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (n.getRecipientUserId() != null && !n.getRecipientUserId().equals(requestingUserId)) {
            throw new IllegalStateException("Notification does not belong to user " + requestingUserId);
        }
        n.markRead();
        return notificationRepository.save(n);
    }

    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadFor(userId);
    }

    private void sendEmailIfPossible(Notification n) {
        EmailNotificationSender sender = emailSender.getIfAvailable();
        if (sender == null || n.getRecipientUserId() == null) return;

        userRepository.findById(n.getRecipientUserId()).ifPresent(user -> {
            String email = user.getEmail();
            if (email != null && !email.isBlank()) {
                sender.send(n, email);
                n.markEmailSent();
                notificationRepository.save(n);
            }
        });
    }
}
