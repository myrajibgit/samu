package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import com.example.util.CodeFileItem

enum class CodeStudioViewMode {
    EDITOR,
    LIVE_PREVIEW,
    TERMINAL
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeStudioScreen(
    codeFiles: List<CodeFileItem>,
    selectedFile: CodeFileItem?,
    editorContent: String,
    isAiCoding: Boolean,
    aiSuggestion: String,
    terminalLog: String,
    onSelectFile: (CodeFileItem) -> Unit,
    onUpdateContent: (String) -> Unit,
    onSaveFile: () -> Unit,
    onCreateNewFile: (String) -> Unit,
    onDeleteCurrentFile: () -> Unit,
    onRunCode: () -> Unit,
    onShareFile: () -> Unit,
    onAskClaudeCode: (String) -> Unit,
    onApplyAiSuggestion: () -> Unit
) {
    var viewMode by remember { mutableStateOf(CodeStudioViewMode.EDITOR) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var claudePromptInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("code_studio_screen")
    ) {
        // Top Toolbar: File tabs & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = AccentMint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Claude Code Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            IconButton(
                onClick = {
                    onSaveFile()
                    onRunCode()
                    viewMode = if (selectedFile?.language == "html") CodeStudioViewMode.LIVE_PREVIEW else CodeStudioViewMode.TERMINAL
                },
                modifier = Modifier.size(36.dp).testTag("action_run_code")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Review code",
                    tint = AccentMint
                )
            }

