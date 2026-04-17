# Kalan Görevler ve Öncelikler

> Son Güncelleme: 17 Nisan 2026
> Durum: Hafta 3 tamamlandı, Hafta 4'e geçiliyor

---

## ✅ Tamamlanan (Hafta 3 Sonu)

### Backend Core
- ✅ DDD altyapısı (AggregateRoot, Entity, ValueObject, DomainEvent)
- ✅ Domain Event sistemi (Publisher, Store, Listener, Async)
- ✅ 4 modül (Core, Inventory, Logistics, Auth)
- ✅ 10 Aggregate Root + 35+ Domain Event
- ✅ JWT authentication + Role-based authorization
- ✅ Multi-tenancy (Pool Operator tenant)
- ✅ Flyway migrations + PostgreSQL schema
- ✅ Sentetik veri üreticisi (DataSeeder)

### Inventory Module
- ✅ FillerStock & LossRecord domain model
- ✅ Moving average zaiyat tahmini
- ✅ Eşik kontrolü → otomatik CollectionRequest
- ✅ Event handlers (stock threshold, collection completed)
- ✅ Rol bazlı veri filtresi

### Logistics Module - CVRP Optimization
- ✅ **Clarke-Wright Savings Algorithm** (custom implementation)
- ✅ **Multi-vehicle CVRP optimizer** (CVRPOptimizer.java)
- ✅ **Route constraints** (800km, 10h, 30min/stop)
- ✅ **2-opt local search** optimization
- ✅ **Geographic clustering** (prevents Adana→İzmir→Urfa routes)
- ✅ **Constraint enforcement** (auto-reject infeasible routes)
- ✅ RouteOptimizationService & Controller
- ✅ POST /api/logistics/optimize/multi-vehicle endpoint
- ✅ Haversine distance calculator
- ✅ Comprehensive documentation (3 markdown files)
- ✅ Test scripts (demo & multi-vehicle test)

### Documentation
- ✅ PROJECT_DOCUMENTATION.md (full domain model)
- ✅ CVRP_IMPLEMENTATION_SUMMARY.md
- ✅ MULTI_STOP_ROUTE_EXAMPLE.md
- ✅ INPUT_OUTPUT_EXAMPLE.md
- ✅ README.md güncellemesi

---

## 🔄 Devam Eden / Kısa Vadeli (Hafta 4)

### 1. Swagger/OpenAPI Dokümantasyonu ⚡ (Yüksek Öncelik)
**Süre:** 2-3 saat
**Açıklama:** Tüm endpoint'ler için OpenAPI annotations ekle

**Alt Görevler:**
- [ ] Logistics controller'lara `@Operation`, `@ApiResponse`, `@Tag` annotations ekle
- [ ] Inventory controller'lara Swagger annotations ekle
- [ ] Core controller'lara Swagger annotations ekle
- [ ] DTO'lara `@Schema` annotations ekle (description, example)
- [ ] Swagger UI üzerinde test et
- [ ] Authentication için `@SecurityRequirement` ekle

**Dosyalar:**
- `logistics/controller/*.java`
- `inventory/controller/*.java`
- `core/controller/*.java`

---

### 2. Integration Tests (Multi-Vehicle CVRP) ⚡ (Yüksek Öncelik)
**Süre:** 4-5 saat
**Açıklama:** Uçtan uca CVRP optimization testi

**Test Senaryoları:**
- [ ] Multiple collection requests → Multi-vehicle optimization → Valid plans created
- [ ] Geographic spread → Constraint violations → Routes rejected
- [ ] Nearby fillers → Route merging → Multi-stop route
- [ ] Capacity overflow → Multiple vehicles used
- [ ] Empty request pool → Error handling

**Dosya:**
- `src/test/java/ardaaydinkilinc/Cam_Sise/logistics/integration/RouteOptimizationIntegrationTest.java`

---

### 3. API Response İyileştirmesi 📊 (Orta Öncelik)
**Süre:** 2-3 saat
**Açıklama:** routeStopsJson → routeStops (parsed object list)

**Değişiklik:**
```java
// ❌ Şu an
"routeStopsJson": "[{\"fillerId\":10,...}]"

// ✅ Olmalı
"routeStops": [
  {
    "sequence": 1,
    "fillerId": 10,
    "latitude": 40.19,
    "longitude": 29.37,
    "pallets": 35,
    "separators": 0
  }
]
```

**Dosyalar:**
- `logistics/domain/CollectionPlan.java` - Add `@Transient List<RouteStop> routeStops` getter
- `logistics/domain/vo/RouteStop.java` - Use existing VO
- `RouteOptimizationService.java` - Parse JSON to RouteStop list

---

