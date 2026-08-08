package com.audiolab.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    fun copyUriToCache(context: Context, uri: Uri, prefix: String = "input"): File? {
        return try {
            val fileName = getFileName(context, uri)
            val extension = fileName.substringAfterLast(".", "tmp")
            val cacheFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getOutputFile(context: Context, prefix: String, extension: String): File {
        val outputDir = File(context.getExternalFilesDir(null), "AudioLab")
        if (!outputDir.exists()) outputDir.mkdirs()
        return File(outputDir, "${prefix}_${System.currentTimeMillis()}.$extension")
    }

    fun getTempFile(context: Context, extension: String): File {
        return File(context.cacheDir, "temp_${System.currentTimeMillis()}.$extension")
    }

    fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun formatTimeSeconds(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return String.format("%02d:%02d", mins, secs)
    }

    fun cleanTempFiles(context: Context) {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("temp_") || file.name.startsWith("preview_")) {
                file.delete()
            }
        }
    }
}
