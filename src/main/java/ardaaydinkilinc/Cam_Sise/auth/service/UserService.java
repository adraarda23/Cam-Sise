package ardaaydinkilinc.Cam_Sise.auth.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
