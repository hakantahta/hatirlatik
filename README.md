# Hatırlatık - Görev Hatırlatma Uygulaması

Hatırlatık, günlük görevlerinizi yönetmenize ve zamanında hatırlamanıza yardımcı olan bir Android uygulamasıdır.

## Özellikler

- 📝 Görev oluşturma ve düzenleme
- ⏰ Bildirim ve alarm ile hatırlatma
- 🌙 Karanlık/Açık tema desteği
- 📅 Tarih ve saat bazlı planlama
- 🔍 Görev filtreleme ve arama
- ✅ Görev tamamlama takibi
- ⚙️ Özelleştirilebilir ayarlar

## Teknik Özellikler

### Kullanılan Teknolojiler

- **Dil:** Java
- **Minimum SDK:** API 24 (Android 7.0)
- **Hedef SDK:** API 34 (Android 14)

### Mimari ve Bileşenler

- **Mimari Pattern:** MVVM (Model-View-ViewModel)
- **Veritabanı:** Room Database
- **Asenkron İşlemler:** WorkManager, ExecutorService
- **UI Bileşenleri:** Material Design Components
- **Navigation:** Navigation Component
- **Dependency Injection:** Manuel DI

### Kullanılan Kütüphaneler

```gradle
dependencies {
    // AndroidX Core
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'

    // Material Design
    implementation 'com.google.android.material:material:1.11.0'

    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'

    // Navigation Component
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'

    // WorkManager
    implementation 'androidx.work:work-runtime:2.9.0'

    // Lifecycle Components
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'

    // AdMob
    implementation 'com.google.android.gms:play-services-ads:22.6.0'
}
```

## Kurulum

1. Projeyi klonlayın:
```bash
git clone https://github.com/kullaniciadi/hatirlatik.git
```

2. Android Studio'da açın ve gerekli bağımlılıkların yüklenmesini bekleyin.

3. `local.properties` dosyasına AdMob uygulama ID'nizi ekleyin:
```properties
ADMOB_APP_ID=ca-app-pub-xxx~yyy
```

4. Uygulamayı derleyin ve çalıştırın.

## Proje Yapısı

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/tht/hatirlatik/
│   │   │   ├── database/          # Room DB sınıfları
│   │   │   ├── model/            # Veri modelleri
│   │   │   ├── repository/       # Repository sınıfları
│   │   │   ├── ui/              # Fragment ve Adapter'lar
│   │   │   ├── viewmodel/       # ViewModel sınıfları
│   │   │   ├── notification/    # Bildirim yönetimi
│   │   │   ├── workers/         # WorkManager worker'ları
│   │   │   ├── receivers/       # Broadcast receiver'lar
│   │   │   ├── preferences/     # Ayarlar yönetimi
│   │   │   └── utils/          # Yardımcı sınıflar
│   │   └── res/                # Kaynaklar (layout, drawable, vb.)
│   └── test/                   # Birim testleri
└── build.gradle               # Uygulama yapılandırması
```

## Katkıda Bulunma

1. Bu repository'yi fork edin
2. Yeni bir branch oluşturun (`git checkout -b feature/yeniOzellik`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik: XYZ'`)
4. Branch'inizi push edin (`git push origin feature/yeniOzellik`)
5. Pull Request oluşturun

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## İletişim

- Geliştirici: [Ad Soyad]
- E-posta: [E-posta adresi]
- LinkedIn: [LinkedIn profil linki]
- Twitter: [@twitter_kullanici_adi] 