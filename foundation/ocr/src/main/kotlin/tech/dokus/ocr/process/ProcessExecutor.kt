package tech.dokus.ocr.process

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Result of a CLI process execution.
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean
)

/**
 * Executes CLI commands via ProcessBuilder with timeout support.
 * No shell injection - uses argument array directly.
 */
internal object ProcessExecutor {

    /**
     * Execute a command with timeout.
     *
     * @param command Command and arguments as a list (no shell escaping needed)
     * @param timeout Maximum time to wait for completion
     * @param workingDir Optional working directory for the process
     * @return ProcessResult with exit code, stdout, stderr, and timeout flag
     */
    fun execute(
        command: List<String>,
        timeout: Duration,
        workingDir: File? = null
    ): ProcessResult {
        val processBuilder = ProcessBuilder(command)
            .redirectErrorStream(false)

        workingDir?.let { processBuilder.directory(it) }

        val process = processBuilder.start()

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        // Read streams in separate threads to prevent deadlock
        val stdoutThread = Thread {
            try {
                process.inputStream.bufferedReader().forEachLine { stdout.appendLine(it) }
            } catch (_: Exception) {
                // Stream closed, ignore
            }
        }
        val stderrThread = Thread {
            try {
                process.errorStream.bufferedReader().forEachLine { stderr.appendLine(it) }
            } catch (_: Exception) {
                // Stream closed, ignore
            }
        }

        stdoutThread.start()
        stderrThread.start()

        val completed = process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)

        if (!completed) {
            process.destroyForcibly()
            stdoutThread.interrupt()
            stderrThread.interrupt()
            return ProcessResult(
                exitCode = -1,
                stdout = stdout.toString(),
                stderr = stderr.toString(),
                timedOut = true
            )
        }

        // Wait for stream readers to finish (with timeout)
        stdoutThread.join(1000)
        stderrThread.join(1000)

        return ProcessResult(
            exitCode = process.exitValue(),
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            timedOut = false
        )
    }

    /**
     * Check if a command exists on the system PATH.
     *
     * @param command The command name to check
     * @return true if the command is available
     */
    fun commandExists(command: String): Boolean {
        return try {
            // Use 'which' on Unix-like systems
            val result = execute(
                listOf("which", command),
                Duration.parse("5s")
            )
            result.exitCode == 0 && result.stdout.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }
}