### 4. Frontend Panel İskeleti 🌐 (Orta-Yüksek Öncelik)
**Süre:** 8-10 saat
**Açıklama:** React + Vite + React Router + JWT interceptor

**Alt Görevler:**
- [ ] Proje setup (Vite + React + TypeScript)
- [ ] Routing structure (Login, Dashboard, Admin, Customer)
- [ ] JWT storage (localStorage) + axios interceptor
- [ ] Login sayfası (POST /api/auth/login)
- [ ] Rol bazlı navigation (ADMIN, COMPANY_STAFF, CUSTOMER)
- [ ] Basic layout (Navbar, Sidebar, Content area)

**Teknoloji Stack:**
- React 18 + TypeScript
- Vite (build tool)
- React Router v6
- Axios (HTTP client)
- Tailwind CSS veya Material-UI

---

## ⬜ Bekleyen / Orta Vadeli

### 5. COMPANY_STAFF Dashboard 📊
**Süre:** 12-15 saat
**Components:**
- [ ] Envanter özeti kartları (toplam stok, kritik stoklar)
- [ ] Talep havuzu tablosu (onay bekleyenler, onaylananlar)
- [ ] Rota optimizasyonu butonu → Modal (depot seç, tarih seç, araç sayısı)
- [ ] Oluşturulan rotalar listesi
- [ ] Filtre & arama

**API Entegrasyonları:**
- GET /api/inventory/stocks (summary)
- GET /api/logistics/collection-requests?status=PENDING
- POST /api/logistics/optimize/multi-vehicle
- GET /api/logistics/collection-plans

---

### 6. CUSTOMER Dashboard 🏭
**Süre:** 8-10 saat
**Components:**
- [ ] Kendi stok durumu (palet & separator)
- [ ] Zaiyat oranı görüntüleme
- [ ] Manuel talep formu (asset tipi, miktar, notlar)
- [ ] Kendi talep geçmişi
- [ ] Onay durumu takibi

**API Entegrasyonları:**
- GET /api/inventory/stocks/my-stocks (filtered by fillerId)
- GET /api/inventory/loss-records/my-records
- POST /api/logistics/collection-requests/manual
- GET /api/logistics/collection-requests/my-requests

---

### 7. ADMIN Panel 👨‍💼
**Süre:** 6-8 saat
**Components:**
- [ ] Kullanıcı listesi (CRUD)
- [ ] Rol atama
- [ ] Pool Operator yönetimi
- [ ] Filler listesi
- [ ] Sistem istatistikleri

**API Entegrasyonları:**
- GET/POST/PUT/DELETE /api/users
- GET/POST/PUT /api/pool-operators
- GET/POST/PUT /api/fillers

---

### 8. Harita Entegrasyonu 🗺️
**Süre:** 10-12 saat
**Kütüphane:** Leaflet.js + React-Leaflet
**Features:**
- [ ] Depot marker (home icon)
- [ ] Filler markers (factory icons)
- [ ] Route polyline (araç rotası çizimi)
- [ ] Popup'lar (filler bilgileri, toplama miktarı)
- [ ] Route sequence numbers on map

**Data Flow:**
```
CollectionPlan.routeStops → Extract coordinates
→ Leaflet Polyline with markers
→ Color-coded by vehicle
```

---

### 9. Performance Optimization 🚀
**Süre:** 4-6 saat
- [ ] FillerStock endpoint'leri için pagination (Spring Data Pageable)
- [ ] CollectionRequest listesi için pagination
- [ ] CVRP optimizer için caching (distance matrix)
- [ ] Database indexes (fillerId, poolOperatorId, status)
- [ ] N+1 query problemlerini düzelt (fetch join)

---

### 10. Docker & Deployment 🐳
**Süre:** 6-8 saat
- [ ] Dockerfile (multi-stage build: Maven + OpenJDK)
- [ ] docker-compose.yml (app + PostgreSQL)
- [ ] Environment variables için .env support
- [ ] Supabase cloud deployment guide
- [ ] CI/CD pipeline (GitHub Actions)

---

## 📝 İyileştirme Fikirleri (Gelecek)

### Multi-day Planning
**Açıklama:** Atanamayanlar için otomatik ertesi gün planlaması
**Süre:** 8-10 saat
**Implementation:**
- Unassigned requests'i tespit et
- plannedDate += 1 day
- Re-run optimization for next day
- Notify user about rescheduled requests

---

### Time Windows (Dolumcu Çalışma Saatleri)
**Açıklama:** Her filler için operation hours (08:00-17:00)
**Süre:** 6-8 saat
**Changes:**
- Add `operatingHours` to Filler entity
- Modify CVRP to respect time windows
- Calculate arrival times at each stop
- Reject routes that violate time windows

