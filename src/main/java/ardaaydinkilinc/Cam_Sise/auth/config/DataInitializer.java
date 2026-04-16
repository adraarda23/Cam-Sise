package ardaaydinkilinc.Cam_Sise.auth.config;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.core.service.PoolOperatorService;
import ardaaydinkilinc.Cam_Sise.core.service.FillerService;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Uygulama başlarken test verilerini oluşturur
 * Eğer veriler zaten varsa tekrar oluşturmaz
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoolOperatorService poolOperatorService;
    private final FillerService fillerService;

    @Override
    public void run(String... args) {
        log.info("🚀 Starting data initialization...");
        initializePoolOperators();
        initializeFillers();
        initializeUsers();
        log.info("✅ Data initialization completed!");
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

    private void initializePoolOperators() {
        log.info("📦 Initializing Pool Operators...");

        try {
            // Check if any pool operator exists
            if (poolOperatorService.findAll().isEmpty()) {
                // Create default pool operator
                PoolOperator poolOperator = poolOperatorService.registerPoolOperator(
                        "Şişecam Havuz Yönetim A.Ş.",
                        "1234567890",
                        "+90 224 123 4567",
                        "info@sisecamhavuz.com.tr",
                        "Mehmet Yılmaz"
                );
                log.info("✅ Pool Operator oluşturuldu: id={}, name={}",
                        poolOperator.getId(), poolOperator.getCompanyName());
            } else {
                log.info("ℹ️ Pool Operator zaten mevcut, yeni oluşturulmadı");
            }
        } catch (Exception e) {
            log.error("❌ Pool Operator oluşturulurken hata: {}", e.getMessage());
        }
    }

    private void initializeFillers() {
        log.info("🏭 Initializing Fillers...");

        try {
            // Check if any filler exists
            if (fillerService.findAll().isEmpty()) {
                // Create test fillers
                Filler filler1 = fillerService.registerFiller(
                        1L, // poolOperatorId
                        "Anadolu Cam Dolum A.Ş.",
                        "Organize Sanayi Bölgesi 1. Cadde No:15",
                        "Gemlik",
                        "Bursa",
                        "16600",
                        "Türkiye",
                        40.4313, // latitude
                        29.1546, // longitude
                        "+90 224 555 0101",
                        "anadolu@camdolum.com.tr",
                        "Ahmet Demir",
                        "9876543210"
                );
                log.info("✅ Filler oluşturuldu: id={}, name={}", filler1.getId(), filler1.getName());

                Filler filler2 = fillerService.registerFiller(
                        1L, // poolOperatorId
                        "Marmara Meşrubat San. Tic. A.Ş.",
                        "Atatürk Bulvarı No:234",
                        "Osmangazi",
                        "Bursa",
                        "16200",
                        "Türkiye",
                        40.1885,
                        29.0610,
                        "+90 224 555 0202",
                        "marmara@mesrubat.com.tr",
                        "Ayşe Kaya",
                        "5555555555"
                );
                log.info("✅ Filler oluşturuldu: id={}, name={}", filler2.getId(), filler2.getName());

                Filler filler3 = fillerService.registerFiller(
                        1L, // poolOperatorId
                        "Uludağ İçecek Dolum Ltd.",
                        "Sanayi Mahallesi 45. Sokak No:8",
                        "Nilüfer",
                        "Bursa",
                        "16110",
                        "Türkiye",
                        40.2050,
                        28.8846,
                        "+90 224 555 0303",
                        "uludag@icecek.com.tr",
                        "Fatma Çelik",
                        "7777777777"
                );
                log.info("✅ Filler oluşturuldu: id={}, name={}", filler3.getId(), filler3.getName());

                log.info("🎯 Toplam {} filler oluşturuldu", fillerService.findAll().size());
            } else {
                log.info("ℹ️ Filler zaten mevcut, yeni oluşturulmadı");
            }
        } catch (Exception e) {
            log.error("❌ Filler oluşturulurken hata: {}", e.getMessage());
        }
    }
}
