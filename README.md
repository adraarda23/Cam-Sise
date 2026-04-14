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
┌─────────────────────────────────────────────────┐
│              Modular Monolith                   │
│                                                 │
│  ┌──────────────────┐  ┌─────────────────────┐  │
│  │  Inventory       │  │  Logistics          │  │
│  │  Module          │  │  Module             │  │
│  │                  │  │                     │  │
│  │  Controller      │  │  Controller         │  │
│  │  Service         │  │  Service            │  │
│  │  Repository      │  │  Repository         │  │
│  │  Domain          │  │  Domain             │  │
│  └──────────────────┘  └─────────────────────┘  │
│          │                      │               │
│          └──────────┬───────────┘               │
│                     │ Domain Events             │
└─────────────────────┼───────────────────────────┘
                      ▼
                 PostgreSQL
```

**Dış yapı:** Modular Monolith — her bounded context kendi modülünde  
**İç yapı:** Layered Architecture — her modülde Controller → Service → Repository → Domain  
**Tasarım prensibi:** Domain-Driven Design (DDD)

---

## Domain Modeli (Ubiquitous Language)

| Terim | Açıklama |
|---|---|
| **Pool Operator (Havuz Operatörü)** | Sistemin sahibi. Bursa/Gemlik merkezli, tek depo. |
| **Depot (Depo)** | Gemlik'teki merkez tesis. Araçların çıkış ve dönüş noktası. |
| **Glass Manufacturer (Cam Üreticisi)** | Operatörden palet alır, camı dolumcuya sevk eder. Aracı. 5 adet. |
| **Filler (Dolumcu)** | İçecek/gıda dolumu yapan fabrikalar (Coca-Cola, Pepsi vb.). 250 adet. |
| **Asset** | Takibi yapılan palet veya separatör. |
| **Inflow** | Bir dolumcuya ulaşan asset kaydı (başlangıç bakiyesi). |
| **Loss Record (Zaiyat Kaydı)** | Dolumcudaki kayıp/hasar oranı. Girilmezse moving average ile tahmin edilir. |
| **Collection Request (Toplama Talebi)** | Dolumcudan toplama isteği. Otomatik (eşik tetikli) veya Manuel (dolumcudan). |
| **Collection Plan (Toplama Planı)** | CVRP optimizer çıktısı. Hangi aracın hangi sırayla hangi dolumcuları ziyaret ettiği. |
| **Vehicle (Araç)** | Toplama için kullanılan araç. Kapasitesi ve bağlı olduğu depo tanımlı. |

---

## Kullanıcı Rolleri

| Rol | Açıklama |
|---|---|
| `ADMIN` | Kullanıcı CRUD, sistem ayarları, zaiyat oran varsayılanları. Tam yetki. |
| `COMPANY_STAFF` | Operasyon personeli. Tüm dolumcuları, asset'leri, talepleri görür. Rota oluşturur. |
| `CUSTOMER` | Dolumcu firmanın kullanıcısı. Sadece kendi stoku ve taleplerini görür. Manuel talep açar. |

Yetkilendirme: **JWT tabanlı**, rol odaklı, stateless.

---

## Teknoloji Stack

| Katman | Teknoloji |
|---|---|
| Backend | Java 21 + Spring Boot |
| ORM | Spring Data JPA + Hibernate |
| Veritabanı | PostgreSQL (Supabase — cloud, shared) |
| Migration | Flyway |
| Güvenlik | Spring Security + JWT (jjwt) |
| Rota Optimizasyonu | Jsprit veya OR-Tools (Hafta 3'te eklenecek) |
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

### Inventory Module
Dolumculardaki asset bakiyelerinin takibi.

- Asset inflow / geri toplama hareketleri
- Dolumcu bazında anlık stok hesabı
- Moving average tabanlı zaiyat tahmini
- Raporlama endpoint'leri
- Rol bazlı veri filtresi

### Logistics Module
Geri toplama planlaması ve rota optimizasyonu.

- Unified Collection Pool (otomatik + manuel talepler tek havuzda)
- CVRP tabanlı rota optimizasyonu (tek depo: Gemlik)
- Haversine mesafe matrisi
- Toplama planı oluşturma ve kalıcı saklama

---

## Proje Yapısı

```
src/
├── main/
│   ├── java/
│   │   └── com.example.havuz/
│   │       ├── inventory/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   └── domain/
│   │       ├── logistics/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   └── domain/
│   │       ├── auth/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   └── domain/
│   │       └── shared/
│   │           └── domain/   ← paylaşılan value object'ler
│   └── resources/
│       ├── db/
│       │   └── migration/    ← Flyway SQL dosyaları (V1__, V2__...)
│       └── application.properties
└── test/
```

---

## Roadmap

> Durum ikonları: ✅ Tamamlandı · 🔄 Devam ediyor · ⬜ Bekliyor

### Hafta 1 — Temel, Domain ve Auth İskeleti

- ✅ Proje iskeleti ve build ayarları
- ✅ Supabase bağlantısı ve Flyway migration yapısı
- ✅ Ubiquitous language sözlüğü (dokümantasyon)
- ⬜ Inventory bounded context domain modeli
- ⬜ Logistics bounded context domain modeli
- ✅ JWT auth iskeleti (login endpoint, token üretimi)
- ✅ 3 rollü erişim filtresi (ADMIN, COMPANY_STAFF, CUSTOMER)
- ⬜ Sentetik veri üreticisi (1 depo, 5 cam üreticisi, 250 dolumcu, geçmiş veri)
- ⬜ Smoke test — `GET /actuator/health` → UP

### Hafta 2 — Envanter Yönetimi Modülü

- ⬜ Asset, Inflow, LossRecord entity + repository katmanı
- ⬜ Hareket kayıt servisi (inflow / geri toplama)
- ⬜ Dolumcu bazında anlık stok hesaplama servisi
- ⬜ Moving average tabanlı zaiyat tahmin servisi
- ⬜ Raporlama REST endpoint'leri
- ⬜ Rol bazlı veri filtresi (CUSTOMER sadece kendi verisini görür)
- ⬜ Birim ve entegrasyon testleri
- ⬜ Swagger dokümantasyonu — inventory endpoint'leri

### Hafta 3 — Rota Optimizasyonu ve Unified Collection Pool

- ⬜ Rota optimizasyon kütüphanesi entegrasyonu
- ⬜ Haversine mesafe matrisi
- ⬜ CVRP problem formülasyonu (tek depo: Gemlik)
- ⬜ CollectionRequest domain modeli + durum makinesi (PENDING → APPROVED → SCHEDULED → COMPLETED)
- ⬜ Otomatik talep üretimi (envanter eşik dinleyicisi)
- ⬜ Manuel talep endpoint'i (CUSTOMER rolü)
- ⬜ Talep onay/red servisi (COMPANY_STAFF rolü)
- ⬜ Toplama planı oluşturma ve kalıcı saklama
- ⬜ Uçtan uca test senaryosu

### Hafta 4 — Frontend Panel ve Tez Yazımı

- ⬜ Frontend iskeleti (framework TBD) + routing + JWT interceptor
- ⬜ Login ekranı ve rol bazlı yönlendirme
- ⬜ ADMIN paneli — kullanıcı yönetimi
- ⬜ COMPANY_STAFF dashboard — envanter özeti, talep havuzu, rota ekranı
- ⬜ CUSTOMER dashboard — stok, zaiyat, manuel talep formu
- ⬜ Harita entegrasyonu — Gemlik depo + dolumcu lokasyonları + aktif rota
- ⬜ Demo senaryosu hazırlığı
- ⬜ Tez dökümanı yazımı

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
