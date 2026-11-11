package com.tdev.whatisthis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0B1020.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val cover = ImageView(this).apply {
            setImageResource(R.drawable.dvd_photo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(200), dp(200))
        }

        val title = TextView(this).apply {
            text = "WHAT IS THIS"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
        }

        status = TextView(this).apply {
            setTextColor(0xFFB0B8D0.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }

        button = Button(this).apply {
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 18f
            background = GradientDrawable().apply {
                setColor(0xFF3DDC84.toInt())
                cornerRadius = 40f * d
            }
            setTextColor(0xFF073042.toInt())
            setPadding(dp(36), dp(12), dp(36), dp(12))
            setOnClickListener { onButton() }
        }

        root.addView(cover)
        root.addView(title)
        root.addView(status)
        root.addView(button)
        setContentView(root)

        // Bildirim izni (Android 13+): servis bildirimi görünsün diye, zorunlu değil
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        status.text = if (ok) "İzin tamam. Başlat'a bas." else "Ekranın üstünde gösterme izni lazım."
        button.text = if (ok) "BAŞLAT" else "İZNİ VER"
    }

    private fun onButton() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        finish()
    }
}
