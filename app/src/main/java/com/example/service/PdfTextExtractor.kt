package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfTextExtractor {

    suspend fun extractPdfInfo(context: Context, uri: Uri): PdfExtractResult = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            val contentResolver = context.contentResolver
            tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
            } ?: return@withContext PdfExtractResult.Error("Could not open PDF file")

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) {
                return@withContext PdfExtractResult.Error("PDF is empty")
            }

            // Render first page to compact bitmap for AI vision analysis
            val page = renderer.openPage(0)
            val pageWidth = page.width.coerceAtLeast(1)
            val pageHeight = page.height.coerceAtLeast(1)
            val width = 1000
            val height = ((pageHeight.toFloat() * (1000f / pageWidth)).toInt()).coerceIn(100, 1600)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val base64 = ByteArrayOutputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }

            PdfExtractResult.Success(
                pageCount = pageCount,
                firstPageBitmap = bitmap,
                firstPageBase64 = base64
            )
        } catch (e: Exception) {
            PdfExtractResult.Error(e.message ?: "Failed to process PDF file")
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            try { tempFile?.delete() } catch (_: Exception) {}
        }
    }

    sealed class PdfExtractResult {
        data class Success(
            val pageCount: Int,
            val firstPageBitmap: Bitmap?,
            val firstPageBase64: String?
        ) : PdfExtractResult()

        data class Error(val message: String) : PdfExtractResult()
    }
}
