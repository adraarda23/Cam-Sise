package ardaaydinkilinc.Cam_Sise.notification.controller;

import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.notification.service.NotificationService;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Bildirim ve uyarı API'leri")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Operation(summary = "Bildirimleri listele", description = "Giriş yapan kullanıcının bildirimlerini sayfalı listeler. unreadOnly=true ile sadece okunmamışlar.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationListResponse> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserId(httpRequest);
        Page<Notification> result = notificationService.listForUser(userId, unreadOnly, page, size);
        return ResponseEntity.ok(new NotificationListResponse(
                result.getContent().stream().map(NotificationDto::from).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber()
        ));
    }

    @Operation(summary = "Okunmamış bildirim sayısı", description = "Navbar bell badge'i için kullanılır.")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnreadCountResponse> unreadCount(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        long count = notificationService.unreadCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @Operation(summary = "Bildirimi okundu işaretle")
    @PostMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        Notification n = notificationService.markRead(id, userId);
        return ResponseEntity.ok(NotificationDto.from(n));
    }

    @Operation(summary = "Tüm bildirimleri okundu işaretle")
    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarkAllReadResponse> markAllRead(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        int updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(new MarkAllReadResponse(updated));
    }

    @Operation(
            summary = "Test bildirimi gönder (email + in-app)",
            description = "Giriş yapan kullanıcıya CRITICAL severity'de bir test bildirimi gönderir. " +
                    "User.email doldurulmuş ve app.notifications.email.enabled=true ise mail de atılır."
    )
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TestNotificationResponse> sendTest(
            @RequestBody(required = false) TestNotificationRequest body,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserId(httpRequest);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String email = body != null && body.email() != null && !body.email().isBlank()
                ? body.email()
                : user.getEmail();

        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            user.updateEmail(email);
            userRepository.save(user);
        }

        Notification n = notificationService.notifyUser(
                userId,
                user.getPoolOperatorId(),
                user.getFillerId(),
                NotificationType.SYSTEM_INFO,
                NotificationSeverity.CRITICAL,
                "🧪 Test bildirimi — Cam-Sise",
                "Bu bir test bildirimidir. Bu mesajı email olarak alıyorsanız SMTP konfigürasyonu çalışıyor demektir.\n\n"
                        + "Gönderen: SMTP üzerinden Cam-Sise notification servisi\n"
                        + "Severity: CRITICAL (email atılması için gerekli seviye)\n"
                        + "Zaman: " + java.time.LocalDateTime.now(),
                "/notifications"
        );

        return ResponseEntity.ok(new TestNotificationResponse(
                n.getId(),
                user.getEmail(),
                n.isEmailSent(),
                "Bildirim oluşturuldu. Email gönderildi mi: " + n.isEmailSent()
        ));
    }

    private Long currentUserId(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header missing");
        }
        String token = header.substring(7);
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // ===== DTOs =====

    public record NotificationDto(
            Long id,
            Long recipientUserId,
            Long poolOperatorId,
            Long fillerId,
            String type,
            String severity,
            String title,
            String body,
            String actionUrl,
            boolean read,
            boolean emailSent,
            String createdAt,
            String readAt
    ) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(
                    n.getId(), n.getRecipientUserId(), n.getPoolOperatorId(), n.getFillerId(),
                    n.getType().name(), n.getSeverity().name(),
                    n.getTitle(), n.getBody(), n.getActionUrl(),
                    n.isRead(), n.isEmailSent(),
                    n.getCreatedAt() != null ? n.getCreatedAt().toString() : null,
                    n.getReadAt() != null ? n.getReadAt().toString() : null
            );
        }
    }

    public record NotificationListResponse(
            List<NotificationDto> content,
            long totalElements,
            int totalPages,
            int page
    ) {}

    public record UnreadCountResponse(long count) {}

    public record MarkAllReadResponse(int updated) {}

    public record TestNotificationRequest(String email) {}

    public record TestNotificationResponse(
            Long notificationId,
            String emailAddress,
            boolean emailSent,
            String message
    ) {}
}
