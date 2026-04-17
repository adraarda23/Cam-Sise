# Multi-Stop Route: Adım Adım Örnek

## Senaryo: Bir Araç Birden Fazla Dolumcudan Toplama Yapar mı?

**CEVAP: EVET!** Clarke-Wright algoritması yakın dolumcuları aynı rotada birleştirir.

---

## 📍 Örnek Senaryo: Bursa Bölgesi

### Başlangıç Durumu

**Depot:**
- Gemlik Depo (40.43°N, 29.17°E)

**Onaylanmış Collection Requests:**
- **Filler #10** (Bursa - İnegöl): 35 palet, (40.19°N, 29.37°E)
- **Filler #11** (Bursa - Merkez): 28 palet, (40.18°N, 29.35°E)
- **Filler #12** (Bursa - Nilüfer): 42 ayırıcı, (40.21°N, 29.38°E)

**Araç Kapasitesi:** 1500 palet, 1500 ayırıcı

---

## 🔄 Clarke-Wright Algoritması Adımları

### Adım 1: Initial Routes (Tekli Rotalar)

Her filler için ayrı rota oluştur:

```
Route A: Gemlik → Filler 10 → Gemlik
         Distance: 65 + 65 = 130 km
         Load: 35 palet

Route B: Gemlik → Filler 11 → Gemlik
         Distance: 62 + 62 = 124 km
         Load: 28 palet

Route C: Gemlik → Filler 12 → Gemlik
         Distance: 68 + 68 = 136 km
         Load: 42 ayırıcı
```

**Toplam:** 3 araç, 390 km

---

### Adım 2: Calculate Savings

Hangi fillerleri birleştirmek mesafe tasarrufu sağlar?

**Formül:** `Saving(i,j) = Distance(depot→i) + Distance(depot→j) - Distance(i→j)`

```
Saving(10, 11) = 65 + 62 - 8  = 119 km ✅
Saving(10, 12) = 65 + 68 - 12 = 121 km ✅
Saving(11, 12) = 62 + 68 - 10 = 120 km ✅
```

**En yüksek tasarruf:** Filler 10 & 12 (121 km)

---

### Adım 3: Merge Routes (Rota Birleştir)

**Sıra 1: Merge 10 & 12**

```
BEFORE:
Route A: Gemlik → 10 → Gemlik (130 km)
Route C: Gemlik → 12 → Gemlik (136 km)

CHECK CAPACITY:
35 palet + 42 ayırıcı = 77 units ✅ (< 1500)

CHECK ROUTE ENDS:
Filler 10 is at end of Route A? YES ✅
Filler 12 is at end of Route C? YES ✅

MERGE:
Route X: Gemlik → 10 → 12 → Gemlik
         Distance: 65 + 12 + 68 = 145 km
         Load: 35 palet + 42 ayırıcı

SAVING: 130 + 136 - 145 = 121 km saved! ✅
```

**Mevcut Rotalar:**
- Route X: Gemlik → 10 → 12 → Gemlik (145 km)
- Route B: Gemlik → 11 → Gemlik (124 km)

---

**Sıra 2: Merge X & B (10-12 rotası ile 11)**

```
CHECK SAVINGS:
Saving(10, 11) = 119 km
Saving(12, 11) = 120 km (higher!)

Try merge 12 & 11:

BEFORE:
Route X: Gemlik → 10 → 12 → Gemlik (145 km)
Route B: Gemlik → 11 → Gemlik (124 km)

CHECK CAPACITY:
35 palet + 42 ayırıcı + 28 palet = 105 units ✅

MERGE OPTIONS:
Option A: Gemlik → 10 → 12 → 11 → Gemlik
          Distance: 65 + 12 + 10 + 62 = 149 km

Option B: Gemlik → 11 → 10 → 12 → Gemlik
          Distance: 62 + 8 + 12 + 68 = 150 km

CHOOSE Option A (shorter!) ✅

Route Y: Gemlik → 10 → 12 → 11 → Gemlik
         Distance: 149 km
         Load: 35 palet + 42 ayırıcı + 28 palet
```

---

### Adım 4: 2-opt Optimization (Durak Sırası Optimizasyonu)

Mevcut rota: `10 → 12 → 11`

Tüm olası sıraları dene:

```
1. Gemlik → 10 → 12 → 11 → Gemlik = 149 km
2. Gemlik → 10 → 11 → 12 → Gemlik = 147 km ✅ (BEST!)
3. Gemlik → 11 → 10 → 12 → Gemlik = 150 km
4. Gemlik → 11 → 12 → 10 → Gemlik = 152 km
5. Gemlik → 12 → 10 → 11 → Gemlik = 153 km
6. Gemlik → 12 → 11 → 10 → Gemlik = 151 km
```

**En iyi sıra:** `10 → 11 → 12` (147 km)

---

### Adım 5: Constraint Validation

```
Route: Gemlik → 10 → 11 → 12 → Gemlik

Distance: 147 km ✅ (< 800 km limit)

Duration Calculation:
  Driving time: 147 km ÷ 50 km/h = 176 minutes
  Service time: 3 stops × 30 min = 90 minutes
  Total: 176 + 90 = 266 minutes ✅ (< 600 min limit)

Capacity: 63 palet + 42 ayırıcı ✅ (< 1500 limit)

RESULT: ROUTE APPROVED! ✅
```

---

## 📦 Final Output

### API Response JSON

```json
{
  "plans": [
    {
      "id": 101,
      "depotId": 1,
      "status": "GENERATED",
      "plannedDate": "2026-04-21",
      "totalDistance": {
        "kilometers": 147.0
      },
      "estimatedDuration": {
        "minutes": 266,
        "remainingMinutes": 26
      },
      "totalCapacityPallets": 63,
      "totalCapacitySeparators": 42,
      "assignedVehicleId": null,
      "routeStopsJson": "[
        {
          \"sequence\": 1,
          \"fillerId\": 10,
          \"latitude\": 40.19,
          \"longitude\": 29.37,
          \"pallets\": 35,
          \"separators\": 0
        },
        {
          \"sequence\": 2,
          \"fillerId\": 11,
          \"latitude\": 40.18,
          \"longitude\": 29.35,
          \"pallets\": 28,
          \"separators\": 0
        },
        {
          \"sequence\": 3,
          \"fillerId\": 12,
          \"latitude\": 40.21,
          \"longitude\": 29.38,
          \"pallets\": 0,
          \"separators\": 42
        }
      ]"
    }
  ],
  "vehiclesUsed": 1,
  "totalDistanceKm": 147.0,
  "totalPallets": 63,
  "totalSeparators": 42
}
```

---

## 🚚 Araç Rotası Görsel

```
┌─────────────────────────────────────────────────┐
│  🏭 GEMLIK DEPO (40.43°N, 29.17°E)             │
│  Başlangıç Zamanı: 08:00                        │
└─────────────────────────────────────────────────┘
              ↓
              │ 65 km, ~78 dakika
              ↓
┌─────────────────────────────────────────────────┐
│  📍 STOP 1: Filler #10 (Bursa - İnegöl)        │
│  Lokasyon: 40.19°N, 29.37°E                     │
│  Varış: 09:18                                   │
│  İşlem: 35 PALET yükleme                        │
│  Servis süresi: 30 dakika                       │
│  Ayrılış: 09:48                                 │
│  Toplam yük: 35 palet                           │
└─────────────────────────────────────────────────┘
              ↓
              │ 8 km, ~10 dakika
              ↓
┌─────────────────────────────────────────────────┐
│  📍 STOP 2: Filler #11 (Bursa - Merkez)        │
│  Lokasyon: 40.18°N, 29.35°E                     │
│  Varış: 09:58                                   │
│  İşlem: 28 PALET yükleme                        │
│  Servis süresi: 30 dakika                       │
│  Ayrılış: 10:28                                 │
│  Toplam yük: 63 palet                           │
└─────────────────────────────────────────────────┘
              ↓
              │ 12 km, ~14 dakika
              ↓
┌─────────────────────────────────────────────────┐
│  📍 STOP 3: Filler #12 (Bursa - Nilüfer)       │
│  Lokasyon: 40.21°N, 29.38°E                     │
│  Varış: 10:42                                   │
│  İşlem: 42 AYIRICI yükleme                      │
│  Servis süresi: 30 dakika                       │
│  Ayrılış: 11:12                                 │
│  Toplam yük: 63 palet + 42 ayırıcı              │
└─────────────────────────────────────────────────┘
              ↓
              │ 62 km, ~74 dakika
              ↓
┌─────────────────────────────────────────────────┐
│  🏭 GEMLIK DEPO'ya DÖNÜŞ                        │
│  Varış: 12:26                                   │
│  Toplam süre: 4 saat 26 dakika                  │
│  Toplam mesafe: 147 km                          │
│  Yük: 63 palet + 42 ayırıcı                     │
└─────────────────────────────────────────────────┘
```

