# Palet & Separatör Havuz Yönetim Sistemi

Cam şişe dolum sektöründe kullanılan **yeniden kullanılabilir palet ve separatörlerin** geri toplama sürecini yöneten, envanter takibi ve rota optimizasyonu sunan bir backend + panel uygulaması.

> Bursa Teknik Üniversitesi — Bilgisayar Mühendisliği Bitirme Tezi  
> Desteksiz (anonim şirket) proje — Cartonplast iş modeli referans alınmıştır.


---

## İş Döngüsü

```
Havuz Operatörü (Gemlik)
        │
        │  palet + separatör gönderir
        ▼
  Cam Üreticisi (5 adet)
        │
        │  cam ürünleri + palet + separatör ile sevk eder
        ▼
   Dolumcu / Filler (250 adet)
        │
        │  ürünler satılır, boş palet + separatör kalır
        ▼
Havuz Operatörü toplama rotasıyla geri alır (Gemlik'e döner)
```

Sistemin odağı döngünün **son bacağıdır**: dolumculardaki birikmiş asset'lerin takibi ve optimize edilmiş geri toplanması.

---

## Mimari

```
┌───────────────────────────────────────────────────────────────┐
│                    Modular Monolith                           │
│                                                               │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌─────────┐    │
│  │   Core   │  │ Inventory │  │ Logistics │  │  Auth   │    │
│  │  Module  │  │  Module   │  │  Module   │  │ Module  │    │
│  │          │  │           │  │           │  │         │    │
│  │PoolOp.   │  │FillerStock│  │VehicleType│  │  User   │    │
│  │Filler    │  │LossRecord │  │  Depot    │  │         │    │
│  │          │  │           │  │  Vehicle  │  │         │    │
│  │          │  │           │  │Collection │  │         │    │
│  │          │  │           │  │  Request  │  │         │    │
│  │          │  │           │  │Collection │  │         │    │
│  │          │  │           │  │   Plan    │  │         │    │
│  └────┬─────┘  └─────┬─────┘  └─────┬─────┘  └────┬────┘    │
│       │              │              │             │          │
│       └──────────────┴──────────────┴─────────────┘          │
│                      │                                       │
│                Domain Events Bus                             │
│                      │                                       │
│            ┌─────────▼─────────┐                             │
│            │  Event Sourcing   │                             │
│            │  (Audit Logging)  │                             │
│            └───────────────────┘                             │
└───────────────────────────────────────────────────────────────┘
                      │
                      ▼
              PostgreSQL (Supabase)
```

**Dış yapı:** Modular Monolith — her bounded context kendi modülünde
**İç yapı:** DDD-based Layered Architecture

**Katman organizasyonu:**
- `controller/` — REST Controllers (Presentation Layer)
- `service/` — Application Services & Use Cases (Application Layer)
- `domain/` — Aggregates, Entities, Value Objects, Domain Events (Domain Layer)
- `repository/` — Data Access (Infrastructure Layer)

**Tasarım prensibi:** Domain-Driven Design (DDD)
- **Tactical Patterns:** Aggregate Root, Entity, Value Object, Domain Event, Repository
- **Strategic Patterns:** Bounded Context, Ubiquitous Language, Modular structure
**Event Yönetimi:** Domain Events + Event Sourcing (Audit Logging)
**Multi-Tenancy:** Pool Operator (Tenant) bazlı izolasyon

---

## Domain Modeli (Ubiquitous Language)

