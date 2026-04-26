package com.smaarig.glyphbarcomposer.utils

import android.content.Context
import android.net.Uri
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    fun zipFiles(outputFile: File, files: Map<String, File>) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            files.forEach { (name, file) ->
                if (file.exists()) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun unzipToDirectory(context: Context, zipUri: Uri, targetDir: File): List<File> {
        if (!targetDir.exists()) targetDir.mkdirs()
        val extractedFiles = mutableListOf<File>()

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    // Security check: ensure the file is within the target directory
                    if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedFiles.add(file)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return extractedFiles
    }

    fun isZipFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bis = BufferedInputStream(inputStream)
                bis.mark(4)
                val buffer = ByteArray(4)
                val read = bis.read(buffer)
                // ZIP magic number: 50 4B 03 04
                read == 4 && buffer[0] == 0x50.toByte() && buffer[1] == 0x4B.toByte() && buffer[2] == 0x03.toByte() && buffer[3] == 0x04.toByte()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
