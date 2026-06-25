package com.dlzz.coder.debug

import android.content.Context
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Utility for exporting logs to shareable files.
 * Handles both in-app LogCollector entries and system logcat capture.
 */
object LogExporter {

    /**
     * Export in-app collected logs to cache directory and return the File.
     * Also captures recent logcat output for the app's process.
     */
    suspend fun exportLogs(context: Context, minLevel: LogCollector.Level = LogCollector.Level.DEBUG): File? =
        withContext(Dispatchers.IO) {
            writeLogFile(context.applicationContext, minLevel)
        }

    private fun writeLogFile(context: Context, minLevel: LogCollector.Level): File? {
        val timeStamp = android.text.format.DateFormat.format(
            "yyyy-MM-dd_HH-mm-ss",
            System.currentTimeMillis()
        ).toString()

        val logDir = File(context.cacheDir, "logs").apply { mkdirs() }
        val logFile = File(logDir, "coder_log_$timeStamp.txt")

        return try {
            val sb = StringBuilder()

            sb.appendLine("=== Coder Android App - Log Export ===")
            sb.appendLine("Export Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            sb.appendLine("App Version: ${getAppVersion(context)}")
            sb.appendLine("==========================================")
            sb.appendLine()

            sb.appendLine("--- App Logs (LogCollector) ---")
            sb.appendLine(LogCollector.exportText(minLevel))
            sb.appendLine()

            sb.appendLine("--- System Logcat (last 500 lines) ---")
            val logcatText = captureLogcat(500)
            sb.appendLine(logcatText)

            logFile.writeText(sb.toString(), Charsets.UTF_8)
            logFile
        } catch (e: Exception) {
            LogCollector.e("LogExporter", "Failed to export logs", e)
            null
        }
    }

    /**
     * Capture recent logcat output for this process.
     * Uses `logcat -d` (dump mode, non-blocking).
     */
    private fun captureLogcat(maxLines: Int): String {
        return try {
            val process = ProcessBuilder()
                .command("logcat", "-d", "-t", maxLines.toString(), "--pid=${android.os.Process.myPid()}")
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(3, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return "(logcat capture timed out)"
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            output.ifBlank { "(logcat returned no output)" }
        } catch (e: Exception) {
            "(logcat capture failed: ${e.message})"
        }
    }

    /**
     * Get the sharing URI for a log file via FileProvider.
     */
    fun getShareUri(context: Context, file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
