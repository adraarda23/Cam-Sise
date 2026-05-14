# Refactor — Demo Senaryoları ve Hocaya Sunum Notları

Bu doküman hocanın 2026-05 feedback'ine karşılık yapılan 5 haftalık refactor sonunda
jüri sunumunda canlı gösterilecek senaryoları içerir.

## Hocanın 7 Feedback Maddesi → Yapılan Müdahale

| # | Hoca Feedback'i | Karşılığı (kod / dosya) | Demo'da Nereden Gösterilir |
|---|---|---|---|
| 1 | Sentetik veri ile anomali tespiti + alarm | `analytics/anomaly/` paketi: `AnomalyDetectionService`, `ZScoreDetector` (içerik `AnomalyDetectionService` içinde), `DayOfWeekBaseline`, `AnomalyScheduler` (proactive 06:00), `InventoryEventHandler#handleAssetInflowRecorded` (reactive) | Inflow kaydet → `StockAnomalyDetected` event → NotificationBell'de kırmızı badge + email |
| 2 | Sistem araç önerisi | `logistics/service/fleet/VehicleAssignmentService` → 3 alternatif (En Ucuz / En Az Araç / Dengeli) | `RouteOptimizationPage` → "Filo Önerisi Al" butonu → 3 kart |
| 3 | Kuş uçuşu yerine yol ağı | `logistics/service/routing/`: `DistanceProvider` interface + `HaversineDistanceProvider`, `OsrmDistanceProvider` (router.project-osrm.org), `CachedDistanceProvider` decorator + `DistanceCacheEntry` DB tablo | `application.properties: app.routing.distance-provider=osrm` → tekrar optimize → harita yol üzerinde polyline çizer |
| 4 | Tutarlılık kontrolleri (500 palet → 300+300 yapılamasın) | **Zaten vardı**: `CollectionRequestService.createManual` 82-122 satır arası PENDING merge + availability validation | Coca-Cola Bursa için 300 palet talep et (kabul) → tekrar 250 talep et (reject — "kullanılabilir 200") |
| 5 | Araç kapasitesi-talep eşlemesi | Filo Öneri kartlarında her seçenek için `totalCapacity` ↔ `totalDemand` ile `slackPercent` | Filo Önerisi kartında "Atıl kapasite: %15" |
| 6 | İstatistiksel hata oranı / güven aralığı (45 ± 3.4) | `shared/domain/vo/ConfidenceInterval` value object, `LossRate` 3-arg constructor (mean, stdDev, sampleSize), `StockForecastService` → `ConfidenceInterval`, REST endpoint `GET /api/inventory/stocks/{fillerId}/{assetType}/forecast` | StocksPage'te "Tahmini kullanılabilir: ~123 ± 4"; FillerDetailPage'te `ReferenceArea` ile 7 günlük confidence band |
| 7 | UI tutarlılığı (kesin vs tahmin farklı renk) | `tailwind.config.js` color tokens: `actual` (yeşil), `estimated` (turuncu), `anomaly` (kırmızı); `components/common/{ActualValue, EstimatedValue, ConfidenceBadge, DataLegend}` | Navbar'da `DataLegend` chip'i, her sayfada tutarlı renk kullanımı |

## Demo Adımları (15 dakikalık sunum)

### 1. Açılış (1 dk)
- Sayfa: `/dashboard`
- Anlat: "Modüler monolit, Spring Boot 4, React 19. Hocanın 7 noktasının her birini koda yansıttık."
- Göster: Navbar'daki `DataLegend` chip'i ("Gerçekleşmiş / Tahmin / Anomali").

### 2. Stok ekranı — Kesin vs Tahmin (3 dk)
- Sayfa: `/stocks`
- Bir filler kartına bak. **Yeşil "127"** fiili stok, altında **turuncu italik "~123 ± 4"** tahmini kullanılabilir.
- Tooltip: "Bu değer son 30 günlük veriye dayalı tahmindir. Güven seviyesi %95."
- Sebep: `LossRate` artık `(percentage, stdDev, sampleSize)` ve `EstimatedValue` componenti `1.96 × stdDev / √n` ile margin hesaplıyor.