---

### Vehicle Type Selection
**Açıklama:** Rota uzunluğuna göre otomatik araç tipi seçimi
**Süre:** 4-5 saat
**Logic:**
- Short routes (<200km) → Small vehicle
- Medium routes (200-500km) → Medium vehicle
- Long routes (500-800km) → Large vehicle

---

### Driver Assignment
**Açıklama:** Sürücü bilgisi ve çalışma saati takibi
**Süre:** 8-10 saat
**New Domain:**
- Driver aggregate (name, license, workingHours)
- Driver → Vehicle assignment
- Driver working hour constraints
- Driver rotation scheduling

---

### Cost Calculation
**Açıklama:** Yakıt maliyeti ve rota karlılık analizi
**Süre:** 6-8 saat
**Calculation:**
- Distance × fuel consumption × fuel price
- Labor cost (driver hours)
- Vehicle maintenance cost estimation
- ROI analysis per route

---

### Real-time Tracking (İleri Seviye)
**Açıklama:** Araç konumu GPS tracking
**Süre:** 15-20 saat
**Stack:**
- WebSocket / Server-Sent Events
- GPS simulator (for demo)
- Live map updates
- ETA recalculation

---

## Öncelik Sıralaması (Hafta 4)

### Yüksek Öncelik (Mutlaka Yapılmalı)
1. ⚡ Swagger/OpenAPI dokümantasyonu (2-3h)
2. ⚡ Integration tests (4-5h)
3. 🌐 Frontend iskelet + Login (8-10h)
4. 📊 COMPANY_STAFF dashboard (temel özellikler) (8-10h)

**Toplam:** ~22-28 saat (Hafta 4 kapasitesi)

---

### Orta Öncelik (İmkan Varsa)
5. 📊 API response iyileştirmesi (2-3h)
6. 🏭 CUSTOMER dashboard (6-8h)
7. 🗺️ Harita entegrasyonu (temel özellikler) (6-8h)

**Toplam:** +14-19 saat

---

### Düşük Öncelik (Tez Sonrası)
8. 👨‍💼 ADMIN panel (gelecek)
9. 🚀 Performance optimization (gelecek)
10. 🐳 Docker & deployment (gelecek)
11. İyileştirme fikirleri (uzun vadeli)

---

## Hafta 4 Hedefleri

### Minimum Viable Demo (MVP)
- ✅ Backend fully functional (already done!)
- ⬜ Login + JWT auth working on frontend
- ⬜ COMPANY_STAFF can trigger CVRP optimization
- ⬜ COMPANY_STAFF can view generated routes
- ⬜ CUSTOMER can submit manual requests
- ⬜ Basic map showing depot + fillers (optional)
- ⬜ Swagger docs for all endpoints

### Tez Yazımı (Paralel)
- ⬜ Giriş bölümü (problem tanımı, motivasyon)
- ⬜ Literatür taraması (CVRP, DDD, multi-tenancy)
- ⬜ Sistem tasarımı (mimari diyagramlar)
- ⬜ Implementation (kod örnekleri, algoritma açıklaması)
- ⬜ Test sonuçları (CVRP performance, screenshots)
- ⬜ Sonuç ve gelecek çalışmalar

---

## Risk ve Engeller

### Zaman Kısıtları
- **Risk:** Frontend development zaman alabilir
- **Mitigasyon:** MVP'ye odaklan, advanced features'ları ertele

### Teknik Zorluklar
- **Risk:** Harita entegrasyonu kompleks olabilir
- **Mitigasyon:** Başlangıçta static markers, route çizimi sonra

### Veri Kalitesi
- **Risk:** Synthetic data gerçekçi olmayabilir
- **Mitigasyon:** Filler'ları belirli bölgelerde cluster'la (Bursa, İstanbul, İzmir)

---

## Başarı Kriterleri

### Backend (Tamamlandı ✅)
- [x] Clarke-Wright CVRP working
- [x] Multi-vehicle routes generated
- [x] Constraints enforced (800km, 10h)
- [x] REST API complete
- [x] Documentation comprehensive

### Frontend (Hafta 4 Hedefi)
- [ ] Login works, JWT stored
- [ ] Role-based navigation
- [ ] COMPANY_STAFF can optimize routes
- [ ] CUSTOMER can submit requests
- [ ] Basic map visualization

### Tez (Hafta 4 Hedefi)
- [ ] 50+ sayfa yazım
- [ ] Tüm bölümler taslak halinde
- [ ] Diyagramlar ve ekran görüntüleri hazır
- [ ] Algoritma performance analizi yapılmış

---

**Not:** Bu liste dinamik bir dokümandır. Her hafta güncellenir.
