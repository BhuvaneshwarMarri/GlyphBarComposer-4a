package com.smaarig.glyphbarcomposer.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smaarig.glyphbarcomposer.common.WidgetConstants

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetConstants.ACTION_UPDATE_WIDGETS) {
            val intensities = intent.getIntArrayExtra(WidgetConstants.EXTRA_INTENSITIES)?.toList()
            val isPlaying = if (intent.hasExtra(WidgetConstants.EXTRA_IS_PLAYING)) {
                intent.getBooleanExtra(WidgetConstants.EXTRA_IS_PLAYING, false)
            } else null
            
            val playlistId = if (intent.hasExtra(WidgetConstants.EXTRA_PLAYLIST_ID)) {
                intent.getLongExtra(WidgetConstants.EXTRA_PLAYLIST_ID, -1L)
            } else null
            
            val playlistName = intent.getStringExtra(WidgetConstants.EXTRA_PLAYLIST_NAME)

            updateAllWidgets(
                context = context,
                intensities = intensities,
                isPlaying = isPlaying,
                playlistId = playlistId,
                playlistName = playlistName
            )
        }
    }
}
