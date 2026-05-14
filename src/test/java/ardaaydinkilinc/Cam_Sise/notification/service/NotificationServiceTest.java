package ardaaydinkilinc.Cam_Sise.notification.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationSeverity;
import ardaaydinkilinc.Cam_Sise.notification.domain.vo.NotificationType;
import ardaaydinkilinc.Cam_Sise.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repo;
    @Mock private UserRepository userRepository;
    @Mock private ObjectProvider<EmailNotificationSender> emailProvider;

    @InjectMocks
    private NotificationService service;

    @Test
    @DisplayName("notifyUser persists and returns notification")
    void notifyUserPersists() {
        when(emailProvider.getIfAvailable()).thenReturn(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification n = service.notifyUser(
                1L, 10L, null,
                NotificationType.STOCK_ANOMALY, NotificationSeverity.CRITICAL,
                "title", "body", "/url");

        assertThat(n.getRecipientUserId()).isEqualTo(1L);
        assertThat(n.getType()).isEqualTo(NotificationType.STOCK_ANOMALY);
        assertThat(n.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("notifyAllStaff fans out only to COMPANY_STAFF active users")
    void notifyAllStaff() {
        when(emailProvider.getIfAvailable()).thenReturn(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User staff = User.register(10L, "staff1", "p", "Staff One", Role.COMPANY_STAFF, null);
        User customer = User.register(10L, "cust1", "p", "Customer One", Role.CUSTOMER, 5L);
        User inactiveStaff = User.register(10L, "staff2", "p", "Staff Two", Role.COMPANY_STAFF, null);
        inactiveStaff.deactivate();

        when(userRepository.findByPoolOperatorId(10L))
                .thenReturn(List.of(staff, customer, inactiveStaff));

        int n = service.notifyAllStaff(10L, null,
                NotificationType.SYSTEM_INFO, NotificationSeverity.INFO,
                "t", "b", "/u");

        assertThat(n).isEqualTo(1);
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("notifyFillerCustomer only fires when an active CUSTOMER user is linked")
    void notifyFillerCustomerHappyPath() {
        when(emailProvider.getIfAvailable()).thenReturn(null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User customer = User.register(10L, "cust1", "p", "Customer One", Role.CUSTOMER, 5L);
        when(userRepository.findByFillerIdAndActiveTrue(5L)).thenReturn(Optional.of(customer));

        Optional<Notification> result = service.notifyFillerCustomer(
                5L, 10L,
                NotificationType.STOCK_ANOMALY, NotificationSeverity.WARNING,
                "t", "b", "/u");

        assertThat(result).isPresent();
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("notifyFillerCustomer returns empty when no customer mapped")
    void notifyFillerCustomerEmpty() {
        when(userRepository.findByFillerIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        Optional<Notification> result = service.notifyFillerCustomer(
                99L, 10L,
                NotificationType.STOCK_ANOMALY, NotificationSeverity.WARNING,
                "t", "b", "/u");

        assertThat(result).isEmpty();
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("markRead rejects notification belonging to another user")
    void markReadEnforcesOwnership() {
        Notification owned = Notification.create(1L, 10L, null,
                NotificationType.SYSTEM_INFO, NotificationSeverity.INFO,
                "t", "b", null);
        when(repo.findById(99L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.markRead(99L, 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("markRead sets read flag and persists")
    void markReadHappyPath() {
        Notification owned = Notification.create(1L, 10L, null,
                NotificationType.SYSTEM_INFO, NotificationSeverity.INFO,
                "t", "b", null);
        when(repo.findById(99L)).thenReturn(Optional.of(owned));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification updated = service.markRead(99L, 1L);

        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Email sender invoked when available and user has email")
    void emailSentWhenAvailable() {
        EmailNotificationSender sender = org.mockito.Mockito.mock(EmailNotificationSender.class);
        when(emailProvider.getIfAvailable()).thenReturn(sender);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User u = User.register(10L, "user", "p", "User", Role.COMPANY_STAFF, null);
        u.updateEmail("user@example.com");
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(userRepository.findById(any())).thenReturn(Optional.of(u));

        service.notifyUser(1L, 10L, null,
                NotificationType.STOCK_ANOMALY, NotificationSeverity.CRITICAL,
                "t", "b", "/u");

        verify(sender, times(1)).send(any(), org.mockito.ArgumentMatchers.eq("user@example.com"));
    }
}
