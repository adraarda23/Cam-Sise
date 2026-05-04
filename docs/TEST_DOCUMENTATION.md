# Test Documentation

## Overview

| Category        | Count |
|-----------------|-------|
| Unit tests      | ~95   |
| Integration tests | ~15 |
| Smoke tests     | 2     |
| **Total**       | **~112** |

Run all tests:
```
mvn test
```

Generate JaCoCo coverage report:
```
mvn test
open target/site/jacoco/index.html
```

---

## Test Structure

### Smoke / Context

| Class | What it verifies |
|-------|-----------------|
| `CamSiseApplicationTests` | Spring context loads |
| `SmokeTest` | Context loads and beans are registered |

---

### Domain Layer (Unit)

These tests exercise aggregate business logic in isolation — no Spring context, no mocks.

#### `PoolOperatorTest`
| Test | Scenario |
|------|----------|
| `shouldRegisterNewPoolOperator` | register() sets correct fields and publishes PoolOperatorRegistered |
| `shouldActivatePoolOperator` | activate() flips active=true and publishes PoolOperatorActivated |
| `shouldDeactivatePoolOperator` | deactivate() flips active=false and publishes PoolOperatorDeactivated |
| `shouldUpdateContactInfo` | updateContactInfo() replaces phone/email/contactPersonName |
| `shouldThrowExceptionWhenActivatingAlreadyActivePoolOperator` | activate() on active operator throws IllegalStateException |
| `shouldThrowExceptionWhenDeactivatingAlreadyInactivePoolOperator` | deactivate() on inactive operator throws IllegalStateException |
| `shouldPublishPoolOperatorRegisteredEvent` | event payload contains correct companyName and taxId |

#### `FillerStockTest`
| Test | Scenario |
|------|----------|
| `shouldInitializeStock` | initialize() sets fillerId, assetType, threshold, zero currentQuantity |
| `shouldRecordInflow` | recordInflow() increases quantity and publishes AssetInflowRecorded |
| `shouldRecordCollection` | recordCollection() decreases quantity and publishes AssetCollected |
| `shouldPublishStockThresholdExceededEvent` | inflow above threshold publishes both AssetInflowRecorded + StockThresholdExceeded |
| `shouldNotPublishThresholdEventWhenBelowThreshold` | inflow below threshold publishes only AssetInflowRecorded |
| `shouldUpdateThreshold` | updateThreshold() changes thresholdQuantity |
| `shouldUpdateLossRate` | updateEstimatedLossRate() changes lossRate |
| `shouldThrowExceptionWhenCollectionExceedsStock` | collect > current throws IllegalArgumentException |
| `shouldThrowExceptionForNegativeInflow` | negative inflow throws IllegalArgumentException |
| `shouldThrowExceptionForNegativeCollection` | negative collection throws IllegalArgumentException |

#### `CollectionRequestTest`
Tests all state transitions of the CollectionRequest aggregate.

| Nested class | Transitions tested |
|---|---|
| `CreateAutomatic` | creates with PENDING + AUTO_THRESHOLD, publishes CollectionRequestCreated |
| `CreateManual` | creates with PENDING + MANUAL_CUSTOMER, publishes CollectionRequestCreated |
| `Approve` | PENDING→APPROVED, sets approvedByUserId; throws on APPROVED/CANCELLED/COMPLETED |
| `Reject` | PENDING→REJECTED, sets rejectionReason; throws on APPROVED/already-REJECTED |
| `Cancel` | PENDING/APPROVED/SCHEDULED→CANCELLED; throws on COMPLETED/already-CANCELLED |
| `Schedule` | APPROVED→SCHEDULED, sets collectionPlanId; throws on PENDING/COMPLETED |
| `Complete` | SCHEDULED→COMPLETED; throws on PENDING/APPROVED |
| `UpdateQuantity` | updates quantity when PENDING; throws on zero/negative/non-PENDING |

#### `CollectionPlanTest`
Tests all state transitions of the CollectionPlan aggregate.

