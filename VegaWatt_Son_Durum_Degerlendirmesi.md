# VegaWatt Son Durum Değerlendirmesi

## Genel sonuç

Bu sürüm **arkadaşlarına sunmaya ve üç kişilik geliştirmeye başlamaya hazır**. Önceki hâline göre ciddi şekilde ilerlemiş:

- Backend modüler monolith olarak düzenli.
- Kafka retry/DLT yapısı eklenmiş.
- Kafka topic’leri kodla oluşturuluyor.
- Ignite’ta ev ve cihaz state’i aynı transaction içinde güncelleniyor.
- Startup reconciliation eklenmiş.
- Bildirimler kalıcı `notification_jobs` tablosuna taşınmış.
- Fatura hesabı billing period üzerinden sorgulanıyor.
- Frontend baştan düzenlenmiş, feature-based mimariye geçirilmiş.
- React Query polling, wizard, filtreler, grafikler, Gemini önerileri, dark mode ve erişilebilir modal bulunuyor.

Ancak backend için henüz **“her hata senaryosunda tamamen güvenli”** diyemem. Normal senaryo gayet iyi çalışıyor; kalan problemler daha çok retry, ay geçişi, servis yeniden başlatma ve bildirim tutarlılığıyla ilgili.

### Mevcut puanım

| Bölüm | Değerlendirme |
|---|---:|
| Backend mimarisi | **8.5/10** |
| Backend normal çalışma akışı | **8.5/10** |
| Backend hata dayanıklılığı | **7/10** |
| Telemetri sensör servisi | **7.5/10** |
| Frontend mimarisi | **8.5/10** |
| Frontend görünüm ve UX | **8/10** |
| Test yapısı | **7/10** |
| Genel proje seviyesi | **8.2/10** |

Kritik maddeleri çözdükten sonra proje rahatlıkla **9/10 seviyesine** çıkar.

---

# Sistem şu anda nasıl çalışıyor?

## 1. Frontend açıldığında

Frontend `GET /api/v1/homes/live` endpoint’ine iki saniyede bir istek gönderiyor.

React Query şu verileri yönetiyor:

- Evlerin canlı enerji tüketimi
- Güncel maliyet
- Enerji kotası
- Bütçe kotası
- Aktif tarife
- Son güncelleme zamanı

Ana dashboard’da:

- Toplam ev sayısı
- Toplam enerji
- Toplam maliyet
- Dikkat gerektiren ev sayısı
- Ev arama
- Durum filtresi
- Tarife filtresi
- Sıralama

bulunuyor.

Desktop’ta tablo, mobilde kart görünümü kullanılıyor.

## 2. Kullanıcı yeni ev eklediğinde

Frontend dört aşamalı wizard gösteriyor:

1. Ev bilgileri
2. Enerji ve bütçe hedefleri
3. Cihazlar
4. Kontrol ve gönderim

Sonunda şu endpoint çağrılıyor:

```text
POST /api/v1/homes
```

Core içerisinde:

```text
RegisterHomeUseCase
        ↓
Home ve Appliance domain nesneleri
        ↓
PostgreSQL kayıtları
        ↓
BillingAccount oluşturma
        ↓
Outbox event oluşturma
        ↓
Ignite başlangıç state'i
```

Ev kaydı doğrudan Kafka’ya gönderilmiyor. Bunun yerine aynı PostgreSQL transaction’ı içerisinde `outbox_events` tablosuna yazılıyor. Bu doğru ve profesyonel bir karar.

## 3. Outbox Kafka’ya aktarılıyor

`OutboxRelayScheduler` iki saniyede bir gönderilmemiş event’leri alıyor.

```text
outbox_events
        ↓
vegawatt.asset-registration.v1
```

Registration topic:

- Üç partition’a sahip.
- `cleanup.policy=compact` olarak tanımlanmış.
- Kafka message key olarak `homeId` kullanılıyor.

Bu sayede aynı evin en güncel yapılandırmasının topic üzerinde korunması amaçlanmış.

