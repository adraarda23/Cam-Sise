package ardaaydinkilinc.Cam_Sise.shared.config;

import ardaaydinkilinc.Cam_Sise.core.domain.Filler;
import ardaaydinkilinc.Cam_Sise.core.domain.PoolOperator;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import ardaaydinkilinc.Cam_Sise.core.repository.PoolOperatorRepository;
import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Depot;
import ardaaydinkilinc.Cam_Sise.logistics.domain.Vehicle;
import ardaaydinkilinc.Cam_Sise.logistics.domain.VehicleType;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.Capacity;
import ardaaydinkilinc.Cam_Sise.logistics.domain.vo.DriverInfo;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Synthetic data generator for testing and development.
 * Creates realistic test data including:
 * - 1 Pool Operator (tenant)
 * - 1 Depot in Gemlik
 * - 250 Fillers across Turkey
 * - Stock records for each filler
 * - Vehicle types and vehicles
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final PoolOperatorRepository poolOperatorRepository;
    private final FillerRepository fillerRepository;
    private final DepotRepository depotRepository;
    private final FillerStockRepository fillerStockRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;

    private final Random random = new Random();

    @PostConstruct
    @Transactional
    public void seedData() {
        // Check if data already exists
        if (poolOperatorRepository.count() > 0) {
            log.info("✅ Data already exists, skipping synthetic data generation");
            return;
        }

        log.info("🌱 Starting synthetic data generation...");

        // 1. Create Pool Operator
        PoolOperator poolOperator = createPoolOperator();
        log.info("✅ Created PoolOperator: {}", poolOperator.getCompanyName());

        // 2. Create Depot
        Depot depot = createDepot(poolOperator.getId());
        log.info("✅ Created Depot: {}", depot.getName());

        // 3. Create Vehicle Types
        List<VehicleType> vehicleTypes = createVehicleTypes(poolOperator.getId());
        log.info("✅ Created {} vehicle types", vehicleTypes.size());

        // 4. Create Vehicles
        List<Vehicle> vehicles = createVehicles(depot.getId(), vehicleTypes);
        log.info("✅ Created {} vehicles", vehicles.size());

        // 5. Create Fillers
        List<Filler> fillers = createFillers(poolOperator.getId(), 250);
        log.info("✅ Created {} fillers", fillers.size());

        // 6. Create Filler Stocks
        int stockCount = createFillerStocks(fillers);
        log.info("✅ Created {} stock records", stockCount);

        log.info("🎉 Synthetic data generation completed successfully!");
    }

    private PoolOperator createPoolOperator() {
        PoolOperator poolOperator = PoolOperator.register(
                "Havuz A.Ş.",
                new TaxId("1234567890"),
                new ContactInfo(
                        "05321234567",
                        "info@havuz.com.tr",
                        "Ahmet Yılmaz"
                )
        );
        return poolOperatorRepository.save(poolOperator);
    }

    private Depot createDepot(Long poolOperatorId) {
        Depot depot = Depot.create(
                poolOperatorId,
                "Gemlik Merkez Depo",
                new Address(
                        "Atatürk Caddesi No:123",
                        "Gemlik",
                        "Bursa",
                        "16600",
                        "Türkiye"
                ),
                new GeoCoordinates(40.4333, 29.1667)
        );
        return depotRepository.save(depot);
    }

    private List<VehicleType> createVehicleTypes(Long poolOperatorId) {
        List<VehicleType> types = new ArrayList<>();

        VehicleType small = VehicleType.create(
                poolOperatorId,
                "Küçük Kamyon",
                "7.5 tonluk kamyon",
                new Capacity(500, 500) // pallets, separators
        );
        types.add(vehicleTypeRepository.save(small));

        VehicleType medium = VehicleType.create(
                poolOperatorId,
                "Orta Kamyon",
                "12 tonluk kamyon",
                new Capacity(1000, 1000)
        );
        types.add(vehicleTypeRepository.save(medium));

        VehicleType large = VehicleType.create(
                poolOperatorId,
                "Büyük Kamyon",
                "18 tonluk kamyon",
                new Capacity(1500, 1500)
        );
        types.add(vehicleTypeRepository.save(large));

        return types;
    }

    private List<Vehicle> createVehicles(Long depotId, List<VehicleType> vehicleTypes) {
        List<Vehicle> vehicles = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            VehicleType type = vehicleTypes.get(random.nextInt(vehicleTypes.size()));

            Vehicle vehicle = Vehicle.register(
                    depotId,
                    type.getId(),
                    String.format("16 ABC %03d", i + 1)
            );

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

        String[] districts = {
                "Merkez", "Organize Sanayi", "Küçük Sanayi", "Yeni Mahalle", "Kıyı Mahalle"
        };

        for (int i = 1; i <= count; i++) {
            String city = cities[random.nextInt(cities.length)];
            String district = districts[random.nextInt(districts.length)];

            // Random coordinates within Turkey (approx: 36-42 lat, 26-45 lon)
            double latitude = 36.0 + (random.nextDouble() * 6.0);
            double longitude = 26.0 + (random.nextDouble() * 19.0);

            Filler filler = Filler.register(
                    poolOperatorId,
                    String.format("Dolumcu %d A.Ş.", i),
                    new Address(
                            String.format("Sanayi Caddesi No:%d", random.nextInt(500) + 1),
                            district,
                            city,
                            String.format("%05d", random.nextInt(90000) + 10000),
                            "Türkiye"
                    ),
                    new GeoCoordinates(latitude, longitude),
                    new ContactInfo(
                            String.format("053%08d", random.nextInt(100000000)),
                            String.format("info@dolumcu%d.com", i),
                            String.format("Yetkili %d", i)
                    ),
                    new TaxId(String.format("%010d", 1000000000L + i))
            );

            fillers.add(fillerRepository.save(filler));
        }

        return fillers;
    }

    private int createFillerStocks(List<Filler> fillers) {
        int count = 0;

        for (Filler filler : fillers) {
            // Create PALLET stock
            FillerStock palletStock = FillerStock.initialize(
                    filler.getId(),
                    AssetType.PALLET,
                    100, // threshold
                    new LossRate(5.0 + (random.nextDouble() * 10.0)) // 5-15% loss rate
            );

            // Add some initial stock (0-200 units)
            int palletQuantity = random.nextInt(201);
            if (palletQuantity > 0) {
                palletStock.recordInflow(palletQuantity, "INITIAL_STOCK");
            }

            fillerStockRepository.save(palletStock);
            count++;

            // Create SEPARATOR stock
            FillerStock separatorStock = FillerStock.initialize(
                    filler.getId(),
                    AssetType.SEPARATOR,
                    100, // threshold
                    new LossRate(5.0 + (random.nextDouble() * 10.0)) // 5-15% loss rate
            );

            // Add some initial stock (0-200 units)
            int separatorQuantity = random.nextInt(201);
            if (separatorQuantity > 0) {
                separatorStock.recordInflow(separatorQuantity, "INITIAL_STOCK");
            }

            fillerStockRepository.save(separatorStock);
            count++;
        }

        return count;
    }
}
