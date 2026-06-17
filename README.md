# 🖱️ Auto Clicker Android

Kotlin ile yazılmış, AccessibilityService kullanarak otomatik tıklama işlemini gerçekleştiren Android uygulaması.

## 📋 Özellikler

- ✅ Yüzen Buton (Floating Button) - WindowManager ile ekranın üzerinde her zaman görünür
- ✅ Sürükle-Bırak Özelliği - Butonu ekran üzerinde istediğiniz yere taşıyabilirsiniz
- ✅ AccessibilityService - Sistem düzeyinde tıklama işlemleri
- ✅ Coroutines - UI donmayan arka plan işlemleri
- ✅ Modern MVVM Mimarisi

## 🔧 Teknik Gereksinimler

- Android API 26+ (Android 8.0)
- Kotlin 1.9+
- Gradle 8.0+

## 📲 Gerekli İzinler

1. **SYSTEM_ALERT_WINDOW** - Yüzen buton için
2. **BIND_ACCESSIBILITY_SERVICE** - Tıklama işlemleri için
3. **VIBRATE** (Opsiyonel) - Geri bildirim için

## 🚀 Kurulum

### 1. Projeyi Klonlayın
```bash
git clone https://github.com/ArseusGayMC/auto-clicker-android.git
cd auto-clicker-android
```

### 2. Android Studio'da Açın
- Android Studio açın
- `File → Open` → Proje dizinini seçin

### 3. Gradle Sync
- Gradle dependencies otomatik olarak indirilecek

## 📦 APK Oluşturma

### Debug APK
```bash
./gradlew assembleDebug
```
Oluşan APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (Signing ile)
```bash
./gradlew assembleRelease
```
Oluşan APK: `app/build/outputs/apk/release/app-release.apk`

## ⚙️ Kurulum Sonrası Ayarları

### 1. Overlay İzni Ver
- Uygulamayı başlat
- "Ayarları Aç" butonuna bas
- Overlay izni vermeyi kabul et

### 2. Accessibility Service'i Etkinleştir
- Ayarlar → Erişilebilirlik → Hizmetler
- "Auto Clicker" servisini etkinleştir
- Gerekli izinleri onayla

## 💻 Kullanım

1. Uygulamayı aç
2. "Yüzen Buton Başlat" butonuna bas
3. Mor yüzen buton ekranda görünecek
4. Butonu istediğiniz yere sürükleyin
5. Butona **basılı tutun** → Tıklama başlar
6. **Parmağınızı çekin** → Tıklama durur

## 📁 Proje Yapısı

```
auto-clicker-android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/autoclicker/
│   │   ├── service/
│   │   │   ├── AutoClickerAccessibilityService.kt
│   │   │   └── FloatingButtonService.kt
│   │   ├── repository/
│   │   │   └── ClickerRepository.kt
│   │   ├── viewmodel/
│   │   │   └── AutoClickerViewModel.kt
│   │   └── ui/
│   │       └── MainActivity.kt
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── values/strings.xml
│   │   └── xml/accessibility_service_config.xml
│   └── AndroidManifest.xml
├── build.gradle.kts (Project)
└── app/build.gradle.kts (App)
```

## ⚠️ Önemli Notlar

- **Google Play Policy Violation**: Bu uygulama Google Play Store'a yüklenemez
- **Etik Kullanım**: Sadece yasal amaçlar için kullanılmalıdır
- **Anti-Cheat**: Game'lerin anti-cheat sistemlerini bypass etmek için kullanamazsınız

## 🔍 Troubleshooting

### Problem: "Accessibility Service etkinleştirilemedi"
**Çözüm**: Ayarlar → Erişilebilirlik → Hizmetler → Auto Clicker'i manuel olarak etkinleştir

### Problem: "Yüzen buton görünmüyor"
**Çözüm**: Ayarlar → Uygulamalar → Auto Clicker → İzinler → "Diğer uygulamaların üzerine görüntülenmesine izin ver" seçeneğini aktifleştir

### Problem: Tıklama çalışmıyor
**Çözüm**: 
1. Accessibility service etkin mi kontrol et
2. Cihazı yeniden başlat
3. Uygulamayı force stop yap ve yeniden aç

## 📝 Lisans

MIT License - Detaylar için LICENSE dosyasına bakın

## 👨‍💻 Geliştirici

ArseusGayMC

## 🤝 Katkı

Katkılar hoş karşılanır! PR göndermekten çekinmeyin.