## 4. Sensör uygulaması evi öğreniyor

`RegistrationEventConsumer`, registration topic’ini dinliyor.

Her cihaz için:

```text
HomeRegistry.upsert()
        ↓
ApplianceSimulationScheduler.ensureScheduled()
```

çalışıyor.

Her cihaza bir simülasyon görevi atanıyor. Görev belirli aralıklarla Watt değeri üretiyor ve Kafka telemetry topic’ine gönderiyor.

## 5. Telemetri Core’a geliyor

Sensör tarafından şu bilgiler gönderiliyor:

```text
eventId
eventVersion
occurredAt
homeId
applianceId
powerWatt
measurementIntervalSeconds
```

Kafka key olarak yine `homeId` kullanılıyor. Aynı eve ait mesajların aynı partition üzerinden sıralı ilerlemesi açısından doğru.

Core tarafında:

```text
TelemetryEventKafkaConsumer
        ↓
ProcessTelemetryUseCase
```

çalışıyor.

Sonrasında:

1. Event daha önce işlendi mi kontrol ediliyor.
2. Ev PostgreSQL’den bulunuyor.
3. Cihaz PostgreSQL’den bulunuyor.
4. Cihazın gerçekten bu eve ait olduğu doğrulanıyor.
5. Watt değeri kWh’a çevriliyor.
6. Ev ve cihazın Ignite state’i aynı Ignite transaction’ında güncelleniyor.
7. PostgreSQL transaction’ında:
   - Event processed olarak işaretleniyor.
   - Billing account güncelleniyor.
   - Kota olayları kaydediliyor.
   - Penalty durumu kaydediliyor.
   - Anomali ve recovery olayları kaydediliyor.
   - Notification job oluşturuluyor.

Bu akışın genel tasarımı gerçekten kuvvetli.

## 6. Kota ve tarife sistemi

Sistem ayrı ayrı şunları takip ediyor:

- Enerji kotasının yüzde 80’i
- Enerji kotasının yüzde 100’ü
- Bütçe kotasının yüzde 80’i
- Bütçe kotasının yüzde 100’ü

Aynı kota türü tek ölçümde hem yüzde 80’i hem yüzde 100’ü geçerse yalnızca yüzde 100 olayı gönderiliyor. Bu önceki sürüme göre iyi bir düzeltme.

Bütçe yüzde 100’ü geçtiğinde ceza tarifesi aktif oluyor. Sınırı aşan mevcut event normal tarifeyle, bundan sonraki event’ler ceza tarifesiyle hesaplanıyor.

## 7. Cihaz anomalisi

Her cihaz için Ignite içerisinde:

```text
consecutiveBreachCount
anomalous
```

tutuluyor.

Üç ardışık limit ihlalinde cihaz anormal oluyor. Normal değere döndüğünde:

```text
consecutiveBreachCount = 0
anomalous = false
```

yapılıyor ve recovery event’i yazılıyor.

## 8. Gemini ve e-posta akışı

Kota veya anomali olduğunda artık bildirim doğrudan Kafka consumer thread’inden çalıştırılmıyor.

PostgreSQL’e kalıcı olarak:

```text
notification_jobs
```

kaydı oluşturuluyor.

Arka plandaki worker:

```text
NotificationJobWorker
        ↓
NotificationOrchestrator
        ↓
Gemini
        ↓
AiRecommendation
        ↓
E-posta
```

akışını gerçekleştiriyor.

Gemini çalışmıyorsa sistem fallback metni üretiyor. Bu sayede AI problemi ana telemetri akışını durdurmuyor.

## 9. Frontend detay ekranı

Bir eve tıklanınca modal açılıyor.

Modalda:

- Güncel enerji
- Güncel maliyet
- Kota progress barları
- Cihaz tablosu
- Üç aşamalı ihlal göstergesi
- Birikimli enerji/maliyet grafiği
- Gemini önerileri
- E-posta gönderim durumu

gösteriliyor.

