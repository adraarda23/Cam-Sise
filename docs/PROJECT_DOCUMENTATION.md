# Palet & Separatör Havuz Yönetim Sistemi - Teknik Dokümantasyon

> **Versiyon:** 1.0.0
> **Son Güncelleme:** 2026-04-16
> **Proje:** Bursa Teknik Üniversitesi - Bilgisayar Mühendisliği Bitirme Tezi

---

## İçindekiler

1. [Genel Bakış](#1-genel-bakış)
2. [Mimari Tasarım](#2-mimari-tasarım)
3. [Domain Model](#3-domain-model)
   - 3.1. [Core Module](#31-core-module)
   - 3.2. [Inventory Module](#32-inventory-module)
   - 3.3. [Logistics Module](#33-logistics-module)
   - 3.4. [Auth Module](#34-auth-module)
4. [Domain Events](#4-domain-events)
5. [API Endpoints](#5-api-endpoints)
6. [Veritabanı Şeması](#6-veritabanı-şeması)
7. [Deployment](#7-deployment)

---

## 1. Genel Bakış

### 1.1. Proje Amacı

Cam şişe dolum sektöründe kullanılan yeniden kullanılabilir palet ve separatörlerin geri toplama sürecini yöneten, envanter takibi ve rota optimizasyonu sunan bir backend + panel uygulaması.

### 1.2. Teknik Stack

| Katman | Teknoloji |
|--------|-----------|
| **Backend** | Java 21 + Spring Boot 4.0.5 |
| **ORM** | Spring Data JPA + Hibernate |
| **Veritabanı** | PostgreSQL (Supabase) |
| **Migration** | Flyway |
| **Güvenlik** | Spring Security + JWT (jjwt 0.12.6) |
| **API Dokümantasyonu** | SpringDoc OpenAPI 3.0.2 |
| **Build Tool** | Maven 3.9+ |

### 1.3. Mimari Yaklaşım

- **Dış Yapı:** Modular Monolith
- **İç Yapı:** Layered Architecture (Controller → Service → Repository → Domain)
- **Tasarım Prensibi:** Domain-Driven Design (DDD)
- **Event Yönetimi:** Domain Events + Event Sourcing (Audit Logging)
- **Multi-Tenancy:** Pool Operator (Tenant) bazlı izolasyon

---

## 2. Mimari Tasarım

### 2.1. Modül Yapısı

```
src/main/java/ardaaydinkilinc/Cam_Sise/
│
├── shared/                    # Paylaşılan altyapı
│   ├── domain/
│   │   ├── base/             # DDD base sınıfları
│   │   ├── vo/               # Paylaşılan Value Object'ler
│   │   └── event/            # Event altyapısı
│   └── config/               # Global konfigürasyonlar
│
├── core/                      # Tenant & Filler yönetimi
│   ├── domain/
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── inventory/                 # Stok takibi
│   ├── domain/
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── logistics/                 # Toplama planlaması & rota optimizasyonu
│   ├── domain/
│   ├── repository/
│   ├── service/
│   └── controller/
│
└── auth/                      # Kimlik doğrulama & yetkilendirme
    ├── domain/
    ├── repository/
    ├── service/
    └── controller/
```

### 2.2. DDD Kavramları

#### 2.2.1. Aggregate Root

Aggregate Root, bir aggregate'in giriş noktasıdır ve aggregate içindeki tüm değişiklikleri kontrol eder.

**Temel Özellikler:**
- Kimliğe sahiptir (ID)
- Business kurallarını uygular
- Domain Event'leri fırlatır
- Transaction sınırını belirler

#### 2.2.2. Value Object

Value Object, kimliği olmayan, değer eşitliği ile karşılaştırılan, immutable nesnelerdir.

**Temel Özellikler:**
- Kimliksiz (ID yok)
- Immutable (değiştirilemez)
- Değer eşitliği (field'lar aynıysa eşit)
- Kendi validasyonunu yapar

#### 2.2.3. Domain Event

Domain Event, aggregate içinde geçmişte gerçekleşmiş bir olayı temsil eder.

**Temel Özellikler:**
- Geçmiş zaman (past tense)
- Immutable
- Modüller arası iletişim sağlar
- Audit trail için kullanılır

---

## 3. Domain Model

### 3.1. Core Module

Core Module, sistemin omurgasını oluşturan tenant (PoolOperator) ve müşteri (Filler) yönetiminden sorumludur.

---

#### 3.1.1. PoolOperator (Aggregate Root)

**Açıklama:** Sistemin sahibi firma (Tenant). Multi-tenancy için kullanılır.

**Sorumluluklar:**
- Firma bilgilerini yönetme
- Aktif/pasif durumunu kontrol etme
- Tenant izolasyonu sağlama

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `companyName` | String | Firma adı |
| `taxId` | TaxId | Vergi kimlik numarası (Value Object) |
| `contactInfo` | ContactInfo | İletişim bilgileri (Value Object) |
| `active` | Boolean | Aktif/pasif durumu |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni pool operator kaydı
public static PoolOperator register(
    String companyName,
    TaxId taxId,
    ContactInfo contactInfo
)

// Aktif yap
public void activate()

// Pasif yap
public void deactivate()

// İletişim bilgilerini güncelle
public void updateContactInfo(ContactInfo newContactInfo)
```

**Fırlattığı Event'ler:**
- `PoolOperatorRegistered` - Yeni firma kaydedildiğinde
- `PoolOperatorActivated` - Firma aktif yapıldığında
- `PoolOperatorDeactivated` - Firma pasif yapıldığında

**İlişkiler:**
- `1 PoolOperator` → `N Filler`
- `1 PoolOperator` → `N Depot`
- `1 PoolOperator` → `N User`
- `1 PoolOperator` → `N VehicleType`

---

#### 3.1.2. Filler (Aggregate Root)

**Açıklama:** Dolumcu firma. Palet ve separatör alan, kullanan ve geri veren müşteri.

**Sorumluluklar:**
- Dolumcu bilgilerini yönetme
- Lokasyon bilgilerini tutma
- Aktif/pasif durumunu kontrol etme

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `poolOperatorId` | Long | Ait olduğu tenant (Foreign Key) |
| `name` | String | Dolumcu firma adı |
| `address` | Address | Adres bilgisi (Value Object) |
| `location` | GeoCoordinates | GPS koordinatları (Value Object) |
| `contactInfo` | ContactInfo | İletişim bilgileri (Value Object) |
| `taxId` | TaxId | Vergi kimlik numarası (Value Object) |
| `active` | Boolean | Aktif/pasif durumu |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni dolumcu kaydı
public static Filler register(
    Long poolOperatorId,
    String name,
    Address address,
    GeoCoordinates location,
    ContactInfo contactInfo,
    TaxId taxId
)

// Aktif yap
public void activate()

// Pasif yap
public void deactivate()

// İletişim bilgilerini güncelle
public void updateContactInfo(ContactInfo newContactInfo)

// Lokasyonu güncelle
public void updateLocation(GeoCoordinates newLocation)
```

**Fırlattığı Event'ler:**
- `FillerRegistered` - Yeni dolumcu kaydedildiğinde
- `FillerActivated` - Dolumcu aktif yapıldığında
- `FillerDeactivated` - Dolumcu pasif yapıldığında

**İlişkiler:**
- `N Filler` → `1 PoolOperator`
- `1 Filler` → `N FillerStock`
- `1 Filler` → `N LossRecord`
- `1 Filler` → `N CollectionRequest`

---

### 3.2. Inventory Module

Inventory Module, dolumculardaki palet ve separatör stoklarının takibinden sorumludur.

---

#### 3.2.1. FillerStock (Aggregate Root)

**Açıklama:** Bir dolumcunun belirli bir asset tipi (palet veya separatör) için stok durumu.

**Sorumluluklar:**
- Anlık stok miktarını tutma
- Giriş (inflow) ve çıkış (collection) kayıtlarını işleme
- Eşik değeri kontrolü yapma
- Zaiyat oranı ile tahmini stok hesaplama

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `fillerId` | Long | Dolumcu ID (Foreign Key) |
| `assetType` | AssetType | PALLET veya SEPARATOR (Enum) |
| `currentQuantity` | Integer | Anlık stok miktarı |
| `thresholdQuantity` | Integer | Toplama tetikleme eşiği |
| `estimatedLossRate` | LossRate | Tahmini zaiyat oranı (Value Object) |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// İlk stok kaydı oluştur
public static FillerStock initialize(
    Long fillerId,
    AssetType assetType,
    Integer thresholdQuantity,
    LossRate estimatedLossRate
)

// Asset girişi kaydet
public void recordInflow(int quantity, String referenceId)

// Asset toplama kaydet
public void recordCollection(int quantity, String collectionPlanId)

// Eşik değerini güncelle
public void updateThreshold(int newThreshold)

// Zaiyat oranını güncelle
public void updateEstimatedLossRate(LossRate newRate)

// Tahmini kullanılabilir stok (zaiyat oranı düşülmüş)
public int getEstimatedAvailableQuantity()
```

**Fırlattığı Event'ler:**
- `AssetInflowRecorded` - Asset girişi kaydedildiğinde
- `AssetCollected` - Asset toplandığında
- `StockThresholdExceeded` - Stok eşik değeri aşıldığında (→ Logistics modülüne sinyal)

**İlişkiler:**
- `N FillerStock` → `1 Filler`
- Unique constraint: `(fillerId, assetType)`

---

#### 3.2.2. LossRecord (Aggregate Root)

**Açıklama:** Bir dolumcunun belirli bir dönem için zaiyat/hasar oranı kaydı.

**Sorumluluklar:**
- Gerçek zaiyat oranını kaydetme (dolumcu bildirimi)
- Tahmini zaiyat oranını hesaplama (moving average)
- En iyi kullanılabilir oranı sağlama

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `fillerId` | Long | Dolumcu ID (Foreign Key) |
| `assetType` | AssetType | PALLET veya SEPARATOR (Enum) |
| `actualRate` | LossRate | Gerçek zaiyat oranı (Value Object, nullable) |
| `estimatedRate` | LossRate | Tahmini zaiyat oranı (Value Object) |
| `calculationPeriod` | Period | Hesaplama dönemi (Value Object) |
| `lastUpdated` | LocalDateTime | Son güncellenme zamanı |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |

**Domain Metodları:**

```java
// Tahmini oran ile kayıt oluştur
public static LossRecord createWithEstimate(
    Long fillerId,
    AssetType assetType,
    LossRate estimatedRate,
    Period period
)

// Gerçek zaiyat oranını güncelle (dolumcu bildirimi)
public void updateActualRate(LossRate newActualRate)

// Tahmini oranı yeniden hesapla (moving average)
public void recalculateEstimatedRate(LossRate newEstimatedRate, Period newPeriod)

// En iyi kullanılabilir oranı al (actual varsa onu, yoksa estimated)
public LossRate getBestAvailableRate()
```

**Fırlattığı Event'ler:**
- `LossRecordUpdated` - Gerçek zaiyat oranı güncellendiğinde
- `EstimatedLossRateCalculated` - Tahmini oran hesaplandığında

**İlişkiler:**
- `N LossRecord` → `1 Filler`

---

#### 3.2.3. Inventory Module - Value Objects

##### AssetType (Enum)

```java
public enum AssetType {
    PALLET("Palet"),
    SEPARATOR("Separatör");
}
```

##### LossRate (Value Object)

**Açıklama:** Zaiyat/kayıp oranı (yüzde cinsinden).

**Alanlar:**
- `percentage` (double): 0-100 arası

**Metodlar:**
```java
public int calculateLoss(int totalQuantity)        // Kayıp miktarını hesapla
public int calculateRemaining(int totalQuantity)   // Kalan miktarı hesapla
public String formatted()                          // "5.25%" formatında
```

##### StockMovement (Value Object)

**Açıklama:** Tek bir stok hareketi kaydı.

**Alanlar:**
- `type` (MovementType): INFLOW, COLLECTION, ADJUSTMENT
- `quantity` (int): Miktar
- `occurredAt` (LocalDateTime): Gerçekleşme zamanı
- `referenceId` (String): İlişkili kayıt ID'si

**Metodlar:**
```java
public int getSignedQuantity()  // INFLOW için pozitif, COLLECTION için negatif
```

##### Period (Value Object)

**Açıklama:** Zaman dilimi.

**Alanlar:**
- `startDate` (LocalDate): Başlangıç tarihi
- `endDate` (LocalDate): Bitiş tarihi

**Metodlar:**
```java
public long getDays()                          // Gün sayısı
public long getMonths()                        // Ay sayısı
public boolean contains(LocalDate date)        // Tarih aralıkta mı?
public String formatted()                      // Formatlanmış string
```

---

### 3.3. Logistics Module

Logistics Module, toplama planlaması, araç yönetimi ve rota optimizasyonundan sorumludur.

---

#### 3.3.1. VehicleType (Aggregate Root)

**Açıklama:** Araç tipi katalogu. Operatör tarafından tanımlanır, araç kaydında seçilir.

**Sorumluluklar:**
- Araç tipi bilgilerini yönetme
- Kapasite tanımlarını tutma
- Aktif/pasif durumunu kontrol etme

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `poolOperatorId` | Long | Ait olduğu tenant (Foreign Key) |
| `name` | String | Araç tipi adı ("7.5 Ton Kamyon") |
| `description` | String | Açıklama (nullable) |
| `capacity` | Capacity | Kapasite bilgisi (Value Object) |
| `active` | Boolean | Aktif/pasif durumu |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni araç tipi oluştur
public static VehicleType create(
    Long poolOperatorId,
    String name,
    String description,
    Capacity capacity
)

// Kapasiteyi güncelle
public void updateCapacity(Capacity newCapacity)

// Pasif yap
public void deactivate()
```

**Fırlattığı Event'ler:**
- `VehicleTypeCreated` - Yeni araç tipi oluşturulduğunda
- `VehicleTypeCapacityUpdated` - Kapasite güncellendiğinde
- `VehicleTypeDeactivated` - Araç tipi pasif yapıldığında

**İlişkiler:**
- `N VehicleType` → `1 PoolOperator`
- `1 VehicleType` → `N Vehicle`

---

#### 3.3.2. Depot (Aggregate Root)

**Açıklama:** Depo/tesis. Araçların çıkış ve dönüş noktası.

**Sorumluluklar:**
- Depo bilgilerini yönetme
- Lokasyon bilgilerini tutma
- Araç atamasını yönetme

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `poolOperatorId` | Long | Ait olduğu tenant (Foreign Key) |
| `name` | String | Depo adı |
| `address` | Address | Adres bilgisi (Value Object) |
| `location` | GeoCoordinates | GPS koordinatları (Value Object) |
| `vehicleIds` | List<Long> | Bu depodaki araçlar |
| `active` | Boolean | Aktif/pasif durumu |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni depo oluştur
public static Depot create(
    Long poolOperatorId,
    String name,
    Address address,
    GeoCoordinates location
)

// Araç ekle
public void addVehicle(Long vehicleId)

// Araç çıkar
public void removeVehicle(Long vehicleId)

// Araç sayısını al
public int getVehicleCount()
```

**Fırlattığı Event'ler:**
- `DepotCreated` - Yeni depo oluşturulduğunda
- `VehicleAddedToDepot` - Depoya araç eklendiğinde
- `VehicleRemovedFromDepot` - Depodan araç çıkarıldığında

**İlişkiler:**
- `N Depot` → `1 PoolOperator`
- `1 Depot` → `N Vehicle`
- `1 Depot` → `N CollectionPlan`

---

#### 3.3.3. Vehicle (Aggregate Root)

**Açıklama:** Toplama aracı.

**Sorumluluklar:**
- Araç bilgilerini yönetme
- Durum (status) yönetimi
- Rota ataması ve sürücü bilgisi tutma

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `depotId` | Long | Bağlı olduğu depo (Foreign Key) |
| `vehicleTypeId` | Long | Araç tipi (Foreign Key) |
| `plateNumber` | String | Plaka (Unique) |
| `status` | VehicleStatus | AVAILABLE, ON_ROUTE, MAINTENANCE, INACTIVE (Enum) |
| `currentDriver` | DriverInfo | Sürücü bilgisi (Value Object, nullable) |
| `currentCollectionPlanId` | Long | Aktif toplama planı (nullable) |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni araç kaydet
public static Vehicle register(
    Long depotId,
    Long vehicleTypeId,
    String plateNumber
)

// Rotaya ata
public void assignToRoute(Long collectionPlanId, DriverInfo driver)

// Depodan çık
public void departFromDepot()

// Depoya dön
public void returnToDepot()

// Durum değiştir
public void changeStatus(VehicleStatus newStatus)
```

**Fırlattığı Event'ler:**
- `VehicleRegistered` - Yeni araç kaydedildiğinde
- `VehicleAssignedToRoute` - Araç rotaya atandığında
- `VehicleDepartedFromDepot` - Araç depodan çıktığında
- `VehicleReturnedToDepot` - Araç depoya döndüğünde
- `VehicleStatusChanged` - Araç durumu değiştiğinde

**İlişkiler:**
- `N Vehicle` → `1 Depot`
- `N Vehicle` → `1 VehicleType`
- `1 Vehicle` → `0..1 CollectionPlan` (aktif plan)

---

#### 3.3.4. CollectionRequest (Aggregate Root)

**Açıklama:** Toplama talebi. Otomatik (eşik tetikli) veya manuel (dolumcu talebli) olabilir.

**Sorumluluklar:**
- Talep bilgilerini yönetme
- Durum geçişlerini kontrol etme
- Onay/red işlemlerini yönetme

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `fillerId` | Long | Dolumcu ID (Foreign Key) |
| `assetType` | AssetType | PALLET veya SEPARATOR (Enum) |
| `estimatedQuantity` | Integer | Tahmini miktar |
| `status` | RequestStatus | PENDING, APPROVED, REJECTED, CANCELLED, SCHEDULED, COMPLETED (Enum) |
| `source` | RequestSource | AUTO_THRESHOLD veya MANUAL_CUSTOMER (Enum) |
| `approvedByUserId` | Long | Onaylayan kullanıcı (nullable) |
| `rejectionReason` | String | Red sebebi (nullable) |
| `collectionPlanId` | Long | Bağlı olduğu plan (nullable) |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Otomatik talep oluştur (eşik tetikli)
public static CollectionRequest createAutomatic(
    Long fillerId,
    AssetType assetType,
    Integer estimatedQuantity
)

// Manuel talep oluştur (dolumcu talebli)
public static CollectionRequest createManual(
    Long fillerId,
    AssetType assetType,
    Integer estimatedQuantity,
    Long requestingUserId
)

// Onayla
public void approve(Long approvingUserId)

// Reddet
public void reject(String reason)

// İptal et
public void cancel()

// Planlandı olarak işaretle
public void schedule(Long planId)

// Tamamlandı olarak işaretle
public void complete()
```

**Fırlattığı Event'ler:**
- `CollectionRequestCreated` - Yeni talep oluşturulduğunda
- `CollectionRequestApproved` - Talep onaylandığında
- `CollectionRequestRejected` - Talep reddedildiğinde
- `CollectionRequestCancelled` - Talep iptal edildiğinde

**İlişkiler:**
- `N CollectionRequest` → `1 Filler`
- `N CollectionRequest` → `0..1 CollectionPlan`

---

#### 3.3.5. CollectionPlan (Aggregate Root)

**Açıklama:** CVRP optimizer tarafından oluşturulan toplama rota planı.

**Sorumluluklar:**
- Rota bilgilerini tutma
- Araç ataması yönetme
- Plan durum geçişlerini kontrol etme
- Toplama işlemini başlatma ve tamamlama

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `depotId` | Long | Çıkış deposu (Foreign Key) |
| `assignedVehicleId` | Long | Atanan araç (nullable) |
| `totalDistance` | Distance | Toplam mesafe (Value Object) |
| `estimatedDuration` | Duration | Tahmini süre (Value Object) |
| `totalCapacityPallets` | Integer | Toplam palet kapasitesi |
| `totalCapacitySeparators` | Integer | Toplam separatör kapasitesi |
| `status` | PlanStatus | GENERATED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED (Enum) |
| `plannedDate` | LocalDate | Planlanan tarih |
| `routeStopsJson` | String | Rota durakları (JSON) |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Optimizer tarafından plan oluştur
public static CollectionPlan generate(
    Long depotId,
    Distance totalDistance,
    Duration estimatedDuration,
    Integer totalCapacityPallets,
    Integer totalCapacitySeparators,
    LocalDate plannedDate,
    String routeStopsJson
)

// Araca ata
public void assignVehicle(Long vehicleId)

// Toplamayı başlat (araç çıktı)
public void start()

// Toplamayı tamamla
public void complete(int actualPalletsCollected, int actualSeparatorsCollected)

// Planı iptal et
public void cancel()
```

**Fırlattığı Event'ler:**
- `CollectionPlanGenerated` - Yeni plan oluşturulduğunda
- `RouteAssignedToVehicle` - Plan araca atandığında
- `CollectionStarted` - Toplama başladığında
- `CollectionCompleted` - Toplama tamamlandığında (→ Inventory modülüne sinyal)

**İlişkiler:**
- `N CollectionPlan` → `1 Depot`
- `N CollectionPlan` → `0..1 Vehicle`

---

#### 3.3.6. Logistics Module - Value Objects

##### Capacity (Value Object)

**Açıklama:** Araç/plan kapasitesi.

**Alanlar:**
- `pallets` (int): Palet kapasitesi
- `separators` (int): Separatör kapasitesi

**Metodlar:**
```java
public boolean canAccommodate(Capacity required)   // Yeterli kapasite var mı?
public Capacity subtract(Capacity used)            // Kullanılan kapasiteyi çıkar
public Capacity add(Capacity other)                // Kapasiteleri topla
public boolean isEmpty()                           // Kapasite boş mu?
public String formatted()                          // "500 pallets, 800 separators"
```

##### RouteStop (Value Object)

**Açıklama:** Rota üzerindeki bir durak.

**Alanlar:**
- `fillerId` (Long): Dolumcu ID
- `location` (GeoCoordinates): Lokasyon
- `stopOrder` (int): Durak sırası
- `estimatedPallets` (int): Tahmini palet miktarı
- `estimatedSeparators` (int): Tahmini separatör miktarı
- `estimatedServiceTime` (Duration): Tahmini servis süresi

**Metodlar:**
```java
public Capacity getEstimatedCapacity()  // Tahmini kapasite
```

##### DriverInfo (Value Object)

**Açıklama:** Sürücü bilgisi.

**Alanlar:**
- `name` (String): Sürücü adı
- `licenseNumber` (String): Ehliyet numarası (6-12 karakter)
- `phone` (String): Telefon (nullable)

##### RequestSource (Enum)

```java
public enum RequestSource {
    AUTO_THRESHOLD,      // Sistem otomatik oluşturdu (stok eşiği)
    MANUAL_CUSTOMER      // Dolumcu manuel talep açtı
}
```

##### RequestStatus (Enum)

```java
public enum RequestStatus {
    PENDING,       // Onay bekliyor
    APPROVED,      // Onaylandı
    REJECTED,      // Reddedildi
    CANCELLED,     // İptal edildi
    SCHEDULED,     // Plana dahil edildi
    COMPLETED      // Tamamlandı
}
```

##### VehicleStatus (Enum)

```java
public enum VehicleStatus {
    AVAILABLE,     // Kullanılabilir
    ON_ROUTE,      // Rotada
    MAINTENANCE,   // Bakımda
    INACTIVE       // Pasif
}
```

##### PlanStatus (Enum)

```java
public enum PlanStatus {
    GENERATED,     // Oluşturuldu
    ASSIGNED,      // Araca atandı
    IN_PROGRESS,   // Devam ediyor
    COMPLETED,     // Tamamlandı
    CANCELLED      // İptal edildi
}
```

---

### 3.4. Auth Module

Auth Module, kimlik doğrulama ve yetkilendirme işlemlerinden sorumludur.

---

#### 3.4.1. User (Aggregate Root)

**Açıklama:** Sistem kullanıcısı.

**Sorumluluklar:**
- Kullanıcı bilgilerini yönetme
- Rol değişikliğini yönetme
- Şifre güncellemesini yönetme

**Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | Long | Primary Key |
| `poolOperatorId` | Long | Ait olduğu tenant (Foreign Key) |
| `username` | String | Kullanıcı adı (Unique) |
| `password` | String | Şifre (hashed) |
| `fullName` | String | Tam ad |
| `role` | Role | ADMIN, COMPANY_STAFF, CUSTOMER (Enum) |
| `fillerId` | Long | CUSTOMER rolü için dolumcu ID (nullable) |
| `active` | Boolean | Aktif/pasif durumu |
| `createdAt` | LocalDateTime | Oluşturulma zamanı |
| `updatedAt` | LocalDateTime | Güncellenme zamanı |

**Domain Metodları:**

```java
// Yeni kullanıcı kaydet
public static User register(
    Long poolOperatorId,
    String username,
    String password,
    String fullName,
    Role role,
    Long fillerId
)

// Rol değiştir
public void changeRole(Role newRole)

// Şifre güncelle
public void updatePassword(String newPassword)
```

**Fırlattığı Event'ler:**
- `UserRegistered` - Yeni kullanıcı kaydedildiğinde
- `UserRoleChanged` - Kullanıcı rolü değiştiğinde

**İlişkiler:**
- `N User` → `1 PoolOperator`
- `N User` (CUSTOMER) → `0..1 Filler`

---

#### 3.4.2. Role (Enum)

```java
public enum Role {
    ADMIN,           // Tam yetki
    COMPANY_STAFF,   // Operasyon personeli
    CUSTOMER         // Dolumcu kullanıcısı
}
```

**Yetki Matrisi:**

| Rol | Yetkiler |
|-----|----------|
| **ADMIN** | Kullanıcı CRUD, sistem ayarları, zaiyat oran varsayılanları, tüm veriler |
| **COMPANY_STAFF** | Tüm dolumcuları görür, rota oluşturur, talepleri onaylar/reddeder |
| **CUSTOMER** | Sadece kendi stoğunu görür, manuel talep açar |

---

### 3.5. Shared Value Objects

#### 3.5.1. Address (Value Object)

**Alanlar:**
- `street` (String): Sokak
- `city` (String): Şehir (required)
- `province` (String): İl
- `postalCode` (String): Posta kodu
- `country` (String): Ülke (required)

**Metodlar:**
```java
public String getFullAddress()  // Formatlanmış tam adres
```

---

#### 3.5.2. GeoCoordinates (Value Object)

**Alanlar:**
- `latitude` (double): Enlem (-90 ile 90 arası)
- `longitude` (double): Boylam (-180 ile 180 arası)

**Metodlar:**
```java
public Distance distanceTo(GeoCoordinates other)  // Haversine mesafe hesaplama
```

---

#### 3.5.3. Distance (Value Object)

**Alanlar:**
- `kilometers` (double): Kilometre cinsinden mesafe

**Metodlar:**
```java
public double toMeters()                // Metreye çevir
public Distance add(Distance other)     // Mesafe topla
public Distance subtract(Distance other) // Mesafe çıkar
```

---

#### 3.5.4. Duration (Value Object)

**Alanlar:**
- `minutes` (int): Dakika cinsinden süre

**Metodlar:**
```java
public int toHours()                   // Saate çevir
public int getRemainingMinutes()       // Kalan dakikalar
public Duration add(Duration other)    // Süre topla
public String format()                 // "2 hours 30 minutes"
```

---

#### 3.5.5. ContactInfo (Value Object)

**Alanlar:**
- `phone` (String): Telefon (regex: `^\+?[0-9]{10,15}$`)
- `email` (String): E-posta (regex: `^[A-Za-z0-9+_.-]+@(.+)$`)
- `contactPersonName` (String): İletişim kişisi adı

---

#### 3.5.6. TaxId (Value Object)

**Alanlar:**
- `value` (String): 10 haneli vergi kimlik numarası

**Metodlar:**
```java
public String formatted()  // "XXX-XXX-XX-XX" formatında
```

---

#### 3.5.7. Money (Value Object)

**Alanlar:**
- `amount` (BigDecimal): Tutar
- `currency` (Currency): TRY, USD, EUR (Enum)

**Metodlar:**
```java
public Money add(Money other)        // Para topla (aynı para birimi)
public Money subtract(Money other)   // Para çıkar (aynı para birimi)
public String formatted()            // "₺ 100.50"
```

---

## 4. Domain Events

### 4.1. Event Altyapısı

#### 4.1.1. DomainEvent (Interface)

```java
public interface DomainEvent {
    LocalDateTime occurredAt();      // Olayın gerçekleşme zamanı
    String eventType();              // Event tipi (sınıf adı)
}
```

#### 4.1.2. DomainEventPublisher (Service)

Spring ApplicationEventPublisher kullanarak event'leri yayınlar.

```java
public void publish(DomainEvent event)
public void publishAll(List<DomainEvent> events)
```

#### 4.1.3. DomainEventStore (Entity)

Tüm event'leri audit logging için saklar.

**Alanlar:**
- `id` (Long): Primary Key
- `eventType` (String): Event sınıf adı
- `eventData` (String): JSON formatında event verisi
- `occurredAt` (LocalDateTime): Olayın gerçekleşme zamanı
- `storedAt` (LocalDateTime): Veritabanına yazılma zamanı
- `aggregateId` (String): Event'i fırlatan aggregate ID (nullable)
- `aggregateType` (String): Aggregate tipi (nullable)

#### 4.1.4. DomainEventListener (Component)

Tüm event'leri dinler ve asenkron olarak DomainEventStore'a kaydeder.

```java
@Async
@EventListener
public void handleDomainEvent(DomainEvent event)
```

---

### 4.2. Event Listesi

#### 4.2.1. Core Module Events

| Event | Aggregate | Tetikleyici | Dinleyiciler |
|-------|-----------|-------------|--------------|
| `PoolOperatorRegistered` | PoolOperator | Yeni firma kaydı | Audit |
| `PoolOperatorActivated` | PoolOperator | Firma aktif yapıldı | Audit |
| `PoolOperatorDeactivated` | PoolOperator | Firma pasif yapıldı | Audit |
| `FillerRegistered` | Filler | Yeni dolumcu kaydı | Inventory (FillerStock oluştur) |
| `FillerActivated` | Filler | Dolumcu aktif yapıldı | Audit |
| `FillerDeactivated` | Filler | Dolumcu pasif yapıldı | Audit |

---

#### 4.2.2. Inventory Module Events

| Event | Aggregate | Tetikleyici | Dinleyiciler |
|-------|-----------|-------------|--------------|
| `AssetInflowRecorded` | FillerStock | Asset girişi kaydedildi | Audit |
| `AssetCollected` | FillerStock | Asset toplandı | Audit |
| `StockThresholdExceeded` | FillerStock | Stok eşik değeri aştı | **Logistics (CollectionRequest oluştur)** |
| `LossRecordUpdated` | LossRecord | Gerçek zaiyat oranı güncellendi | Audit, FillerStock |
| `EstimatedLossRateCalculated` | LossRecord | Tahmini oran hesaplandı | Audit, FillerStock |

---

#### 4.2.3. Logistics Module Events

| Event | Aggregate | Tetikleyici | Dinleyiciler |
|-------|-----------|-------------|--------------|
| `VehicleTypeCreated` | VehicleType | Yeni araç tipi oluşturuldu | Audit |
| `VehicleTypeCapacityUpdated` | VehicleType | Kapasite güncellendi | Audit |
| `VehicleTypeDeactivated` | VehicleType | Araç tipi pasif yapıldı | Audit |
| `DepotCreated` | Depot | Yeni depo oluşturuldu | Audit |
| `VehicleAddedToDepot` | Depot | Depoya araç eklendi | Audit |
| `VehicleRemovedFromDepot` | Depot | Depodan araç çıkarıldı | Audit |
| `VehicleRegistered` | Vehicle | Yeni araç kaydedildi | Audit, Depot |
| `VehicleAssignedToRoute` | Vehicle | Araç rotaya atandı | Audit |
| `VehicleDepartedFromDepot` | Vehicle | Araç depodan çıktı | Audit |
| `VehicleReturnedToDepot` | Vehicle | Araç depoya döndü | Audit |
| `VehicleStatusChanged` | Vehicle | Araç durumu değişti | Audit |
| `CollectionRequestCreated` | CollectionRequest | Yeni talep oluşturuldu | Audit |
| `CollectionRequestApproved` | CollectionRequest | Talep onaylandı | Audit |
| `CollectionRequestRejected` | CollectionRequest | Talep reddedildi | Audit |
| `CollectionRequestCancelled` | CollectionRequest | Talep iptal edildi | Audit |
| `CollectionPlanGenerated` | CollectionPlan | Yeni plan oluşturuldu | Audit |
| `RouteAssignedToVehicle` | CollectionPlan | Plan araca atandı | Audit, Vehicle |
| `CollectionStarted` | CollectionPlan | Toplama başladı | Audit, Vehicle |
| `CollectionCompleted` | CollectionPlan | Toplama tamamlandı | **Inventory (FillerStock güncelle)**, Audit |

---

#### 4.2.4. Auth Module Events

| Event | Aggregate | Tetikleyici | Dinleyiciler |
|-------|-----------|-------------|--------------|
| `UserRegistered` | User | Yeni kullanıcı kaydedildi | Audit |
| `UserRoleChanged` | User | Kullanıcı rolü değişti | Audit |

---

### 4.3. Modüller Arası İletişim

#### 4.3.1. Inventory → Logistics

**Event:** `StockThresholdExceeded`

**Akış:**
1. FillerStock'ta `recordInflow()` çağrılır
2. Stok eşik değerini aşar
3. `StockThresholdExceeded` event'i fırlatılır
4. Logistics modülündeki event handler dinler
5. Otomatik `CollectionRequest` oluşturulur

---

#### 4.3.2. Logistics → Inventory

**Event:** `CollectionCompleted`

**Akış:**
1. CollectionPlan'da `complete()` çağrılır
2. `CollectionCompleted` event'i fırlatılır
3. Inventory modülündeki event handler dinler
4. İlgili FillerStock'larda `recordCollection()` çağrılır
5. Stoklar güncellenir

---

#### 4.3.3. Core → Inventory

**Event:** `FillerRegistered`

**Akış:**
1. Yeni Filler kaydedilir
2. `FillerRegistered` event'i fırlatılır
3. Inventory modülündeki event handler dinler
4. Her asset tipi için FillerStock kaydı oluşturulur

---

## 5. API Endpoints

> **Not:** Bu bölüm controller'lar yazıldıkça güncellenecektir.

### 5.1. Auth Endpoints

**Base URL:** `/api/auth`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| POST | `/login` | Kullanıcı girişi, JWT token alır | Public |
| POST | `/register` | Yeni kullanıcı kaydı | ADMIN |

---

### 5.2. Core Module Endpoints

**Base URL:** `/api/pool-operators`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

**Base URL:** `/api/fillers`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

---

### 5.3. Inventory Module Endpoints

**Base URL:** `/api/inventory/stocks`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

**Base URL:** `/api/inventory/loss-records`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

---

### 5.4. Logistics Module Endpoints

**Base URL:** `/api/logistics/requests`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

**Base URL:** `/api/logistics/plans`

| Method | Endpoint | Açıklama | Rol |
|--------|----------|----------|-----|
| - | - | *Yakında eklenecek* | - |

---

## 6. Veritabanı Şeması

### 6.1. Migration Versiyonları

| Versiyon | Açıklama | Dosya |
|----------|----------|-------|
| V1 | İlk migration | `V1__initial_schema.sql` |
| V2 | Auth tabloları | `V2__auth_tables.sql` |
| V3 | Domain model tabloları | `V3__create_domain_model_tables.sql` |

---

### 6.2. Tablo Listesi

#### Core Module

- `pool_operators` - Tenant firmaları
- `fillers` - Dolumcu firmaları

#### Inventory Module

- `filler_stocks` - Stok kayıtları
- `loss_records` - Zaiyat kayıtları

#### Logistics Module

- `vehicle_types` - Araç tipi katalogu
- `depots` - Depolar
- `depot_vehicles` - Depo-araç ilişkisi
- `vehicles` - Araçlar
- `collection_requests` - Toplama talepleri
- `collection_plans` - Toplama planları

#### Auth Module

- `users` - Kullanıcılar

#### Shared

- `domain_events` - Event store (audit log)

---

### 6.3. İndeksler

Tüm foreign key'ler ve sık sorgulanan alanlar için indeks oluşturulmuştur:
- `pool_operator_id` (tenant isolation)
- `filler_id`
- `depot_id`
- `vehicle_id`
- `status` alanları
- `event_type`, `occurred_at` (event store)
- GPS koordinatları (`location_latitude`, `location_longitude`)

---

## 7. Deployment

> **Not:** Bu bölüm deployment süreci tamamlandıkça güncellenecektir.

### 7.1. Gereksinimler

- Java 21+
- Maven 3.8+
- PostgreSQL 14+
- 2GB RAM (minimum)

### 7.2. Ortam Değişkenleri

```properties
# Database
spring.datasource.url=jdbc:postgresql://HOST:5432/DB_NAME
spring.datasource.username=USERNAME
spring.datasource.password=PASSWORD

# JWT
jwt.secret=YOUR_SECRET_KEY
jwt.expiration=86400000

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## Versiyon Geçmişi

| Versiyon | Tarih | Değişiklikler |
|----------|-------|---------------|
| 1.0.0 | 2026-04-16 | İlk doküman oluşturuldu - Domain Model & Events |

---

**Son Güncelleme:** 2026-04-16
**Doküman Sorumlusu:** Arda Aydın Kılınç
**İletişim:** [E-posta adresi]
