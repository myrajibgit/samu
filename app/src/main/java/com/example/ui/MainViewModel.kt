package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PocketLlmDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.downloader.ModelDownloadManager
import com.example.engine.GgufParser
import com.example.engine.LlamaCppEngine
import com.example.engine.LocalInferenceEngine
import com.example.engine.OllamaBridgeClient
import com.example.engine.OllamaModel
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DeviceHardwareStats
import com.example.model.DownloadStatus
import com.example.model.GgufMetadata
import com.example.model.MessageSender
import com.example.model.ModelItem
import com.example.util.CodeFileItem
import com.example.util.CodeWorkspaceManager
import com.example.util.HardwareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab {
    CHAT,
    CODE,
    MODELS,
    HISTORY,
    SETTINGS
}


data class SystemPromptPreset(
    val title: String,
    val description: String,
    val prompt: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PocketLlmDatabase.getDatabase(application)
    private val downloadManager = ModelDownloadManager(application, db.modelDao())
    val modelRepository = ModelRepository(application, db.modelDao(), downloadManager)
    val chatRepository = ChatRepository(db.conversationDao(), db.chatMessageDao())

    val apiKeyManager = com.example.util.ApiKeyManager(application)

    // Real on-device llama.cpp engine (runs downloaded GGUF files on the CPU, fully offline).
    private val llamaEngine = LlamaCppEngine(application.contentResolver)
    val engineLoadedModel: StateFlow<LlamaCppEngine.LoadedModel?> = llamaEngine.loadedModel
    val isEngineLoading: StateFlow<Boolean> = llamaEngine.isLoading
    val engineError: StateFlow<String?> = llamaEngine.lastError

    /** Dismisses the engine error banner once the user has read it. */
    fun dismissEngineError() {
        llamaEngine.clearError()
    }
    private val inferenceEngine = LocalInferenceEngine(llamaEngine)
    private val ollamaClient = OllamaBridgeClient()

    private val _isCloudAiActive = MutableStateFlow(apiKeyManager.isConfigured())
    val isCloudAiActive: StateFlow<Boolean> = _isCloudAiActive.asStateFlow()

    fun updateApiKey(key: String) {
        apiKeyManager.saveApiKey(key)
        _isCloudAiActive.value = apiKeyManager.isConfigured()
    }

    // Navigation & Tabs
    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Conversations
    val allConversations: StateFlow<List<Conversation>> = chatRepository.getAllConversationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Models
    val allModels: StateFlow<List<ModelItem>> = modelRepository.getAllModelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedModels: StateFlow<List<ModelItem>> = modelRepository.getInstalledModelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _modelSearchQuery = MutableStateFlow("")
    val modelSearchQuery: StateFlow<String> = _modelSearchQuery.asStateFlow()

    private val _selectedModelId = MutableStateFlow("smollm2-135m")
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _activeModel = MutableStateFlow<ModelItem?>(null)
    val activeModel: StateFlow<ModelItem?> = _activeModel.asStateFlow()

    // Chat Generation State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()

    private val _streamingThinking = MutableStateFlow("")
    val streamingThinking: StateFlow<String> = _streamingThinking.asStateFlow()

    private val _generationTps = MutableStateFlow(0f)
    val generationTps: StateFlow<Float> = _generationTps.asStateFlow()

    private val _generationTokensCount = MutableStateFlow(0)
    val generationTokensCount: StateFlow<Int> = _generationTokensCount.asStateFlow()

    private var generationJob: Job? = null

    // Ollama Hub Bridge State
    private val _ollamaHostUrl = MutableStateFlow("http://10.0.2.2:11434")
    val ollamaHostUrl: StateFlow<String> = _ollamaHostUrl.asStateFlow()

    private val _isOllamaConnected = MutableStateFlow(false)
    val isOllamaConnected: StateFlow<Boolean> = _isOllamaConnected.asStateFlow()

    private val _ollamaModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val ollamaModels: StateFlow<List<OllamaModel>> = _ollamaModels.asStateFlow()

    private val _isCheckingOllama = MutableStateFlow(false)
    val isCheckingOllama: StateFlow<Boolean> = _isCheckingOllama.asStateFlow()

    private val _ollamaStatusMessage = MutableStateFlow<String?>(null)
    val ollamaStatusMessage: StateFlow<String?> = _ollamaStatusMessage.asStateFlow()

    private val _isPullingOllamaModel = MutableStateFlow(false)
    val isPullingOllamaModel: StateFlow<Boolean> = _isPullingOllamaModel.asStateFlow()

    private val _ollamaPullProgress = MutableStateFlow(0f)
    val ollamaPullProgress: StateFlow<Float> = _ollamaPullProgress.asStateFlow()

    private val _ollamaPullStatusText = MutableStateFlow("")
    val ollamaPullStatusText: StateFlow<String> = _ollamaPullStatusText.asStateFlow()

    // Hardware & Inspector
    private val _hardwareStats = MutableStateFlow(HardwareUtils.getHardwareStats(application))
    val hardwareStats: StateFlow<DeviceHardwareStats> = _hardwareStats.asStateFlow()

    private val _inspectedMetadata = MutableStateFlow<GgufMetadata?>(null)
    val inspectedMetadata: StateFlow<GgufMetadata?> = _inspectedMetadata.asStateFlow()

    private val _inspectedModelName = MutableStateFlow("")
    val inspectedModelName: StateFlow<String> = _inspectedModelName.asStateFlow()

    // System Prompts Presets
    val promptPresets = listOf(
        SystemPromptPreset(
            title = "Helpful Assistant",
            description = "Balanced, clear, and friendly answers.",
            prompt = "You are a helpful, concise AI assistant running locally on mobile devices. You deliver accurate, structured answers."
        ),
        SystemPromptPreset(
            title = "Coding Specialist",
            description = "Specialized in Kotlin, Python, and clean architecture.",
            prompt = "You are an expert software engineer. Provide high-quality, production-ready code with concise explanations and best practices."
        ),
        SystemPromptPreset(
            title = "Concise & Fast",
            description = "Direct answers with zero fluff for quick mobile glances.",
            prompt = "Answer directly and concisely in 1-3 sentences or bullet points. No conversational filler."
        ),
        SystemPromptPreset(
            title = "Reasoning & Deep Thinker",
            description = "Step-by-step logic breakdown with chain of thought.",
            prompt = "Break down problems logically. Show step-by-step deductions and state clear conclusions."
        )
    )

    // Code Studio Workspace State
    val codeWorkspaceManager = CodeWorkspaceManager(application)
    private val _codeFiles = MutableStateFlow<List<CodeFileItem>>(emptyList())
    val codeFiles: StateFlow<List<CodeFileItem>> = _codeFiles.asStateFlow()

    private val _selectedCodeFile = MutableStateFlow<CodeFileItem?>(null)
    val selectedCodeFile: StateFlow<CodeFileItem?> = _selectedCodeFile.asStateFlow()

    private val _codeEditorContent = MutableStateFlow("")
    val codeEditorContent: StateFlow<String> = _codeEditorContent.asStateFlow()

    private val _isAiCoding = MutableStateFlow(false)
    val isAiCoding: StateFlow<Boolean> = _isAiCoding.asStateFlow()

    private val _aiCodeSuggestion = MutableStateFlow("")
    val aiCodeSuggestion: StateFlow<String> = _aiCodeSuggestion.asStateFlow()

    private val _terminalConsoleLog = MutableStateFlow("")
    val terminalConsoleLog: StateFlow<String> = _terminalConsoleLog.asStateFlow()

    init {
        refreshCodeFiles()
        // Observe conversations and pick initial active one
        viewModelScope.launch {
            allConversations.collectLatest { list ->
                if (list.isNotEmpty() && _activeConversationId.value == null) {
                    selectConversation(list.first().id)
                }
            }
        }

        // Keep active model updated
        viewModelScope.launch {
            combine(allModels, _selectedModelId) { models, id ->
                models.find { it.id == id } ?: models.firstOrNull()
            }.collectLatest { model ->
                _activeModel.value = model
            }
        }

        // Refresh hardware stats periodically
        refreshHardwareStats()
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setModelSearchQuery(query: String) {
        _modelSearchQuery.value = query
    }

    fun selectConversation(conversationId: Long) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            val conv = chatRepository.getConversationById(conversationId)
            _activeConversation.value = conv
            conv?.let {
                _selectedModelId.value = it.modelId
            }

            chatRepository.getMessagesFlow(conversationId).collectLatest { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewConversation(title: String = "New Chat") {
        viewModelScope.launch {
            val currentModel = _selectedModelId.value
            val currentSysPrompt = _activeConversation.value?.systemPrompt
                ?: "You are a helpful, concise AI assistant running locally on mobile devices."
            val newId = chatRepository.createConversation(
                title = title,
                modelId = currentModel,
                systemPrompt = currentSysPrompt
            )
            selectConversation(newId)
            _currentTab.value = AppTab.CHAT
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            val remaining = allConversations.value.filter { it.id != conversationId }
            if (remaining.isNotEmpty()) {
                selectConversation(remaining.first().id)
            } else {
                createNewConversation()
            }
        }
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
        val currentConv = _activeConversation.value
        if (currentConv != null) {
            val updated = currentConv.copy(modelId = modelId)
            _activeConversation.value = updated
            viewModelScope.launch {
                chatRepository.updateConversation(updated)
            }
        }
    }

    fun updateConversationSettings(systemPrompt: String, temperature: Float, topP: Float) {
        val currentConv = _activeConversation.value ?: return
        val updated = currentConv.copy(
            systemPrompt = systemPrompt,
            temperature = temperature,
            topP = topP
        )
        _activeConversation.value = updated
        viewModelScope.launch {
            chatRepository.updateConversation(updated)
        }
    }

    // 1-Tap Download Actions
    fun downloadModel(model: ModelItem) {
        modelRepository.startDownload(model)
        refreshHardwareStats()
    }

    fun pauseModelDownload(modelId: String) {
        modelRepository.pauseDownload(modelId)
    }

    fun cancelModelDownload(modelId: String) {
        modelRepository.cancelDownload(modelId)
        refreshHardwareStats()
    }

    fun deleteModel(model: ModelItem) {
        viewModelScope.launch {
            modelRepository.deleteModel(model)
            refreshHardwareStats()
        }
    }

    fun loadModelInRam(modelId: String) {
        viewModelScope.launch {
            val model = modelRepository.getModelById(modelId) ?: return@launch
            modelRepository.setLoadedModel(modelId)
            _selectedModelId.value = modelId
            llamaEngine.loadIntoRam(model)
        }
    }

    /** Frees the llama.cpp context + weights from RAM. */
    fun unloadEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            llamaEngine.release()
            modelRepository.unloadAllModels()
        }
    }

    fun inspectGgufFile(model: ModelItem) {
        _inspectedModelName.value = model.name
        _inspectedMetadata.value = modelRepository.inspectGguf(model)
    }

    fun clearInspector() {
        _inspectedMetadata.value = null
        _inspectedModelName.value = ""
    }

    fun importLocalFile(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val result = modelRepository.importLocalFile(uri, displayName)
            result.onSuccess { item ->
                _selectedModelId.value = item.id
                refreshHardwareStats()
            }
        }
    }

    fun addCustomUrlModel(name: String, url: String, paramCount: String, quant: String) {
        viewModelScope.launch {
            modelRepository.addCustomUrlModel(name, url, paramCount, quant)
        }
    }

    // Chat Message Send & Execution
    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return

        val convId = _activeConversationId.value ?: return
        val conv = _activeConversation.value ?: return
        val model = _activeModel.value ?: allModels.value.firstOrNull() ?: return

        viewModelScope.launch {
            // Save user message
            val userMsg = ChatMessage(
                conversationId = convId,
                sender = MessageSender.USER,
                content = userText.trim(),
                timestamp = System.currentTimeMillis()
            )
            chatRepository.saveMessage(userMsg)

            // Update conversation title if first message
            if (messages.value.isEmpty() || conv.title == "New Chat") {
                val newTitle = userText.take(28).trim().replace("\n", " ") + (if (userText.length > 28) "…" else "")
                val updatedConv = conv.copy(title = newTitle)
                _activeConversation.value = updatedConv
                chatRepository.updateConversation(updatedConv)
            }

            // Start generation
            startStreamingInference(conv, model, userText)
        }
    }

    private fun startStreamingInference(
        conversation: Conversation,
        model: ModelItem,
        latestUserText: String
    ) {
        generationJob?.cancel()

        _isGenerating.value = true
        _streamingResponse.value = ""
        _streamingThinking.value = ""
        _generationTps.value = 0f
        _generationTokensCount.value = 0

        generationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val history = messages.value.map {
                val role = if (it.sender == MessageSender.USER) "user" else "assistant"
                Pair(role, it.content)
            } + listOf(Pair("user", latestUserText))

            try {
                // If using Ollama remote model or Ollama host mode
                if (model.isOllamaRemote && _isOllamaConnected.value) {
                    executeOllamaInference(conversation, model, history, startTime)
                } else {
                    executeLocalInference(conversation, model, history, startTime)
                }
            } catch (e: Exception) {
                // Cancellation (stop button) keeps whatever streamed so far; a real failure
                // with nothing generated must not look like a silent no-op.
                if (e !is kotlinx.coroutines.CancellationException &&
                    _streamingResponse.value.isBlank() &&
                    _streamingThinking.value.isBlank()
                ) {
                    _streamingResponse.value =
                        "❌ **Generation failed.** ${e.message ?: "The native engine stopped unexpectedly."}"
                }
                finalizeMessage(conversation.id, model.name, startTime)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun executeLocalInference(
        conversation: Conversation,
        model: ModelItem,
        history: List<Pair<String, String>>,
        startTime: Long
    ) {
        val modelFile = if (model.localFilePath != null) File(model.localFilePath) else null

        inferenceEngine.generateStream(
            model = model,
            modelFile = modelFile,
            messages = history,
            systemPrompt = conversation.systemPrompt,
            temperature = conversation.temperature,
            topP = conversation.topP
        ).collectLatest { chunk ->
            if (chunk.isThinking) {
                _streamingThinking.value += chunk.token
            } else {
                _streamingResponse.value += chunk.token
            }
            _generationTps.value = chunk.tokensPerSecond
            _generationTokensCount.value = chunk.tokensGenerated

            if (chunk.isDone) {
                finalizeMessage(conversation.id, model.name, startTime)
            }
        }
    }

    private suspend fun executeOllamaInference(
        conversation: Conversation,
        model: ModelItem,
        history: List<Pair<String, String>>,
        startTime: Long
    ) {
        var tokenCount = 0
        var lastTps = 0f
        ollamaClient.chatStream(
            baseUrl = _ollamaHostUrl.value,
            modelName = model.name,
            messages = history,
            systemPrompt = conversation.systemPrompt,
            temperature = conversation.temperature,
            topP = conversation.topP
        ).collectLatest { chunk ->
            _streamingResponse.value += chunk.text
            if (chunk.tokensGenerated > 0) {
                tokenCount = chunk.tokensGenerated
                lastTps = chunk.tokensPerSecond
                _generationTokensCount.value = tokenCount
                _generationTps.value = lastTps
            }
            if (chunk.isDone) {
                finalizeMessage(conversation.id, "Ollama (${model.name})", startTime, tokenCount, lastTps)
            }
        }
    }

    private suspend fun finalizeMessage(
        conversationId: Long,
        modelName: String,
        startTime: Long,
        tokensCount: Int? = null,
        tps: Float? = null
    ) {
        val finalAnswer = _streamingResponse.value.trim()
        val finalThinking = _streamingThinking.value.trim().ifEmpty { null }
        val duration = System.currentTimeMillis() - startTime
        val totalTokens = tokensCount ?: _generationTokensCount.value
        val speed = tps ?: _generationTps.value

        if (finalAnswer.isNotEmpty() || finalThinking != null) {
            val assistantMsg = ChatMessage(
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                content = finalAnswer.ifEmpty { "Model generation complete." },
                thinkingContent = finalThinking,
                timestamp = System.currentTimeMillis(),
                tokensPerSecond = if (speed > 0f) speed else null,
                totalTokens = if (totalTokens > 0) totalTokens else null,
                generationTimeMs = duration,
                modelUsed = modelName
            )
            chatRepository.saveMessage(assistantMsg)
        }

        _streamingResponse.value = ""
        _streamingThinking.value = ""
        _isGenerating.value = false
    }

    fun stopGeneration() {
        val convId = _activeConversationId.value
        val model = _activeModel.value
        if (convId != null && model != null && _isGenerating.value) {
            generationJob?.cancel()
            llamaEngine.stopGeneration()
            viewModelScope.launch {
                finalizeMessage(convId, model.name, System.currentTimeMillis() - 1000)
            }
        }
    }

    // Ollama Hub Bridge Methods
    fun setOllamaHostUrl(url: String) {
        _ollamaHostUrl.value = url
    }

    fun testOllamaConnection() {
        viewModelScope.launch {
            _isCheckingOllama.value = true
            _ollamaStatusMessage.value = "Connecting to ${_ollamaHostUrl.value}..."
            val result = ollamaClient.checkConnection(_ollamaHostUrl.value)
            _isCheckingOllama.value = false
            result.onSuccess { list ->
                _isOllamaConnected.value = true
                _ollamaModels.value = list
                _ollamaStatusMessage.value = "Connected! Found ${list.size} remote model(s)."
            }.onFailure { error ->
                _isOllamaConnected.value = false
                _ollamaStatusMessage.value = "Connection failed: ${error.localizedMessage ?: "Unreachable host"}"
            }
        }
    }

    fun pullOllamaModel(modelName: String) {
        if (modelName.isBlank()) return
        viewModelScope.launch {
            _isPullingOllamaModel.value = true
            _ollamaPullProgress.value = 0f
            _ollamaPullStatusText.value = "Initiating pull for $modelName..."
            try {
                ollamaClient.pullModel(_ollamaHostUrl.value, modelName.trim())
                    .collectLatest { p ->
                        _ollamaPullStatusText.value = p.status
                        _ollamaPullProgress.value = p.progress
                    }
                _ollamaPullStatusText.value = "Pull completed! Refreshing models..."
                testOllamaConnection()
            } catch (e: Exception) {
                _ollamaPullStatusText.value = "Pull failed: ${e.localizedMessage}"
            } finally {
                _isPullingOllamaModel.value = false
            }
        }
    }

    fun refreshHardwareStats() {
        _hardwareStats.value = HardwareUtils.getHardwareStats(getApplication())
    }

    // Code Studio Workspace Methods
    fun refreshCodeFiles() {
        val files = codeWorkspaceManager.listFiles()
        _codeFiles.value = files
        if (_selectedCodeFile.value == null && files.isNotEmpty()) {
            selectCodeFile(files.first())
        }
    }

    fun selectCodeFile(file: CodeFileItem) {
        _selectedCodeFile.value = file
        _codeEditorContent.value = file.content
        _aiCodeSuggestion.value = ""
    }

    fun updateEditorContent(content: String) {
        _codeEditorContent.value = content
    }

    fun saveCurrentFile() {
        val file = _selectedCodeFile.value ?: return
        val saved = codeWorkspaceManager.saveFile(file.name, _codeEditorContent.value)
        _selectedCodeFile.value = saved
        refreshCodeFiles()
    }

    fun createNewFile(name: String, content: String = "") {
        val cleanName = if (name.contains(".")) name else "$name.py"
        val created = codeWorkspaceManager.saveFile(cleanName, content)
        refreshCodeFiles()
        selectCodeFile(created)
    }

    fun deleteCurrentFile() {
        val file = _selectedCodeFile.value ?: return
        codeWorkspaceManager.deleteFile(file.name)
        refreshCodeFiles()
        _selectedCodeFile.value = _codeFiles.value.firstOrNull()
        _codeEditorContent.value = _selectedCodeFile.value?.content ?: ""
    }

    fun applyAiSuggestion() {
        val suggestion = _aiCodeSuggestion.value
        if (suggestion.isNotBlank()) {
            // Extract code if wrapped in markdown code blocks
            val codeBlockRegex = "```(?:[a-zA-Z0-9_-]+)?\\s*([\\s\\S]*?)```".toRegex()
            val match = codeBlockRegex.find(suggestion)
            val extracted = if (match != null) match.groupValues[1].trim() else suggestion.trim()
            _codeEditorContent.value = extracted
            saveCurrentFile()
            _aiCodeSuggestion.value = ""
        }
    }

    fun runCurrentCode() {
        val file = _selectedCodeFile.value ?: return
        saveCurrentFile()
        _terminalConsoleLog.value = if (file.language == "html") {
            "No in-app browser engine on Android — copy this HTML into a browser to render it.\n" +
                "Static check for ${file.name}: ${describeWorkspaceFile(file)}"
        } else {
            // Honest static review: Android has no Python/Ruby interpreter and we will not
            // pretend to have executed the file.
            "Static review of ${file.name} (NOT executed — no runtime is bundled):\n" +
                staticReview(file.name, _codeEditorContent.value)
        }
    }

    fun shareCurrentFile() {
        val file = _selectedCodeFile.value ?: return
        saveCurrentFile()
        codeWorkspaceManager.shareFile(file.name)
    }

    fun askClaudeCode(instruction: String) {
        val file = _selectedCodeFile.value ?: return
        val currentCode = _codeEditorContent.value
        viewModelScope.launch {
            _isAiCoding.value = true
            _aiCodeSuggestion.value = ""

            val prompt = """
You are Claude Code, an elite software engineer and code architect.
User Request: $instruction
Current File: ${file.name} (Language: ${file.language})
Current Code:
```${file.language}
$currentCode
```

Instructions:
1. Provide the complete, working, production-ready code.
2. Put the code inside a standard markdown code block: ```${file.language} ... ```
3. Ensure no placeholders like '// TODO' or 'rest of code unchanged' are used. Write the entire complete code so it can be directly applied to the editor.
""".trimIndent()

            val model = _activeModel.value ?: allModels.value.firstOrNull()
            if (model != null) {
                val modelFile = model.localFilePath?.let { File(it) }
                val messages = listOf("user" to prompt)
                val sb = StringBuilder()
                inferenceEngine.generateStream(
                    model = model,
                    modelFile = modelFile,
                    messages = messages,
                    systemPrompt = "You are Claude Code, an autonomous programming agent. You write impeccable, working, clean code.",
                    temperature = 0.2f,
                    topP = 0.9f
                ).collect { chunk ->
                    sb.append(chunk.token)
                    _aiCodeSuggestion.value = sb.toString()
                }
            } else {
                _aiCodeSuggestion.value = "// Please select or download a model first in the Models tab."
            }
            _isAiCoding.value = false
        }
    }

    override fun onCleared() {
        llamaEngine.shutdown()
        super.onCleared()
    }

    private fun describeWorkspaceFile(file: CodeFileItem): String {
        val lines = file.content.lines()
        return "${lines.size} lines, ${file.content.length} chars."
    }

    /**
     * A deliberately modest static report instead of a fabricated interpreter run.
     * It reports facts it can actually verify (size, imports, TODO markers) and never
     * claims the code ran.
     */
    private fun staticReview(name: String, code: String): String {
        val sb = StringBuilder()
        val lines = code.lines()
        sb.append("• Lines: ${lines.size}\n")

        val imports = lines.map { it.trim() }
            .filter { it.startsWith("import ") || it.startsWith("from ") || it.startsWith("#include") }
        sb.append("• Imports/includes: ${imports.size}\n")

        val unfinished = lines.filter {
            val t = it.trim()
            t.startsWith("TODO") || t.contains("TODO:") || t.contains("FIXME") || t.contains("pass #")
        }
        if (unfinished.isNotEmpty()) {
            sb.append("• Unfinished markers: ${unfinished.size} (TODO/FIXME)\n")
        }

        val prints = lines.count { it.trim().startsWith("print(") || it.trim().startsWith("println(") }
        if (prints > 0) {
            sb.append("• print/println calls (would print when run elsewhere): $prints\n")
        }

        val brackets = code.count { it == '{' } - code.count { it == '}' }
        val parens = code.count { it == '(' } - code.count { it == ')' }
        sb.append(if (brackets == 0 && parens == 0) "• Brackets: balanced" else "• Brackets unbalanced ({}: $brackets, (): $parens)")
        sb.append("\n\nTo actually execute $name, open it in a desktop toolchain — PocketLLM has no Python/Kotlin runtime.")
        return sb.toString()
    }
}
