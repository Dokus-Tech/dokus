package tech.dokus.ocr.process

import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
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
 *
 * CRITICAL: Streams are drained concurrently to prevent deadlock.
 * If a process writes enough to stdout/stderr to fill OS buffers,
 * it will block. We must read both streams while the process runs.
 */
internal object ProcessExecutor {

    /** Max bytes to read from stdout/stderr to prevent OOM */
    private const val MAX_OUTPUT_BYTES = 10 * 1024 * 1024 // 10 MB

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

        // Capture references for concurrent stream reading
        val stdoutRef = AtomicReference<ByteArray>(ByteArray(0))
        val stderrRef = AtomicReference<ByteArray>(ByteArray(0))
        val readersComplete = CountDownLatch(2)

        // Drain stdout concurrently - must happen while process runs to prevent buffer deadlock
        val stdoutThread = Thread({
            try {
                stdoutRef.set(readStreamSafely(process.inputStream))
            } finally {
                readersComplete.countDown()
            }
        }, "ocr-stdout-reader")

        // Drain stderr concurrently
        val stderrThread = Thread({
            try {
                stderrRef.set(readStreamSafely(process.errorStream))
            } finally {
                readersComplete.countDown()
            }
        }, "ocr-stderr-reader")

        stdoutThread.isDaemon = true
        stderrThread.isDaemon = true
        stdoutThread.start()
        stderrThread.start()

        try {
            val completed = process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)

            if (!completed) {
                // Timeout: forcefully terminate the process
                process.destroyForcibly()
                // Wait briefly for process to actually die
                process.waitFor(2, TimeUnit.SECONDS)

                // Close streams to unblock reader threads (interrupt doesn't work for blocking I/O)
                closeStreamsSafely(process)

                // Give readers a moment to finish with whatever they captured
                readersComplete.await(1, TimeUnit.SECONDS)

                return ProcessResult(
                    exitCode = -1,
                    stdout = String(stdoutRef.get(), Charsets.UTF_8),
                    stderr = String(stderrRef.get(), Charsets.UTF_8),
                    timedOut = true
                )
            }

            // Process completed normally - wait for readers to finish draining
            // They should complete quickly since process has exited
            readersComplete.await(5, TimeUnit.SECONDS)

            return ProcessResult(
                exitCode = process.exitValue(),
                stdout = String(stdoutRef.get(), Charsets.UTF_8),
                stderr = String(stderrRef.get(), Charsets.UTF_8),
                timedOut = false
            )
        } finally {
            // Ensure process is terminated and streams are closed
            if (process.isAlive) {
                process.destroyForcibly()
            }
            closeStreamsSafely(process)
        }
    }

    /**
     * Read stream content safely with size limit.
     * Reads entire stream to byte array to avoid line-by-line blocking issues.
     */
    private fun readStreamSafely(stream: InputStream): ByteArray {
        return try {
            val bytes = stream.readBytes()
            if (bytes.size > MAX_OUTPUT_BYTES) {
                bytes.copyOf(MAX_OUTPUT_BYTES)
            } else {
                bytes
            }
        } catch (_: Exception) {
            // Stream closed or error - return what we have
            ByteArray(0)
        }
    }

    /**
     * Close process streams safely, ignoring errors.
     */
    private fun closeStreamsSafely(process: Process) {
        try { process.inputStream.close() } catch (_: Exception) {}
        try { process.errorStream.close() } catch (_: Exception) {}
        try { process.outputStream.close() } catch (_: Exception) {}
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
