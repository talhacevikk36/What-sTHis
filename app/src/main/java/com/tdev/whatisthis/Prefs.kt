package com.tdev.whatisthis

import android.content.Context

/** Kullanıcı ayarları (SharedPreferences). */
object Prefs {
    private const val FILE = "settings"

    // Varsayılanlar
    const val DEF_CLOSE_AT = 4      // 4. basışta kapanır (ilk 3'ü yeni DVD)
    const val DEF_BUTTON_ALPHA = 100 // yüzde
    const val DEF_SIZE_DP = 120
    const val DEF_SPEED_DP = 220

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun closeAt(c: Context) = sp(c).getInt("closeAt", DEF_CLOSE_AT)
    fun buttonAlpha(c: Context) = sp(c).getInt("buttonAlpha", DEF_BUTTON_ALPHA)
    fun sizeDp(c: Context) = sp(c).getInt("sizeDp", DEF_SIZE_DP)
    fun speedDp(c: Context) = sp(c).getInt("speedDp", DEF_SPEED_DP)

    fun put(c: Context, key: String, value: Int) = sp(c).edit().putInt(key, value).apply()
    fun reset(c: Context) = sp(c).edit().clear().apply()
}
