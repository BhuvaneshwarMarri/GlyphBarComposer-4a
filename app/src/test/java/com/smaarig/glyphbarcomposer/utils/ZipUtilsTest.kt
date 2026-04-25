package com.smaarig.glyphbarcomposer.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

class ZipUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `zipFiles creates a valid zip file`() {
        val file1 = tempFolder.newFile("file1.txt").apply { writeText("Hello") }
        val file2 = tempFolder.newFile("file2.txt").apply { writeText("World") }
        val zipFile = File(tempFolder.root, "test.zip")
        
        ZipUtils.zipFiles(zipFile, mapOf("f1.txt" to file1, "f2.txt" to file2))
        
        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)
    }

    @Test
    fun `isZipFile returns true for valid zip`() {
        val zipFile = tempFolder.newFile("test.zip")
        val file = tempFolder.newFile("data.txt").apply { writeText("data") }
        ZipUtils.zipFiles(zipFile, mapOf("data.txt" to file))
        
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns FileInputStream(zipFile)
        
        assertTrue(ZipUtils.isZipFile(context, uri))
    }

    @Test
    fun `unzipToDirectory extracts files correctly`() {
        val file1 = tempFolder.newFile("f1.txt").apply { writeText("Hello") }
        val zipFile = File(tempFolder.root, "test.zip")
        ZipUtils.zipFiles(zipFile, mapOf("f1.txt" to file1))
        
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val targetDir = tempFolder.newFolder("extracted")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns FileInputStream(zipFile)
        
        val extracted = ZipUtils.unzipToDirectory(context, uri, targetDir)
        assertEquals(1, extracted.size)
        assertEquals("Hello", extracted[0].readText())
    }
}
