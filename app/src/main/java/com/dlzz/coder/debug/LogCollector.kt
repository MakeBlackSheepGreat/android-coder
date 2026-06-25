package com.dlzz.coder.debug

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-app log collector for diagnostics.
 * Captures logcat output tagged with our app's tags plus any explicitly
 * added entries. Bounded to [MAX_ENTRIES] to prevent unbounded memory growth.
 */
object LogCollector {
    private const val MAX_ENTRIES = 2000
    private val entries = ConcurrentLinkedDeque<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    enum class Level(val priority: Int, val label: String) {
        DEBUG(Log.DEBUG, "D"),
        INFO(Log.INFO, "I"),
        WARN(Log.WARN, "W"),
        ERROR(Log.ERROR, "E");
    }

    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    ) {
        fun formatted(): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            val throwableText = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
            return "$time ${level.label}/$tag: $message$throwableText"
        }
    }

    fun d(tag: String, message: String) {
        add(LogEntry(System.currentTimeMillis(), Level.DEBUG, tag, message))
        Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        add(LogEntry(System.currentTimeMillis(), Level.INFO, tag, message))
        Log.i(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        add(LogEntry(System.currentTimeMillis(), Level.WARN, tag, message, throwable))
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        add(LogEntry(System.currentTimeMillis(), Level.ERROR, tag, message, throwable))
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }

    private fun add(entry: LogEntry) {
        entries.addLast(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.pollFirst()
        }
    }

    fun getAll(): List<LogEntry> = entries.toList()

    fun filter(minLevel: Level = Level.DEBUG): List<LogEntry> =
        entries.filter { it.level.priority >= minLevel.priority }

    fun clear() {
        entries.clear()
    }

    fun size(): Int = entries.size

    /**
     * Export logs as a plain text string.
     * @param minLevel minimum log level to include
     * @return formatted log text
     */
    fun exportText(minLevel: Level = Level.DEBUG): String {
        val sb = StringBuilder()
        sb.appendLine("=== Coder Android App Log Export ===")
        sb.appendLine("Exported at: ${dateFormat.format(Date())}")
        sb.appendLine("Total entries: ${size()}")
        sb.appendLine("Filter: >= ${minLevel.label}")
        sb.appendLine("==========================================")
        sb.appendLine()
        filter(minLevel).forEach { entry ->
            sb.appendLine(entry.formatted())
        }
        sb.appendLine()
        sb.appendLine("=== End of log ===")
        return sb.toString()
    }

    /**
     * Export logs to a file.
     * @param filePath absolute path to write the log file
     * @param minLevel minimum log level to include
     * @return true if file was written successfully
     */
    fun exportToFile(filePath: String, minLevel: Level = Level.DEBUG): Boolean {
        return try {
            val text = exportText(minLevel)
            java.io.File(filePath).writeText(text, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e(tag = "LogCollector", message = "Failed to export logs to $filePath", throwable = e)
            false
        }
    }
}