Ev detayları iki saniyede, öneriler yirmi saniyede bir güncelleniyor. Geçmiş veriler yalnızca modal açıldığında veya zaman aralığı değiştirildiğinde alınıyor. Bu performans açısından doğru.

---

# Bu sürümde gerçekten iyi yapılmış noktalar

## Backend’in paket yapısı

Kodun şu şekilde ayrılmış olması güçlü:

```text
home
billing
telemetry
anomaly
history
notification
common
```

Her modül içerisinde domain, application, infrastructure ve API sorumluluklarının ayrılması projenin okunmasını kolaylaştırıyor.

## Transactional Outbox

Ev kaydıyla Kafka gönderimi arasındaki dual-write problemi doğru şekilde ele alınmış.

```text
PostgreSQL transaction
  ├── Home
  ├── Appliances
  ├── BillingAccount
  └── OutboxEvent
```

Ardından event Kafka’ya aktarılıyor.

Bu projeyi sıradan CRUD projesinden ayıran önemli özelliklerden biri.

## Ignite atomik güncelleme

Yeni `IgniteTelemetryLiveStateAdapter`, ev ve cihaz state’ini aynı transaction içerisinde güncelliyor.

Önceki sürümde ev güncellenip cihaz güncellenemeyebilirdi. Bu açık büyük ölçüde kapatılmış.

## Kafka retry ve DLT

Artık her hata doğrudan elle DLT’ye gönderilmiyor.

- Geçici hatalar exponential backoff ile retry ediliyor.
- Kalıcı hatalar doğrudan DLT’ye gidiyor.
- DLT topic’i açıkça oluşturuluyor.
- Telemetry topic altı partition’a sahip.

Bu daha doğru bir Kafka kullanımı.

## Startup reconciliation

Core başladığında PostgreSQL’deki evleri okuyup eksik Ignite state’lerini yeniden oluşturuyor. Aynı zamanda registration event’lerini outbox’a yeniden ekliyor.

Bu, core ve Ignite birlikte yeniden başladığında sistemi önemli ölçüde toparlıyor.

## Frontend mimarisi

Frontend artık tek büyük component’ten oluşmuyor.

```text
features/dashboard
features/home-details
features/home-registration
features/history
features/recommendations
shared
```

şeklinde ayrılmış.

Ayrıca:

- TypeScript strict açık.
- `any` kullanılmıyor.
- React Query var.
- Input validation var.
- Accessible dialog var.
- Focus trap var.
- Escape ile modal kapanıyor.
- Light/dark theme tokenları var.
- Mobil görünüm düşünülmüş.
- API hataları Problem Detail üzerinden okunuyor.

Bu yapı üç kişiyle geliştirmeye uygun.

---

# Sunumdan önce düzeltilmesini önerdiğim kritik noktalar

## 1. E-posta başarısız olsa bile notification job başarılı sayılıyor

Şu anda en net tutarsızlıklardan biri bu.

`DispatchAdvisoryEmailUseCase`, SMTP hatasını yakalıyor:

```text
emailStatus = FAILED
```

olarak kaydediyor ama exception’ı tekrar fırlatmıyor.

Daha sonra `NotificationOrchestrator` işlemin başarılı olduğunu düşünüp:

```text
notificationJob.status = SENT
```

yapıyor.

Sonuç:

```text
AiRecommendation.emailStatus = FAILED
NotificationJob.status = SENT
```

Aynı işlem için iki farklı gerçek oluşuyor ve e-posta tekrar denenmiyor.

### Yapılması gereken

`DispatchAdvisoryEmailUseCase` sonucu açıkça dönmeli:

```java
enum EmailDispatchResult {
    SENT,
    FAILED
}
```

veya hata durumunda recommendation’ı `FAILED` kaydettikten sonra exception tekrar fırlatılmalı.

Notification job yalnızca e-posta gerçekten gönderildiyse tamamlanmalı.

Daha sağlam tasarım:

```text
Notification job
      ↓
Recommendation yoksa oluştur
      ↓
Mevcut recommendation'ı kullan
      ↓
E-postayı gönder
      ↓
Başarılıysa job COMPLETED
Başarısızsa job PENDING + retry
```

