package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.dao.ModelDao
import com.example.data.local.entity.ModelEntity
import com.example.downloader.ModelDownloadManager
import com.example.engine.GgufParser
import com.example.model.DownloadStatus
import com.example.model.GgufMetadata
import com.example.model.ModelItem
import com.example.util.HardwareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ModelRepository(
    private val context: Context,
    private val modelDao: ModelDao,
    val downloadManager: ModelDownloadManager
) {

    fun getAllModelsFlow(): Flow<List<ModelItem>> {
        return modelDao.getAllModelsFlow().map { entities ->
            entities.map { it.toModelItem() }
        }
    }

    fun getInstalledModelsFlow(): Flow<List<ModelItem>> {
        return modelDao.getInstalledModelsFlow().map { entities ->
            entities.map { it.toModelItem() }
        }
    }

    suspend fun getModelById(id: String): ModelItem? = withContext(Dispatchers.IO) {
        modelDao.getModelById(id)?.toModelItem()
    }

    suspend fun setLoadedModel(modelId: String) = withContext(Dispatchers.IO) {
        modelDao.unloadAllModels()
        modelDao.updateLoadedInRam(modelId, true)
    }

    suspend fun unloadAllModels() = withContext(Dispatchers.IO) {
        modelDao.unloadAllModels()
    }

    fun startDownload(model: ModelItem) {
        downloadManager.startDownload(
            modelId = model.id,
            url = model.downloadUrl,
            expectedSizeBytes = model.sizeBytes
        )
    }

    fun pauseDownload(modelId: String) {
        downloadManager.pauseDownload(modelId)
    }

    fun cancelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
    }

    suspend fun deleteModel(model: ModelItem) = withContext(Dispatchers.IO) {
        downloadManager.deleteDownloadedModel(model.id)
        if (model.isCustomImport) {
            modelDao.deleteById(model.id)
        } else {
            modelDao.updateDownloadProgress(
                id = model.id,
                status = DownloadStatus.NOT_DOWNLOADED.name,
                progress = 0f,
                speed = "",
                downloadedBytes = 0L,
                localPath = null
            )
            modelDao.updateLoadedInRam(model.id, false)
        }
    }

    suspend fun importLocalFile(uri: Uri, displayName: String): Result<ModelItem> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream from system picker"))

            val importedFile = downloadManager.importCustomFile(inputStream, displayName)
            val metadata = GgufParser.parse(importedFile)

            val modelId = "custom-" + UUID.randomUUID().toString().take(8)
            val entity = ModelEntity(
                id = modelId,
                name = if (metadata.isValidGguf && metadata.modelName != "Unknown") metadata.modelName else displayName.substringBeforeLast("."),
                family = if (metadata.isValidGguf) metadata.architecture.replaceFirstChar { it.uppercase() } else "Custom",
                parameterCount = if (metadata.tensorCount > 0) "${metadata.tensorCount} tensors" else "Imported",
                quantization = "Custom GGUF",
                sizeBytes = importedFile.length(),
                sizeFormatted = HardwareUtils.formatBytes(importedFile.length()),
                downloadUrl = "",
                localFilePath = importedFile.absolutePath,
                downloadStatus = DownloadStatus.COMPLETED.name,
                downloadProgress = 1.0f,
                downloadSpeed = "Imported",
                downloadedBytes = importedFile.length(),
                isLoadedInRam = false,
                contextLength = if (metadata.contextLength > 0) metadata.contextLength.toInt() else 2048,
                description = "Locally imported GGUF weights (${metadata.architecture}, ${metadata.tensorCount} tensors).",
                recommendedRam = "Device RAM",
                tags = "Imported,GGUF,Custom",
                isCustomImport = true
            )

            modelDao.insertOrUpdate(entity)
            Result.success(entity.toModelItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomUrlModel(
        name: String,
        downloadUrl: String,
        parameterSize: String,
        quantization: String
    ): Result<ModelItem> = withContext(Dispatchers.IO) {
        try {
            val modelId = "url-" + UUID.randomUUID().toString().take(8)
            val entity = ModelEntity(
                id = modelId,
                name = name.ifBlank { "Custom GGUF Model" },
                family = "Custom",
                parameterCount = parameterSize.ifBlank { "1B" },
                quantization = quantization.ifBlank { "Q4_K_M" },
                sizeBytes = 800_000_000L,
                sizeFormatted = "~800 MB",
                downloadUrl = downloadUrl.trim(),
                localFilePath = null,
                downloadStatus = DownloadStatus.NOT_DOWNLOADED.name,
                downloadProgress = 0f,
                contextLength = 4096,
                description = "User added direct GGUF model download.",
                recommendedRam = "2 GB RAM",
                tags = "Custom,URL,GGUF",
                isCustomImport = true
            )
            modelDao.insertOrUpdate(entity)
            Result.success(entity.toModelItem())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun inspectGguf(model: ModelItem): GgufMetadata {
        val file = if (model.localFilePath != null) {
            File(model.localFilePath)
        } else {
            downloadManager.getModelFile(model.id)
        }
        return GgufParser.parse(file)
    }

    private fun ModelEntity.toModelItem(): ModelItem {
        val status = try {
            DownloadStatus.valueOf(downloadStatus)
        } catch (_: Exception) {
            DownloadStatus.NOT_DOWNLOADED
        }

        return ModelItem(
            id = id,
            name = name,
            family = family,
            parameterCount = parameterCount,
            quantization = quantization,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            downloadUrl = downloadUrl,
            localFilePath = localFilePath,
            downloadStatus = status,
            downloadProgress = downloadProgress,
            downloadSpeed = downloadSpeed,
            downloadedBytes = downloadedBytes,
            isLoadedInRam = isLoadedInRam,
            contextLength = contextLength,
            description = description,
            recommendedRam = recommendedRam,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
            isCustomImport = isCustomImport
        )
    }
}
