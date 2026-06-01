package com.smaarig.glyphbarcomposer.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SequencePlayerConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as GlyphApplication
        val repository = app.repository

        setContent {
            GlyphBarComposerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val playlists by repository.allPlaylists.collectAsState(initial = emptyList())

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_sequence_title).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "CHOOSE A GLYPH SEQUENCE FOR YOUR WIDGET",
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        if (playlists.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_sequences_found),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(playlists) { playlist ->
                                    SequenceListItem(playlist) {
                                        onSequenceSelected(playlist)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onSequenceSelected(playlist: PlaylistWithSteps) {
        val context = this
        MainScope().launch {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_ID] = playlist.playlist.id
                    this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_NAME] = playlist.playlist.name
                    this[GlyphSequencePlayerWidget.IS_PLAYING] = false
                }
            }

            GlyphSequencePlayerWidget().update(context, glanceId)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    @Composable
    private fun SequenceListItem(playlist: PlaylistWithSteps, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121212))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1DB954)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.playlist.name.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${playlist.steps.size} STEPS • GLYPHBAR",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
