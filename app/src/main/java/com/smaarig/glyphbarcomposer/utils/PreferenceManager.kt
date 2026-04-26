package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USE_OLD_VERSION = "use_old_version"
    }

    var useOldVersion: Boolean
        get() = prefs.getBoolean(KEY_USE_OLD_VERSION, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_OLD_VERSION, value).apply()
}
