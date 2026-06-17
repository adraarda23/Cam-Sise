# Sistem Dokümantasyonu — Palet & Separatör Havuz Yönetim Sistemi

> **Kapsam:** Bu belge projenin **tamamını** (backend + frontend) kapsayan, kaynak koddan **doğrulanmış** güncel teknik dokümantasyondur.
> **Son doğrulama:** 2026-06-15 (kaynak kod taraması).
> **Not:** Kök dizindeki `README.md` ve `docs/PROJECT_DOCUMENTATION.md` daha eskidir; sayısal değerler ve modül listesi için bu belgeyi esas alın.

---

## 1. Genel Bakış

Cam şişe dolum sektöründe kullanılan **yeniden kullanılabilir palet ve separatörlerin** geri toplama sürecini yöneten; envanter takibi, eşik tetikli otomatik talep üretimi ve kapasiteli araç rota optimizasyonu (CVRP) sunan bir **backend + web panel** uygulamasıdır. Bursa Teknik Üniversitesi Bilgisayar Mühendisliği bitirme çalışması; sektördeki palet/separatör havuz (pooling) iş modeli referans alınmıştır.

### İş Döngüsü

```
Havuz Operatörü (Gemlik)
        │  palet + separatör gönderir
        ▼
  Cam Üreticisi (5)
        │  cam ürünleri + palet + separatör ile sevk eder
        ▼
   Dolumcu / Filler (250)
        │  ürünler satılır, boş palet + separatör birikir
        ▼
Havuz Operatörü optimize rotayla geri toplar (Gemlik'e döner)
```

Sistem döngünün **son bacağına** odaklanır: dolumculardaki birikmiş varlıkların takibi ve optimize edilmiş geri toplanması.

---

## 2. Mimari

### 2.1 Dış Yapı — Modular Monolith
Her bounded context kendi modülünde; tek deployable birim. Mikroservis karmaşıklığı olmadan modül sınırları (bounded context) korunur.

### 2.2 İç Yapı — DDD tabanlı Katmanlı Mimari
Her modül `controller / service / domain / repository` katmanlarına ayrılır. `domain/event`, `domain/vo`, `service/event` alt paketleri DDD desenlerini ayırır.

```
┌──────────────────────── Modular Monolith ────────────────────────┐
│  core · inventory · logistics · auth · analytics(+anomaly)        │
│  chat · settings · notification                                   │
│                          │                                        │
│                  Domain Events Bus  ──►  Event Store (Audit)      │
│                                          → Elasticsearch          │
│  ┌─────────────────────── Shared Kernel ───────────────────────┐ │
│  │ DDD base (AggregateRoot/Entity/ValueObject) · Event altyapısı│ │
│  │ Shared VO'lar · Exception handling · JWT util · CORS · OpenAPI│ │
│  └──────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
        │                                   │
        ▼                                   ▼
  PostgreSQL (Supabase)            Elasticsearch (audit log)
  H2 (test ortamı)
```

### 2.3 Tasarım Prensipleri
- **Domain-Driven Design** — Taktiksel: Aggregate Root, Entity, Value Object, Domain Event, Repository. Stratejik: Bounded Context, Ubiquitous Language, modüler yapı.
- **Olay Güdümlü** — Modüller arası gevşek bağlı haberleşme Domain Event'lerle.
- **Çok-Kiracılı (Multi-Tenant)** — `PoolOperator` (tenant) bazlı veri izolasyonu.

