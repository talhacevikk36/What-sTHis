package com.tdev.whatisthis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import kotlin.math.max

/** Kullanıcının seçtiği foto ve sesi uygulamanın kendi klasöründe saklar. */
object CustomMedia {

    private const val MAX_PHOTO_PX = 1024

    fun photoFile(ctx: Context) = File(ctx.filesDir, "custom_photo.png")
    fun soundFile(ctx: Context) = File(ctx.filesDir, "custom_sound.bin")

    fun hasPhoto(ctx: Context) = photoFile(ctx).exists()
    fun hasSound(ctx: Context) = soundFile(ctx).exists()

    /** Fotoyu küçültüp döndürmeyi düzeltip PNG olarak kaydeder. Başarılıysa true. */
    fun savePhoto(ctx: Context, uri: Uri): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            false
        } else {
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_PX) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }

            if (bmp == null) {
                false
            } else {
                val degrees = ctx.contentResolver.openInputStream(uri)?.use { s ->
                    when (ExifInterface(s).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
                if (degrees != 0f) {
                    val m = Matrix().apply { postRotate(degrees) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }

                val tmp = File(ctx.filesDir, "custom_photo.tmp")
                tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                tmp.renameTo(photoFile(ctx)) || run { photoFile(ctx).delete(); tmp.renameTo(photoFile(ctx)) }
            }
        }
    } catch (_: Exception) {
        false
    }

    /** Sesi kopyalar ve çalınabildiğini doğrular. Geçersizse eskisini bozmaz. */
    fun saveSound(ctx: Context, uri: Uri): Boolean {
        val tmp = File(ctx.filesDir, "custom_sound.tmp")
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return false

            // Gerçekten ses dosyası mı?
            val mp = MediaPlayer()
            try {
                mp.setDataSource(tmp.path)
                mp.prepare()
            } finally {
                mp.release()
            }

            soundFile(ctx).delete()
            tmp.renameTo(soundFile(ctx))
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    fun reset(ctx: Context) {
        photoFile(ctx).delete()
        soundFile(ctx).delete()
    }
}
