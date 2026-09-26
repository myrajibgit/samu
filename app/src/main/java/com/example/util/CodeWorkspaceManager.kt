package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

data class CodeFileItem(
    val id: String,
    val name: String,
    val content: String,
    val language: String,
    val lastModified: Long = System.currentTimeMillis()
)

class CodeWorkspaceManager(private val context: Context) {
    private val workspaceDir: File = File(context.filesDir, "code_workspace").apply {
        if (!exists()) mkdirs()
    }

    init {
        initializeDefaultFilesIfEmpty()
    }

    private fun initializeDefaultFilesIfEmpty() {
        val existingFiles = workspaceDir.listFiles()
        if (existingFiles == null || existingFiles.isEmpty()) {
            // Default HTML5 playable game
            val defaultHtml = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>Neon Bounce</title>
<style>
  body { margin: 0; background: #0c0f17; color: #00ffaa; font-family: monospace; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; overflow: hidden; }
  h2 { margin: 0 0 10px 0; color: #00ffaa; text-shadow: 0 0 10px #00ffaa; }
  canvas { background: #131926; border: 2px solid #00ffaa; border-radius: 8px; box-shadow: 0 0 20px rgba(0,255,170,0.3); touch-action: none; }
  #score { font-size: 18px; margin-bottom: 8px; }
  .instructions { font-size: 12px; color: #8899aa; margin-top: 8px; }
</style>
</head>
<body>
  <h2>NEON BOUNCE</h2>
  <div id="score">SCORE: 0</div>
  <canvas id="gameCanvas" width="320" height="420"></canvas>
  <div class="instructions">Drag finger/touch left & right to move paddle!</div>

<script>
  const canvas = document.getElementById("gameCanvas");
  const ctx = canvas.getContext("2d");
  const scoreEl = document.getElementById("score");

  let score = 0;
  let paddleWidth = 75, paddleHeight = 10;
  let paddleX = (canvas.width - paddleWidth) / 2;
  let ballX = canvas.width / 2, ballY = canvas.height - 40;
  let dx = 3, dy = -3;
  let ballRadius = 6;

  function movePaddle(clientX) {
    const rect = canvas.getBoundingClientRect();
    const relativeX = clientX - rect.left;
    if (relativeX > 0 && relativeX < canvas.width) {
      paddleX = Math.max(0, Math.min(canvas.width - paddleWidth, relativeX - paddleWidth / 2));
    }
  }

  canvas.addEventListener("touchmove", (e) => {
    e.preventDefault();
    movePaddle(e.touches[0].clientX);
  }, { passive: false });

  canvas.addEventListener("mousemove", (e) => {
    movePaddle(e.clientX);
  });

  function draw() {
    ctx.fillStyle = "#131926";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Ball
    ctx.beginPath();
    ctx.arc(ballX, ballY, ballRadius, 0, Math.PI * 2);
    ctx.fillStyle = "#ff0077";
    ctx.shadowBlur = 10;
    ctx.shadowColor = "#ff0077";
    ctx.fill();
    ctx.closePath();

    // Paddle
    ctx.beginPath();
    ctx.rect(paddleX, canvas.height - paddleHeight - 8, paddleWidth, paddleHeight);
    ctx.fillStyle = "#00ffaa";
    ctx.shadowBlur = 12;
    ctx.shadowColor = "#00ffaa";
    ctx.fill();
    ctx.closePath();

    // Wall bounce
    if (ballX + dx > canvas.width - ballRadius || ballX + dx < ballRadius) {
      dx = -dx;
    }
    if (ballY + dy < ballRadius) {
      dy = -dy;
    } else if (ballY + dy > canvas.height - paddleHeight - 12) {
      if (ballX > paddleX && ballX < paddleX + paddleWidth) {
        dy = -dy;
        score += 10;
        scoreEl.innerText = "SCORE: " + score;
        dx += (dx > 0 ? 0.2 : -0.2);
        dy += (dy > 0 ? 0.2 : -0.2);
      } else if (ballY + dy > canvas.height - ballRadius) {
        // Reset
        score = 0;
        scoreEl.innerText = "SCORE: 0 (Tap to retry)";
        ballX = canvas.width / 2;
        ballY = canvas.height - 40;
        dx = 3;
        dy = -3;
      }
    }

    ballX += dx;
    ballY += dy;
    requestAnimationFrame(draw);
  }

  draw();
</script>
</body>
</html>
""".trimIndent()
            saveFile("game.html", defaultHtml)

            val defaultPython = """# Python Algorithm: Fibonacci & Prime Matrix Generator
import math

def is_prime(n):
    if n <= 1: return False
    for i in range(2, int(math.isqrt(n)) + 1):
        if n % i == 0: return False
    return True

def generate_fibonacci(count):
    fib = [0, 1]
    while len(fib) < count:
        fib.append(fib[-1] + fib[-2])
    return fib

print("--- llm-offline Python Execution Engine ---")
count = 10
fibs = generate_fibonacci(count)
print(f"First {count} Fibonacci numbers: {fibs}")

primes = [x for x in fibs if is_prime(x)]
print(f"Fibonacci primes found: {primes}")
print("Execution completed with returncode 0.")
""".trimIndent()
            saveFile("main.py", defaultPython)

            val defaultKotlin = """// llm-offline Mobile Kotlin Solution
package com.example.solution

fun main() {
    println("llm-offline Claude Code Engine ready.")
    val items = listOf("SmolLM2", "Llama 3.2", "Qwen 2.5", "DeepSeek R1")
    val stats = items.map { it.uppercase() }
    println("Supported models: " + stats.joinToString(", "))
}
""".trimIndent()
            saveFile("solution.kt", defaultKotlin)
        }
    }

    fun listFiles(): List<CodeFileItem> {
        val files = workspaceDir.listFiles() ?: return emptyList()
        return files.map { file ->
            val lang = detectLanguage(file.name)
            CodeFileItem(
                id = file.name,
                name = file.name,
                content = file.readText(),
                language = lang,
                lastModified = file.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun saveFile(name: String, content: String): CodeFileItem {
        val target = File(workspaceDir, name)
        target.writeText(content)
        val lang = detectLanguage(name)
        return CodeFileItem(
            id = name,
            name = name,
            content = content,
            language = lang,
            lastModified = target.lastModified()
        )
    }

    fun deleteFile(name: String): Boolean {
        val target = File(workspaceDir, name)
        return if (target.exists()) target.delete() else false
    }

    fun shareFile(fileName: String) {
        val target = File(workspaceDir, fileName)
        if (!target.exists()) return

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(Intent.EXTRA_TEXT, target.readText())
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Share $fileName").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    private fun detectLanguage(name: String): String {
        return when {
            name.endsWith(".html", ignoreCase = true) || name.endsWith(".htm", ignoreCase = true) -> "html"
            name.endsWith(".py", ignoreCase = true) -> "python"
            name.endsWith(".js", ignoreCase = true) || name.endsWith(".ts", ignoreCase = true) -> "javascript"
            name.endsWith(".kt", ignoreCase = true) -> "kotlin"
            name.endsWith(".cpp", ignoreCase = true) || name.endsWith(".c", ignoreCase = true) -> "cpp"
            name.endsWith(".json", ignoreCase = true) -> "json"
            else -> "text"
        }
    }
}