| Terim | Açıklama |
|---|---|
| **Pool Operator (Havuz Operatörü)** | Sistemin sahibi firma (Tenant). Multi-tenancy için kullanılır. |
| **Depot (Depo)** | Depo/tesis. Araçların çıkış ve dönüş noktası. Bir operatörün birden fazla deposu olabilir. |
| **Glass Manufacturer (Cam Üreticisi)** | Operatörden palet alır, camı dolumcuya sevk eder. Aracı. 5 adet. |
| **Filler (Dolumcu)** | İçecek/gıda dolumu yapan fabrikalar (Coca-Cola, Pepsi vb.). 250 adet. |
| **Asset** | Takibi yapılan palet veya separatör. |
| **FillerStock** | Bir dolumcunun belirli bir asset tipi için stok durumu. |
| **Inflow** | Bir dolumcuya ulaşan asset kaydı (başlangıç bakiyesi). |
| **Loss Record (Zaiyat Kaydı)** | Dolumcudaki kayıp/hasar oranı. Girilmezse moving average ile tahmin edilir. |
| **Collection Request (Toplama Talebi)** | Dolumcudan toplama isteği. Otomatik (eşik tetikli) veya Manuel (dolumcudan). |
| **Collection Plan (Toplama Planı)** | CVRP optimizer çıktısı. Hangi aracın hangi sırayla hangi dolumcuları ziyaret ettiği. |
| **Vehicle (Araç)** | Toplama için kullanılan araç. Depoya bağlı, araç tipine sahip. |
| **VehicleType (Araç Tipi)** | Araç tipi katalogu. Admin tarafından tanımlanır, kapasite bilgisi içerir. |
| **Domain Event** | Aggregate'lerde gerçekleşen önemli olaylar. Modüller arası iletişim sağlar. |

---

## Kullanıcı Rolleri

| Rol | Açıklama |
|---|---|
| `ADMIN` | Kullanıcı CRUD, sistem ayarları, zaiyat oran varsayılanları. Tam yetki. |
| `COMPANY_STAFF` | Operasyon personeli. Tüm dolumcuları, asset'leri, talepleri görür. Rota oluşturur. |
| `CUSTOMER` | Dolumcu firmanın kullanıcısı. Sadece kendi stoku ve taleplerini görür. Manuel talep açar. |

Yetkilendirme: **JWT tabanlı**, rol odaklı, stateless.

---

## Dokümantasyon

📖 **[Detaylı Teknik Dokümantasyon](docs/PROJECT_DOCUMENTATION.md)**

Aggregate'ler, Value Object'ler, Domain Event'ler, API endpoint'leri ve daha fazlası için detaylı dokümantasyona bakınız.

**İçerik:**
- Domain Model (10 Aggregate Root detaylı açıklama)
- Domain Events (35+ event ve modüller arası iletişim)
- Value Objects (19 adet)
- DDD Kavramları
- Multi-Tenancy Mimarisi
- Event Sourcing & Audit Logging
- API Endpoint Referansı (gelecekte doldurulacak)
- Veritabanı Şeması

📖 **[CVRP Rota Optimizasyonu Dokümantasyonu](docs/)**

Çoklu araç rota optimizasyonu için detaylı dokümantasyon:
- **[CVRP Implementation Summary](docs/CVRP_IMPLEMENTATION_SUMMARY.md)** - Clarke-Wright algoritması detayları, test sonuçları
- **[Multi-Stop Route Example](docs/MULTI_STOP_ROUTE_EXAMPLE.md)** - Adım adım algoritma çalışması, görsel örnekler
- **[Input/Output Examples](docs/INPUT_OUTPUT_EXAMPLE.md)** - API kullanımı, request/response formatları

---

## Teknoloji Stack

| Katman | Teknoloji |
|---|---|
| Backend | Java 21 + Spring Boot |
| ORM | Spring Data JPA + Hibernate |
| Veritabanı | PostgreSQL (Supabase — cloud, shared) |
| Migration | Flyway |
| Güvenlik | Spring Security + JWT (jjwt) |
| Rota Optimizasyonu | Clarke-Wright Savings Algorithm (Custom Implementation) |
| | Multi-vehicle CVRP with constraints (800km, 10h) |
| API Dokümantasyonu | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Frontend | TBD |
| Harita | Leaflet + OpenStreetMap (TBD) |

---

## Kurulum

### Gereksinimler

- Java 21+
- Maven 3.8+
- Supabase hesabı (veya herhangi bir PostgreSQL instance)

### 1. Projeyi klonla

```bash
git clone https://github.com/KULLANICI_ADIN/bitirme-tezi.git
cd bitirme-tezi
```

### 2. Ortam değişkenlerini ayarla

