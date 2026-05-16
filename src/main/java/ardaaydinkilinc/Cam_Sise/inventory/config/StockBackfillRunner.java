package ardaaydinkilinc.Cam_Sise.inventory.config;

import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Stok kaydı olmayan eski dolumcular için varsayılan stokları TEK SEFER startup'ta
 * oluşturur. Eskiden bu backfill her stok-listesi isteğinde çalışıyordu ve dolumcu
 * başına 2 sorgu ile ciddi bir N+1 yaratıyordu — artık burada, bir kez, 2 toplu
 * sorgu ile yapılıyor.
 *
 * <p>{@link Ordered#LOWEST_PRECEDENCE} ile en son çalışır; böylece DataSeeder /
 * DataInitializer tüm dolumcuları oluşturduktan sonra devreye girer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class StockBackfillRunner implements CommandLineRunner {

    private final FillerStockService fillerStockService;

    @Override
    public void run(String... args) {
        try {
            int created = fillerStockService.backfillMissingStocks();
            if (created == 0) {
                log.info("Stock backfill: tüm dolumcuların stok kaydı mevcut, işlem yok");
            }
        } catch (Exception e) {
            log.error("Stock backfill başarısız oldu", e);
        }
    }
}
