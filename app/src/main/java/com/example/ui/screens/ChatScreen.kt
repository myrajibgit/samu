package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DeviceHardwareStats
import com.example.model.MessageSender
import com.example.model.ModelItem
import com.example.ui.AppTab
import com.example.ui.SystemPromptPreset
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.HardwareMonitorBadge
import com.example.ui.components.SystemPromptSheet
import com.example.ui.theme.AccentMint
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversation: Conversation?,
    activeModel: ModelItem?,
    messages: List<ChatMessage>,
    hardwareStats: DeviceHardwareStats,
    isGenerating: Boolean,
    streamingResponse: String,
    streamingThinking: String,
    generationTps: Float,
    generationTokensCount: Int,
    promptPresets: List<SystemPromptPreset>,
    isEngineLoading: Boolean = false,
    engineError: String? = null,
    onDismissEngineError: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onNewChat: () -> Unit,
    onOpenModelSelector: () -> Unit,
    onUpdateSettings: (prompt: String, temp: Float, topP: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto scroll down when new message or streaming arrives
    LaunchedEffect(messages.size, streamingResponse.length) {
        val totalCount = messages.size + (if (isGenerating) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    if (showSettingsSheet && conversation != null) {
        SystemPromptSheet(
            initialPrompt = conversation.systemPrompt,
            initialTemperature = conversation.temperature,
            initialTopP = conversation.topP,
            presets = promptPresets,
            onSave = onUpdateSettings,
            onDismiss = { showSettingsSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                title = {
                    // Model Selector Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                            .clickable { onOpenModelSelector() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("model_selector_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentMint)
                        )
                        Text(
                            text = activeModel?.name ?: "Select Model",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    // Hardware Stats Mini Badge
                    HardwareMonitorBadge(stats = hardwareStats)

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("chat_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Parameters",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = AccentMint
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Message Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Native engine status: loading weights, or a failure the user must know about.
                if (isEngineLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentMint.copy(alpha = 0.12f))
                            .border(1.dp, AccentMint.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("engine_loading_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏳ Loading model weights into RAM… large models can take a while.",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentMint)
                        )
                    }
                }

                if (engineError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentRose.copy(alpha = 0.12f))
                            .border(1.dp, AccentRose.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                            .testTag("engine_error_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = engineError,
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentRose),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentRose,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismissEngineError() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("engine_error_dismiss")
                        )
                    }
                }

                // If generating, show live stop action banner
                AnimatedVisibility(visible = isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AccentRose.copy(alpha = 0.15f))
                                .border(1.dp, AccentRose.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .clickable { onStopGeneration() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("stop_generation_chip"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = AccentRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Stop Generating",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRose
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        placeholder = {
                            Text(
                                text = "Message ${activeModel?.name?.take(16) ?: "llm-offline"}…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isGenerating) AccentMint else DarkSurfaceVariant)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isGenerating) Color(0xFF003824) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty() && !isGenerating) {
                // Empty state with quick suggestions
                EmptyChatWelcome(
                    activeModel = activeModel,
                    onSuggestionClick = { prompt ->
                        inputText = prompt
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(message = msg)
                    }

                    // Live Streaming Bubble
                    if (isGenerating && (streamingResponse.isNotEmpty() || streamingThinking.isNotEmpty())) {
                        item {
                            ChatMessageBubble(
                                message = ChatMessage(
                                    conversationId = conversation?.id ?: 0L,
                                    sender = MessageSender.ASSISTANT,
                                    content = streamingResponse,
                                    thinkingContent = streamingThinking.ifEmpty { null },
                                    tokensPerSecond = generationTps,
                                    totalTokens = generationTokensCount,
                                    modelUsed = activeModel?.name ?: "Local Model",
                                    isStreaming = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatWelcome(
    activeModel: ModelItem?,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AccentMint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = AccentMint,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = activeModel?.name ?: "llm-offline",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "${activeModel?.parameterCount ?: "On-Device"} • ${activeModel?.quantization ?: "GGUF"} • 100% Offline",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "QUICK PROMPTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        val suggestions = listOf(
            "Write a Kotlin function to stream items in Compose",
            "Explain quantum computing in simple terms",
            "What is GGUF format and why is it used?",
            "How does Ollama compare to on-device mobile AI?"
        )

        for (suggestion in suggestions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
