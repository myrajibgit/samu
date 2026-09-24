package com.example.engine

import com.example.model.GgufMetadata
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GgufParser {
    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little endian ('G'=0x47, 'G'=0x47, 'U'=0x55, 'F'=0x46)

    fun parse(file: File): GgufMetadata {
        if (!file.exists() || file.length() < 16) {
            return GgufMetadata(
                isValidGguf = false,
                fileSizeBytes = if (file.exists()) file.length() else 0L,
                errorMessage = "File does not exist or is too small (< 16 bytes)"
            )
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(1024 * 64) // Read up to 64KB for header KV
                val bytesRead = raf.read(headerBytes)
                if (bytesRead < 16) {
                    return GgufMetadata(
                        isValidGguf = false,
                        fileSizeBytes = file.length(),
                        errorMessage = "Could not read sufficient header bytes"
                    )
                }

                val buffer = ByteBuffer.wrap(headerBytes, 0, bytesRead)
                buffer.order(ByteOrder.LITTLE_ENDIAN)

                val magic = buffer.int
                if (magic != GGUF_MAGIC) {
                    return GgufMetadata(
                        isValidGguf = false,
                        fileSizeBytes = file.length(),
                        errorMessage = "Invalid GGUF magic header. Expected 'GGUF', found: 0x${Integer.toHexString(magic)}"
                    )
                }

                val version = buffer.int
                val tensorCount = buffer.long
                val metadataKvCount = buffer.long

                val properties = mutableMapOf<String, String>()
                var architecture = "Unknown"
                var modelName = file.nameWithoutExtension
                var contextLength = 0L
                var embeddingLength = 0L
                var blockCount = 0L

                // Attempt to read some key-value pairs safely
                var kvParsed = 0
                while (kvParsed < metadataKvCount && buffer.remaining() > 16 && kvParsed < 100) {
                    try {
                        val keyLen = buffer.long
                        if (keyLen <= 0 || keyLen > 256 || buffer.remaining() < keyLen) break
                        val keyBytes = ByteArray(keyLen.toInt())
                        buffer.get(keyBytes)
                        val key = String(keyBytes, Charsets.UTF_8)

                        val valueType = buffer.int
                        val valueStr = parseGgufValue(buffer, valueType)

                        properties[key] = valueStr

                        when {
                            key == "general.architecture" -> architecture = valueStr
                            key == "general.name" -> modelName = valueStr
                            key.endsWith(".context_length") -> contextLength = valueStr.toLongOrNull() ?: 0L
                            key.endsWith(".embedding_length") -> embeddingLength = valueStr.toLongOrNull() ?: 0L
                            key.endsWith(".block_count") -> blockCount = valueStr.toLongOrNull() ?: 0L
                        }

                        kvParsed++
                    } catch (e: Exception) {
                        break
                    }
                }

                return GgufMetadata(
                    isValidGguf = true,
                    version = version,
                    tensorCount = tensorCount,
                    metadataKvCount = metadataKvCount,
                    architecture = architecture,
                    modelName = modelName,
                    contextLength = contextLength,
                    embeddingLength = embeddingLength,
                    blockCount = blockCount,
                    fileSizeBytes = file.length(),
                    rawProperties = properties
                )
            }
        } catch (e: Exception) {
            return GgufMetadata(
                isValidGguf = false,
                fileSizeBytes = file.length(),
                errorMessage = "Parser error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    private fun parseGgufValue(buffer: ByteBuffer, type: Int): String {
        return when (type) {
            0 -> buffer.get().toUByte().toString() // UINT8
            1 -> buffer.get().toString() // INT8
            2 -> buffer.short.toUShort().toString() // UINT16
            3 -> buffer.short.toString() // INT16
            4 -> buffer.int.toUInt().toString() // UINT32
            5 -> buffer.int.toString() // INT32
            6 -> buffer.float.toString() // FLOAT32
            7 -> (buffer.get() != 0.toByte()).toString() // BOOL
            8 -> { // STRING
                val strLen = buffer.long
                if (strLen in 1..4096 && buffer.remaining() >= strLen) {
                    val strBytes = ByteArray(strLen.toInt())
                    buffer.get(strBytes)
                    String(strBytes, Charsets.UTF_8)
                } else {
                    "<string len=$strLen>"
                }
            }
            9 -> { // ARRAY
                val itemType = buffer.int
                val arrayLen = buffer.long
                "<array of $arrayLen items>"
            }
            10 -> buffer.long.toULong().toString() // UINT64
            11 -> buffer.long.toString() // INT64
            12 -> buffer.double.toString() // FLOAT64
            else -> "<unknown type $type>"
        }
    }
}
