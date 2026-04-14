package ardaaydinkilinc.Cam_Sise.auth.config;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Uygulama başlarken test kullanıcılarını oluşturur
 * Eğer kullanıcılar zaten varsa tekrar oluşturmaz
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeUsers();
    }

    private void initializeUsers() {
        // ADMIN kullanıcısı
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .role(Role.ADMIN)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("✅ ADMIN kullanıcısı oluşturuldu: username=admin, password=admin123");
        }

        // COMPANY_STAFF kullanıcısı
        if (!userRepository.existsByUsername("staff")) {
            User staff = User.builder()
                    .username("staff")
                    .password(passwordEncoder.encode("staff123"))
                    .fullName("Company Staff")
                    .role(Role.COMPANY_STAFF)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(staff);
            log.info("✅ COMPANY_STAFF kullanıcısı oluşturuldu: username=staff, password=staff123");
        }

        // CUSTOMER kullanıcısı
        if (!userRepository.existsByUsername("customer")) {
            User customer = User.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Customer User")
                    .role(Role.CUSTOMER)
                    .fillerId(1L) // Test dolumcu ID'si
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(customer);
            log.info("✅ CUSTOMER kullanıcısı oluşturuldu: username=customer, password=customer123, fillerId=1");
        }

        log.info("🎯 Toplam kullanıcı sayısı: {}", userRepository.count());
    }
}