Ayrıca `ai_recommendations.trigger_event_id` üzerine unique constraint eklenmeli. Aksi hâlde retry sırasında aynı operational event için birden fazla recommendation oluşabilir.

---

## 2. Ay değişiminde canlı fatura state’i sıfırlanmıyor

Repository artık doğru şekilde:

```text
findByHomeIdAndBillingPeriod
```

kullanıyor. Fakat Ignite `HomeLiveState` içerisinde billing period bilgisi bulunmuyor.

Örneğin uygulama 31 Temmuz gecesinden 1 Ağustos’a kesintisiz çalışırsa:

- PostgreSQL’de Ağustos için yeni billing account açılır.
- Ancak Ignite hâlâ Temmuz ayının enerji ve maliyet toplamını taşır.
- Ceza tarifesi Ağustos’a taşınabilir.
- Kota yüzdeleri sıfırlanmaz.
- Yeni billing account yalnızca yeni increment’i saklarken dashboard eski toplamı gösterir.

### Yapılması gereken

`HomeLiveState` içerisine:

```text
billingPeriod
```

eklenmeli.

Her telemetri event’inde:

```java
currentPeriod = BillingPeriodResolver.currentPeriod(eventTime);

if (!current.billingPeriod().equals(currentPeriod)) {
    current = recoverFromLedger(home, currentPeriod);
}
```

kontrolü yapılmalı.

Yeni ayda:

- Ev enerji toplamı sıfırlanmalı.
- Maliyet sıfırlanmalı.
- Kota bildirimleri sıfırlanmalı.
- Ceza tarifesi kapanmalı.
- Yeni billing account kullanılmalı.

---

## 3. PostgreSQL hatasında cihaz state’i iki kere artabilir

Şu anda PostgreSQL persist başarısız olursa yalnızca evin Ignite state’i ledger’dan geri kuruluyor.

Ancak cihaz state’i geri alınmıyor.

Senaryo:

1. Cihaz enerjisi Ignite’ta artıyor.
2. İhlal sayacı Ignite’ta artıyor.
3. PostgreSQL başarısız oluyor.
4. Ev state’i geri alınıyor.
5. Cihaz state’i ileri durumda kalıyor.
6. Kafka retry aynı event’i tekrar işliyor.
7. Cihaz enerjisi ve ihlal sayacı tekrar artıyor.

Sonuç olarak:

- Evin toplam enerjisi doğru olabilir.
- Cihazın toplam enerjisi fazla olabilir.
- Tek gerçek ihlal iki ihlal olarak sayılabilir.
- Cihaz normalden erken anomalous olabilir.

### Yapılması gereken

Ignite güncellemesinden önce hem ev hem cihazın eski state’i saklanmalı.

Hata durumunda ikisi birlikte aynı Ignite transaction’ında geri yüklenmeli:

```text
previousHomeState
previousApplianceState
        ↓
telemetryLiveStatePort.restore(...)
```

Yalnızca ev state’ini PostgreSQL’den yeniden oluşturmak yeterli değil.

---

## 4. Yalnızca sensör servisi yeniden başlatılırsa evleri öğrenemeyebilir

Registration topic compacted yapılmış, ancak sensör consumer’ı sabit group id kullanıyor:

```text
vegawatt-sensors-registration
```

`auto-offset-reset=earliest`, yalnızca consumer group için kayıtlı offset yoksa çalışır.

Senaryo:

1. Core ve sensors çalışıyor.
2. Registration event’leri tüketildi.
3. Kafka çalışmaya devam ediyor.
4. Sadece sensors container yeniden başlatılıyor.
5. Consumer eski committed offset’ten devam ediyor.
6. Önceki ev kayıtlarını tekrar okumuyor.
7. `HomeRegistry` boş kalıyor.
8. Telemetri üretilmiyor.

Core da yeniden başlatılırsa registration event’leri tekrar yayınlandığı için toparlanır. Ama yalnızca sensors restart senaryosu hâlâ eksik.