            IconButton(
                onClick = onSaveFile,
                modifier = Modifier.size(36.dp).testTag("action_save_code")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onShareFile,
                modifier = Modifier.size(36.dp).testTag("action_share_code")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(editorContent))
                    showCopiedToast = true
                },
                modifier = Modifier.size(36.dp).testTag("action_copy_code")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // File Tabs Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F131C))
                .border(width = 0.5.dp, color = DarkCardBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(codeFiles) { file ->
                val isSelected = file.name == selectedFile?.name
                Surface(
                    onClick = { onSelectFile(file) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DarkCard else Color.Transparent,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentMint.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.testTag("file_tab_${file.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconColor = when (file.language) {
                            "html" -> Color(0xFFFF5722)
                            "python" -> Color(0xFF3776AB)
                            "kotlin" -> Color(0xFF7F52FF)
                            else -> AccentMint
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(iconColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = file.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                IconButton(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier.size(32.dp).testTag("btn_new_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New File",
                        tint = AccentMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // View Mode Switcher: [ Editor | Live Web | Terminal ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewMode == CodeStudioViewMode.EDITOR,
                onClick = { viewMode = CodeStudioViewMode.EDITOR },
                label = { Text("Code Editor", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkCard,
                    selectedLabelColor = AccentMint,
                    selectedLeadingIconColor = AccentMint
                )
            )

            if (selectedFile?.language == "html") {
                FilterChip(
                    selected = viewMode == CodeStudioViewMode.LIVE_PREVIEW,
                    onClick = {
                        onSaveFile()
                        viewMode = CodeStudioViewMode.LIVE_PREVIEW
                    },
                    label = { Text("Live Web Game/App", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DarkCard,
                        selectedLabelColor = AccentMint,
                        selectedLeadingIconColor = AccentMint
                    )
                )
            }

            FilterChip(
                selected = viewMode == CodeStudioViewMode.TERMINAL,
                onClick = {
                    onSaveFile()
                    onRunCode()
                    viewMode = CodeStudioViewMode.TERMINAL
                },
                label = { Text("Output / Review", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkCard,
                    selectedLabelColor = AccentMint,
                    selectedLeadingIconColor = AccentMint
                )
            )
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
        ) {
            when (viewMode) {
                CodeStudioViewMode.EDITOR -> {
                    CodeEditorView(
                        content = editorContent,
                        onContentChange = onUpdateContent
                    )
                }

                CodeStudioViewMode.LIVE_PREVIEW -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL(null, editorContent, "text/html", "utf-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, editorContent, "text/html", "utf-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                CodeStudioViewMode.TERMINAL -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = terminalLog.ifBlank { "Ready to execute. Tap ▶ to compile and run." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AccentMint,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Quick Code Access Bar (Symbols & Keywords for touch devices)
        if (viewMode == CodeStudioViewMode.EDITOR) {
            QuickSyntaxBar(onInsert = { symbol ->
                onUpdateContent(editorContent + symbol)
            })
        }

        // AI Suggestion Box (If available)
        if (aiSuggestion.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF13221C),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentMint)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Claude Code Solution Ready",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentMint
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onApplyAiSuggestion,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = Color(0xFF003824)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Apply to Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = aiSuggestion.take(180) + if (aiSuggestion.length > 180) "..." else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Claude Code Assistant Bottom Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface),
            color = DarkSurface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Quick Action Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    item {
                        ActionChip(
                            label = "Fix & Optimize",
                            icon = Icons.Default.Bolt,
                            onClick = { onAskClaudeCode("Analyze this code and optimize performance, fixing any edge cases.") }
                        )
                    }
                    item {
                        ActionChip(
                            label = "Add Touch Game Controls",
                            icon = Icons.Default.SportsEsports,
                            onClick = { onAskClaudeCode("Add responsive touch gesture controls and enhanced neon particle visuals.") }
                        )
                    }
                    item {
                        ActionChip(
                            label = "Explain Logic",
                            icon = Icons.Default.MenuBook,
                            onClick = { onAskClaudeCode("Explain line-by-line how this algorithm works.") }
                        )
                    }
                    item {
                        ActionChip(
                            label = "Add Unit Tests",
                            icon = Icons.Default.CheckCircle,
                            onClick = { onAskClaudeCode("Write comprehensive unit tests covering all functions.") }
                        )
                    }
                }

                // AI Prompt Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = claudePromptInput,
                        onValueChange = { claudePromptInput = it },
                        placeholder = { Text("Ask Claude Code to write or modify...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("claude_prompt_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (claudePromptInput.isNotBlank()) {
                                onAskClaudeCode(claudePromptInput)
                                claudePromptInput = ""
                            }
                        },
                        enabled = !isAiCoding && claudePromptInput.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (!isAiCoding && claudePromptInput.isNotBlank()) AccentMint else DarkCard)
                            .testTag("btn_ask_claude")
                    ) {
                        if (isAiCoding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentMint,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (claudePromptInput.isNotBlank()) Color(0xFF003824) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New Code File") },
            text = {
                Column {
                    Text("Enter filename (e.g. script.js, app.py, style.css, solution.kt):", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            onCreateNewFile(newFileNameInput.trim())
                            newFileNameInput = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = Color(0xFF003824))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CodeEditorView(
    content: String,
    onContentChange: (String) -> Unit
) {
    val lines = content.lines()
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        // Line Numbers Gutter
        Column(
            modifier = Modifier
                .width(36.dp)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            lines.indices.forEach { index ->
                Text(
                    text = "${index + 1}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF4A5568),
                    lineHeight = 18.sp
                )
            }
        }

        // Code Editor Text Field
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 18.sp
            ),
            cursorBrush = SolidColor(AccentMint),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .testTag("code_editor_textarea")
        )
    }
}

@Composable
private fun QuickSyntaxBar(onInsert: (String) -> Unit) {
    val symbols = listOf("    ", "(", ")", "{", "}", "[", "]", "=", ":", ";", "\"", "'", "<", ">", "def ", "fun ", "class ", "return ")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E121A))
            .border(width = 0.5.dp, color = DarkCardBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(symbols) { sym ->
            Surface(
                onClick = { onInsert(sym) },
                shape = RoundedCornerShape(4.dp),
                color = DarkCard,
                modifier = Modifier.height(26.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sym == "    ") "TAB" else sym.trim(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentMint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
