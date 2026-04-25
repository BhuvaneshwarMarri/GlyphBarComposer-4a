package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PreferenceManagerTest {

    private lateinit var preferenceManager: PreferenceManager
    private val context = mockk<Context>()
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    @Before
    fun setup() {
        every { context.getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        // editor.putBoolean returns itself for chaining
        every { editor.putBoolean(any(), any()) } returns editor
        preferenceManager = PreferenceManager(context)
    }

    @Test
    fun `get useOldVersion returns value from prefs`() {
        every { sharedPreferences.getBoolean("use_old_version", true) } returns false
        assertEquals(false, preferenceManager.useOldVersion)
    }

    @Test
    fun `set useOldVersion updates prefs`() {
        preferenceManager.useOldVersion = false
        verify { editor.putBoolean("use_old_version", false) }
        verify { editor.apply() }
    }
}
