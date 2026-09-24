package com.example.engine

object PromptFormatter {
    fun format(
        family: String,
        messages: List<Pair<String, String>>, // role, content
        systemPrompt: String
    ): String {
        return when (family.lowercase()) {
            "llama" -> formatLlama3(messages, systemPrompt)
            "qwen", "smollm" -> formatChatML(messages, systemPrompt)
            "deepseek" -> formatDeepSeek(messages, systemPrompt)
            "tinyllama" -> formatTinyLlama(messages, systemPrompt)
            else -> formatChatML(messages, systemPrompt)
        }
    }

    private fun formatLlama3(messages: List<Pair<String, String>>, systemPrompt: String): String {
        val sb = StringBuilder("<|begin_of_text|>")
        if (systemPrompt.isNotBlank()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>")
        }
        for ((role, content) in messages) {
            sb.append("<|start_header_id|>$role<|end_header_id|>\n\n$content<|eot_id|>")
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    private fun formatChatML(messages: List<Pair<String, String>>, systemPrompt: String): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        }
        for ((role, content) in messages) {
            sb.append("<|im_start|>$role\n$content<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun formatDeepSeek(messages: List<Pair<String, String>>, systemPrompt: String): String {
        val sb = StringBuilder("<｜begin of sentence｜>")
        if (systemPrompt.isNotBlank()) {
            sb.append("<｜System｜>$systemPrompt")
        }
        for ((role, content) in messages) {
            val tag = if (role == "user") "User" else "Assistant"
            sb.append("<｜$tag｜>$content")
        }
        sb.append("<｜Assistant｜><think>\n")
        return sb.toString()
    }

    private fun formatTinyLlama(messages: List<Pair<String, String>>, systemPrompt: String): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("<|system|>\n$systemPrompt</s>\n")
        }
        for ((role, content) in messages) {
            sb.append("<|$role|>\n$content</s>\n")
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }
}
