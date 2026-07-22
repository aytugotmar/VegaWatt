# VegaWatt — Kişi 1 (Core Backend ve Veri Tutarlılığı) Gelişim ve Uygulama Raporu

**Proje:** VegaWatt (VoltWise IoT Energy Analytics & Budget Auditing)  
**Rol:** Kişi 1 — Core Backend & Data Consistency Lead  
**Önerilen Git Branch:** `feature/core-consistency`  
**Oluşturulma Tarihi:** 2026-07-22  
**Son Güncelleme:** 2026-07-22  

---

## 1. Görev Tanımı ve İzole Sorumluluk Haritası

Ekip içi dosya ve modül çakışmalarını (merge conflict) engellemek amacıyla **Kişi 1** yalnızca aşağıdaki paket ve dizinlerde yetkilidir. Diğer arkadaşların alanındaki dosyalara doğrudan dokunulmayacak, ihtiyaç halinde ortak API sözleşmesi (DTO/Endpoint) üzerinden iletişim kurulacaktır.

### 📍 Kişi 1 Sorumluluk Alanları (İzinli Paketler):
* `vegawatt-core/src/main/java/com/vegawatt/core/billing/` (Fatura ve bütçe yönetimi)
* `vegawatt-core/src/main/java/com/vegawatt/core/telemetry/application/` (Telemetri işleme akışı)
* `vegawatt-core/src/main/java/com/vegawatt/core/home/domain/` (Ev ve cihaz domain modelleri)
* `vegawatt-core/src/main/java/com/vegawatt/core/home/infrastructure/` (Ignite & DB adaptörleri)
* `vegawatt-core/src/main/java/com/vegawatt/core/history/` (Tarihsel snapshot ve aggregation endpoint'i)
* `vegawatt-core/src/main/resources/db/migration/` (Flyway SQL veritabanı şema ve constraint betikleri)

### ⛔ Dokunulmayacak (Diğer Arkadaşların) Alanları:
* 🔴 **Kişi 2 Alanı:** `common/config`, `common/outbox`, `notification`, `vegawatt-telemetry-sensors`, `docker-compose.yml`
* 🔴 **Kişi 3 Alanı:** `vegawatt-web` (Tüm Frontend React kodları)

---

## 2. Görev Takip Çizelgesi

| # | Görev Adı | İlgili Paket / Dizin | Durum |
|---|---|---|:---:|
| **G1** | **Aylık Fatura & Kota Sıfırlama (Billing Period Rollover)** | `billing`, `home/infrastructure` | ✅ Tamamlandı |
| **G2** | **İkili Ignite State Rollback Compensation (Hata Dayanıklılığı)** | `telemetry/application`, `home/infrastructure` | ✅ Tamamlandı |
| **G3** | **Idempotency & Concurrency Güçlendirme (Mükerrer Telemetri)** | `telemetry/application` | ✅ Tamamlandı |
| **G4** | **`occurredAt` vs `processedAt` Zaman Ayrımı** | `telemetry`, `history` | ⏳ Bekliyor |
| **G5** | **Veritabanı Kısıtlamaları & Şema Tamamlama (DB Constraints)** | `db/migration` | ⏳ Bekliyor |
| **G6** | **History Aggregation Endpoint (Grafik Veri Özetleme)** | `history` | ⏳ Bekliyor |
| **G7** | **Billing & Telemetry Entegrasyon Testleri** | `src/test/java/.../billing`, `telemetry` | ⏳ Bekliyor |

---

## 3. Adım Adım Detaylı Uygulama Planı

### 🔹 Aşama 1: Aylık Fatura & Kota Sıfırlama (Billing Period Rollover) [✅ TAMAMLANDI]
* **Problem:** Uygulama kesintisiz çalışırken yeni aya geçildiğinde Ignite state eski ayın harcama ve ceza durumunu taşımaya devam edebilir.
* **Yapılanlar:**
  1. `HomeLiveState` ve `HomeLiveStateCacheValue` içine `billingPeriod` (örn: `"2026-07"`) eklendi.
  2. `IgniteHomeLiveStateAdapter` ve `IgniteTelemetryLiveStateAdapter` serileştiricileri `billingPeriod` alanı ile güncellendi.
  3. `EvaluateHomeBillingUseCase` içinde gelen telemetri tarihi ile Ignite dönemi karşılaştırılıp ay geçişi otomatik algılandı. Yeni aya geçildiğinde harcama, maliyet, kota oranları ve ceza tarifesi sıfırlanıp yeni dönemin `BillingAccount` verisine bağlandı.
  4. `EvaluateHomeBillingUseCaseTest` yazılarak 69 birim testinin tamamı yeşile geçirildi (`mvn test` BUILD SUCCESS).

### 🔹 Aşama 2: İkili Ignite State Rollback Compensation [✅ TAMAMLANDI]
* **Problem:** Telemetri işlenirken PostgreSQL yazımı çökerse ev state'i geri yükleniyor fakat cihazın ihlal sayacı Ignite'ta yüksek kalıyor.
* **Yapılanlar:**
  1. `TelemetryLiveStatePort` arayüzüne ve `IgniteTelemetryLiveStateAdapter` adaptörüne tek Ignite transaction'ında hem ev hem cihaz durumlarını restore eden atomik `restore(...)` metodu eklendi.
  2. `ProcessTelemetryUseCase` içinde güncelleme öncesi `previousHome` ve `previousAppliance` snapshot'ları saklandı. DB hatasında hem ev hem cihaz eski durumlarına atomik olarak geri yüklendi.

### 🔹 Aşama 3: Duplicate Event Concurrency & Idempotency [✅ TAMAMLANDI]
* **Problem:** Aynı telemetri mesajı eşzamanlı gelirse yarış durumu (race condition) oluşabilir.
* **Yapılanlar:**
  1. Eşzamanlı mükerrer event yazımında DB seviyesinde `DataIntegrityViolationException` oluştuğunda Ignite canlı durumunun çift artması engellendi; `compensateLiveState` çalıştırılarak işlem güvenle atlandı.
  2. `ProcessTelemetryUseCaseTest` içerisinde test senaryoları doğrulanıp 70 birim testinin tamamı başarıyla geçti (`mvn test` BUILD SUCCESS).

### 🔹 Aşama 4: `occurredAt` vs `processedAt` Ayrımı
* **Problem:** Olayın gerçekleşme zamanı ile sisteme işlenme zamanı birbirine karışabiliyor.
* **Yapılacaklar:**
  1. Olay zamanı (`occurredAt`) telemetri paketinden alınıp fatura/log tarihlerinde kullanılacak.
  2. Sistem zamanı (`processedAt`) ise sadece audit ve işleme kaydında kullanılacak.

### 🔹 Aşama 5: Veritabanı Constraint ve Şema İyileştirmeleri
* **Problem:** Veritabanında ilişki bütünlüğünü tam korumayan eksik Foreign Key ve Unique constraint'ler var.
* **Yapılacaklar:**
  1. `V2__add_missing_constraints.sql` migration dosyası oluşturulacak.
  2. `ai_recommendations.trigger_event_id` üzerine Unique constraint eklenecek.
  3. Fatura ve cihaz tablolarına Foreign Key kısıtlamaları pekiştirilecek.

### 🔹 Aşama 6: History Aggregation Endpoint (Grafik Özetleme)
* **Problem:** 30 günlük tarihsel veride 40.000+ nokta frontend'i ve DB'yi yavaşlatıyor.
* **Yapılacaklar:**
  1. `GET /api/v1/homes/{id}/history` endpoint'ine `granularity` (`MINUTE`, `FIVE_MINUTES`, `HOUR`, `DAY`) opsiyonu eklenecek.
  2. SQL `DATE_TRUNC` veya Java kümeleme ile veri noktaları özetlenip frontend'e hafifletilmiş olarak sunulacak.

### 🔹 Aşama 7: Entegrasyon Testleri (Verification & Confidence)
* **Problem:** Sadece unit testler yetersiz, gerçek akış doğrulaması gerekiyor.
* **Yapılacaklar:**
  1. Fatura hesaplama ve ceza tarifesi testleri.
  2. Telemetri işleme ve hata anında rollback testleri.

---

## 4. Ortak Sözleşme Değişiklik Günlüğü (Diğer Ekip Üyelerine Duyurular)

> Diğer ekip üyelerinin (Kişi 2 ve Kişi 3) kendi kodlarını uyarlayabilmeleri için burada güncellenen DTO ve Endpoint değişiklikleri duyurulacaktır.

* **[2026-07-22 - G1]:** `HomeLiveState` record yapısına `billingPeriod` ("yyyy-MM") eklendi. Frontend ve Sensör DTO'ları etkilenmedi.

---

## 5. Güncelleme ve İşlem Logu (Changelog)

- **[2026-07-22]:** Proje gelişim raporu oluşturuldu. Sorumluluk sınırları çizildi ve 7 aşamalı uygulama planı tanımlandı.
- **[2026-07-22]:** **Aşama 1 (Billing Period Rollover)** tamamlandı. `HomeLiveState` ve Ignite adaptörlerine `billingPeriod` entegre edildi.
- **[2026-07-22]:** **Aşama 2 & 3 (Dual Ignite Compensation & Idempotency)** tamamlandı. Atomik `restore(...)` metodu Ignite adaptörüne eklendi. DB hatalarında hem ev hem cihaz durumlarının geri yüklenmesi sağlandı. Eşzamanlı mükerrer telemetri olaylarında Ignite sayaçlarının çift artması önlendi. 70 unit testinin tamamı başarıyla geçti (`mvn test` BUILD SUCCESS).
