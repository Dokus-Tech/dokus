package be.police.pulse.foundation.ktor.routes

import be.police.pulse.foundation.ktor.ApplicationInfo
import be.police.pulse.foundation.ktor.DetailedHealthInfo
import be.police.pulse.foundation.ktor.HealthCheck
import be.police.pulse.foundation.ktor.HealthStatus
import be.police.pulse.foundation.ktor.HeapMemoryInfo
import be.police.pulse.foundation.ktor.JvmInfo
import be.police.pulse.foundation.ktor.MemoryInfo
import be.police.pulse.foundation.ktor.NonHeapMemoryInfo
import be.police.pulse.foundation.ktor.ServerStatus
import be.police.pulse.foundation.ktor.SystemInfo
import be.police.pulse.foundation.ktor.ThreadInfo
import be.police.pulse.foundation.ktor.db.DatabaseFactory
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.seconds

fun Routing.healthRoutes() {
    val databaseFactory by inject<DatabaseFactory>()

    get("/health/live") {
        call.respond(
            HttpStatusCode.OK, HealthStatus(
                status = ServerStatus.UP,
                checks = mapOf(
                    "service" to HealthCheck(ServerStatus.UP, "Service is running")
                )
            )
        )
    }

    get("/health/ready") {
        // Readiness check - service is ready to handle requests
        val checks = mutableMapOf<String, HealthCheck>()
        var overallStatus = ServerStatus.UP

        // Database check
        val dbCheck = checkDatabase(databaseFactory)
        checks["database"] = dbCheck
        if (dbCheck.status != ServerStatus.UP) {
            overallStatus = ServerStatus.DOWN
        }

        // Memory check
        val memoryCheck = checkMemory()
        checks["memory"] = memoryCheck
        if (memoryCheck.status == ServerStatus.DOWN) {
            overallStatus = ServerStatus.DOWN
        }

        // Disk space check
        val diskCheck = checkDiskSpace()
        checks["disk"] = diskCheck
        if (diskCheck.status == ServerStatus.DOWN) {
            overallStatus = ServerStatus.DOWN
        }

        // Thread check
        val threadCheck = checkThreads()
        checks["threads"] = threadCheck

        val statusCode =
            if (overallStatus == ServerStatus.UP) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

        call.respond(
            statusCode, HealthStatus(
                status = overallStatus,
                checks = checks
            )
        )
    }

    get("/health") {
        // Detailed health information
        val runtime = Runtime.getRuntime()
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val threadMXBean = ManagementFactory.getThreadMXBean()
        val osMXBean = ManagementFactory.getOperatingSystemMXBean()

        val healthInfo = DetailedHealthInfo(
            status = ServerStatus.UP,
            application = ApplicationInfo(
                name = "pulse-auth-service",
                version = "1.0.0",
                environment = System.getenv("ENVIRONMENT") ?: "development"
            ),
            jvm = JvmInfo(
                version = System.getProperty("java.version"),
                vendor = System.getProperty("java.vendor"),
                uptime = ManagementFactory.getRuntimeMXBean().uptime
            ),
            memory = MemoryInfo(
                heap = HeapMemoryInfo(
                    used = memoryMXBean.heapMemoryUsage.used,
                    committed = memoryMXBean.heapMemoryUsage.committed,
                    max = memoryMXBean.heapMemoryUsage.max
                ),
                nonHeap = NonHeapMemoryInfo(
                    used = memoryMXBean.nonHeapMemoryUsage.used,
                    committed = memoryMXBean.nonHeapMemoryUsage.committed
                )
            ),
            threads = ThreadInfo(
                count = threadMXBean.threadCount,
                daemon = threadMXBean.daemonThreadCount,
                peak = threadMXBean.peakThreadCount
            ),
            system = SystemInfo(
                processors = osMXBean.availableProcessors,
                loadAverage = osMXBean.systemLoadAverage,
                arch = System.getProperty("os.arch"),
                os = System.getProperty("os.name")
            )
        )

        call.respond(HttpStatusCode.OK, healthInfo)
    }
}

