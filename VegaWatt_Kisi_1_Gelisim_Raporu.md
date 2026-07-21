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
| **G1** | **Aylık Fatura & Kota Sıfırlama (Billing Period Rollover)** | `billing`, `home/infrastructure` | ⏳ Bekliyor |
| **G2** | **İkili Ignite State Rollback Compensation (Hata Dayanıklılığı)** | `telemetry/application`, `home/infrastructure` | ⏳ Bekliyor |
| **G3** | **Idempotency & Concurrency Güçlendirme (Mükerrer Telemetri)** | `telemetry/application` | ⏳ Bekliyor |
| **G4** | **`occurredAt` vs `processedAt` Zaman Ayrımı** | `telemetry`, `history` | ⏳ Bekliyor |
| **G5** | **Veritabanı Kısıtlamaları & Şema Tamamlama (DB Constraints)** | `db/migration` | ⏳ Bekliyor |
| **G6** | **History Aggregation Endpoint (Grafik Veri Özetleme)** | `history` | ⏳ Bekliyor |
| **G7** | **Billing & Telemetry Entegrasyon Testleri** | `src/test/java/.../billing`, `telemetry` | ⏳ Bekliyor |

---

## 3. Adım Adım Detaylı Uygulama Planı

### 🔹 Aşama 1: Aylık Fatura & Kota Sıfırlama (Billing Period Rollover)
* **Problem:** Uygulama kesintisiz çalışırken yeni aya geçildiğinde Ignite state eski ayın harcama ve ceza durumunu taşımaya devam edebilir.
* **Yapılacaklar:**
  1. `HomeLiveState` ve `HomeLiveStateEntity` içine `billingPeriod` (Örn: `"2026-07"`) alanı eklenecek.
  2. Telemetri işlenirken gelen event'in ayı ile `HomeLiveState` içindeki ay karşılaştırılacak.
  3. Ay değişimi tespit edildiğinde ev harcama toplamı, maliyeti ve ceza tarifesi sıfırlanıp yeni ayın `BillingAccount` kaydıyla senkronize edilecek.

### 🔹 Aşama 2: İkili Ignite State Rollback Compensation
* **Problem:** Telemetri işlenirken PostgreSQL yazımı çökerse ev state'i geri yükleniyor fakat cihazın ihlal sayacı Ignite'ta yüksek kalıyor.
* **Yapılacaklar:**
  1. Telemetri güncellemesinden önce **hem ev hem de cihaz** mevcut Ignite state'leri saklanacak.
  2. DB işlemi exception verirse catching bloğunda hem ev hem cihaz state'i tek Ignite transaction'ı ile eski haline restore edilecek.

### 🔹 Aşama 3: Duplicate Event Concurrency & Idempotency
* **Problem:** Aynı telemetri mesajı eşzamanlı gelirse yarış durumu (race condition) oluşabilir.
* **Yapılacaklar:**
  1. `ProcessedTelemetryEvent` kontrolü ve DB kilit mekanizması sıkılaştırılacak.
  2. Tekrarlanan mesajlarda Ignite sayacının çift artması engellenecek.

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

* *(Henüz değişiklik yapılmadı - İlk geliştirme başladığında burası güncellenecektir.)*

---

## 5. Güncelleme ve İşlem Logu (Changelog)

- **[2026-07-22]:** Proje gelişim raporu oluşturuldu. Sorumluluk sınırları çizildi ve 7 aşamalı uygulama planı tanımlandı.
