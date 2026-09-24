package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.AccentMint
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.CodeBlockBg
import com.example.ui.theme.CodeBlockBorder
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Locale

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.sender == MessageSender.USER
    var isThinkingExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message Bubble
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, AccentMint.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        } else {
            // Assistant Message Bubble
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(14.dp)
            ) {
                // Header with model badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AccentMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Model",
                                modifier = Modifier.size(14.dp),
                                tint = AccentMint
                            )
                        }
                        Text(
                            text = message.modelUsed ?: "Local Model",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Copy action
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PocketLLM", message.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Collapsible Thinking Section (DeepSeek R1 style)
                if (!message.thinkingContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { isThinkingExpanded = !isThinkingExpanded }
                            .padding(10.dp)
                    ) {
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
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isThinkingExpanded) "Thought Process" else "Thinking Process (tap to inspect)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentPurple,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Icon(
                                imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle thinking",
                                tint = AccentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isThinkingExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = message.thinkingContent,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Formatted content (handles code blocks and standard text)
                FormattedMessageContent(content = message.content)

                // Performance Telemetry Badge (tokens/sec, total tokens, elapsed time)
                if (message.tokensPerSecond != null || message.totalTokens != null || message.generationTimeMs != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (message.tokensPerSecond != null && message.tokensPerSecond > 0f) {
                            Text(
                                text = String.format(Locale.US, "⚡ %.1f tok/s", message.tokensPerSecond),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = AccentMint
                                )
                            )
                        }
                        if (message.generationTimeMs != null && message.generationTimeMs > 0) {
                            Text(
                                text = String.format(Locale.US, "⏱ %.1fs", message.generationTimeMs / 1000f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (message.totalTokens != null && message.totalTokens > 0) {
                            Text(
                                text = "📊 ${message.totalTokens} tokens",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun FormattedMessageContent(content: String) {
    val context = LocalContext.current
    val parts = remember(content) { splitIntoMarkdownParts(content) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (part in parts) {
            when (part) {
                is ContentPart.Text -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                is ContentPart.Code -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CodeBlockBg)
                            .border(1.dp, CodeBlockBorder, RoundedCornerShape(8.dp))
                    ) {
                        // Code block header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = part.language.ifEmpty { "code" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", part.code))
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Code block content
                        Text(
                            text = part.code,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

sealed class ContentPart {
    data class Text(val text: String) : ContentPart()
    data class Code(val code: String, val language: String) : ContentPart()
}

fun splitIntoMarkdownParts(content: String): List<ContentPart> {
    val result = mutableListOf<ContentPart>()
    val codeFenceRegex = "```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```".toRegex()
    var lastIndex = 0

    codeFenceRegex.findAll(content).forEach { match ->
        if (match.range.first > lastIndex) {
            val textBefore = content.substring(lastIndex, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                result.add(ContentPart.Text(textBefore))
            }
        }
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        result.add(ContentPart.Code(code, lang))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < content.length) {
        val remaining = content.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            result.add(ContentPart.Text(remaining))
        }
    }

    if (result.isEmpty() && content.isNotBlank()) {
        result.add(ContentPart.Text(content))
    }

    return result
}
