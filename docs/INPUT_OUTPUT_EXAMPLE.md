# Multi-Stop Route Input/Output Examples

## Bir Araç Birden Fazla Dolumcudan Toplama Yapabilir mi?

**EVET!** Sistem Clarke-Wright algoritması ile birden fazla dolumcuyu aynı rotada birleştiriyor.

## Örnek: Adana → İzmir → Urfa Rotası

### ❌ BÖYLE BİR ROTA OLUŞMAZ

```
Gemlik Depo (40.43, 29.17)
   ↓ 900 km
Adana (37.00, 35.32)
   ↓ 560 km
İzmir (38.42, 27.14)
   ↓ 850 km
Urfa (37.15, 38.79)
   ↓ 950 km
Gemlik'e Dönüş

Toplam: ~3260 km ❌ (Limit: 800 km)
Süre: ~65 saat ❌ (Limit: 10 saat)
```

**Sistem bu rotayı reddeder!** Log çıktısı:

```
WARN: Route violates constraints: distance=3260.00 km, duration=3920 min
```

---

## ✅ BÖYLE BİR ROTA OLUŞUR

### Örnek 1: Bursa Bölgesi Multi-Stop Route

```
INPUT (API Request):
POST /api/logistics/optimize/multi-vehicle
{
  "depotId": 1,              // Gemlik Depo
  "plannedDate": "2026-04-21",
  "maxVehicles": 5
}

Onaylanmış Collection Requests:
- Filler #10 (Bursa): 35 palet
- Filler #11 (Bursa): 28 palet
- Filler #12 (Bursa): 42 ayırıcı
- Filler #15 (Eskişehir): 50 ayırıcı
```

**ROUTE GENERATION (Clarke-Wright Algoritması):**

1. **Initial Routes** (Her filler için tekli rota):
   ```
   Route A: Gemlik → Filler 10 → Gemlik (120 km)
   Route B: Gemlik → Filler 11 → Gemlik (130 km)
   Route C: Gemlik → Filler 12 → Gemlik (125 km)
   Route D: Gemlik → Filler 15 → Gemlik (180 km)
   ```

2. **Calculate Savings** (Hangi fillerleri birleştirmek karlı?):
   ```
   Saving(10,11) = 120 + 130 - 15 = 235 km tasarruf ✅ (yakınlar)
   Saving(10,12) = 120 + 125 - 20 = 225 km tasarruf ✅
   Saving(10,15) = 120 + 180 - 160 = 140 km tasarruf ⚠️ (uzaklar)
   Saving(11,12) = 130 + 125 - 18 = 237 km tasarruf ✅ (en iyi!)
   ```

3. **Merge Routes** (En yüksek tasarruftan başla):
   ```
   Step 1: Merge 11 & 12 → Route X: Gemlik → 11 → 12 → Gemlik (165 km)
   Step 2: Merge 10 & X  → Route Y: Gemlik → 10 → 11 → 12 → Gemlik (195 km)

   Filler 15 uzak, ekleme distance > 800 km olur ❌
   → Ayrı route: Route Z: Gemlik → 15 → Gemlik (180 km)
   ```

4. **2-opt Optimization** (Durak sırası optimizasyonu):
   ```
   Route Y sırasını kontrol et:
   - Gemlik → 10 → 11 → 12 → Gemlik (195 km) ✅
   - Gemlik → 10 → 12 → 11 → Gemlik (198 km) ❌ (daha uzun)
   - Gemlik → 11 → 10 → 12 → Gemlik (192 km) ✅ (en iyi!)

   Final: Gemlik → 11 → 10 → 12 → Gemlik
   ```

5. **Constraint Validation**:
   ```
   Route Y: 192 km ✅ (< 800 km)
           Duration: 231 dakika ✅ (< 600 dakika)
           Capacity: 63 palet + 42 ayırıcı ✅ (< 1500)

   Route Z: 180 km ✅
           Duration: 216 dakika ✅
           Capacity: 50 ayırıcı ✅
   ```

**OUTPUT (API Response):**

```json
{
  "plans": [
    {
      "id": 101,
      "depotId": 1,
      "status": "GENERATED",
      "plannedDate": "2026-04-21",
      "totalDistance": {
        "kilometers": 192.0
      },
      "estimatedDuration": {
        "minutes": 231
      },
      "totalCapacityPallets": 63,
      "totalCapacitySeparators": 42,
      "assignedVehicleId": null,
      "routeStopsJson": "[
        {\"sequence\":1, \"fillerId\":11, \"latitude\":40.18, \"longitude\":29.35, \"pallets\":28, \"separators\":0},
        {\"sequence\":2, \"fillerId\":10, \"latitude\":40.19, \"longitude\":29.37, \"pallets\":35, \"separators\":0},
        {\"sequence\":3, \"fillerId\":12, \"latitude\":40.21, \"longitude\":29.38, \"pallets\":0, \"separators\":42}
      ]"
    },
    {
      "id": 102,
      "depotId": 1,
      "status": "GENERATED",
      "plannedDate": "2026-04-21",
      "totalDistance": {
        "kilometers": 180.0
      },
      "estimatedDuration": {
        "minutes": 216
      },
      "totalCapacityPallets": 0,
      "totalCapacitySeparators": 50,
      "assignedVehicleId": null,
      "routeStopsJson": "[
        {\"sequence\":1, \"fillerId\":15, \"latitude\":39.78, \"longitude\":30.52, \"pallets\":0, \"separators\":50}
      ]"
    }
  ],
  "vehiclesUsed": 2,
  "totalDistanceKm": 372.0,
  "totalPallets": 63,
  "totalSeparators": 92
}
```

