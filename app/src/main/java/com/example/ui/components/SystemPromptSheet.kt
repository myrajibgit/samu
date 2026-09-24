package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SystemPromptPreset
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptSheet(
    initialPrompt: String,
    initialTemperature: Float,
    initialTopP: Float,
    presets: List<SystemPromptPreset>,
    onSave: (prompt: String, temperature: Float, topP: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPrompt by remember { mutableStateOf(initialPrompt) }
    var currentTemperature by remember { mutableFloatStateOf(initialTemperature) }
    var currentTopP by remember { mutableFloatStateOf(initialTopP) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null,
        modifier = Modifier.testTag("system_prompt_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = AccentMint)
                Text(
                    text = "Model Parameters & Persona",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets row
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .clickable { currentPrompt = preset.prompt }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AccentCyan
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // System prompt text field
            Text(
                text = "SYSTEM PROMPT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = currentPrompt,
                onValueChange = { currentPrompt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("system_prompt_input"),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Temperature Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temperature (Creativity)",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = String.format(Locale.US, "%.2f", currentTemperature),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = AccentMint,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Slider(
                value = currentTemperature,
                onValueChange = { currentTemperature = it },
                valueRange = 0.1f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentMint,
                    activeTrackColor = AccentMint,
                    inactiveTrackColor = DarkCardBorder
                ),
                modifier = Modifier.testTag("temperature_slider")
            )

            // Top-P Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top-P (Nucleus Sampling)",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = String.format(Locale.US, "%.2f", currentTopP),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Slider(
                value = currentTopP,
                onValueChange = { currentTopP = it },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = DarkCardBorder
                ),
                modifier = Modifier.testTag("top_p_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onSave(currentPrompt, currentTemperature, currentTopP)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_parameters_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMint,
                    contentColor = Color(0xFF003824)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Parameters", fontWeight = FontWeight.Bold)
            }
        }
    }
}
