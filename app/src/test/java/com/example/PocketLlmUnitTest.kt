package com.example

import com.example.data.local.CuratedModels
import com.example.engine.GgufParser
import com.example.engine.PromptFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PocketLlmUnitTest {

    @Test
    fun testCuratedModelsCatalog() {
        val models = CuratedModels.getDefaultModels()
        assertTrue("Curated models list should not be empty", models.isNotEmpty())

        val ids = models.map { it.id }
        assertTrue("SmolLM2 135M should be available for 1-tap download", ids.contains("smollm2-135m"))
        assertTrue("Llama 3.2 1B should be available for 1-tap download", ids.contains("llama-3.2-1b"))
        assertTrue("Qwen 2.5 0.5B should be available", ids.contains("qwen2.5-0.5b"))
        assertTrue("DeepSeek R1 should be available", ids.contains("deepseek-r1-1.5b"))

        for (model in models) {
            assertTrue("Model name should not be blank", model.name.isNotBlank())
            assertTrue("Download URL should point to Hugging Face GGUF", model.downloadUrl.startsWith("https://huggingface.co/"))
            assertTrue("Size should be positive", model.sizeBytes > 0)
            assertNotNull(model.quantization)
        }
    }

    @Test
    fun testPromptFormatterLlama3() {
        val messages = listOf(
            Pair("user", "Hello PocketLLM!")
        )
        val formatted = PromptFormatter.format(
            family = "Llama",
            messages = messages,
            systemPrompt = "You are a local assistant."
        )
        assertTrue(formatted.contains("<|begin_of_text|>"))
        assertTrue(formatted.contains("<|start_header_id|>system<|end_header_id|>"))
        assertTrue(formatted.contains("You are a local assistant."))
        assertTrue(formatted.contains("<|start_header_id|>user<|end_header_id|>"))
        assertTrue(formatted.contains("Hello PocketLLM!"))
        assertTrue(formatted.contains("<|start_header_id|>assistant<|end_header_id|>"))
    }

    @Test
    fun testPromptFormatterChatML() {
        val messages = listOf(
            Pair("user", "Tell me about mobile AI")
        )
        val formatted = PromptFormatter.format(
            family = "Qwen",
            messages = messages,
            systemPrompt = "Be concise."
        )
        assertTrue(formatted.contains("<|im_start|>system\nBe concise.<|im_end|>"))
        assertTrue(formatted.contains("<|im_start|>user\nTell me about mobile AI<|im_end|>"))
        assertTrue(formatted.contains("<|im_start|>assistant"))
    }

    @Test
    fun testPromptFormatterDeepSeek() {
        val messages = listOf(
            Pair("user", "2 + 2 = ?")
        )
        val formatted = PromptFormatter.format(
            family = "DeepSeek",
            messages = messages,
            systemPrompt = "Think step by step."
        )
        assertTrue(formatted.contains("<｜begin of sentence｜>"))
        assertTrue(formatted.contains("<｜System｜>Think step by step."))
        assertTrue(formatted.contains("<｜User｜>2 + 2 = ?"))
        assertTrue(formatted.contains("<｜Assistant｜><think>"))
    }

    @Test
    fun testTwoPlusTwoMath() {
        val engine = com.example.engine.LocalInferenceEngine()
        val result = engine.solveMathExpression("can you say what is 2+2")
        assertNotNull("Should parse and solve 2+2", result)
        assertTrue("Result should contain 4", result!!.second.contains("4"))

        val result2 = engine.solveMathExpression("calculate 15 * 8")
        assertNotNull(result2)
        assertTrue("Result should contain 120", result2!!.second.contains("120"))
    }

    @Test
    fun testGgufParserWithEmptyFile() {

        val tempFile = File.createTempFile("test_empty", ".gguf")
        try {
            val metadata = GgufParser.parse(tempFile)
            assertFalse("Empty file should not be valid GGUF", metadata.isValidGguf)
            assertNotNull(metadata.errorMessage)
        } finally {
            tempFile.delete()
        }
    }
}