### Çözüm seçenekleri

En uygun çözüm, sensör uygulamasının başlangıçta compacted registration topic’ini baştan okuyarak registry oluşturmasıdır.

Bunun için:

- `ConsumerSeekAware` ile başlangıçta beginning’e seek etmek,
- Sensör registry’sini kalıcı saklamak,
- Core’dan bootstrap endpoint’i almak

seçeneklerinden biri uygulanabilir.

Bu proje için compacted topic’i baştan okumak yeterli olur.

---

## 5. Scheduled işler aynı thread’i bloke edebilir

Core içerisinde şu scheduled işler bulunuyor:

- Outbox relay
- Snapshot capture
- Notification worker

Özel bir core `TaskScheduler` tanımlanmadığı için varsayılan durumda bu işler aynı sınırlı scheduler üzerinde çalışabilir.

Daha önemlisi:

```java
kafkaTemplate.send(...).get();
```

bloklayıcı.

Notification worker da Gemini ve SMTP çağrılarını sıralı gerçekleştiriyor. Elli bildirim ve sekiz saniyelik timeout düşünülürse diğer scheduled işler uzun süre bekleyebilir.

### Yapılması gereken

En azından ayrı scheduler/executor’lar kullanılmalı:

```text
outboxTaskScheduler
snapshotTaskScheduler
notificationExecutor
```

Outbox için:

```java
.get(10, TimeUnit.SECONDS)
```

gibi açık timeout konulmalı.

Notification worker:

1. Job’u atomik olarak claim etmeli.
2. Bounded executor’a aktarmalı.
3. Aynı job’un iki worker tarafından alınmasını engellemeli.

Tek core instance için mevcut yapı çalışır; fakat dayanıklılık ve ölçek için düzeltilmeli.

---

# Önemli frontend–backend tutarsızlıkları

## 1. TypeScript decimal tipleri backend cevabıyla tam uyumlu değil

Frontend şu alanları `string` olarak tanımlıyor:

```ts
currentEnergyKwh: string;
currentCost: string;
energyQuotaPercentage: string;
```

Ancak Spring/Jackson `BigDecimal` alanlarını JSON number olarak gönderir:

```json
{
  "currentCost": 123.45
}
```

Runtime’da `toSafeNumber` hem string hem number kabul ettiği için ekran çalışıyor. Fakat TypeScript API sözleşmesi gerçeği yansıtmıyor.

### Düzeltilmesi gereken tip

```ts
export type DecimalValue = string | number;
```

Sonra:

```ts
currentCost: DecimalValue;
currentEnergyKwh: DecimalValue;
```

kullanılmalı.

---

## 2. GET isteklerinde gereksiz `Content-Type: application/json` gönderiliyor

`apiFetch` bütün isteklere şu header’ı ekliyor:

```text
Content-Type: application/json
```

GET isteklerinde bu gerekli değil. Frontend ve backend farklı origin’de çalıştığında tarayıcı gereksiz CORS preflight istekleri üretebilir.

İki saniyelik polling düşünüldüğünde gereksiz yük yaratabilir.

### Daha doğru yaklaşım

```ts
const headers = new Headers(init?.headers);
headers.set("Accept", "application/json");

if (init?.body) {
  headers.set("Content-Type", "application/json");
}
```

---

## 3. İlk API çağrısı başarısız olursa “ev yok” ekranı gösterilebilir

Dashboard’da API tamamen kapalıyken ve cache’te daha önce veri yokken:

- `isError = true`
- `homes = undefined`
- `hasHomes = false`

oluyor.

Sonrasında frontend:

```text
Henüz kayıtlı ev yok
```

ekranını gösterebilir.

Bu yanlış yönlendirir; sistemde ev yokmuş gibi görünür, oysa backend’e ulaşılamıyordur.

### Olması gereken

Ayrı bir full-page error state:

```text
Canlı verilere ulaşılamıyor

VegaWatt Core şu anda cevap vermiyor.
Bağlantıyı kontrol edip yeniden deneyin.
```