---

## ❌ Reddedilen Rota Örneği

### Adana → İzmir → Urfa Rotası

```
INPUT:
- Filler #5 (Adana): 40 palet
- Filler #8 (İzmir): 35 palet
- Filler #18 (Urfa): 30 palet

PROCESS:
┌─────────────────────────────────────────────────┐
│  🏭 GEMLIK DEPO (40.43°N, 29.17°E)             │
└─────────────────────────────────────────────────┘
              ↓ 900 km ❌ (Çok uzak!)
┌─────────────────────────────────────────────────┐
│  📍 Filler #5 (ADANA) - 37.00°N, 35.32°E       │
└─────────────────────────────────────────────────┘
              ↓ 560 km ❌
┌─────────────────────────────────────────────────┐
│  📍 Filler #8 (İZMİR) - 38.42°N, 27.14°E       │
└─────────────────────────────────────────────────┘
              ↓ 850 km ❌
┌─────────────────────────────────────────────────┐
│  📍 Filler #18 (URFA) - 37.15°N, 38.79°E       │
└─────────────────────────────────────────────────┘
              ↓ 950 km ❌
┌─────────────────────────────────────────────────┐
│  🏭 GEMLIK DEPO'ya DÖNÜŞ                        │
└─────────────────────────────────────────────────┘

TOTAL DISTANCE: 3,260 km ❌ (Limit: 800 km)
TOTAL DURATION: 3,912 dakika (65 saat!) ❌ (Limit: 600 dakika)

CONSTRAINT VIOLATION!
Log: "Route violates constraints: distance=3260.00 km, duration=3912 min"

RESULT: ROUTE REJECTED! ❌
```

---

## 💡 Özet

### ✅ Sistem Yapar:
1. **Yakın fillerleri birleştirir** → Bursa bölgesindeki 3 filler → 1 rota
2. **Mesafe tasarrufu sağlar** → 390 km yerine 147 km (243 km tasarruf!)
3. **Araç sayısını azaltır** → 3 araç yerine 1 araç
4. **Durak sırasını optimize eder** → 2-opt ile en kısa rota

### ❌ Sistem Yapmaz:
1. **Uzak bölgeleri birleştirmez** → Adana + İzmir + Urfa ❌
2. **800 km sınırını aşmaz** → Fiziksel olarak imkansız
3. **10 saat sınırını aşmaz** → Günlük çalışma süresi

### 📊 Veri Akışı:

```
INPUT (API Request):
{
  "depotId": 1,
  "plannedDate": "2026-04-21",
  "maxVehicles": 5
}

      ↓ Collection Requests (Approved)

Clarke-Wright Algorithm:
1. Initial Routes
2. Calculate Savings
3. Merge Routes
4. 2-opt Optimization
5. Validate Constraints

      ↓ Valid Routes Only

OUTPUT (API Response):
{
  "plans": [{...}],
  "vehiclesUsed": 1,
  "totalDistanceKm": 147.0,
  "totalPallets": 63,
  "totalSeparators": 42
}
```

---

## 📝 Kod Referansı

**Route Merging Logic:**
`CVRPOptimizer.java:390-407`

```java
// Merge routes based on savings
for (Saving saving : savings) {
    Route routeI = findRouteContaining(routes, requests.get(saving.i));
    Route routeJ = findRouteContaining(routes, requests.get(saving.j));

    if (routeI != null && routeJ != null && routeI != routeJ) {
        if (canMergeRoutes(routeI, routeJ, vehicleCapacity, saving.i, saving.j, requests)) {
            Route merged = mergeRoutes(routeI, routeJ, ...);
            routes.remove(routeI);
            routes.remove(routeJ);
            routes.add(merged);
        }
    }
}
```

**Constraint Validation:**
`CVRPOptimizer.java:418-426`

```java
int duration = routeConstraints.calculateTotalDuration(distance, optimizedRoute.size());
if (routeConstraints.isDistanceAcceptable(distance) &&
        routeConstraints.isDurationAcceptable(duration)) {
    solutions.add(new RouteSolution(optimizedRoute, distance, load));
} else {
    log.warn("Route violates constraints: distance={} km, duration={} min", ...);
}
```
