package ardaaydinkilinc.Cam_Sise.shared.config;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.StockMovementHistory;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.StockMovementHistoryRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.VehicleStatus;
import ardaaydinkilinc.Cam_Sise.logistics.repository.DepotRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleRepository;
import ardaaydinkilinc.Cam_Sise.logistics.repository.VehicleTypeRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.Address;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.ContactInfo;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.TaxId;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Synthetic data generator for testing and development.
 *
 * <p>Generates:
 * <ul>
 *   <li>1 PoolOperator + 1 Depot (Gemlik)</li>
 *   <li>3 VehicleTypes (small / medium / large)</li>
 *   <li>5 Vehicles</li>
 *   <li>{@code app.seed.filler-count} Fillers (default 250)</li>
 *   <li>For each Filler×AssetType: FillerStock + 90 days of
 *       {@link StockMovementHistory} carrying:
 *       <ul>
 *         <li>monthly seasonal multiplier (peak in summer for beverages)</li>
 *         <li>weekday/weekend differentiation</li>
 *         <li>gaussian noise</li>
 *         <li>occasional spikes on ~3% of fillers (anomaly bait)</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private static final int HISTORY_DAYS = 90;
    private static final long ANOMALY_SEED_FRACTION_DIVISOR = 30L;
    private static final double NOISE_STDDEV = 4.0;

    private final PoolOperatorRepository poolOperatorRepository;
    private final FillerRepository fillerRepository;
    private final DepotRepository depotRepository;
    private final FillerStockRepository fillerStockRepository;
    private final StockMovementHistoryRepository movementHistoryRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${app.seed.filler-count:250}")
    private int fillerCount;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private final Random random = new Random(42L);

    @PostConstruct
    @Transactional
    public void seedData() {
        if (!seedEnabled) {
            log.info("⏭️ Data seeding disabled via app.seed.enabled=false");
            return;
        }
        if (poolOperatorRepository.count() > 0) {
            log.info("✅ Data already exists, skipping synthetic data generation");
            return;
        }

        log.info("🌱 Starting synthetic data generation...");

        PoolOperator poolOperator = createPoolOperator();
        log.info("✅ Created PoolOperator: {}", poolOperator.getCompanyName());

        Depot depot = createDepot(poolOperator.getId());
        log.info("✅ Created Depot: {}", depot.getName());

        List<VehicleType> vehicleTypes = createVehicleTypes(poolOperator.getId());
        log.info("✅ Created {} vehicle types", vehicleTypes.size());

        List<Vehicle> vehicles = createVehicles(depot.getId(), vehicleTypes);
        log.info("✅ Created {} vehicles", vehicles.size());

        List<Filler> fillers = createFillers(poolOperator.getId(), fillerCount);
        log.info("✅ Created {} fillers", fillers.size());

        SeedingStats stats = createFillerStocksWithHistory(fillers);
        log.info("✅ Created {} stock records and {} historical movements ({} anomaly-flagged fillers)",
                stats.stocks, stats.movements, stats.anomalyFlagged);

        log.info("🎉 Synthetic data generation completed successfully!");
    }

    private PoolOperator createPoolOperator() {
        PoolOperator poolOperator = PoolOperator.register(
                "Havuz A.Ş.",
                new TaxId("1234567890"),
                new ContactInfo("05321234567", "info@havuz.com.tr", "Ahmet Yılmaz")
        );
        return poolOperatorRepository.save(poolOperator);
    }

    private Depot createDepot(Long poolOperatorId) {
        Depot depot = Depot.create(
                poolOperatorId,
                "Gemlik Merkez Depo",
                new Address("Atatürk Caddesi No:123", "Gemlik", "Bursa", "16600", "Türkiye"),
                new GeoCoordinates(40.4333, 29.1667)
        );
        return depotRepository.save(depot);
    }

    private List<VehicleType> createVehicleTypes(Long poolOperatorId) {
        List<VehicleType> types = new ArrayList<>();
        types.add(vehicleTypeRepository.save(VehicleType.create(
                poolOperatorId, "Küçük Kamyon", "7.5 tonluk kamyon", new Capacity(500, 500))));
        types.add(vehicleTypeRepository.save(VehicleType.create(
                poolOperatorId, "Orta Kamyon", "12 tonluk kamyon", new Capacity(1000, 1000))));
        types.add(vehicleTypeRepository.save(VehicleType.create(
                poolOperatorId, "Büyük Kamyon", "18 tonluk kamyon", new Capacity(1500, 1500))));
        return types;
    }

    private List<Vehicle> createVehicles(Long depotId, List<VehicleType> vehicleTypes) {
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VehicleType type = vehicleTypes.get(random.nextInt(vehicleTypes.size()));
            Vehicle vehicle = Vehicle.register(
                    depotId, type.getId(), String.format("16 ABC %03d", i + 1));
            vehicles.add(vehicleRepository.save(vehicle));
        }
        return vehicles;
    }

    private List<Filler> createFillers(Long poolOperatorId, int count) {
        List<Filler> fillers = new ArrayList<>();
        String[] cities = {
                "İstanbul", "Ankara", "İzmir", "Bursa", "Antalya",
                "Adana", "Konya", "Gaziantep", "Kayseri", "Mersin",
                "Eskişehir", "Diyarbakır", "Samsun", "Denizli", "Şanlıurfa",
                "Adapazarı", "Malatya", "Kahramanmaraş", "Erzurum", "Van"
        };
        String[] districts = {"Merkez", "Organize Sanayi", "Küçük Sanayi", "Yeni Mahalle", "Kıyı Mahalle"};

        for (int i = 1; i <= count; i++) {
            String city = cities[random.nextInt(cities.length)];
            String district = districts[random.nextInt(districts.length)];
            double latitude = 36.0 + (random.nextDouble() * 6.0);
            double longitude = 26.0 + (random.nextDouble() * 19.0);

            Filler filler = Filler.register(
                    poolOperatorId,
                    String.format("Dolumcu %d A.Ş.", i),
                    new Address(
                            String.format("Sanayi Caddesi No:%d", random.nextInt(500) + 1),
                            district, city,
                            String.format("%05d", random.nextInt(90000) + 10000),
                            "Türkiye"),
                    new GeoCoordinates(latitude, longitude),
                    new ContactInfo(
                            String.format("053%08d", random.nextInt(100000000)),
                            String.format("info@dolumcu%d.com", i),
                            String.format("Yetkili %d", i)),
                    new TaxId(String.format("%010d", 1000000000L + i))
            );
            fillers.add(fillerRepository.save(filler));
        }
        return fillers;
    }

    private SeedingStats createFillerStocksWithHistory(List<Filler> fillers) {
        SeedingStats stats = new SeedingStats();

        for (Filler filler : fillers) {
            boolean willHaveAnomaly = random.nextLong(ANOMALY_SEED_FRACTION_DIVISOR) == 0L;
            if (willHaveAnomaly) stats.anomalyFlagged++;

            stats.add(seedFillerAsset(filler.getId(), AssetType.PALLET,
                    /*baseInflow*/ 12, /*lossPct*/ 5.0 + (random.nextDouble() * 10.0), willHaveAnomaly));
            stats.add(seedFillerAsset(filler.getId(), AssetType.SEPARATOR,
                    /*baseInflow*/ 8, /*lossPct*/ 3.0 + (random.nextDouble() * 8.0), willHaveAnomaly));
        }
        return stats;
    }

    private SeedingStats seedFillerAsset(Long fillerId, AssetType assetType,
                                          int baseDailyInflow, double lossPct, boolean withAnomaly) {
        SeedingStats stats = new SeedingStats();

        FillerStock stock = FillerStock.initialize(fillerId, assetType, 100, new LossRate(lossPct));
        fillerStockRepository.save(stock);
        stats.stocks++;

        LocalDate startDate = LocalDate.now().minusDays(HISTORY_DAYS);
        int runningQty = 0;
        int daysSinceLastCollection = 0;
        int collectionCadenceDays = 10 + random.nextInt(6); // every 10-15 days

        // Spike day for anomaly-flagged fillers (somewhere in last 30 days)
        int spikeDay = withAnomaly ? HISTORY_DAYS - 1 - random.nextInt(30) : -1;

        for (int day = 0; day < HISTORY_DAYS; day++) {
            LocalDate date = startDate.plusDays(day);

            int inflowQty = computeDailyInflow(date, baseDailyInflow);
            if (day == spikeDay) {
                inflowQty = (int) Math.round(inflowQty * 3.5); // ~3.5x normal — clearly anomalous
            }

            if (inflowQty > 0) {
                runningQty += inflowQty;
                StockMovementHistory inflow = StockMovementHistory.inflow(
                        fillerId, assetType, inflowQty, runningQty,
                        "SEED-INF",
                        date.atTime(LocalTime.of(8 + random.nextInt(8), random.nextInt(60))));
                movementHistoryRepository.save(inflow);
                stats.movements++;
            }

            daysSinceLastCollection++;
            boolean collectToday = daysSinceLastCollection >= collectionCadenceDays
                    && runningQty > 40;
            if (collectToday) {
                int loss = (int) Math.round(runningQty * (lossPct / 100.0));
                int collectedQty = Math.max(1, runningQty - loss);
                collectedQty = Math.min(collectedQty, runningQty);
                runningQty -= collectedQty;

                StockMovementHistory coll = StockMovementHistory.collection(
                        fillerId, assetType, collectedQty, runningQty,
                        "SEED-PLAN",
                        date.atTime(LocalTime.of(14 + random.nextInt(4), random.nextInt(60))));
                movementHistoryRepository.save(coll);
                stats.movements++;
                daysSinceLastCollection = 0;
            }
        }

        // Sync FillerStock.currentQuantity to match the simulated running total
        if (runningQty > 0) {
            stock.recordInflow(runningQty, "SEED-FINAL-SYNC");
            fillerStockRepository.save(stock);
        }
        stock.clearDomainEvents();

        return stats;
    }

    private int computeDailyInflow(LocalDate date, int baseInflow) {
        double seasonal = seasonalMultiplier(date.getMonthValue());
        double weekday = weekdayMultiplier(date.getDayOfWeek());
        double noise = random.nextGaussian() * NOISE_STDDEV;
        double daily = baseInflow * seasonal * weekday + noise;
        // ~30% of days have no inflow (idle days)
        if (random.nextDouble() < 0.3) {
            return 0;
        }
        return Math.max(0, (int) Math.round(daily));
    }

    /**
     * Beverage industry: peak Jun-Aug, dip Dec-Feb. Range ~[0.6, 1.5].
     */
    private double seasonalMultiplier(int month) {
        return switch (month) {
            case 6, 7, 8 -> 1.5;       // summer peak
            case 5, 9 -> 1.25;          // shoulder
            case 4, 10 -> 1.05;
            case 3, 11 -> 0.9;
            case 1, 2, 12 -> 0.65;     // winter low
            default -> 1.0;
        };
    }

    /**
     * Weekday/weekend differentiation. Weekends are lighter.
     */
    private double weekdayMultiplier(DayOfWeek dow) {
        return switch (dow) {
            case SATURDAY -> 0.6;
            case SUNDAY -> 0.3;
            default -> 1.0;
        };
    }

    private static class SeedingStats {
        int stocks = 0;
        int movements = 0;
        int anomalyFlagged = 0;

        void add(SeedingStats other) {
            stocks += other.stocks;
            movements += other.movements;
        }
    }
}
