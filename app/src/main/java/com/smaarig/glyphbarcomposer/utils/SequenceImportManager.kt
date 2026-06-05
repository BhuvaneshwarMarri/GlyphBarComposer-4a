package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.smaarig.glyphbarcomposer.model.ImportResult
import java.io.InputStream

/**
 * Manager to handle sequence imports from URIs, detecting the format automatically.
 */
object SequenceImportManager {

    /**
     * Imports a sequence from the given URI.
     * Detects format by extension, MIME type, or content sniffing.
     * Returns null if the format is not recognized.
     */
    fun importFromUri(context: Context, uri: Uri): ImportResult? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri)
        val mimeType = contentResolver.getType(uri)

        return when {
            // BUG-9 FIX: Use ignoreCase=true — some email clients uppercase file extensions
            // (e.g. .GBSEQ.JSON), which caused the extension check to fail and fall through
            // to the slower content-sniffing path (or fail outright on MIME mismatch).
            fileName?.endsWith(".gbseq.json", ignoreCase = true) == true || mimeType == "application/vnd.glyphbar.sequence+json" -> {
                contentResolver.openInputStream(uri)?.use { JsonSequenceImporter.parse(it) }
            }
            fileName?.endsWith(".gbseq.csv", ignoreCase = true) == true || mimeType == "text/csv" -> {
                contentResolver.openInputStream(uri)?.use { CsvSequenceImporter.parse(it) }
            }
            else -> {
                // Sniff content
                sniffFormat(context, uri)
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun sniffFormat(context: Context, uri: Uri): ImportResult? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) return null

                val sniffed = String(buffer, 0, bytesRead, Charsets.UTF_8).trim()
                val isJson = sniffed.startsWith("{") || sniffed.startsWith("\uFEFF{")
                val isCsv = sniffed.startsWith("# GlyphBar") || sniffed.startsWith("\uFEFF# GlyphBar")

                when {
                    isJson -> {
                        // Re-open stream for full parse
                        context.contentResolver.openInputStream(uri)?.use { JsonSequenceImporter.parse(it) }
                    }
                    isCsv -> {
                        // Re-open stream for full parse
                        context.contentResolver.openInputStream(uri)?.use { CsvSequenceImporter.parse(it) }
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}