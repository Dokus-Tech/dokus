package be.police.pulse.foundation.ktor

import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: ServerStatus,
    val checks: Map<String, HealthCheck>,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HealthCheck(
    val status: ServerStatus,
    val message: String? = null,
    val details: Map<String, String>? = null
)

@Serializable
data class DetailedHealthInfo(
    val status: ServerStatus,
    val application: ApplicationInfo,
    val jvm: JvmInfo,
    val memory: MemoryInfo,
    val threads: ThreadInfo,
    val system: SystemInfo
)

@Serializable
enum class ServerStatus {
    UP,
    WARN,
    DOWN,
    UNKNOWN
}

@Serializable
data class ApplicationInfo(
    val name: String,
    val version: String,
    val environment: String
)

@Serializable
data class JvmInfo(
    val version: String,
    val vendor: String,
    val uptime: Long
)

@Serializable
data class MemoryInfo(
    val heap: HeapMemoryInfo,
    val nonHeap: NonHeapMemoryInfo
)

@Serializable
data class HeapMemoryInfo(
    val used: Long,
    val committed: Long,
    val max: Long
)

@Serializable
data class NonHeapMemoryInfo(
    val used: Long,
    val committed: Long
)

@Serializable
data class ThreadInfo(
    val count: Int,
    val daemon: Int,
    val peak: Int
)

@Serializable
data class SystemInfo(
    val processors: Int,
    val loadAverage: Double,
    val arch: String,
    val os: String
)