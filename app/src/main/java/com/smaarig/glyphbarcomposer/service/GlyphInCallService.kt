package com.smaarig.glyphbarcomposer.service

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.ContactBindingWithPlaylist
import com.smaarig.glyphbarcomposer.data.SequenceStep
import kotlinx.coroutines.*

/**
 * InCallService implementation for high-priority glyph overrides during calls.
 * This service is bound by the system when a call event occurs.
 */
class GlyphInCallService : InCallService() {

    companion object {
        private const val TAG = "GlyphInCallService"
        const val ACTION_RESET_GLYPHS = "com.smaarig.glyphbarcomposer.ACTION_RESET_GLYPHS"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null
    
    @Volatile private var isRinging = false
    private lateinit var glyphController: GlyphController

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallState(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        glyphController = GlyphController.getInstance(applicationContext)
        Log.d(TAG, "InCallService created")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: $call")
        call.registerCallback(callCallback)
        handleCallState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: $call")
        call.unregisterCallback(callCallback)
        if (isRinging) {
            isRinging = false
            stopGlyphLoop()
        }
    }

    private fun handleCallState(call: Call, state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                if (!isRinging) {
                    isRinging = true
                    val handle = call.details.handle
                    val number = handle?.schemeSpecificPart ?: ""
                    Log.d(TAG, "Incoming call detected (RINGING). Number: $number")
                    if (number.isNotEmpty()) {
                        handleIncomingCall(number)
                    }
                }
            }
            Call.STATE_ACTIVE, Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                if (isRinging) {
                    isRinging = false
                    Log.d(TAG, "Call handled (state=$state). Stopping glyphs.")
                    stopGlyphLoop()
                }
            }
        }
    }

    private fun handleIncomingCall(number: String) {
        serviceScope.launch {
            val contactId = withContext(Dispatchers.IO) { resolveContactId(number) }
            Log.d(TAG, "Resolved contactId: $contactId for $number")
            if (contactId == null) return@launch

            val binding = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@GlyphInCallService)
                    .playlistDao()
                    .getContactBinding(contactId)
            }

            if (binding != null) {
                Log.d(TAG, "Found custom glyph binding for ${binding.binding.contactName}")
                startGlyphLoop(binding)
            }
        }
    }

    private fun resolveContactId(number: String): String? {
        val normalized = PhoneNumberUtils.normalizeNumber(number)
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalized)
        )
        return try {
            contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
            )?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) {
            Log.e(TAG, "Contact lookup error: ${e.message}")
            null
        }
    }

    private fun startGlyphLoop(binding: ContactBindingWithPlaylist) {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            val steps = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@GlyphInCallService)
                    .playlistDao()
                    .getPlaylistWithSteps(binding.binding.playlistId)
                    ?.steps
                    ?.sortedBy { it.stepIndex }
                    ?: emptyList()
            }

            if (steps.isEmpty()) return@launch

            Log.d(TAG, "Starting persistent glyph loop: ${steps.size} steps")
            try {
                // Initial session open for priority
                glyphController.turnOffGlyphs()
                
                while (isRinging && isActive) {
                    for (step in steps) {
                        if (!isRinging || !isActive) break
                        
                        // Apply state with explicit intensities
                        glyphController.applyGlyphStateWithIntensities(step.channelIntensities, step.durationMs)
                        
                        delay(step.durationMs.toLong() + 50L)
                    }
                }
            } catch (e: CancellationException) {
                // Expected
            } catch (e: Exception) {
                Log.e(TAG, "Playback loop error: ${e.message}")
            } finally {
                safelyTurnOff()
            }
        }
    }

    private fun stopGlyphLoop() {
        playbackJob?.cancel()
        playbackJob = null
        safelyTurnOff()
    }

    private fun safelyTurnOff() {
        try {
            glyphController.turnOffGlyphs()
        } catch (e: Exception) {
            Log.e(TAG, "safelyTurnOff error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGlyphLoop()
        serviceScope.cancel()
        Log.d(TAG, "InCallService destroyed")
    }
}
