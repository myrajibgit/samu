package com.example.downloader

import android.content.Context
import com.example.data.local.dao.ModelDao
import com.example.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.Locale

class ModelDownloadManager(
    private val context: Context,
    private val modelDao: ModelDao
) {
    private val downloadScope = CoroutineScope(Dispatchers.IO)
    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val modelsDir: File by lazy {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun startDownload(
        modelId: String,
        url: String,
        expectedSizeBytes: Long,
        onProgress: ((progress: Float, speed: String) -> Unit)? = null,
        onComplete: ((file: File) -> Unit)? = null,
        onError: ((error: String) -> Unit)? = null
    ) {
        // Cancel existing job if any
        cancelDownload(modelId, deletePartial = false)

        val destinationFile = File(modelsDir, "$modelId.gguf")

        val job = downloadScope.launch {
            try {
                modelDao.updateDownloadProgress(
                    id = modelId,
                    status = DownloadStatus.DOWNLOADING.name,
                    progress = 0f,
                    speed = "Starting...",
                    downloadedBytes = destinationFile.length(),
                    localPath = null
                )

                val existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L

                val requestBuilder = Request.Builder().url(url)
                if (existingBytes > 0) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val call = okHttpClient.newCall(requestBuilder.build())
                activeCalls[modelId] = call

                val response = call.execute()

                if (!response.isSuccessful && response.code != 206) {
                    // If range request failed (416 Range Not Satisfiable), delete and restart
                    if (response.code == 416) {
                        destinationFile.delete()
                        val retryCall = okHttpClient.newCall(Request.Builder().url(url).build())
                        activeCalls[modelId] = retryCall
                        val retryResponse = retryCall.execute()
                        if (!retryResponse.isSuccessful) {
                            throw Exception("HTTP ${retryResponse.code}: ${retryResponse.message}")
                        }
                        processResponse(modelId, retryResponse, destinationFile, 0L, expectedSizeBytes, onProgress, onComplete)
                        return@launch
                    }
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                processResponse(modelId, response, destinationFile, existingBytes, expectedSizeBytes, onProgress, onComplete)

            } catch (e: CancellationException) {
                // User paused or canceled
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Download failed"
                modelDao.updateDownloadProgress(
                    id = modelId,
                    status = DownloadStatus.FAILED.name,
                    progress = 0f,
                    speed = "",
                    downloadedBytes = destinationFile.length(),
                    localPath = null
                )
                withContext(Dispatchers.Main) {
                    onError?.invoke(errorMsg)
                }
            } finally {
                activeCalls.remove(modelId)
                activeJobs.remove(modelId)
            }
        }

        activeJobs[modelId] = job
    }

    private suspend fun processResponse(
        modelId: String,
        response: okhttp3.Response,
        destinationFile: File,
        existingBytes: Long,
        expectedSizeBytes: Long,
        onProgress: ((progress: Float, speed: String) -> Unit)?,
        onComplete: ((file: File) -> Unit)?
    ) {
        val body = response.body ?: throw Exception("Empty response body from model repository")
        val contentLength = body.contentLength()
        val totalLength = if (contentLength > 0) existingBytes + contentLength else expectedSizeBytes

        val append = existingBytes > 0 && response.code == 206
        val outputStream = FileOutputStream(destinationFile, append)
        val inputStream = body.byteStream()

        val buffer = ByteArray(64 * 1024)
        var totalBytesRead = existingBytes
        var bytesSinceLastSpeedCheck = 0L
        var lastSpeedCheckTime = System.currentTimeMillis()
        var currentSpeedStr = "0 MB/s"
        var lastDbUpdateTime = 0L

        try {
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1 && coroutineContext.isActive) {
                outputStream.write(buffer, 0, read)
                totalBytesRead += read
                bytesSinceLastSpeedCheck += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedCheckTime
                if (elapsed >= 1000) {
                    val bytesPerSec = (bytesSinceLastSpeedCheck * 1000f) / elapsed
                    val mbPerSec = bytesPerSec / (1024 * 1024)
                    currentSpeedStr = if (mbPerSec >= 1.0f) {
                        String.format(Locale.US, "%.1f MB/s", mbPerSec)
                    } else {
                        val kbPerSec = bytesPerSec / 1024
                        String.format(Locale.US, "%.0f KB/s", kbPerSec)
                    }
                    bytesSinceLastSpeedCheck = 0
                    lastSpeedCheckTime = now

                    val progress = if (totalLength > 0) {
                        (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    withContext(Dispatchers.Main) {
                        onProgress?.invoke(progress, currentSpeedStr)
                    }
                }

                // Update database progress periodically (every 1.5 seconds)
                if (now - lastDbUpdateTime > 1500) {
                    lastDbUpdateTime = now
                    val progress = if (totalLength > 0) {
                        (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    modelDao.updateDownloadProgress(
                        id = modelId,
                        status = DownloadStatus.DOWNLOADING.name,
                        progress = progress,
                        speed = currentSpeedStr,
                        downloadedBytes = totalBytesRead,
                        localPath = null
                    )
                }
            }

            outputStream.flush()
        } finally {
            try { outputStream.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
        }

        // Successfully downloaded
        modelDao.updateDownloadProgress(
            id = modelId,
            status = DownloadStatus.COMPLETED.name,
            progress = 1.0f,
            speed = "Ready",
            downloadedBytes = destinationFile.length(),
            localPath = destinationFile.absolutePath
        )

        withContext(Dispatchers.Main) {
            onComplete?.invoke(destinationFile)
        }
    }

    fun pauseDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeJobs[modelId]?.cancel()
        activeCalls.remove(modelId)
        activeJobs.remove(modelId)

        downloadScope.launch {
            val file = File(modelsDir, "$modelId.gguf")
            modelDao.updateDownloadProgress(
                id = modelId,
                status = DownloadStatus.PAUSED.name,
                progress = 0f,
                speed = "Paused",
                downloadedBytes = if (file.exists()) file.length() else 0L,
                localPath = null
            )
        }
    }

    fun cancelDownload(modelId: String, deletePartial: Boolean = true) {
        activeCalls[modelId]?.cancel()
        activeJobs[modelId]?.cancel()
        activeCalls.remove(modelId)
        activeJobs.remove(modelId)

        val file = File(modelsDir, "$modelId.gguf")
        if (deletePartial && file.exists()) {
            file.delete()
        }

        downloadScope.launch {
            modelDao.updateDownloadProgress(
                id = modelId,
                status = DownloadStatus.NOT_DOWNLOADED.name,
                progress = 0f,
                speed = "",
                downloadedBytes = 0L,
                localPath = null
            )
        }
    }

    fun deleteDownloadedModel(modelId: String) {
        cancelDownload(modelId, deletePartial = true)
        val file = File(modelsDir, "$modelId.gguf")
        if (file.exists()) file.delete()
    }

    fun getModelFile(modelId: String): File {
        return File(modelsDir, "$modelId.gguf")
    }

    fun importCustomFile(sourceStream: java.io.InputStream, originalName: String): File {
        val safeName = originalName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val destinationFile = File(modelsDir, safeName)
        sourceStream.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        return destinationFile
    }
}
