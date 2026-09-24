package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.engine.OllamaModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CodeBlockBg
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.util.HardwareUtils
import java.util.Locale

@Composable
fun OllamaScreen(
    hostUrl: String,
    isConnected: Boolean,
    isChecking: Boolean,
    statusMessage: String?,
    models: List<OllamaModel>,
    isPulling: Boolean,
    pullProgress: Float,
    pullStatusText: String,
    onHostUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onPullModel: (String) -> Unit,
    onSelectOllamaModel: (OllamaModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var pullInputModel by remember { mutableStateOf("llama3.2") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Ollama Hub Bridge",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Connect to any Ollama server on LAN, localhost, or emulator host",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Connection Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, if (isConnected) AccentMint else DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "OLLAMA HOST ADDRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hostUrl,
                    onValueChange = onHostUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ollama_host_input"),
                    placeholder = { Text("http://10.0.2.2:11434") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick preset chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PresetHostChip("Emulator (10.0.2.2)") {
                        onHostUrlChange("http://10.0.2.2:11434")
                    }
                    PresetHostChip("Localhost (11434)") {
                        onHostUrlChange("http://localhost:11434")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTestConnection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_ollama_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) AccentMint else AccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isConnected) "Connected (Tap to Refresh)" else "Connect to Ollama", fontWeight = FontWeight.Bold)
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isConnected) AccentMint else AccentRose,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Pull Model Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AccentMint)
                    Text(
                        text = "1-Tap Ollama Model Pull",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pull any official Ollama model library tag directly to your host:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pullInputModel,
                        onValueChange = { pullInputModel = it },
                        modifier = Modifier.weight(1f).testTag("ollama_pull_input"),
                        placeholder = { Text("llama3.2, deepseek-r1:1.5b…") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { onPullModel(pullInputModel) },
                        enabled = pullInputModel.isNotBlank() && !isPulling && isConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMint),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp).testTag("ollama_pull_button")
                    ) {
                        Text("Pull", color = Color(0xFF003824), fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(visible = isPulling) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = pullStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentMint
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%.0f%%", pullProgress * 100f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { pullProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentMint,
                            trackColor = DarkCardBorder
                        )
                    }
                }
            }
        }

        // Remote Models List
        item {
            Text(
                text = "HOST MODELS (${models.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        if (models.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isConnected) "Connect to an Ollama server to view host models." else "No models installed on host. Use Pull above to get started!",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(models) { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = m.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${m.family} • ${m.parameterSize} • ${m.quantizationLevel} • ${HardwareUtils.formatBytes(m.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Button(
                        onClick = { onSelectOllamaModel(m) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Chat", color = AccentMint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Terminal Instructions Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "How to run Ollama for LAN Pairing",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "On your Mac/Linux/Windows machine, start Ollama with LAN binding enabled:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CodeBlockBg)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "OLLAMA_HOST=0.0.0.0 OLLAMA_ORIGINS=\"*\" ollama serve",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AccentMint
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetHostChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = AccentCyan
            )
        )
    }
}