`src/main/resources/application.properties` dosyasını oluştur (`.gitignore`'da olmalı):

```properties
spring.datasource.url=jdbc:postgresql://HOST:5432/postgres
spring.datasource.username=KULLANICI_ADI
spring.datasource.password=SIFRE

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Referans için `application-example.properties` dosyası repoda mevcuttur.

### 3. Uygulamayı başlat

```bash
mvn spring-boot:run
```

### 4. Smoke test

```
GET http://localhost:8080/actuator/health
→ {"status":"UP"}
```

### 5. API dokümantasyonu

```
http://localhost:8080/swagger-ui/index.html
```

---

## Modüller

### Core Module
Tenant (PoolOperator) ve müşteri (Filler) yönetimi.

**Aggregate Roots:**
- `PoolOperator` - Sistemin sahibi firma (tenant)
- `Filler` - Dolumcu firma bilgileri

**Sorumluluklar:**
- Multi-tenancy yönetimi
- Firma ve dolumcu CRUD işlemleri
- Lokasyon ve iletişim bilgileri

---

### Inventory Module
Dolumculardaki asset bakiyelerinin takibi.

**Aggregate Roots:**
- `FillerStock` - Stok durumu ve hareketleri
- `LossRecord` - Zaiyat/kayıp oranları

**Sorumluluklar:**
- Asset inflow / collection hareketleri
- Dolumcu bazında anlık stok hesabı
- Moving average tabanlı zaiyat tahmini
- Eşik değeri kontrolü (→ otomatik toplama talebi)
- Raporlama endpoint'leri
- Rol bazlı veri filtresi

---

### Logistics Module
Geri toplama planlaması ve rota optimizasyonu.

**Aggregate Roots:**
- `VehicleType` - Araç tipi katalogu
- `Depot` - Depo yönetimi
- `Vehicle` - Araç kaydı ve durum takibi
- `CollectionRequest` - Toplama talepleri
- `CollectionPlan` - Rota planları

**Sorumluluklar:**
- Unified Collection Pool (otomatik + manuel talepler)
- CVRP tabanlı rota optimizasyonu
- Haversine mesafe matrisi
- Araç ve depo yönetimi
- Talep onay/red akışı
- Toplama planı oluşturma ve takibi

**CVRP Route Optimization:**
- **Algorithm:** Clarke-Wright Savings Algorithm (custom implementation)
- **Features:**
  - Multi-vehicle routing with realistic constraints
  - Geographic clustering (prevents unrealistic routes like Adana→İzmir→Urfa)
  - Constraint enforcement: 800km max distance, 10 hours max duration, 30 min service time per stop
  - 2-opt local search optimization
  - Automatic route rejection for infeasible routes
  - Unassigned request tracking
- **API Endpoint:** `POST /api/logistics/optimize/multi-vehicle`
- **Documentation:** See `docs/CVRP_*.md` files

---

### Auth Module
Kimlik doğrulama ve yetkilendirme.

**Aggregate Roots:**
- `User` - Kullanıcı bilgileri ve rol yönetimi

**Sorumluluklar:**
- JWT tabanlı authentication
- Rol bazlı authorization (ADMIN, COMPANY_STAFF, CUSTOMER)
- Kullanıcı CRUD işlemleri
- Tenant bazlı veri izolasyonu

---

## Proje Yapısı

```
src/
├── main/
│   ├── java/ardaaydinkilinc/Cam_Sise/
│   │   ├── shared/
│   │   │   ├── domain/
│   │   │   │   ├── base/          # DDD base sınıfları
│   │   │   │   │   ├── AggregateRoot.java
│   │   │   │   │   ├── Entity.java
│   │   │   │   │   └── ValueObject.java
│   │   │   │   ├── vo/            # Paylaşılan Value Object'ler
│   │   │   │   │   ├── Address.java
│   │   │   │   │   ├── GeoCoordinates.java
│   │   │   │   │   ├── Distance.java
│   │   │   │   │   ├── Duration.java
│   │   │   │   │   ├── ContactInfo.java
│   │   │   │   │   ├── TaxId.java
│   │   │   │   │   └── Money.java
│   │   │   │   └── event/         # Event altyapısı
│   │   │   │       ├── DomainEvent.java
│   │   │   │       ├── DomainEventPublisher.java
│   │   │   │       ├── DomainEventStore.java
│   │   │   │       ├── DomainEventStoreRepository.java
│   │   │   │       └── DomainEventListener.java
│   │   │   └── config/
│   │   │       └── AsyncConfig.java
│   │   │
│   │   ├── core/
│   │   │   ├── controller/       # REST Controllers
│   │   │   │   ├── PoolOperatorController.java
│   │   │   │   └── FillerController.java
│   │   │   ├── service/          # Application Services
│   │   │   │   ├── service/
│   │   │   │   │   ├── PoolOperatorService.java
│   │   │   │   │   └── FillerService.java
│   │   │   │   └── event/
│   │   │   │       └── CoreEventHandler.java
│   │   │   ├── domain/
│   │   │   │   ├── PoolOperator.java
│   │   │   │   ├── Filler.java
│   │   │   │   └── event/         # Core events
│   │   │   └── repository/
│   │   │       ├── PoolOperatorRepository.java
│   │   │       └── FillerRepository.java
│   │   │
│   │   ├── inventory/
│   │   │   ├── controller/
│   │   │   │   ├── FillerStockController.java
│   │   │   │   └── LossRecordController.java
│   │   │   ├── service/
│   │   │   │   ├── service/
│   │   │   │   │   ├── FillerStockService.java
│   │   │   │   │   └── LossRecordService.java
│   │   │   │   └── event/
│   │   │   │       └── InventoryEventHandler.java
│   │   │   ├── domain/
│   │   │   │   ├── FillerStock.java
│   │   │   │   ├── LossRecord.java
│   │   │   │   ├── vo/            # Inventory Value Objects
│   │   │   │   │   ├── AssetType.java
│   │   │   │   │   ├── LossRate.java
│   │   │   │   │   ├── StockMovement.java
│   │   │   │   │   └── Period.java
│   │   │   │   └── event/         # Inventory events
│   │   │   └── repository/
│   │   │       ├── FillerStockRepository.java
│   │   │       └── LossRecordRepository.java
│   │   │
│   │   ├── logistics/
│   │   │   ├── controller/
│   │   │   │   ├── VehicleTypeController.java
│   │   │   │   ├── DepotController.java
│   │   │   │   ├── VehicleController.java
│   │   │   │   ├── CollectionRequestController.java
│   │   │   │   └── CollectionPlanController.java
│   │   │   ├── service/
│   │   │   │   ├── service/
│   │   │   │   │   ├── VehicleTypeService.java
│   │   │   │   │   ├── DepotService.java
│   │   │   │   │   ├── VehicleService.java
│   │   │   │   │   ├── CollectionRequestService.java
│   │   │   │   │   ├── CollectionPlanService.java
│   │   │   │   │   ├── RouteOptimizationService.java
│   │   │   │   │   ├── CVRPOptimizer.java
│   │   │   │   │   ├── RouteConstraints.java
│   │   │   │   │   └── DistanceCalculator.java
│   │   │   │   └── event/
│   │   │   │       └── LogisticsEventHandler.java
│   │   │   ├── domain/
│   │   │   │   ├── VehicleType.java
│   │   │   │   ├── Depot.java
│   │   │   │   ├── Vehicle.java
│   │   │   │   ├── CollectionRequest.java
│   │   │   │   ├── CollectionPlan.java
│   │   │   │   ├── vo/            # Logistics Value Objects
│   │   │   │   │   ├── Capacity.java
│   │   │   │   │   ├── RouteStop.java
│   │   │   │   │   ├── DriverInfo.java
│   │   │   │   │   └── [enums]
│   │   │   │   └── event/         # Logistics events
│   │   │   └── repository/
│   │   │       ├── VehicleTypeRepository.java
│   │   │       ├── DepotRepository.java
│   │   │       ├── VehicleRepository.java
│   │   │       ├── CollectionRequestRepository.java
│   │   │       └── CollectionPlanRepository.java
│   │   │
│   │   └── auth/
│   │       ├── controller/
│   │       │   └── TestEventController.java
│   │       ├── service/
│   │       │   ├── service/
│   │       │   │   └── UserService.java
│   │       │   └── event/
│   │       │       └── UserEventHandler.java
│   │       ├── domain/
│   │       │   ├── User.java
│   │       │   ├── Role.java
│   │       │   └── event/         # Auth events
│   │       ├── repository/
│   │       │   └── UserRepository.java
│   │       ├── config/
│   │       │   ├── SecurityConfig.java
│   │       │   └── DataInitializer.java
│   │       ├── filter/
│   │       │   └── JwtAuthenticationFilter.java
│   │       └── dto/
│   │
│   └── resources/
│       ├── db/migration/
│       │   ├── V1__initial_schema.sql
│       │   ├── V2__auth_tables.sql
│       │   └── V3__create_domain_model_tables.sql
│       └── application.properties
│
├── docs/
│   ├── PROJECT_DOCUMENTATION.md          # Detaylı teknik dokümantasyon
│   ├── CVRP_IMPLEMENTATION_SUMMARY.md    # Clarke-Wright algoritması özeti
│   ├── MULTI_STOP_ROUTE_EXAMPLE.md       # Adım adım rota örnekleri
│   ├── INPUT_OUTPUT_EXAMPLE.md           # API input/output formatları
│   └── KALAN_GOREVLER.md                 # TODO list ve roadmap
│
└── test/
```

---

## Roadmap

> Durum ikonları: ✅ Tamamlandı · 🔄 Devam ediyor · ⬜ Bekliyor

### Hafta 1 — Temel, Domain ve Auth İskeleti

- ✅ Proje iskeleti ve build ayarları
- ✅ Supabase bağlantısı ve Flyway migration yapısı
- ✅ Ubiquitous language sözlüğü (dokümantasyon)
- ✅ **DDD Base Infrastructure** (AggregateRoot, Entity, ValueObject, DomainEvent)
- ✅ **Domain Event Altyapısı** (Publisher, Store, Listener, Async)
- ✅ **Core Module Domain Model** (PoolOperator, Filler + 6 event)
- ✅ **Inventory Module Domain Model** (FillerStock, LossRecord + 5 event + 4 VO)
- ✅ **Logistics Module Domain Model** (VehicleType, Depot, Vehicle, CollectionRequest, CollectionPlan + 17 event + 8 VO)
- ✅ **Auth Module** (User aggregate güncellemesi + 2 event)
- ✅ **Shared Value Objects** (7 adet: Address, GeoCoordinates, Distance, Duration, ContactInfo, TaxId, Money)
- ✅ **Multi-Tenancy** (Pool Operator tenant yapısı)
- ✅ **Flyway Migration V3** (Tüm domain model tabloları)
- ✅ JWT auth iskeleti (login endpoint, token üretimi)
- ✅ 3 rollü erişim filtresi (ADMIN, COMPANY_STAFF, CUSTOMER)
- ✅ **Detaylı Teknik Dokümantasyon** (docs/PROJECT_DOCUMENTATION.md)
- ✅ **Validation & Exception Handling** (GlobalExceptionHandler, custom exceptions, Bean Validation)
- ✅ **Unit Tests** (JUnit 5 + Mockito + AssertJ - Domain, Service, 25 tests passing)
- ✅ **Sentetik veri üreticisi** (DataSeeder: 1 PoolOperator, 1 Depot, 250 Filler, 500 Stock, 3 VehicleType, 5 Vehicle)
- ✅ Smoke test — `GET /actuator/health` → UP

### Hafta 2 — Envanter Yönetimi Modülü

- ✅ Repository katmanı (FillerStockRepository, LossRecordRepository)
- ✅ Service katmanı (FillerStockService, LossRecordService)
- ✅ Hareket kayıt servisi (inflow / collection)
- ✅ Dolumcu bazında anlık stok hesaplama servisi
- ✅ Moving average tabanlı zaiyat tahmin servisi
- ✅ Event Handler (StockThresholdExceeded → CollectionRequest oluştur)
- ✅ Event Handler (CollectionCompleted → FillerStock güncelle)
- ✅ Raporlama REST endpoint'leri
- ✅ Rol bazlı veri filtresi (CUSTOMER sadece kendi verisini görür)
- ✅ Birim ve entegrasyon testleri
- ⬜ Swagger dokümantasyonu — inventory endpoint'leri

### Hafta 3 — Rota Optimizasyonu ve Unified Collection Pool

- ✅ Repository katmanı (VehicleTypeRepository, DepotRepository, VehicleRepository, CollectionRequestRepository, CollectionPlanRepository)
- ✅ Service katmanı (tüm logistics servisler)
- ✅ **Clarke-Wright Savings Algorithm implementasyonu** (custom implementation, no external dependencies)
- ✅ **Multi-vehicle CVRP optimizer** (CVRPOptimizer.java)
- ✅ **Route constraints configuration** (RouteConstraints.java: 800km, 10h, 30min service time)
- ✅ **2-opt local search optimization** for route improvement
- ✅ **RouteOptimizationService** - orchestrates CVRP with domain logic
- ✅ **RouteOptimizationController** - `POST /api/logistics/optimize/multi-vehicle` endpoint
- ✅ Haversine mesafe matrisi (DistanceCalculator.java)
- ✅ **Geographic clustering** (prevents unrealistic routes like Adana→İzmir→Urfa)
- ✅ **Constraint enforcement** (automatic route rejection for infeasible routes)
- ✅ **Comprehensive CVRP documentation** (3 detailed markdown files in docs/)
- ✅ Durum geçiş kontrolü (RequestStatus, PlanStatus, VehicleStatus)
- ✅ Otomatik talep üretimi (StockThresholdExceeded event handler)
- ✅ Manuel talep endpoint'i (CUSTOMER rolü)
- ✅ Talep onay/red endpoint'leri (COMPANY_STAFF rolü)
- ✅ Toplama planı oluşturma ve kalıcı saklama
- ✅ Event Handler (FillerRegistered → FillerStock oluştur)
- ✅ Test senaryoları (test_multi_vehicle_cvrp.sh, demo_multi_stop_route.sh)
- ✅ Integration tests (multi-vehicle optimization end-to-end test)
- ✅ Swagger dokümantasyonu — logistics endpoint'leri

### Hafta 4 — Frontend Panel ve Tez Yazımı

- ✅ Frontend iskeleti (framework TBD) + routing + JWT interceptor
- ✅ Login ekranı ve rol bazlı yönlendirme
- ✅ ADMIN paneli — kullanıcı yönetimi
- ✅ COMPANY_STAFF dashboard — envanter özeti, talep havuzu, rota ekranı
- ✅ CUSTOMER dashboard — stok, zaiyat, manuel talep formu
- ✅ Harita entegrasyonu — Gemlik depo + dolumcu lokasyonları + aktif rota

### Phase 2 — Planlanan Geliştirmeler

- ✅ Raporlama ve analitik ekranları (toplama istatistikleri, rota verimliliği, stok trendleri)
- ✅ Yapay zeka destekli müşteri hizmetleri chat modülü (Gemini entegrasyonu)
- ✅ Yapay zeka destekli staff hizmetleri chat modülü (Gemini entegrasyonu)
- ⬜ Test kapsamının genişletilmesi (service layer unit testleri, kritik akış entegrasyon testleri)
- ⬜ Bildirim sistemi (stok uyarıları, talep durum değişikliği bildirimleri)
- ⬜ Demo senaryosu hazırlığı ve tez dökümanı yazımı
- ⬜ Deployment altyapısı kurulumu

---

## Git Akışı (Branch Stratejisi)

```
main          → kararlı, her hafta sonu merge edilir
dev           → aktif geliştirme
feature/xxx   → özellik branch'leri (feature/inventory-service, feature/jwt-auth vb.)
```

Commit formatı:
```
feat: dolumcu stok hesaplama servisi eklendi
fix: zaiyat tahmini null pointer hatası giderildi
chore: flyway migration V2 eklendi
docs: ubiquitous language sözlüğü güncellendi
```

---

## Katkı

1. `dev` branch'inden yeni bir `feature/` branch'i aç
2. Değişikliklerini commit'le (yukarıdaki format)
3. `dev`'e pull request aç
4. Review sonrası merge

---

## Lisans

Bu proje Bursa Teknik Üniversitesi bitirme tezi kapsamında geliştirilmektedir.