---

## 4. Ev detayındaki API hataları boş veri gibi gösteriliyor

`HomeDetailsDialog` içinde:

- Live home hatası
- History hatası
- Recommendations hatası

ayrı ele alınmıyor.

History çağrısı hata verirse kullanıcı:

```text
Geçmiş veri yok
```

görebilir.

Recommendations çağrısı hata verirse:

```text
Henüz öneri yok
```

görebilir.

Bunlar “veri yok” ile “veri alınamadı” durumlarını karıştırıyor.

Her query için:

```text
isError
refetch
```

işlenmeli.

---

## 5. Yeni kayıtlı ev gerçekte veri almadan “Normal” görünüyor

Ev oluşturulduğunda Ignite state’i sıfır olarak başlatılıyor ve `lastUpdatedAt` kayıt zamanı oluyor.

Frontend bunu:

```text
Normal
az önce
```

olarak gösterebilir.

Ancak sensör henüz registration event’ini almamış ve ilk telemetri gelmemiş olabilir.

Bu kullanıcı açısından yanıltıcı.

### Backend’e eklenebilecek alanlar

```text
telemetryStatus: WAITING | LIVE | STALE
lastTelemetryAt
```

veya en azından:

```text
hasReceivedTelemetry
```

Frontend durumları:

```text
WAITING → Veri bekleniyor
LIVE    → Canlı
STALE   → Veri gecikiyor
```

olmalı.

Bu özellik projeye çok profesyonel bir görünüm kazandırır.

---

## 6. Ana dashboard cihaz anomalisini bilmiyor

Ev durumu yalnızca şunlara bakılarak hesaplanıyor:

- Enerji kotası
- Bütçe kotası
- Penalty tariff

Bir evin cihazı anomalous olsa fakat kotalar normal olsa dashboard’da ev:

```text
Normal
```

görünebilir.

Ev summary response’una:

```text
activeAnomalyCount
```

eklenmesi iyi olur.

Durum önceliği:

```text
STALE
PENALTY
ANOMALY
CRITICAL
WARNING
NORMAL
```

şeklinde düşünülebilir.

---

## 7. Mobil cihaz detay tablosu hâlâ yatay scroll kullanıyor

Ana dashboard mobilde karta dönüyor; bu iyi.

Fakat cihaz tablosu:

```text
min-width: 720px
overflow-x-auto
```

kullanıyor.

Mobilde kullanıcı tabloyu yatay kaydırmak zorunda kalıyor. Cihazlar mobil görünümde kompakt kartlara dönüştürülmeli.

---

## 8. Otuz günlük grafik çok fazla veri getirebilir

Snapshot her 60 saniyede bir kaydediliyor.

Tek ev için:

```text
1 gün  = 1.440 nokta
7 gün  = 10.080 nokta
30 gün = 43.200 nokta
```

Frontend Recharts’a 43 bin nokta vermek arayüzü yavaşlatabilir.

Backend history endpoint’ine aggregation eklenmeli:

```text
1H  → 1 dakikalık
24H → 5 dakikalık
7D  → saatlik
30D → günlük
```

Örneğin:

```text
GET /history?from=...&to=...&granularity=HOUR
```

Bu hem frontend’i hem PostgreSQL’i rahatlatır.

---

## 9. Bundle biraz büyük

Frontend build başarılı oldu fakat ana JavaScript bundle’ı:

```text
637.18 KB
gzip: 182.79 KB
```

çıktı ve Vite chunk uyarısı verdi.

Sunum için sorun değil. Daha sonra:

- `HomeDetailsDialog`
- Recharts
- `AddHomeWizard`

lazy-load edilebilir.

```ts
const HomeDetailsDialog = lazy(() => import(...));
```

Bu başlangıç yükünü azaltır.

---

# Sensör tarafında geliştirilebilecek noktalar

## Gerçek süre yerine sabit beş saniye gönderiliyor

Scheduler gecikse bile event:

```text
measurementIntervalSeconds = 5
```

