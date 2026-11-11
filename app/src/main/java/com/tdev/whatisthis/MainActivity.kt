package com.tdev.whatisthis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private companion object {
        const val REQ_PHOTO = 101
        const val REQ_SOUND = 102
    }

    private lateinit var cover: ImageView
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var soundInfo: TextView
    private lateinit var resetButton: Button
    private var preview: MediaPlayer? = null

    private val d get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    private fun pill(color: Int, textColor: Int, label: String, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            textSize = 16f
            setTextColor(textColor)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 40f * d
            }
            setPadding(dp(28), dp(10), dp(28), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener { onClick() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        cover = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(180), dp(180))
        }

        val title = TextView(this).apply {
            text = "WHAT IS THIS"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(6))
        }

        status = TextView(this).apply {
            setTextColor(0xFFB0B8D0.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }

        soundInfo = TextView(this).apply {
            setTextColor(0xFF8A93B2.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }

        startButton = pill(0xFF3DDC84.toInt(), 0xFF073042.toInt(), "BAŞLAT") { onStart() }
        val photoButton = pill(0xFF2A3358.toInt(), Color.WHITE, "Foto seç") { pick("image/*", REQ_PHOTO) }
        val soundButton = pill(0xFF2A3358.toInt(), Color.WHITE, "Ses seç") { pick("audio/*", REQ_SOUND) }
        resetButton = pill(0x00000000, 0xFFB0B8D0.toInt(), "Varsayılana dön") {
            CustomMedia.reset(this)
            refresh()
            Toast.makeText(this, "Varsayılan foto ve ses geri geldi", Toast.LENGTH_SHORT).show()
        }

        root.addView(cover)
        root.addView(title)
        root.addView(status)
        root.addView(startButton)
        root.addView(photoButton)
        root.addView(soundButton)
        root.addView(soundInfo)
        root.addView(resetButton)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFF0B1020.toInt())
            isFillViewport = true
            addView(root)
        })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        stopPreview()
        super.onPause()
    }

    private fun refresh() {
        val ok = Settings.canDrawOverlays(this)
        status.text = if (ok) "İzin tamam. Başlat'a bas." else "Ekranın üstünde gösterme izni lazım."
        startButton.text = if (ok) "BAŞLAT" else "İZNİ VER"

        if (CustomMedia.hasPhoto(this)) {
            val bmp = BitmapFactory.decodeFile(CustomMedia.photoFile(this).path)
            if (bmp != null) cover.setImageBitmap(bmp) else cover.setImageResource(R.drawable.dvd_photo)
        } else {
            cover.setImageResource(R.drawable.dvd_photo)
        }

        soundInfo.text = if (CustomMedia.hasSound(this)) "Ses: senin seçtiğin" else "Ses: varsayılan"
        val custom = CustomMedia.hasPhoto(this) || CustomMedia.hasSound(this)
        resetButton.alpha = if (custom) 1f else 0.35f
        resetButton.isEnabled = custom
    }

    private fun pick(mime: String, code: Int) {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mime
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(i, "Seç"), code)
        } catch (_: Exception) {
            Toast.makeText(this, "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQ_PHOTO -> {
                val ok = CustomMedia.savePhoto(this, uri)
                Toast.makeText(this, if (ok) "Foto ayarlandı" else "Bu foto okunamadı", Toast.LENGTH_SHORT).show()
            }
            REQ_SOUND -> {
                val ok = CustomMedia.saveSound(this, uri)
                Toast.makeText(this, if (ok) "Ses ayarlandı" else "Bu ses dosyası çalınamadı", Toast.LENGTH_SHORT).show()
                if (ok) playPreview()
            }
        }
        refresh()
    }

    private fun playPreview() {
        stopPreview()
        try {
            preview = MediaPlayer().apply {
                setDataSource(CustomMedia.soundFile(this@MainActivity).path)
                prepare()
                setOnCompletionListener { stopPreview() }
                start()
            }
        } catch (_: Exception) {
            stopPreview()
        }
    }

    private fun stopPreview() {
        preview?.let { try { it.release() } catch (_: Exception) {} }
        preview = null
    }

    private fun onStart() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        stopPreview()
        startForegroundService(Intent(this, OverlayService::class.java))
        finish()
    }
}
