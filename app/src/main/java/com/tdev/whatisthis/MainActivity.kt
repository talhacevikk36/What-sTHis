package com.tdev.whatisthis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private companion object {
        const val REQ_PHOTO = 101
        const val REQ_SOUND = 102
    }

    private class Slider(val bar: SeekBar, val label: TextView)

    private lateinit var thumb: ImageView
    private lateinit var soundInfo: TextView
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var sCloseAt: Slider
    private lateinit var sAlpha: Slider
    private lateinit var sSize: Slider
    private lateinit var sSpeed: Slider
    private var preview: MediaPlayer? = null

    private val d get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    private fun text(s: String, size: Float, color: Int) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(0xFFBDBDBD.toInt())
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
    }

    /** Başlık + değiştir butonu olan satır. */
    private fun row(left: View, title: String, sub: TextView, onClick: () -> Unit): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(12) }
            addView(text(title, 16f, 0xFF212121.toInt()))
            addView(sub)
        }
        val btn = Button(this).apply {
            text = "Değiştir"
            setOnClickListener { onClick() }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(8), dp(10))
            addView(left)
            addView(col)
            addView(btn)
        }
    }

    /** Başlığı ve değeri üstte, çubuğu altta olan ayar. */
    private fun slider(
        title: String, min: Int, max: Int, step: Int, key: String,
        unit: String, current: () -> Int
    ): Pair<Slider, LinearLayout> {
        val label = text("", 16f, 0xFF212121.toInt())
        val bar = SeekBar(this).apply { this.max = (max - min) / step }
        fun show(v: Int) { label.text = "$title: $v$unit" }

        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                val v = min + p * step
                show(v)
                if (fromUser) Prefs.put(this@MainActivity, key, v)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            addView(label)
            addView(bar)
        }
        val s = Slider(bar, label)
        // Değeri dışarıdan ayarlamak için (refresh)
        bar.tag = Triple(min, step, current)
        return Pair(s, box)
    }

    private fun setSlider(s: Slider, v: Int) {
        @Suppress("UNCHECKED_CAST")
        val t = s.bar.tag as Triple<Int, Int, () -> Int>
        s.bar.progress = ((v - t.first) / t.second).coerceIn(0, s.bar.max)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Eski usul koyu gri başlık çubuğu
        root.addView(TextView(this).apply {
            text = "WHAT IS THIS"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xFF212121.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            )
        })

        status = text("", 14f, 0xFF616161.toInt()).apply { setPadding(dp(16), dp(12), dp(16), dp(12)) }
        root.addView(status)
        root.addView(divider())

        thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        root.addView(row(thumb, "Fotoğraf", text("Ekranda gezen görsel", 13f, 0xFF757575.toInt())) {
            pick("image/*", REQ_PHOTO)
        })
        root.addView(divider())

        soundInfo = text("", 13f, 0xFF757575.toInt())
        val note = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            setColorFilter(0xFF616161.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(32))
        }
        root.addView(row(note, "Ses", soundInfo) { pick("audio/*", REQ_SOUND) })
        root.addView(divider())

        val a = slider("Kaçıncı basışta kapansın", 1, 10, 1, "closeAt", "") { Prefs.closeAt(this) }
        val b = slider("ÇIK butonu opaklığı", 20, 100, 5, "buttonAlpha", "%") { Prefs.buttonAlpha(this) }
        val c = slider("DVD boyutu", 60, 240, 10, "sizeDp", " dp") { Prefs.sizeDp(this) }
        val e = slider("Hız", 80, 600, 20, "speedDp", " dp/sn") { Prefs.speedDp(this) }
        sCloseAt = a.first; sAlpha = b.first; sSize = c.first; sSpeed = e.first
        for (box in listOf(a.second, b.second, c.second, e.second)) {
            root.addView(box)
            root.addView(divider())
        }

        root.addView(Button(this).apply {
            text = "Her şeyi sıfırla"
            setOnClickListener {
                CustomMedia.reset(this@MainActivity)
                Prefs.reset(this@MainActivity)
                refresh()
                Toast.makeText(this@MainActivity, "Sıfırlandı", Toast.LENGTH_SHORT).show()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(16), dp(16), dp(16), 0) }
        })

        startButton = Button(this).apply {
            setOnClickListener { onStartPressed() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(16), dp(8), dp(16), dp(24)) }
        }
        root.addView(startButton)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
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
        status.text = if (ok) "Üstte gösterme izni verildi." else "Ekranın üstünde gösterme izni gerekiyor."
        startButton.text = if (ok) "Başlat" else "İzni ver"

        val bmp = if (CustomMedia.hasPhoto(this))
            BitmapFactory.decodeFile(CustomMedia.photoFile(this).path) else null
        if (bmp != null) thumb.setImageBitmap(bmp) else thumb.setImageResource(R.drawable.dvd_photo)

        soundInfo.text = if (CustomMedia.hasSound(this)) "Seçtiğin ses" else "Varsayılan ses"

        setSlider(sCloseAt, Prefs.closeAt(this))
        setSlider(sAlpha, Prefs.buttonAlpha(this))
        setSlider(sSize, Prefs.sizeDp(this))
        setSlider(sSpeed, Prefs.speedDp(this))
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
                Toast.makeText(this, if (ok) "Fotoğraf ayarlandı" else "Bu fotoğraf okunamadı", Toast.LENGTH_SHORT).show()
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

    private fun onStartPressed() {
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
