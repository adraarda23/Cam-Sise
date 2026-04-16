package ardaaydinkilinc.Cam_Sise.auth.domain;

import ardaaydinkilinc.Cam_Sise.auth.domain.event.UserRegistered;
import ardaaydinkilinc.Cam_Sise.auth.domain.event.UserRoleChanged;
import ardaaydinkilinc.Cam_Sise.shared.domain.base.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User aggregate root
 * Represents a system user with role-based access
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User extends AggregateRoot<Long> {

    @Column(name = "pool_operator_id", nullable = false)
    private Long poolOperatorId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * CUSTOMER rolündeki kullanıcılar için dolumcu ID'si.
     * Diğer roller için null olabilir.
     */
    @Column(name = "filler_id")
    private Long fillerId;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Factory method to register a new user
     */
    public static User register(
            Long poolOperatorId,
            String username,
            String password,
            String fullName,
            Role role,
            Long fillerId
    ) {
        User user = new User();
        user.poolOperatorId = poolOperatorId;
        user.username = username;
        user.password = password;
        user.fullName = fullName;
        user.role = role;
        user.fillerId = fillerId;
        user.active = true;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();

        user.addDomainEvent(new UserRegistered(
                poolOperatorId,
                username,
                role,
                fillerId,
                LocalDateTime.now()
        ));

        return user;
    }

    /**
     * Change user role
     */
    public void changeRole(Role newRole) {
        Role oldRole = this.role;
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();

        addDomainEvent(new UserRoleChanged(
                this.id,
                this.poolOperatorId,
                oldRole,
                newRole,
                LocalDateTime.now()
        ));
    }

    /**
     * Update password
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}