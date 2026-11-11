package com.tdev.whatisthis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Choreographer
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Eski DVD ekran koruyucusu gibi: görsel kenarlara çarpıp seker.
 * Birden fazla sprite aynı anda çizilir.
 */
class BounceView(
    context: Context,
    private val bitmap: Bitmap,
    private val speedPxPerSec: Float
) : View(context) {

    private class Sprite(var x: Float, var y: Float, var vx: Float, var vy: Float)

    private val sprites = ArrayList<Sprite>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var pendingSpawns = 0
    private var lastFrameNanos = 0L
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            step(frameTimeNanos)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Yeni bir DVD ekle (görünüm ölçülene kadar bekletilir). */
    fun addSprite() {
        pendingSpawns++
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun createSprite() {
        val maxX = (width - bitmap.width).coerceAtLeast(0).toFloat()
        val maxY = (height - bitmap.height).coerceAtLeast(0).toFloat()
        val angle = Math.toRadians(Random.nextDouble(25.0, 65.0))
        val vx = (speedPxPerSec * cos(angle)).toFloat() * if (Random.nextBoolean()) 1f else -1f
        val vy = (speedPxPerSec * sin(angle)).toFloat() * if (Random.nextBoolean()) 1f else -1f
        sprites.add(Sprite(Random.nextFloat() * maxX, Random.nextFloat() * maxY, vx, vy))
    }

    private fun step(frameTimeNanos: Long) {
        if (width == 0 || height == 0) return

        while (pendingSpawns > 0) {
            createSprite()
            pendingSpawns--
        }

        val dt = if (lastFrameNanos == 0L) 0f
        else ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameNanos = frameTimeNanos

        val maxX = (width - bitmap.width).coerceAtLeast(0).toFloat()
        val maxY = (height - bitmap.height).coerceAtLeast(0).toFloat()

        for (s in sprites) {
            s.x += s.vx * dt
            s.y += s.vy * dt
            if (s.x < 0f) { s.x = 0f; s.vx = abs(s.vx) }
            else if (s.x > maxX) { s.x = maxX; s.vx = -abs(s.vx) }
            if (s.y < 0f) { s.y = 0f; s.vy = abs(s.vy) }
            else if (s.y > maxY) { s.y = maxY; s.vy = -abs(s.vy) }
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (s in sprites) {
            canvas.drawBitmap(bitmap, s.x, s.y, paint)
        }
    }
}
