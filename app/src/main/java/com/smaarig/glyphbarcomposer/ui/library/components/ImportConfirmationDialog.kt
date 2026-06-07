package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.data.SequenceStep
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun ImportConfirmationDialog(
    steps: List<SequenceStep>,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    
    val totalDuration = steps.sumOf { it.durationMs }
    val minDuration = steps.minOfOrNull { it.durationMs } ?: 0
    val maxDuration = steps.maxOfOrNull { it.durationMs } ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CONFIRM IMPORT",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontFamily = nothingFont,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "You are about to import a glyph sequence with ${steps.size} steps.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = nothingFont,
                    lineHeight = 20.sp
                )
                
                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "SEQUENCE SUMMARY",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = nothingFont,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total duration:", color = Color.Gray, fontSize = 12.sp, fontFamily = nothingFont)
                            Text("${totalDuration}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = nothingFont)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Step range:", color = Color.Gray, fontSize = 12.sp, fontFamily = nothingFont)
                            Text("${minDuration}ms - ${maxDuration}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = nothingFont)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("SEQUENCE NAME", color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp, fontFamily = nothingFont) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = nothingFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        cursorColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(
                    "IMPORT",
                    color = if (name.isNotBlank()) Color(0xFF00C853) else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontFamily = nothingFont,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    color = Color.Gray,
                    fontFamily = nothingFont,
                    letterSpacing = 1.sp
                )
            }
        },
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp)
    )
}
