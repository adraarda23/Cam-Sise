package ardaaydinkilinc.Cam_Sise.notification.repository;

import ardaaydinkilinc.Cam_Sise.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndReadOrderByCreatedAtDesc(
            Long recipientUserId, boolean read, Pageable pageable);

    long countByRecipientUserIdAndRead(Long recipientUserId, boolean read);

    List<Notification> findByPoolOperatorIdAndReadOrderByCreatedAtDesc(Long poolOperatorId, boolean read);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientUserId = :userId AND n.read = false")
    int markAllReadFor(@Param("userId") Long userId);
}