gönderiyor.

Gerçekte event sekiz saniye sonra üretildiyse enerji hesabı düşük çıkar.

Her cihaz için önceki ölçüm zamanı tutulup gerçek fark gönderilmeli.

## “Normal” üretim de güvenli limiti geçebiliyor

Presetlerde örneğin:

```text
Klima güvenli limit: 2000 W
Normal simülasyon maksimumu: 2200 W
```

Normal branch bile 2000 W üstü üretebilir.

Bu nedenle kodda belirtilen yüzde 10 spike olasılığı gerçek anomali olasılığını yansıtmıyor.

İki seçenek var:

- Normal maksimumu güvenli limitin altında tut.
- Bunu bilinçli yapıyorsan “spike probability” yerine gerçekçi yük profili olarak adlandır.

Demo için daha iyi çözüm:

```text
NORMAL
HIGH_LOAD
ANOMALY_BURST
RECOVERY
```

gibi deterministic senaryo modları eklemek.

Böylece sunumda rastgele üç ihlal beklemek zorunda kalmazsınız.

---

# Test durumu

Frontend testlerini bu sürümde çalıştırdım:

```text
21 test geçti
0 başarısız
```

Production build de başarılı oldu.

Archive içerisindeki güncel Surefire raporlarında:

```text
Core:    54 test, 0 hata
Sensors:  8 test, 0 hata
Toplam:  62 test, 0 hata
```

görünüyor. Bu raporlar ana kaynak koddan sonra oluşturulmuş. Ancak ortamda Maven kurulu olmadığı için Java testlerini kendim tekrar çalıştıramadım.

Testlerin zayıf tarafı, çoğunun unit/mock testi olması.

Eksik olan asıl güven testleri:

- Gerçek PostgreSQL + Flyway
- Gerçek Kafka
- Outbox → Kafka
- Kafka → Ignite → PostgreSQL
- Retry → DLT
- Core restart
- Ignite restart
- Sensor-only restart
- Aylık billing rollover
- SMTP failure retry
- Aynı event’in tekrar işlenmesi

Bunlar için Testcontainers çok değerli olur.

---

# Üç kişilik en mantıklı görev dağılımı

## Kişi 1 — Core Backend ve Veri Tutarlılığı

Bu kişi şu paketlerin sahibi olsun:

```text
billing
telemetry/application
home/domain
home/infrastructure
db/migration
```

Görevleri:

1. Billing period rollover’ı tamamlamak.
2. Hata durumunda ev ve cihaz Ignite state’ini birlikte geri almak.
3. Duplicate event concurrency davranışını güçlendirmek.
4. `occurredAt` ve `processedAt` ayrımını yapmak.
5. Veritabanı constraint’lerini tamamlamak.
6. Billing ve telemetry entegrasyon testlerini yazmak.
7. History aggregation endpoint’ini geliştirmek.

Önerilen branch:

```text
feature/core-consistency
```

## Kişi 2 — Kafka, Sensors, Notification ve Operasyon

Bu kişi şu alanların sahibi olsun:

```text
common/config
common/outbox
notification
vegawatt-telemetry-sensors
docker/runtime
```

Görevleri:

1. SMTP başarısızlığında notification retry yapmak.
2. Recommendation tekrarlarını engellemek.
3. Notification job claim/locking mekanizması.
4. Core scheduled işler için ayrı executor’lar.
5. Sensor-only restart recovery.
6. Event version doğrulaması.
7. Gerçek measurement interval.
8. Kontrollü demo senaryosu.
9. Outbox retry backoff ve terminal failure.
10. Kafka/Testcontainers entegrasyon testleri.
11. Actuator, metrics ve consumer lag görünürlüğü.

Önerilen branch:

```text
feature/event-runtime
```

## Kişi 3 — Frontend ve Ürün Deneyimi

Bu kişi bütün:

```text
vegawatt-web
```

dizininin sahibi olsun.

Görevleri:

