package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.DeviceHardwareStats
import com.example.model.DownloadStatus
import com.example.model.GgufMetadata
import com.example.model.ModelItem
import com.example.ui.components.GgufInspectorDialog
import com.example.ui.components.ModelCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentMint
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    allModels: List<ModelItem>,
    installedModels: List<ModelItem>,
    selectedModelId: String,
    hardwareStats: DeviceHardwareStats,
    inspectedMetadata: GgufMetadata?,
    inspectedModelName: String,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (ModelItem) -> Unit,
    onPauseDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (ModelItem) -> Unit,
    onInspectGguf: (ModelItem) -> Unit,
    onClearInspector: () -> Unit,
    onImportLocalFile: (Uri, String) -> Unit,
    onAddCustomUrl: (name: String, url: String, paramCount: String, quant: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFamilyFilter by remember { mutableStateOf("All") }
    var showUrlDialog by remember { mutableStateOf(false) }

    // File Picker for local GGUF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "imported_model.gguf"
            onImportLocalFile(it, fileName)
        }
    }

    // Inspector dialog
    if (inspectedMetadata != null) {
        GgufInspectorDialog(
            modelName = inspectedModelName,
            metadata = inspectedMetadata,
            onDismiss = onClearInspector
        )
    }

    // Custom URL Dialog
    if (showUrlDialog) {
        CustomUrlDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { name, url, params, quant ->
                onAddCustomUrl(name, url, params, quant)
                showUrlDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Model Hub & Library",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1-tap download quantized GGUF models",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Import options
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("import_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import local GGUF",
                            tint = AccentMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("import_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Add GGUF URL",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware summary pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Free Storage: %.1f GB", hardwareStats.freeStorageGb),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = AccentMint
                    )
                )
                Text(
                    text = String.format(Locale.US, "Avail RAM: %.1f GB", hardwareStats.availableRamGb),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan
                    )
                )
                Text(
                    text = "${hardwareStats.cpuCores} Cores",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = AccentMint,
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Discover & 1-Tap (${allModels.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) AccentMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_discover_models")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Installed (${installedModels.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) AccentMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    modifier = Modifier.testTag("tab_installed_models")
                )
            }
        }

        // Search & Filter area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model_search_field"),
                placeholder = {
                    Text("Search models by name, tag, or size…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Family Filter Chips
            val families = listOf("All", "SmolLM", "Qwen", "Llama", "DeepSeek", "TinyLlama", "Gemma")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(families) { family ->
                    val isSelected = selectedFamilyFilter.equals(family, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentMint.copy(alpha = 0.2f) else DarkSurface)
                            .border(1.dp, if (isSelected) AccentMint else DarkCardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedFamilyFilter = family }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = family,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Models List
        val baseList = if (selectedTab == 0) allModels else installedModels
        val filteredList = baseList.filter { model ->
            val matchesQuery = searchQuery.isBlank() ||
                model.name.contains(searchQuery, ignoreCase = true) ||
                model.family.contains(searchQuery, ignoreCase = true) ||
                model.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFamily = selectedFamilyFilter == "All" ||
                model.family.equals(selectedFamilyFilter, ignoreCase = true)
            matchesQuery && matchesFamily
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTab == 1) "No models installed yet.\nTap 'Discover & 1-Tap' to download a model!" else "No models match your search.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        isSelected = model.id == selectedModelId,
                        onSelect = { onSelectModel(model.id) },
                        onDownload = { onDownloadModel(model) },
                        onPause = { onPauseDownload(model.id) },
                        onCancel = { onCancelDownload(model.id) },
                        onDelete = { onDeleteModel(model) },
                        onInspect = { onInspectGguf(model) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, params: String, quant: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var params by remember { mutableStateOf("1B") }
    var quant by remember { mutableStateOf("Q4_K_M") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Add Custom GGUF Model",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Model Name") },
                placeholder = { Text("e.g. My Custom Llama") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("HuggingFace GGUF Download URL") },
                placeholder = { Text("https://huggingface.co/.../resolve/main/model.gguf") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = params,
                    onValueChange = { params = it },
                    label = { Text("Params") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quant,
                    onValueChange = { quant = it },
                    label = { Text("Quant") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onConfirm(name, url, params, quant)
                        }
                    },
                    enabled = url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint)
                ) {
                    Text("Add Model", color = Color(0xFF003824), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