| Nested class | Transitions tested |
|---|---|
| `Generate` | creates with GENERATED + correct fields, publishes CollectionPlanGenerated |
| `AssignVehicle` | GENERATED→ASSIGNED, sets vehicleId; throws on already-ASSIGNED/COMPLETED |
| `Start` | ASSIGNED→IN_PROGRESS, publishes CollectionStarted; throws on GENERATED/COMPLETED |
| `Complete` | IN_PROGRESS→COMPLETED; throws on GENERATED/ASSIGNED |
| `Cancel` | GENERATED/ASSIGNED→CANCELLED; throws on COMPLETED/already-CANCELLED |

#### `VehicleTest`
Tests all Vehicle aggregate state transitions.

| Nested class | Transitions tested |
|---|---|
| `Register` | creates with AVAILABLE status, publishes VehicleRegistered |
| `AssignToPlan` | AVAILABLE→ON_ROUTE, sets planId; throws when not AVAILABLE |
| `AssignToRoute` | AVAILABLE→ON_ROUTE with DriverInfo; throws when not AVAILABLE |
| `DepartFromDepot` | publishes departure event from ON_ROUTE; throws when not ON_ROUTE |
| `ReturnToDepot` | ON_ROUTE→AVAILABLE, clears driver+plan, publishes VehicleReturnedToDepot |
| `ChangeStatus` | AVAILABLE→MAINTENANCE/INACTIVE; ON_ROUTE→MAINTENANCE clears plan; throws ON_ROUTE→AVAILABLE and to ON_ROUTE directly |

---

### Application/Service Layer (Unit)

These tests use Mockito to isolate service logic from repositories.

#### `PoolOperatorServiceTest`
| Test | Scenario |
|------|----------|
| `shouldRegisterNewPoolOperator` | delegates to domain, saves, returns result |
| `shouldThrowExceptionWhenTaxIdExists` | existsByTaxId=true → IllegalArgumentException, no save |
| `shouldActivatePoolOperator` | loads, activates, saves |
| `shouldDeactivatePoolOperator` | loads, deactivates, saves |
| `shouldUpdateContactInfo` | loads, updates, saves |
| `shouldFindPoolOperatorById` | delegates to repository findById |
| `shouldThrowExceptionWhenPoolOperatorNotFound` | empty Optional → IllegalArgumentException |

#### `FillerServiceTest`
| Nested class | Coverage |
|---|---|
| `RegisterFiller` | creates filler; duplicate taxId throws; null taxId skips existence check |
| `ActivateFiller` | activates and saves; throws when not found |
| `DeactivateFiller` | deactivates and saves; throws when not found |
| `UpdateFiller` | updates all fields and saves; throws when not found |
| `FindById` | returns filler; throws when not found |
| `FindByPoolOperator` | returns all; filters by active=true |

#### `FillerStockServiceTest`
| Nested class | Coverage |
|---|---|
| `GetStock` | returns stock by fillerId+assetType; throws when not found |
| `RecordInflow` | increases quantity and saves |
| `RecordCollection` | decreases quantity and saves; throws when exceeds stock |
| `UpdateThreshold` | updates threshold and saves |
| `GetStocksByFiller` | returns all stocks for filler; empty list |

#### `CollectionRequestServiceTest`
| Nested class | Coverage |
|---|---|
| `CreateManual` | below-minimum throws; exceeds-stock throws; creates new request; merges with existing PENDING; merge-exceeds-stock throws; accounts for APPROVED requests in available stock calculation |
| `CreateAutomatic` | creates PENDING request |
| `Approve` | approves PENDING request; throws when not found |
| `Reject` | rejects with reason; throws when not found |
| `Cancel` | cancels request; throws when not found |
| `Schedule` | schedules APPROVED request with planId |
| `Complete` | completes SCHEDULED request |

#### `CollectionPlanServiceTest`
| Nested class | Coverage |
|---|---|
| `GeneratePlan` | saves plan with GENERATED status |
| `AssignVehicle` | transitions to ASSIGNED, calls vehicleService.assignToPlan |
| `StartCollection` | transitions ASSIGNED→IN_PROGRESS |
| `CompleteCollection` | transitions IN_PROGRESS→COMPLETED, completes associated requests, returns vehicle |
| `CancelPlan` | GENERATED→CANCELLED, cancels associated requests; ASSIGNED→CANCELLED returns vehicle to depot |

