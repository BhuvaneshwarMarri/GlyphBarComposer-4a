package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.smaarig.glyphbarcomposer.ui.StyledSaveDialog

@Composable
fun SaveMixDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    StyledSaveDialog(
        title = "Save Mix",
        value = name,
        onValueChange = { name = it },
        onSave = { onSave(name) },
        onDismiss = onDismiss,
        placeholder = "Enter mix name"
    )
}
