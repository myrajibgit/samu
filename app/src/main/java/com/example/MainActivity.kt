package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DownloadStatus
import com.example.model.ModelItem
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodeStudioScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.SettingsScreen

import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val allModels by viewModel.allModels.collectAsStateWithLifecycle()
    val installedModels by viewModel.installedModels.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val hardwareStats by viewModel.hardwareStats.collectAsStateWithLifecycle()

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val streamingResponse by viewModel.streamingResponse.collectAsStateWithLifecycle()
    val streamingThinking by viewModel.streamingThinking.collectAsStateWithLifecycle()
    val generationTps by viewModel.generationTps.collectAsStateWithLifecycle()
    val generationTokensCount by viewModel.generationTokensCount.collectAsStateWithLifecycle()

    val isEngineLoading by viewModel.isEngineLoading.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()

    val inspectedMetadata by viewModel.inspectedMetadata.collectAsStateWithLifecycle()
    val inspectedModelName by viewModel.inspectedModelName.collectAsStateWithLifecycle()

    val ollamaHostUrl by viewModel.ollamaHostUrl.collectAsStateWithLifecycle()
    val isOllamaConnected by viewModel.isOllamaConnected.collectAsStateWithLifecycle()
    val isCheckingOllama by viewModel.isCheckingOllama.collectAsStateWithLifecycle()
    val ollamaStatusMessage by viewModel.ollamaStatusMessage.collectAsStateWithLifecycle()
    val ollamaModels by viewModel.ollamaModels.collectAsStateWithLifecycle()
    val isPullingOllamaModel by viewModel.isPullingOllamaModel.collectAsStateWithLifecycle()
    val ollamaPullProgress by viewModel.ollamaPullProgress.collectAsStateWithLifecycle()
    val ollamaPullStatusText by viewModel.ollamaPullStatusText.collectAsStateWithLifecycle()

    val codeFiles by viewModel.codeFiles.collectAsStateWithLifecycle()
    val selectedCodeFile by viewModel.selectedCodeFile.collectAsStateWithLifecycle()
    val codeEditorContent by viewModel.codeEditorContent.collectAsStateWithLifecycle()
    val isAiCoding by viewModel.isAiCoding.collectAsStateWithLifecycle()
    val aiCodeSuggestion by viewModel.aiCodeSuggestion.collectAsStateWithLifecycle()
    val terminalConsoleLog by viewModel.terminalConsoleLog.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .border(width = 1.dp, color = DarkCardBorder)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.CHAT,
                    onClick = { viewModel.setTab(AppTab.CHAT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Chat",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Chat", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003824),
                        selectedTextColor = AccentMint,
                        indicatorColor = AccentMint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.CODE,
                    onClick = { viewModel.setTab(AppTab.CODE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Code", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003824),
                        selectedTextColor = AccentMint,
                        indicatorColor = AccentMint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_code")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.MODELS,
                    onClick = { viewModel.setTab(AppTab.MODELS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Models",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Models", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003824),
                        selectedTextColor = AccentMint,
                        indicatorColor = AccentMint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_models")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { viewModel.setTab(AppTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("History", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003824),
                        selectedTextColor = AccentMint,
                        indicatorColor = AccentMint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )


                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003824),
                        selectedTextColor = AccentMint,
                        indicatorColor = AccentMint,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.CHAT -> {
                    ChatScreen(
                        conversation = activeConversation,
                        activeModel = activeModel,
                        messages = messages,
                        hardwareStats = hardwareStats,
                        isGenerating = isGenerating,
                        streamingResponse = streamingResponse,
                        streamingThinking = streamingThinking,
                        generationTps = generationTps,
                        generationTokensCount = generationTokensCount,
                        promptPresets = viewModel.promptPresets,
                        isEngineLoading = isEngineLoading,
                        engineError = engineError,
                        onDismissEngineError = { viewModel.dismissEngineError() },
                        onSendMessage = { text -> viewModel.sendMessage(text) },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onNewChat = { viewModel.createNewConversation() },
                        onOpenModelSelector = { viewModel.setTab(AppTab.MODELS) },
                        onUpdateSettings = { p, t, tp -> viewModel.updateConversationSettings(p, t, tp) }
                    )
                }

                AppTab.CODE -> {
                    CodeStudioScreen(
                        codeFiles = codeFiles,
                        selectedFile = selectedCodeFile,
                        editorContent = codeEditorContent,
                        isAiCoding = isAiCoding,
                        aiSuggestion = aiCodeSuggestion,
                        terminalLog = terminalConsoleLog,
                        onSelectFile = { file -> viewModel.selectCodeFile(file) },
                        onUpdateContent = { text -> viewModel.updateEditorContent(text) },
                        onSaveFile = { viewModel.saveCurrentFile() },
                        onCreateNewFile = { name -> viewModel.createNewFile(name) },
                        onDeleteCurrentFile = { viewModel.deleteCurrentFile() },
                        onRunCode = { viewModel.runCurrentCode() },
                        onShareFile = { viewModel.shareCurrentFile() },
                        onAskClaudeCode = { prompt -> viewModel.askClaudeCode(prompt) },
                        onApplyAiSuggestion = { viewModel.applyAiSuggestion() }
                    )
                }

                AppTab.MODELS -> {
                    ModelsScreen(
                        allModels = allModels,
                        installedModels = installedModels,
                        selectedModelId = selectedModelId,
                        hardwareStats = hardwareStats,
                        inspectedMetadata = inspectedMetadata,
                        inspectedModelName = inspectedModelName,
                        onSelectModel = { id ->
                            viewModel.selectModel(id)
                            viewModel.setTab(AppTab.CHAT)
                        },
                        onDownloadModel = { model -> viewModel.downloadModel(model) },
                        onPauseDownload = { id -> viewModel.pauseModelDownload(id) },
                        onCancelDownload = { id -> viewModel.cancelModelDownload(id) },
                        onDeleteModel = { model -> viewModel.deleteModel(model) },
                        onInspectGguf = { model -> viewModel.inspectGgufFile(model) },
                        onClearInspector = { viewModel.clearInspector() },
                        onImportLocalFile = { uri, name -> viewModel.importLocalFile(uri, name) },
                        onAddCustomUrl = { n, u, p, q -> viewModel.addCustomUrlModel(n, u, p, q) }
                    )
                }

                AppTab.HISTORY -> {
                    val allConversations by viewModel.allConversations.collectAsStateWithLifecycle()
                    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
                    HistoryScreen(
                        conversations = allConversations,
                        activeConversationId = activeConvId,
                        onSelectConversation = { id ->
                            viewModel.selectConversation(id)
                            viewModel.setTab(AppTab.CHAT)
                        },
                        onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                        onNewChat = {
                            viewModel.createNewConversation()
                            viewModel.setTab(AppTab.CHAT)
                        }
                    )
                }


                AppTab.SETTINGS -> {
                    val isCloudActive by viewModel.isCloudAiActive.collectAsStateWithLifecycle()
                    SettingsScreen(
                        hardwareStats = hardwareStats,
                        isCloudAiActive = isCloudActive,
                        currentApiKey = viewModel.apiKeyManager.getApiKey(),
                        onSaveApiKey = { viewModel.updateApiKey(it) },
                        onRefreshHardware = { viewModel.refreshHardwareStats() }
                    )
                }
            }
        }
    }
}
