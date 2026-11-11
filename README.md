# WHAT IS THIS (com.tdev.whatisthis)

Ekranın üstünde (overlay) DVD ekran koruyucusu gibi kenarlara çarpıp gezen bir görsel + ses.
Altta **ÇIK** butonu: ilk 3 basışta yeni bir DVD daha çıkar (ses çalar), 4. basışta uygulama kapanır.

## Kendi foto ve sesini koy
- Foto: `app/src/main/res/drawable-nodpi/dvd_photo.png` dosyasını değiştir (PNG, şeffaf olabilir)
- Ses: `app/src/main/res/raw/dvd_sound.wav` dosyasını değiştir (aynı isimle; mp3 ise `dvd_sound.mp3` yap)

## Derleme
- GitHub: repoya yükle, **Actions → Build signed APK** çalışır, APK "Artifacts" içinde.
  Sabit imza için secrets ekle: `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`.
- Yerel: `ANDROID_HOME` ayarla, `./build-local.sh`

## Ayarlar
`OverlayService.kt` içinde: `MAX_SPAWNS`, `SPRITE_DP`, `SPEED_DP_PER_SEC`.

Kapak görseli: `store/cover.png`
