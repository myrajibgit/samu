package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.example.model.DeviceHardwareStats

object HardwareUtils {
    fun getHardwareStats(context: Context): DeviceHardwareStats {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        val statFs = StatFs(context.filesDir.absolutePath)
        val blockSize = statFs.blockSizeLong
        val availableBlocks = statFs.availableBlocksLong
        val totalBlocks = statFs.blockCountLong

        val freeStorage = availableBlocks * blockSize
        val totalStorage = totalBlocks * blockSize

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        return DeviceHardwareStats(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            freeStorageBytes = freeStorage,
            totalStorageBytes = totalStorage,
            cpuCores = cpuCores,
            deviceModel = deviceModel
        )
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024f
        val mb = kb / 1024f
        val gb = mb / 1024f
        return when {
            gb >= 1.0f -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0f -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0f -> String.format(java.util.Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