> **Terminoloji notu:** Sistemde domain event'ler Elasticsearch'e bir **denetim günlüğü / event store (audit log)** olarak yazılır. Bu klasik anlamda **Event Sourcing değildir** (aggregate durumu event'lerden yeniden kurulmaz; durum ilişkisel tablolarda tutulur). Belgede bu yapı "Domain Event tabanlı denetim günlüğü" olarak adlandırılır.

---

## 3. Modüller (9)

| Modül | Sorumluluk | Aggregate Root'lar |
|---|---|---|
| `core` | Havuz operatörü (tenant) ve dolumcu yönetimi | PoolOperator, Filler |
| `inventory` | Asset stok takibi, zaiyat/kayıp hesabı, eşik kontrolü, tahmin | FillerStock, LossRecord |
| `logistics` | Araç, depo, talep, rota planlama (CVRP), filo önerisi, OSRM mesafe | VehicleType, Depot, Vehicle, CollectionRequest, CollectionPlan |
| `auth` | JWT kimlik doğrulama, rol yönetimi, kullanıcı CRUD | User |
| `analytics` | Özet dashboard + `anomaly` alt paketi (z-score anomali tespiti) | — |
| `chat` | Google Gemini tabanlı AI asistan (CUSTOMER + COMPANY_STAFF) | — |
| `settings` | Operatör bazlı iş kuralı konfigürasyonu (min. talep eşikleri) | CompanySettings |
| `notification` | In-app + email (SMTP) bildirim | Notification |
| `shared` | Shared kernel: DDD altyapısı, event bus, audit store, paylaşılan VO'lar, exception handling, JWT util, admin tanılama | — |

---

## 4. Domain Modeli

### 4.1 Aggregate Root'lar (11)
PoolOperator, Filler, User, FillerStock, LossRecord, VehicleType, Depot, Vehicle, CollectionRequest, CollectionPlan, **Notification**.

### 4.2 Ubiquitous Language

| Terim | Açıklama |
|---|---|
| **Pool Operator** | Sistemin sahibi firma (Tenant). Multi-tenancy temeli. |
| **Depot** | Araçların çıkış/dönüş noktası. Operatörün birden çok deposu olabilir. |
| **Glass Manufacturer** | Operatörden palet alır, camı dolumcuya sevk eder (5 adet). |
| **Filler (Dolumcu)** | İçecek/gıda dolumu yapan fabrikalar (250 adet). |
| **Asset** | Takibi yapılan palet veya separatör. |
| **FillerStock** | Bir dolumcunun belirli asset tipi için stok durumu. |
| **Loss Record** | Dolumcudaki kayıp/hasar oranı; girilmezse hareketli ortalama ile tahmin. |
| **Collection Request** | Toplama talebi: otomatik (eşik tetikli) veya manuel (dolumcudan). |
| **Collection Plan** | CVRP çıktısı: hangi araç, hangi sırayla, hangi dolumcuları ziyaret eder. |
| **Vehicle / VehicleType** | Toplama aracı / kapasite bilgili araç tipi katalogu. |
| **Domain Event** | Aggregate'lerde gerçekleşen, modüller arası iletişimi sağlayan olaylar. |

### 4.3 Value Object'ler (21)
Java **record** olarak ve `ValueObject` **marker arayüzünü** uygulayarak tanımlanır (base sınıf değil; değişmez, değer eşitlikli).

- **shared/vo:** Address, ContactInfo, GeoCoordinates, Distance, Duration, Money, TaxId, **ConfidenceInterval**
- **inventory/vo:** AssetType, LossRate, Period, StockMovement
- **logistics/vo:** Capacity, RouteStop, DriverInfo, RequestSource, RequestStatus, PlanStatus, VehicleStatus (enum'lar dahil)
- **notification/vo:** NotificationType, NotificationSeverity

### 4.4 Domain Event'ler (41)
Modüller arası iletişimi sağlar. Örnek akış:

```
StockThresholdExceeded (inventory)  ──►  CollectionRequest oluştur (logistics)
CollectionCompleted    (logistics)  ──►  FillerStock güncelle      (inventory)
FillerRegistered       (core)       ──►  FillerStock oluştur       (inventory)
```

Tam liste (paket bazında): auth (4), core (8), inventory (6), logistics (22), + notification/anomaly olayları.

---

## 5. Çapraz Kesen Konular (Cross-Cutting)

### 5.1 Domain Event Altyapısı + Denetim Günlüğü
- `shared/domain/event/`: `DomainEvent`, `DomainEventPublisher`, `DomainEventStore`.
- `shared/infrastructure/persistence/DomainEventPublishingEntityListener`: JPA entity listener, aggregate kaydedilince event'leri yayınlar.
- `shared/infrastructure/event/`: `DomainEventStoreHandler`, `DomainEventDocument`, `DomainEventSearchRepository` → event'leri **Elasticsearch**'e audit/denetim kaydı olarak yazar.

### 5.2 Çok-Kiracılılık (Multi-Tenancy)
`PoolOperator` tenant kimliği bazlı veri izolasyonu. CUSTOMER yalnızca kendi dolumcusunun verisini görür.

### 5.3 Güvenlik
- **Spring Security + JWT** (jjwt 0.12.6, HS256, 24 saat geçerlilik).
- `auth/filter/JwtAuthenticationFilter` her istekte token doğrular.
- Roller: `ADMIN`, `COMPANY_STAFF`, `CUSTOMER` (stateless, rol bazlı yetkilendirme).

---

## 6. Rota Optimizasyonu Motoru (logistics)

### 6.1 Algoritmalar
- **Tek araç:** Greedy Nearest Neighbor + **2-opt** yerel arama iyileştirmesi.
- **Çoklu araç:** **Clarke-Wright Savings Algorithm** + 2-opt. (`CVRPOptimizer.java`, ~595 satır.)
- Tasarruf formülü: `s(i,j) = d(0,i) + d(0,j) − d(i,j)`.

### 6.2 Kısıtlar (`RouteConstraints`)
Maks. 800 km mesafe, maks. 10 saat süre, durak başına 30 dk servis süresi. Coğrafi kümeleme gerçekçi olmayan rotaları (ör. Adana→İzmir→Urfa) önler; infeasible rotalar otomatik reddedilir, atanamayan talepler ayrıca raporlanır.

### 6.3 Mesafe Sağlayıcı (Strategy Pattern — `logistics/service/routing`)
`DistanceProvider` arayüzü iki gerçekleştirim ile:
- `HaversineDistanceProvider` — kuş uçuşu (fallback).
- `OsrmDistanceProvider` — **OSRM** ile gerçek yol mesafesi/süresi + polyline geometri (public demo: `router.project-osrm.org`).
- `CachedDistanceProvider` + `DistanceCacheEntry` — DB tabanlı mesafe önbelleği (30 gün TTL).
- Sağlayıcı `app.routing.distance-provider` ile seçilir (varsayılan `osrm`).

### 6.4 Filo Önerisi (`logistics/service/fleet`)
`VehicleAssignmentService` + `FleetComposition` — talep yüküne göre uygun araç kombinasyonu önerir; rota doğrudan önerilen filoyla oluşturulabilir.

---

## 7. Envanter ve Zaiyat Tahmini (inventory)

- Asset hareketleri: inflow (giriş) / collection (toplama).
- Dolumcu bazında anlık stok hesabı.
- **Zaiyat tahmini:** kayıt yoksa hareketli ortalama (moving average) ile tahmin; `ConfidenceInterval` VO ile **güven aralığı** üretilir (`commons-math3`).
- **Eşik kontrolü:** stok eşiği aşılınca `StockThresholdExceeded` → otomatik toplama talebi.
- Forecast endpoint'i: `GET /api/inventory/stocks/{fillerId}/{assetType}/forecast`.

### 7.1 Anomali Tespiti (`analytics/anomaly`)
`AnomalyDetectionService` — **z-score** tabanlı, `DayOfWeekBaseline` (haftaiçi/haftasonu ayrı baseline) ile açıklanabilir anomali tespiti. `AnomalyScheduler` cron ile çalışır (`app.anomaly.enabled`, varsayılan kapalı).

---

## 8. AI Asistanı (chat)
- `ChatService` + `GeminiService` (Google Gemini API).
- Rol bazlı sistem prompt'u; canlı stok/talep verisiyle zenginleştirilir.
- CUSTOMER: doğal dilde stok/talep sorgusu + **chat üzerinden toplama talebi** (ACTION_JSON mekanizması).
- COMPANY_STAFF: sistem geneli özet (bekleyen talepler, aktif planlar, eşik aşan dolumcular).
- Çok turlu sohbet geçmişi.

---

## 9. Bildirim (notification)
- `Notification` aggregate; tip (`NotificationType`) ve önem (`NotificationSeverity`) VO'ları.
- **In-app** (okunmamış sayısı, okundu işaretleme) + **email** (`EmailNotificationSender`, SMTP).
- `NotificationEventHandler` domain event'lere abone olur (ör. talep durum değişikliği, stok uyarısı).

---

## 10. Analitik (analytics)
`GET /api/analytics/summary` — toplama talepleri (durum/asset dağılımı), planlar (ort. mesafe/süre), stok özeti (toplam stok, eşik aşan dolumcu sayısı), operatör bazlı filtre.

---

## 11. REST API Referansı (özet)

| Alan | Taban yol | Önemli uçlar |
|---|---|---|
| Auth | `/api/auth` | `POST /login` |
| Users | `/api/users` | `POST /staff`, `POST /customer`, `PUT /{id}`, `POST /{id}/deactivate` |
| Pool Operators | `/api/pool-operators` | `GET /{id}`, activate/deactivate, `PUT /{id}/contact` |
| Fillers | `/api/fillers` | `GET/PUT /{id}`, activate/deactivate, `PUT /{id}/contact|location` |
| Stocks | `/api/inventory/stocks` | `POST /inflow`, `POST /collection`, threshold/loss-rate, `GET .../forecast` |
| Loss Records | `/api/inventory/loss-records` | actual-rate / estimated-rate, sorgular |
| Collection Requests | `/api/logistics/collection-requests` | `POST /manual`, approve/reject/cancel |
| Collection Plans | `/api/logistics/collection-plans` | assign-vehicle, start/complete/cancel, refresh-geometry |
| Route Optimization | `/api/logistics/optimize` | `POST /custom`, `/multi-vehicle`, `/with-fleet`, `/suggest-fleet` |
| Vehicles | `/api/logistics/vehicles` | assign/depart/return, status, delete |
| Vehicle Types | `/api/logistics/vehicle-types` | capacity, deactivate |
| Depots | `/api/logistics/depots` | araç ekle/çıkar |
| Notifications | `/api/notifications` | unread-count, mark-read, mark-all-read |
| Analytics | `/api/analytics` | summary |
| Chat | `/api/chat` | mesaj, `GET /welcome` |
| Settings | `/api/settings` | operatör ayarları |
| Admin Diagnostics | `/api/admin/diagnostics` | `GET /osrm-health`, `POST /mail-test` |

Tam referans: Swagger UI → `http://localhost:8080/swagger-ui/index.html`.

---

## 12. Teknoloji Yığını

### Backend
| Katman | Teknoloji |
|---|---|
| Dil / Runtime | Java 21 |
| Framework | Spring Boot **4.0.5** (webmvc, webflux, data-jpa, security, validation, actuator, mail, devtools) |
| ORM / DB | Spring Data JPA + Hibernate · PostgreSQL (Supabase) · H2 (test) |
| Şema | Hibernate `ddl-auto=update` (Flyway bağımlı ama `spring.flyway.enabled=false`) |
| Güvenlik | Spring Security + JWT (jjwt 0.12.6) |
| Rota | Clarke-Wright + 2-opt (özel), OSRM (gerçek yol), Haversine (fallback) |
| İstatistik | Apache Commons Math3 3.6.1 (güven aralığı, z-score) |
| Audit/Arama | Elasticsearch (Spring Data Elasticsearch) |
| AI | Google Gemini API |
| API Docs | SpringDoc OpenAPI 3.0.2 (Swagger UI) |
| Bildirim | Spring Mail (SMTP) |
| Build / Test | Maven · JUnit 5 + Mockito + AssertJ · JaCoCo 0.8.13 · Lombok |

### Frontend (`cam-sise-frontend-ts`)
| Alan | Teknoloji |
|---|---|
| Çatı | React 19 + **TypeScript** (Create React App) |
| Sunucu durumu | **TanStack React Query** (cache, staleTime 30sn) |
| Form / Doğrulama | **React Hook Form + Zod** (@hookform/resolvers) |
| Grafik | **Recharts** |
| Harita | **Leaflet / react-leaflet** |
| Tasarım | **Tailwind CSS** (+ PostCSS, autoprefixer) |
| Bildirim (UI) | **react-hot-toast** |
| HTTP | **axios** (interceptor'lı, JWT ekler) |
| Routing | react-router-dom v6 |

---

## 13. Frontend Mimarisi (`cam-sise-frontend-ts`)

**73 TS/TSX dosyası, ~9.000 satır.** Katmanlı yapı:

```
src/
├── api/         Backend modülleriyle birebir servisler (authApi, stockApi,
│                routeApi, fleetApi, notificationApi, fillerApi, ...) + axiosConfig
├── hooks/       React Query hook'ları (useStocks, useCollectionRequests, useNotifications)
├── context/     AuthContext (JWT + kullanıcı/rol durumu)
├── components/  Domain bazlı: auth, chat, common, dashboard, filler, map,
│                notification, request, route, stock, vehicle
├── pages/       Sayfalar (Dashboard, Analytics, Fillers, FillerDetail, Users,
│                Settings, Notifications, Stocks, Vehicles, Plans, Requests, ...)
├── types/       api.types.ts (~325 satır), auth.types.ts
└── utils/       constants, errorHandler
```

### 13.1 Rol Bazlı Routing (`ProtectedRoute` + `allowedRoles`)
| Rol | Erişilebilir rotalar |
|---|---|
| Ortak (kimlik doğrulanmış) | `/dashboard`, `/notifications` |
| ADMIN | `/users` |
| COMPANY_STAFF | `/routes`, `/requests`, `/fillers`, `/fillers/:id`, `/stocks`, `/plans`, `/vehicles`, `/customers`, `/settings`, `/analytics` |
| CUSTOMER | `/my-requests` |

### 13.2 Öne Çıkan Bileşenler (danışman geri bildirimlerinin UI karşılığı)
- `EstimatedValue` / `ActualValue` / `ConfidenceBadge` / `DataLegend` — **tahmini vs gerçek veri ayrımı** ve güven aralığının görsel gösterimi.
- `FleetSuggestionCards` + `RouteOptimizationForm` — **filo/araç önerisi** ekranı.
- `RouteMap` (Leaflet) — **OSRM polyline** ile optimize rota görselleştirmesi.
- `FillerDetailPage` — dolumcu detay sayfası (geçmiş, stok, zaiyat).
- `ChatWidget` — Gemini AI asistanı; `NotificationBell` — bildirim entegrasyonu.
- `StaffDashboard` / `CustomerDashboard`, `AnalyticsPage` (Recharts grafikleri).

### 13.3 Konfigürasyon
`.env` → `REACT_APP_API_BASE_URL=http://localhost:8080`. Çalıştırma: `npm start` (dev), `npm run build` (üretim; `build/` mevcut).

---

## 14. Test ve Kalite
- **66 test dosyası, 632 `@Test` metodu** (JUnit 5 + Mockito + AssertJ).
- JaCoCo: **~%80 instruction (17.385/21.821), ~%65 branch (845/1.298)**.
- Spring Boot 4.0 notu: `@WebMvcTest` kaldırıldı; controller testleri `@SpringBootTest` + `MockMvcBuilders` ile yazılır.
- Raporu üretme: `./mvnw test jacoco:report` → `target/site/jacoco/index.html`.
- Doğrulama scriptleri: `test_cvrp.sh`, `test_multi_vehicle_cvrp.sh`, `demo_multi_stop_route.sh`, `test_plan_cancellation.sh`, `check_filler_locations.sh`.

---

## 15. Kurulum ve Çalıştırma

### Backend
```bash
# Gereksinimler: Java 21, Maven 3.8+, PostgreSQL (veya Supabase)
# src/main/resources/application.properties yapılandır (örnek için application-example.properties)
./mvnw spring-boot:run
# Sağlık: GET http://localhost:8080/actuator/health → {"status":"UP"}
# API:    http://localhost:8080/swagger-ui/index.html
```

### Frontend
```bash
cd cam-sise-frontend-ts
npm install
npm start            # http://localhost:3000
```

### Önemli Konfigürasyon Anahtarları (`application.properties`)
- `app.routing.distance-provider=osrm` · `app.routing.cache.ttl-days=30` · `app.routing.constraints.mode=soft`
- `app.anomaly.enabled=false` · `app.anomaly.cron`
- `app.notifications.email.enabled=true` · SMTP ayarları
- `app.seed.enabled=true` · `app.seed.filler-count=250` (sentetik veri üreticisi)
- `jwt.secret`, `jwt.expiration` · `gemini.api-key` · Elasticsearch URI/API key

> 🔐 **Güvenlik:** `application.properties` `.gitignore`'dadır (commit edilmez). Canlı sırlar (DB, JWT, Gemini, Elasticsearch, SMTP) içerdiğinden paylaşımlarda maskelenmeli; sızma şüphesinde anahtarlar rotate edilmelidir.

---

## 16. Bilinen Durumlar / Notlar
- **Flyway kapalı** — şema Hibernate `ddl-auto=update` ile üretiliyor; `db/migration` altında bir migration mevcut ancak çalıştırılmıyor.
- **"Event Sourcing" değil** — domain event'ler Elasticsearch'e denetim günlüğü olarak yazılır; durum event'lerden yeniden kurulmaz.
- **OSRM public demo** bağımlılığı — üretim için self-host önerilir.
- İki frontend klasörü var: **`cam-sise-frontend-ts` güncel ve gerçek olandır**; `cam-sise-frontend` terk edilmiş boş CRA iskeletidir.
