package com.tdev.whatisthis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import kotlin.math.max
import kotlin.math.roundToInt

class OverlayService : Service() {

    companion object {
        /** "Çık" butonuna ilk kaç basışta yeni DVD çıksın. Bundan sonraki basışta uygulama kapanır. */
        const val MAX_SPAWNS = 3

        /** DVD görselinin ekrandaki en büyük kenarı (dp). */
        const val SPRITE_DP = 120f

        /** Hız (dp/sn). */
        const val SPEED_DP_PER_SEC = 220f

        private const val CHANNEL_ID = "overlay"
        private const val NOTIF_ID = 1

        @Volatile
        private var running = false
    }

    private lateinit var wm: WindowManager
    private var bounceView: BounceView? = null
    private var exitButton: Button? = null
    private val players = ArrayList<MediaPlayer>()
    private var presses = 0
    private var exiting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() çağrısına her durumda startForeground ile cevap vermek şart.
        startForegroundCompat()

        if (running) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        setupViews()
        spawn() // ilk DVD
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Overlay", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("WHAT IS THIS")
            .setContentText("Ekranda geziyor...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun setupViews() {
        val density = resources.displayMetrics.density
        val sprite = loadSprite((SPRITE_DP * density).roundToInt())

        // 1) Tüm ekranı kaplayan, dokunuşu geçiren zıplama katmanı
        val bv = BounceView(this, sprite, SPEED_DP_PER_SEC * density)
        val bounceParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            bounceParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        wm.addView(bv, bounceParams)
        bv.start()
        bounceView = bv

        // 2) Altta ÇIK butonu (dokunulabilir tek parça)
        val btn = Button(this).apply {
            text = "ÇIK"
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 18f
            background = GradientDrawable().apply {
                setColor(0xFFE53935.toInt())
                cornerRadius = 40f * density
            }
            val h = (28 * density).toInt()
            val v = (10 * density).toInt()
            setPadding(h, v, h, v)
            minWidth = (140 * density).toInt()
            setOnClickListener { onExitPressed() }
        }
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (56 * density).toInt()
        }
        wm.addView(btn, buttonParams)
        exitButton = btn
    }

    /** Basış 1..MAX_SPAWNS: yeni DVD. Sonraki basış: uygulamadan çık. */
    private fun onExitPressed() {
        presses++
        if (presses <= MAX_SPAWNS) spawn() else exitApp()
    }

    private fun spawn() {
        bounceView?.addSprite()
        playSound()
    }

    private fun playSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.dvd_sound) ?: return
            players.add(mp)
            mp.setOnCompletionListener {
                it.release()
                players.remove(it)
            }
            mp.start()
        } catch (_: Exception) {
            // ses çalamazsa uygulama devam etsin
        }
    }

    private fun loadSprite(targetPx: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(resources, R.drawable.dvd_photo, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }
        val src = BitmapFactory.decodeResource(resources, R.drawable.dvd_photo, opts)
        val scale = targetPx.toFloat() / max(src.width, src.height)
        return Bitmap.createScaledBitmap(
            src,
            (src.width * scale).roundToInt().coerceAtLeast(1),
            (src.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun cleanup() {
        bounceView?.let {
            it.stop()
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        exitButton?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bounceView = null
        exitButton = null
        for (p in players) {
            try { p.release() } catch (_: Exception) {}
        }
        players.clear()
    }

    private fun exitApp() {
        if (exiting) return
        exiting = true
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Uygulamayı tamamen kapat
        Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 250)
    }

    override fun onDestroy() {
        cleanup()
        running = false
        super.onDestroy()
    }
}
