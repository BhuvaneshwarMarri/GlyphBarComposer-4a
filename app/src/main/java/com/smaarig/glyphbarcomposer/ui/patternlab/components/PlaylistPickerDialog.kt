package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistWithSteps>,
    onSelect: (PlaylistWithSteps) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Sequence", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            if (playlists.isEmpty()) {
                Text("No saved sequences found.", color = Color.Gray)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(playlists) { p ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(p) },
                            color = Color.Transparent
                        ) {
                            Text(p.playlist.name, modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White, fontWeight = FontWeight.Black) }
        },
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp)
    )
}
