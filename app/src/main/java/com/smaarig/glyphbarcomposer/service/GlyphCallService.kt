package com.smaarig.glyphbarcomposer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.data.ContactBindingWithPlaylist
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import kotlinx.coroutines.*
import android.telephony.PhoneStateListener
import android.provider.ContactsContract
import android.net.Uri
import android.telephony.PhoneNumberUtils

@Suppress("DEPRECATION")
class GlyphCallService : Service() {
    private val TAG = "GlyphCallService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null
    private var isRinging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notification)
        }
        
        try {
            registerPhoneStateListener()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register phone state listener: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("glyph_calls", "Glyph Call Monitoring", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "glyph_calls")
            .setContentTitle("Glyph Call Sync")
            .setContentText("Monitoring for incoming calls...")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun registerPhoneStateListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        if (!isRinging) {
                            isRinging = true
                            // If phoneNumber is null (common on some Android versions), we'll try to get it from logs or recent calls if possible
                            // But usually it's provided here.
                            phoneNumber?.let { handleIncomingCall(it) }
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> {
                        isRinging = false
                        stopGlyphLoop()
                    }
                }
            }
        }
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun handleIncomingCall(number: String) {
        serviceScope.launch {
            val contactId = getContactIdFromNumber(number)
            if (contactId != null) {
                val db = AppDatabase.getDatabase(this@GlyphCallService)
                val binding = withContext(Dispatchers.IO) {
                    db.playlistDao().getContactBinding(contactId)
                }
                if (binding != null) {
                    Log.d(TAG, "Found binding for contact: ${binding.binding.contactName}")
                    startGlyphLoop(binding)
                }
            }
        }
    }

    private fun getContactIdFromNumber(number: String): String? {
        val normalized = PhoneNumberUtils.normalizeNumber(number)
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        try {
            val cursor = contentResolver.query(uri, projection, null, null, null)
            return cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun startGlyphLoop(binding: ContactBindingWithPlaylist) {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            val glyphController = GlyphController.getInstance(this@GlyphCallService)
            
            val localSteps = mutableListOf<GlyphSequence>()
            val dbSteps: List<SequenceStep> = withContext(Dispatchers.IO) {
                val pSteps = AppDatabase.getDatabase(this@GlyphCallService).playlistDao().getPlaylistWithSteps(binding.binding.playlistId)
                pSteps?.steps ?: emptyList()
            }
            
            val sortedList = dbSteps.sortedBy { it.stepIndex }
            for (s in sortedList) {
                localSteps.add(GlyphSequence(s.channelIntensities, s.durationMs))
            }
            
            if (localSteps.isEmpty()) return@launch

            try {
                // Wait 500ms to let system ringtone start, then override it
                delay(500)
                
                while (isRinging) {
                    // Turn off once per loop to clear system frames
                    glyphController.turnOffGlyphs()
                    delay(20)
                    
                    for (gs in localSteps) {
                        if (!isRinging) break
                        glyphController.applyGlyphStateWithIntensities(gs.channelIntensities, gs.durationMs)
                        delay(gs.durationMs.toLong() + 30) // Slightly faster delay to keep priority
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Loop error: ${e.message}")
            } finally {
                glyphController.turnOffGlyphs()
            }
        }
    }

    private fun stopGlyphLoop() {
        playbackJob?.cancel()
        playbackJob = null
        GlyphController.getInstance(this).turnOffGlyphs()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopGlyphLoop()
    }
}
