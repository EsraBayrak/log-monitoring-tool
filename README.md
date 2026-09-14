# 📡 Log & Konfigürasyon İzleme Aracı (Log Monitoring Tool)

Kurumsal dağıtık sunucu mimarilerinde log takibini kolaylaştırmak, geçmişe dönük metin ve işlem (trace) aramalarını hızlandırmak ve sunucu konfigürasyonlarını tek merkezden yönetmek üzere geliştirilmiş Spring Boot tabanlı web aracıdır.

---

## 🚀 Temel Yetenekler ve Genel İşleyiş

* **Canlı Log Akışı (Live Tail -f & SSE):**  
  Hedef sunuculara JSch kütüphanesi üzerinden güvenli SSH oturumları açılır ve `tail -f` komut çıktısı **Server-Sent Events (SSE)** ile tarayıcıya anlık iletilir. Arayüz tarafında bellek taşmasını (*Page Unresponsive*) engellemek için **1000 satırlık kayar pencere (sliding buffer)** mimarisi devrededir; en eski satırlar dinamik olarak temizlenir.
* **Akıllı Geçmiş Arama (Piped Grep Mimarisi):**  
  Arama kutusuna girilen boşlukla ayrılmış anahtar kelimeler işletim sistemi seviyesinde ardışık boru hatlarına (`grep -in ... | grep -i ...`) dönüştürülür. Kelimelerin satır içi konumundan bağımsız olarak `AND` mantığıyla yüksek performanslı tarama gerçekleştirilir.
* **Gözlemlenebilirlik (Observability PoC):**  
  Elasticsearch ve Kibana entegrasyonuyla yapılandırılmış telekom logları indekslenmiştir. Kibana `telecom-logs*` Data View üzerinden KQL (Kibana Query Language) ile hata seviyeleri (`level: "ERROR"`), oturum (`sessionId`) ve abone (`msisdn`) bazlı filtrelemeler desteklenir.
* **Konfigürasyon Yönetimi (SFTP):**  
  Uzak sunuculardaki kritik konfigürasyon dosyalarına SFTP kanalıyla erişim sağlanır ve arayüz üzerinden içerik görüntüleme imkânı sunulur.
* **Kullanıcı Dostu Arayüz (Empty State):**  
  Sistemde henüz kayıtlı sunucu bulunmadığında kullanıcıyı doğrudan "Yeni Sunucu Ekle" aksiyonuna yönlendiren modern durum yönetimi uygulanmıştır.

---

## 🛠️ Kurulum ve Çalıştırma

### Yöntem 1: Tek Tıkla Masaüstü Başlatıcı (`Baslat.bat`)
Proje kök dizininde yer alan `Baslat.bat` dosyasına çift tıklayarak çalıştırabilirsiniz.
* Dizin bağımsız çalışır (`%~dp0`).
* 8085 portunu meşgul eden eski/askıda kalmış bir işlem varsa otomatik temizler.
* Java ve Maven Wrapper doğrulamalarını yapar.
* Spring Boot uygulamasını ayağa kaldırır ve hazır olduğunda varsayılan tarayıcıda `http://localhost:8085/logMonitoring` adresini açar.

### Yöntem 2: IDE / Terminal Üzerinden Standart Başlatma
Proje dizinindeyken terminalden çalıştırmak için:
```bash
.\mvnw.cmd spring-boot:run

```

Uygulama hazır olduğunda tarayıcınızdan **`http://localhost:8085/logMonitoring`** adresine gidebilirsiniz.

---

## ⚠️ Olası Hata Senaryoları ve Çözüm Rehberi (Troubleshooting)

Projeyi devralacak geliştiriciler ve sistem yöneticileri için karşılaşılabilecek olası senaryolar ve müdahale adımları:

| Hata / Semptom | Olası Neden | Çözüm Adımı |
| :--- | :--- | :--- |
| **`Port 8085 was already in use`** | Önceki Spring Boot süreci arka planda sonlanmamış olabilir. | `Baslat.bat` dosyasını çalıştırın (otomatik temizler) veya PowerShell üzerinden `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8085).OwningProcess -Force` komutunu uygulayın. |
| **`Hata: Failed to fetch` Uyarısı** | Tarayıcı, Spring Boot ve veritabanı tamamen hazır olmadan önce istek atmış olabilir. | Sayfayı **F5** ile yenileyin. Hata sürerse terminalden SQLite/DB bağlantı loglarını inceleyin. |
| **Canlı Akışta Donma (`Page Unresponsive`)** | Tarayıcı DOM ağacında binlerce log satırının birikmesi. | `index.html` dosyasındaki `MAX_LOG_LINES` (1000 satır) sınırlandırmasının ve `slice()` kayar pencere mantığının devrede olduğunu teyit edin. |
| **`HttpMessageNotWritableException`** | `GlobalExceptionHandler`'ın EventStream (SSE) isteklerine uyumsuz format dönmesi. | SSE endpoint'lerinde hata yanıtlarının JSON (`Map<String, String>`) olarak üretildiğini kontrol edin (`WebConfig` ve Exception Handler ayarları). |
| **SSH Bağlantı Hatası (`Auth fail` / `Connection refused`)** | Sunucu kimlik bilgilerinin geçersiz olması veya hedef makinede SSH servisinin (Port 22) kapalı olması. | Arayüzdeki **"Bağlantıyı Test Et"** butonunu kullanarak doğrulama yapın; hedef sunucudaki SSH daemon ve güvenlik duvarı izinlerini denetleyin. |
| **Kibana'da Logların Görünmemesi** | Kibana zaman aralığının (Date Picker) dar seçilmesi veya Data View eşleşmemesi. | Kibana sağ üstündeki zaman filtresini **"Last 24 hours"** veya **"Today"** yapın; Data View adının `telecom-logs*` olduğunu doğrulayın. |

---

## 📌 Sürüm ve Branch Bilgisi
* **Aktif Kararlı Dal:** `production-handover`
* **Java Sürümü:** Java 21
* **Framework:** Spring Boot 4.x / Spring Data JPA & SQLite

