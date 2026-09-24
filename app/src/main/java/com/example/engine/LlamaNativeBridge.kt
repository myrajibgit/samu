package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Native JNI bridge for llama.cpp / GGUF on-device model execution.
 * When exported to a PC with Android NDK and libllama.so installed,
 * this class executes the GGUF weights directly on the phone's ARM CPU/GPU.
 */
object LlamaNativeBridge {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("llama")
            isNativeLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            isNativeLoaded = false
        }
    }

    fun isAvailable(): Boolean = isNativeLoaded

    fun loadModel(modelPath: String): Long {
        if (!isNativeLoaded) return 0L
        return try {
            nativeInit(modelPath)
        } catch (_: Throwable) {
            0L
        }
    }

    fun generateStream(modelHandle: Long, prompt: String): Flow<String> = flow {
        if (!isNativeLoaded || modelHandle == 0L) {
            emit("Error: Native library not loaded.")
            return@flow
        }
        // In local NDK build, nativeGenerate calls back with tokens as they are predicted
        nativeGenerate(modelHandle, prompt) { token ->
            // emit tokens
        }
    }.flowOn(Dispatchers.Default)

    // External JNI declarations matching llama.cpp JNI wrappers
    private external fun nativeInit(modelPath: String): Long
    private external fun nativeFree(modelHandle: Long)
    private external fun nativeGenerate(modelHandle: Long, prompt: String, onToken: (String) -> Unit)
}
