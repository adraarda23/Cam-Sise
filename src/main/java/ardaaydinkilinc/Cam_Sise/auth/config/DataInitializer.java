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
            User admin = User.register(
                    1L, // default pool operator ID
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Admin User",
                    Role.ADMIN,
                    null
            );
            userRepository.save(admin);
            log.info("✅ ADMIN kullanıcısı oluşturuldu: username=admin, password=admin123");
        }

        // COMPANY_STAFF kullanıcısı
        if (!userRepository.existsByUsername("staff")) {
            User staff = User.register(
                    1L, // default pool operator ID
                    "staff",
                    passwordEncoder.encode("staff123"),
                    "Company Staff",
                    Role.COMPANY_STAFF,
                    null
            );
            userRepository.save(staff);
            log.info("✅ COMPANY_STAFF kullanıcısı oluşturuldu: username=staff, password=staff123");
        }

        // CUSTOMER kullanıcısı
        if (!userRepository.existsByUsername("customer")) {
            User customer = User.register(
                    1L, // default pool operator ID
                    "customer",
                    passwordEncoder.encode("customer123"),
                    "Customer User",
                    Role.CUSTOMER,
                    1L // Test dolumcu ID'si
            );
            userRepository.save(customer);
            log.info("✅ CUSTOMER kullanıcısı oluşturuldu: username=customer, password=customer123, fillerId=1");
        }

        log.info("🎯 Toplam kullanıcı sayısı: {}", userRepository.count());
    }
}