#### `VehicleServiceTest`
| Nested class | Coverage |
|---|---|
| `RegisterVehicle` | saves vehicle; duplicate plate throws |
| `AssignToPlan` | transitions to ON_ROUTE and saves |
| `AssignToRoute` | transitions to ON_ROUTE with driver info |
| `DepartFromDepot` | saves vehicle |
| `ReturnToDepot` | transitions to AVAILABLE |
| `ChangeStatus` | normal change does not touch plans; ON_ROUTE→MAINTENANCE cascades plan/request cancellation |

#### `DepotServiceTest`
| Nested class | Coverage |
|---|---|
| `CreateDepot` | saves depot with active=true |
| `AddVehicle` | adds vehicleId; throws on duplicate; throws when depot not found |
| `RemoveVehicle` | removes vehicleId; throws when depot not found |
| `FindById` | returns depot; throws when not found |
| `FindByPoolOperator` | returns all; filters by active=true |

#### `CompanySettingsServiceTest`
| Nested class | Coverage |
|---|---|
| `GetSettings` | returns existing settings; auto-creates defaults when missing; getMinQty dispatches correctly for PALLET and SEPARATOR |
| `UpdateSettings` | updates both quantities; auto-creates then updates when settings do not exist |

#### `AnalyticsServiceTest`
| Nested class | Coverage |
|---|---|
| `RequestStats` | zero counts on empty; counts by asset type (PALLET vs SEPARATOR); groups by status |
| `PlanStats` | zero stats on empty; calculates average distance and duration |
| `StockStats` | sums pallet and separator stocks separately; counts fillers with stock above threshold |

---

### Integration Layer

These tests use `@SpringBootTest` with H2 in-memory database (`@ActiveProfiles("test")`).

#### `CollectionRequestLifecycleIntegrationTest`
Full end-to-end tests against a real database using `@Transactional` (rolls back after each test).

| Nested class | Tests |
|---|---|
| `FullLifecycle` | PENDING→APPROVED→SCHEDULED→COMPLETED persisted; cancel from PENDING; reject from PENDING |
| `MinimumQuantity` | below default minimum (20) rejected; exactly at minimum accepted |
| `StockAvailability` | exceeds available stock rejected; approved requests reduce available stock for subsequent requests |
| `RequestMerging` | second PENDING request merges quantity into first; merge rejected when total exceeds stock |

#### `RouteOptimizationIntegrationTest`
End-to-end CVRP route generation tests. Uses Gemlik depot coordinates and nearby fillers.

| Test | What it verifies |
|---|---|
| `testMultiVehicleOptimization_WithApprovedRequests` | plans generated, ≤5 vehicles, all within 800km/600min constraints |
| `testDistanceConstraintEnforcement` | each plan ≤ 800km |
| `testDurationConstraintEnforcement` | each plan ≤ 600 minutes |
| `testMultiStopRouteGeneration` | fewer plans than requests (routes merged), at least one multi-stop route |
| `testCapacityConstraintEnforcement` | each plan ≤ vehicleType capacity, total capacity equals sum of all requests |
| `testNoApprovedRequests_ThrowsException` | no approved requests → IllegalStateException |
| `testCustomRequestOptimization` | generatePlanForRequests() includes only selected filler IDs |
| `testPlanPersistence` | plans have IDs and are retrievable from DB |
| `testMixedAssetTypes` | plans contain both pallet and separator quantities |
| `testSingleApprovedRequest` | exactly one plan with correct capacity |
| `testRealisticDistanceCalculation` | Haversine distances > 0 and < 200km for nearby fillers |

---

## Coverage Gaps

The following areas have no test coverage:

| Module / Class | Gap |
|---|---|
| `AuthService` | No login/authentication tests |
| `AuthController` | No controller tests |
| `ChatService` / `ChatController` | No AI chat module tests |
| `DomainEventStoreHandler` | No audit logging handler tests |
| REST Controllers (all modules) | No HTTP-layer tests (no `@WebMvcTest` / `MockMvc`) |
| Flyway migrations | Not tested (relies on integration tests hitting H2) |

---

## Configuration

| Profile | Database | Elasticsearch |
|---|---|---|
| default | PostgreSQL | Elastic Cloud |
| `test` | H2 in-memory | Disabled (autoconfiguration excluded) |

Test-specific config: `src/test/resources/application-test.properties`

Coverage report output: `target/site/jacoco/index.html` (generated by `mvn test`)
