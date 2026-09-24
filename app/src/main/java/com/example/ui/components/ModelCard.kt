package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadStatus
import com.example.model.ModelItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelCard(
    model: ModelItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = model.downloadStatus == DownloadStatus.COMPLETED
    val isDownloading = model.downloadStatus == DownloadStatus.DOWNLOADING
    val isPaused = model.downloadStatus == DownloadStatus.PAUSED

    val borderColor = when {
        isSelected -> AccentMint
        isCompleted -> AccentMint.copy(alpha = 0.5f)
        else -> DarkCardBorder
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("model_card_${model.id}")
    ) {
        // Top row: Family icon, Name, Quantization badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentMint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (model.family.equals("DeepSeek", true)) Icons.Default.Psychology else Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (model.family.equals("DeepSeek", true)) AccentPurple else AccentMint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${model.family} • ${model.parameterCount}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Quantization chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = model.quantization,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Description
        if (model.description.isNotBlank()) {
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tags & Specs row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SpecChip(text = "💾 ${model.sizeFormatted}", color = AccentMint)
            SpecChip(text = "🧠 ${model.recommendedRam}", color = AccentCyan)
            SpecChip(text = "📐 ${model.contextLength} ctx", color = AccentPurple)

            for (tag in model.tags.take(3)) {
                SpecChip(text = "#$tag", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Action Area depending on downloadStatus
        when {
            isDownloading -> {
                // Downloading State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (model.downloadSpeed.isNotBlank()) "Downloading (${model.downloadSpeed})" else "Connecting...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = AccentMint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f%%", model.downloadProgress * 100f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { model.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentMint,
                        trackColor = DarkCardBorder
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(36.dp).testTag("pause_download_${model.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AccentCyan, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(36.dp).testTag("cancel_download_${model.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AccentRose, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            isPaused -> {
                // Paused State
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Download Paused",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier.testTag("resume_download_${model.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", color = Color.Black)
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AccentRose)
                        }
                    }
                }
            }

            isCompleted -> {
                // Downloaded / Completed State
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isSelected) "Active in Chat" else "Ready On-Device",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isSelected) AccentMint else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Inspect GGUF button
                        OutlinedButton(
                            onClick = onInspect,
                            modifier = Modifier.testTag("inspect_gguf_${model.id}")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inspect", fontSize = 12.sp)
                        }

                        // Select / Chat button
                        if (!isSelected) {
                            Button(
                                onClick = onSelect,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentMint),
                                modifier = Modifier.testTag("select_model_${model.id}")
                            ) {
                                Text("Select", color = Color(0xFF003824), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Delete button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_model_${model.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete model",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                // Not Downloaded State: Big 1-Tap Download Button!
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_button_${model.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentMint,
                        contentColor = Color(0xFF003824)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-Tap Download (${model.sizeFormatted})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SpecChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = color
        )
    }
}