private suspend fun checkDatabase(databaseFactory: DatabaseFactory): HealthCheck {
    return try {
        withTimeout(5.seconds) {
            transaction(databaseFactory.database) {
                exec("SELECT 1") { rs ->
                    rs.next()
                }
            }
        }
        HealthCheck(
            status = ServerStatus.UP,
            message = "Database connection is healthy",
            details = mapOf(
                "type" to "PostgreSQL",
                "response_time" to "< 5s"
            )
        )
    } catch (e: Exception) {
        HealthCheck(
            status = ServerStatus.DOWN,
            message = "Database connection failed: ${e.message}",
            details = mapOf(
                "error" to (e.message ?: "Unknown error")
            )
        )
    }
}

private fun checkMemory(): HealthCheck {
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val usedMemory = totalMemory - freeMemory

    val percentageUsed = (usedMemory.toDouble() / maxMemory.toDouble()) * 100

    return if (percentageUsed < 90) {
        HealthCheck(
            status = ServerStatus.UP,
            message = "Memory usage is healthy",
            details = mapOf(
                "used" to "${usedMemory / 1024 / 1024}MB",
                "max" to "${maxMemory / 1024 / 1024}MB",
                "percentage" to "${percentageUsed.toInt()}%"
            )
        )
    } else {
        HealthCheck(
            status = if (percentageUsed < 95) ServerStatus.WARN else ServerStatus.DOWN,
            message = "High memory usage detected",
            details = mapOf(
                "used" to "${usedMemory / 1024 / 1024}MB",
                "max" to "${maxMemory / 1024 / 1024}MB",
                "percentage" to "${percentageUsed.toInt()}%"
            )
        )
    }
}

private fun checkDiskSpace(): HealthCheck {
    val roots = File.listRoots()
    val root = roots.firstOrNull() ?: return HealthCheck(
        status = ServerStatus.UNKNOWN,
        message = "Unable to check disk space"
    )

    val totalSpace = root.totalSpace
    val freeSpace = root.freeSpace

    val percentageFree = (freeSpace.toDouble() / totalSpace.toDouble()) * 100

    return if (percentageFree > 10) {
        HealthCheck(
            status = ServerStatus.UP,
            message = "Disk space is healthy",
            details = mapOf(
                "free" to "${freeSpace / 1024 / 1024 / 1024}GB",
                "total" to "${totalSpace / 1024 / 1024 / 1024}GB",
                "percentage_free" to "${percentageFree.toInt()}%"
            )
        )
    } else {
        HealthCheck(
            status = if (percentageFree > 5) ServerStatus.WARN else ServerStatus.DOWN,
            message = "Low disk space detected",
            details = mapOf(
                "free" to "${freeSpace / 1024 / 1024 / 1024}GB",
                "total" to "${totalSpace / 1024 / 1024 / 1024}GB",
                "percentage_free" to "${percentageFree.toInt()}%"
            )
        )
    }
}

private fun checkThreads(): HealthCheck {
    val threadMXBean = ManagementFactory.getThreadMXBean()
    val threadCount = threadMXBean.threadCount

    return if (threadCount < 1000) {
        HealthCheck(
            status = ServerStatus.UP,
            message = "Thread count is healthy",
            details = mapOf(
                "count" to threadCount.toString(),
                "daemon" to threadMXBean.daemonThreadCount.toString(),
                "peak" to threadMXBean.peakThreadCount.toString()
            )
        )
    } else {
        HealthCheck(
            status = ServerStatus.WARN,
            message = "High thread count detected",
            details = mapOf(
                "count" to threadCount.toString(),
                "daemon" to threadMXBean.daemonThreadCount.toString(),
                "peak" to threadMXBean.peakThreadCount.toString()
            )
        )
    }
}