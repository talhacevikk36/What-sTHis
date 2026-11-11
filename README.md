# WHAT IS THIS (com.tdev.whatisthis)

Ekranın üstünde (overlay) DVD ekran koruyucusu gibi kenarlara çarpıp gezen bir görsel + ses.
Altta **ÇIK** butonu: ilk 3 basışta yeni bir DVD daha çıkar (ses çalar), 4. basışta uygulama kapanır.

## Foto ve sesi değiştir
- Uygulamanın içinden: ana ekranda **Foto seç** / **Ses seç** (kalıcı kaydedilir), **Varsayılana dön** ile sıfırla
- Varsayılanları değiştirmek istersen: `app/src/main/res/drawable-nodpi/dvd_photo.png` ve `app/src/main/res/raw/dvd_sound.wav`

## Derleme
- GitHub: repoya yükle, **Actions → Build signed APK** çalışır, APK "Artifacts" içinde.
  Sabit imza için secrets ekle: `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`.
- Yerel: `ANDROID_HOME` ayarla, `./build-local.sh`

## Ayarlar
`OverlayService.kt` içinde: `MAX_SPAWNS`, `SPRITE_DP`, `SPEED_DP_PER_SEC`.

Kapak görseli: `store/cover.png`