---

## Route Visualization

### Araç #1 Rotası (3 durak):

```
🏭 DEPOT: Gemlik (40.43, 29.17)
        ↓ 65 km, 78 dakika
📍 STOP 1: Filler #11 - Bursa
        → Pickup: 28 palet
        → Service time: 30 dakika
        ↓ 8 km, 10 dakika
📍 STOP 2: Filler #10 - Bursa
        → Pickup: 35 palet
        → Service time: 30 dakika
        ↓ 12 km, 14 dakika
📍 STOP 3: Filler #12 - Bursa
        → Pickup: 42 ayırıcı
        → Service time: 30 dakika
        ↓ 107 km, 128 dakika
🏭 DEPOT: Gemlik'e Dönüş

📊 Toplam: 192 km, 231 dakika (3.85 saat)
📦 Yük: 63 palet + 42 ayırıcı
```

### Araç #2 Rotası (1 durak):

```
🏭 DEPOT: Gemlik (40.43, 29.17)
        ↓ 90 km, 108 dakika
📍 STOP 1: Filler #15 - Eskişehir
        → Pickup: 50 ayırıcı
        → Service time: 30 dakika
        ↓ 90 km, 108 dakika
🏭 DEPOT: Gemlik'e Dönüş

📊 Toplam: 180 km, 216 dakika (3.6 saat)
📦 Yük: 50 ayırıcı
```

---

## Adana → İzmir → Urfa Senaryosu

### INPUT:
```
Onaylanmış requestler:
- Filler #5 (Adana): 40 palet
- Filler #8 (İzmir): 35 palet
- Filler #18 (Urfa): 30 palet
```

### PROCESS:

```
1. Clarke-Wright Savings:
   Saving(Adana, İzmir) = 900 + 560 - 560 = 900 km
   Saving(Adana, Urfa) = 900 + 950 - 850 = 1000 km
   Saving(İzmir, Urfa) = 560 + 950 - 850 = 660 km

2. Try to merge (highest saving first):

   Merge Adana + Urfa:
   Route: Gemlik → Adana → Urfa → Gemlik
   Distance: 900 + 850 + 950 = 2700 km ❌
   Duration: 3240 dakika ❌

   ⚠️ CONSTRAINT VIOLATION! Reject this route.

3. Keep as individual routes:
   Route A: Gemlik → Adana → Gemlik (1800 km) ❌ REJECTED
   Route B: Gemlik → İzmir → Gemlik (1120 km) ❌ REJECTED
   Route C: Gemlik → Urfa → Gemlik (1900 km) ❌ REJECTED
```

### OUTPUT:

```json
{
  "plans": [],
  "vehiclesUsed": 0,
  "totalDistanceKm": 0,
  "totalPallets": 0,
  "totalSeparators": 0
}
```

**Log:**
```
INFO: Found 3 approved requests for multi-vehicle optimization
WARN: Route violates constraints: distance=2700.00 km, duration=3240 min
WARN: Route violates constraints: distance=1800.00 km, duration=2160 min
WARN: Route violates constraints: distance=1120.00 km, duration=1344 min
WARN: Route violates constraints: distance=1900.00 km, duration=2280 min
INFO: Multi-vehicle optimization completed: vehicles=0, distance=0 km, coverage=0/3, unassigned=3
WARN: ⚠️ 3 requests could not be assigned (exceeds vehicle/capacity limits)
```

---

## Özet

### ✅ Sistem Yapabilir:
- Yakın bölgedeki fillerleri aynı rotada birleştirme
- 800 km ve 10 saat içindeki rotalar
- Örnek: Bursa → Bursa → Bursa (3 filler, 192 km)

### ❌ Sistem Yapmaz:
- Türkiye'nin farklı bölgelerini tek rotada ziyaret
- 800 km'den uzun rotalar
- 10 saatten uzun rotalar
- Örnek: Adana → İzmir → Urfa (2700+ km)

### Veri Akışı:

```
INPUT → Clarke-Wright Algorithm → Constraint Validation → OUTPUT

Collection Requests → [Optimize] → Valid Routes
(Approved)                          + Rejected Routes
                                    + Unassigned Requests
```
