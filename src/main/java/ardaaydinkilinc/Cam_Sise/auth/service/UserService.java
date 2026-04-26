package ardaaydinkilinc.Cam_Sise.auth.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ardaaydinkilinc.Cam_Sise.shared.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * Application service for User aggregate.
 * Coordinates domain operations and publishes events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user.
     * This method creates a user and automatically publishes UserRegistered event.
     */
    public User registerUser(
            Long poolOperatorId,
            String username,
            String rawPassword,
            String fullName,
            Role role,
            Long fillerId
    ) {
        log.info("Registering new user: username={}, role={}", username, role);

        // CUSTOMER upsert: aynı dolumcuya zaten aktif hesap varsa bilgileri güncelle
        if (role == Role.CUSTOMER && fillerId != null) {
            Optional<User> existing = userRepository.findByFillerIdAndActiveTrue(fillerId);
            if (existing.isPresent()) {
                User user = existing.get();
                // username değiştiyse başka biri kullanıyor mu kontrolü
                if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
                    throw new IllegalArgumentException("Username already exists: " + username);
                }
                user.updateUsername(username);
                user.updateFullName(fullName);
                user.updatePassword(passwordEncoder.encode(rawPassword));
                user = userRepository.save(user);
                log.info("Customer credentials overridden: id={}, fillerId={}", user.getId(), fillerId);
                return user;
            }
        }

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // Create user (event is added to aggregate)
        User user = User.register(
                poolOperatorId,
                username,
                hashedPassword,
                fullName,
                role,
                fillerId
        );

        // Save user (JPA listener will automatically publish events)
        user = userRepository.save(user);

        log.info("User registered successfully: id={}, username={}", user.getId(), username);

        return user;
    }

    /**
     * Change user role.
     * This method updates the role and automatically publishes UserRoleChanged event.
     */
    public User changeUserRole(Long userId, Role newRole) {
        log.info("Changing user role: userId={}, newRole={}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Change role (event is added to aggregate)
        user.changeRole(newRole);

        // Save user (JPA listener will automatically publish events)
        user = userRepository.save(user);

        log.info("User role changed successfully: userId={}, newRole={}", userId, newRole);

        return user;
    }

    /**
     * Update user password.
     */
    public User updatePassword(Long userId, String newRawPassword) {
        log.info("Updating password for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String hashedPassword = passwordEncoder.encode(newRawPassword);
        user.updatePassword(hashedPassword);

        user = userRepository.save(user);

        log.info("Password updated successfully: userId={}", userId);

        return user;
    }

    /**
     * Update user name and optionally password.
     */
    public User updateUser(Long userId, String fullName, String newRawPassword) {
        log.info("Updating user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (fullName != null && !fullName.isBlank()) {
            user.updateFullName(fullName);
        }
        if (newRawPassword != null && !newRawPassword.isBlank()) {
            user.updatePassword(passwordEncoder.encode(newRawPassword));
        }

        user = userRepository.save(user);
        log.info("User updated: userId={}", userId);
        return user;
    }

    /**
     * Find user by ID.
     */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /**
     * Find user by username.
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Find active user by username.
     */
    @Transactional(readOnly = true)
    public User findActiveUserByUsername(String username) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new IllegalArgumentException("Active user not found: " + username));
    }

    /**
     * Find all users belonging to a pool operator (tenant-scoped).
     */
    @Transactional(readOnly = true)
    public List<User> findByPoolOperatorId(Long poolOperatorId) {
        return userRepository.findByPoolOperatorId(poolOperatorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<User> findByPoolOperatorIdPaged(Long poolOperatorId, Role role, String search, int page, int size) {
        String searchParam = (search == null || search.isBlank()) ? "" : search;
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return PageResponse.from(userRepository.findByPoolOperatorIdAndRoleFiltered(poolOperatorId, role, searchParam, pageable));
    }

    /**
     * Deactivate a user (soft delete).
     */
    public User deactivateUser(Long userId) {
        log.info("Deactivating user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.deactivate();
        user = userRepository.save(user);

        log.info("User deactivated: userId={}", userId);
        return user;
    }
}