### 3. Forecast grafiği (2 dk)
- "Detay →" linkine tıkla → `/fillers/:id`
- Üst: fiili + 7 gün tahmin kartı (`ConfidenceBadge`: "Yüksek/Orta/Düşük güven").
- Alt: 7 günlük forecast band'i (Recharts `ReferenceArea` ile aralık görselleştirmesi).
- Sebep: `StockForecastService` daily net flow → `DescriptiveStatistics` → `ConfidenceInterval`.

### 4. Anomali tespiti ve bildirim (3 dk)
- İki sekme aç: birinde `/stocks`, diğerinde `/analytics`.
- Stocks sekmesinde bir filler için "Stok Girişi Kaydet" → büyük bir spike değer (örn 500 adet) → kaydet.
- Geçmiş baseline ile karşılaştırıldığında z-score > 3 → `StockAnomalyDetected` event yayınlanır.
- Analytics sekmesinde: kırmızı banner ("Son 24 saatte 1 kritik anomali"), Anomali Akışı tablosu yenilenir.
- Navbar'da bildirim çanı kırmızı badge gösterir.
- Sebep:
  - `AnomalyDetectionService.checkInflow` (reactive)
  - `AnomalyScheduler` cron 06:00 (proactive — günde bir tüm filler'lar için)
  - `NotificationEventHandler` event'i in-app + email'e çevirir

### 5. Tutarlılık kontrolü (2 dk)
- Sayfa: customer rolüyle `/my-requests`
- Bir filler için 300 palet talebi oluştur (kabul).
- Tekrar 250 palet talebi oluştur (reject — toast: "Toplam talep ... kullanılabilir stoktan ... fazla olamaz").
- Bu hocanın özel olarak vurguladığı senaryo ("500 palet var, 300+300 yapılamasın").
- Sebep: `CollectionRequestService.createManual` mantığı (zaten vardı, demo'da öne çıkardık).

### 6. Filo önerisi ve OSRM (3 dk)
- Sayfa: `/routes`
- "Filo Önerisi Al" → 3 kart: **En Ucuz**, **En Az Araç**, **Dengeli**
- Her kart: araç sayısı, kapasite, atıl yüzdesi, tahmini maliyet (TRY).
- Kart seç → onay banner.
- Sonra "Rotaları Optimize Et" → harita modal'da yol-takipli polyline (OSRM aktifse).
- Sebep: `VehicleAssignmentService`, `OsrmDistanceProvider`, `RouteOptimizationService.persistRouteGeometry`.

### 7. Kapanış (1 dk)
- "7 madde de koda yansıdı, 635 backend testi yeşil, frontend tam migration (React Query + Hook Form + Recharts + Toast). Hocanın dikkat çektiği UI tutarlılığı tek bir tasarım sistemiyle (`actual`/`estimated`/`anomaly` color tokens) çözüldü."

## Test İstatistikleri

| Modül | Test sayısı | Durum |
|---|---|---|
| Backend toplam | 635 | ✅ Tümü yeşil |
| Hafta 1 yeni testler | 56 (LossRate, StockMovement, ConfidenceInterval, LossEstimation, StockForecast, DataSeeder) | ✅ |
| Hafta 2 yeni testler | 20 (AnomalySeverity, AnomalyDetection, DayOfWeekBaseline, AnomalyScheduler, NotificationService) | ✅ |
| Hafta 3 yeni testler | 11 (CachedDistanceProvider, VehicleAssignmentService) | ✅ |
| Frontend | `npm run build` ✅ (warnings var, error yok) | ✅ |

## Konfigürasyon Anahtarları

Demo'da hangi flag'i nasıl açacağını bilmen lazım — `application.properties`:

```properties
# Demo'da OSRM'yi açmak için:
app.routing.distance-provider=osrm
app.routing.cache.enabled=true

# Sıkı constraint mode (jüri yansıması için):
app.routing.constraints.mode=hard

# Anomaly scheduler:
app.anomaly.enabled=true
app.anomaly.cron=0 0 6 * * *

# Email bildirimleri:
app.notifications.email.enabled=true
# + spring.mail.host/port/username/password Spring Boot mail starter ile
```

## Bilinen Kısıtlar

- OSRM public demo rate limit'i var — production'da self-host OSRM önerilir.
- Email SMTP config'i için Gmail app password gerekir; demo'da MailHog local container kullanmak kolay.
- DataSeeder mevcut DB'de veri varsa skip eder (`poolOperatorRepository.count() > 0` kontrolü).
  Temiz seed için Supabase üzerinden tablo drop edilmeli.