1. API decimal tiplerini düzeltmek.
2. GET preflight sorununu kaldırmak.
3. Dashboard full-page error state.
4. Detail/history/recommendation error state’leri.
5. `WAITING`, `LIVE`, `STALE` görünümleri.
6. Dashboard’da anomaly sayısını göstermek.
7. Mobil cihaz kartlarını geliştirmek.
8. Grafik aggregation response’una uyarlamak.
9. Lazy loading ve bundle küçültme.
10. Component ve accessibility testlerini genişletmek.
11. UI’ı gerçek backend hata senaryolarıyla test etmek.

Önerilen branch:

```text
feature/frontend-product
```

---

# Ekip içinde çakışmayı nasıl önlersiniz?

İlk olarak API sözleşmesini birlikte belirleyin.

Örneğin ev summary’ye eklenecek alanlar:

```json
{
  "telemetryStatus": "LIVE",
  "lastTelemetryAt": "2026-07-21T10:00:00Z",
  "activeAnomalyCount": 1,
  "billingPeriod": "2026-07"
}
```

History sözleşmesi:

```text
GET /history?from=&to=&granularity=HOUR
```

Bunlar kararlaştırıldıktan sonra:

- Backend kişisi DTO’ları ve endpoint’i geliştirir.
- Frontend kişisi aynı sözleşmeye göre mock veriyle çalışır.
- Event/runtime kişisi telemetry status ve anomaly bilgisinin nasıl üretileceğini tamamlar.

Aynı anda herkes `HomeLiveStatusResponse`, `ProcessTelemetryUseCase` veya `docker-compose.yml` dosyasını değiştirmesin. Her ortak dosya için tek sahip belirleyin.

---

# Arkadaşlarına projeyi nasıl anlatmalısın?

Önce ürün kimliğini net söyle:

> VegaWatt şu anda bireysel kullanıcı giriş uygulaması değil, birden fazla evin enerji tüketimini yöneten gerçek zamanlı bir enerji izleme ve operasyon panelidir.

E-posta alanı login için değil, kota ve anomali bildirimleri içindir.

Sunum sırası:

1. Swagger’dan veya frontend’den ev oluştur.
2. PostgreSQL’de ev, cihaz, billing account ve outbox kaydını göster.
3. Outbox event’inin Kafka registration topic’ine geçtiğini anlat.
4. Sensors’ın evi öğrendiğini göster.
5. Telemetry topic’e Watt verilerinin aktığını göster.
6. Dashboard’un iki saniyede bir Ignite’tan güncellendiğini göster.
7. Üç ardışık cihaz ihlali oluştur.
8. Cihazın anomalous olduğunu göster.
9. Operational event ve notification job kaydını göster.
10. Gemini önerisini frontend’de göster.
11. Mailpit’te e-postayı göster.
12. Bütçe yüzde 100 olduğunda ceza tarifesine geçişi göster.
13. Gemini kapalıyken fallback mesajını göster.
14. Son olarak üç kişinin geliştireceği kalan alanları anlat.

Bu anlatım arkadaşlarının projeyi hızlı anlamasını sağlar.

# Son kararım

**Mevcut hâli güçlü bir ekip başlangıç noktası ve sunulabilir.** Frontend artık profesyonel bir ürün iskeletine benziyor; backend de event-driven mimari, outbox, Ignite, idempotency, retry/DLT ve durable job gibi ileri konuları gerçekten içeriyor.

Ancak ekibe “her şey tamam” diye sunma. Şöyle sun:

> Ana iş akışı ve mimari tamamlandı. Bundan sonraki geliştirme aşaması veri tutarlılığı, yeniden başlatma senaryoları, bildirim güvenilirliği, gözlemlenebilirlik ve kullanıcı deneyiminin güçlendirilmesi olacak.

Bu yaklaşım hem dürüst hem de profesyonel görünür. En önce düzeltilmesi gereken üç konu: **e-posta job tutarsızlığı, aylık billing rollover ve cihaz Ignite compensation**. Bunlar tamamlandığında backend’in en kritik açıkları büyük ölçüde kapanır.
