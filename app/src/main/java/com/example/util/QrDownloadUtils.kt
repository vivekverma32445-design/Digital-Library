package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrDownloadUtils {

    fun generateQrBitmap(payload: String, sizePx: Int = 800): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
            )
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, width, 0, 0, width, height)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun downloadQrToDevice(
        context: Context,
        payload: String,
        filePrefix: String = "MDDL_Gate_QR"
    ): Boolean {
        val bitmap = generateQrBitmap(payload, 1024)
        if (bitmap == null) {
            Toast.makeText(context, "Could not generate QR bitmap", Toast.LENGTH_SHORT).show()
            return false
        }

        val filename = "${filePrefix}_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MaaDurgaLibrary")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Toast.makeText(context, "QR Code saved to Pictures/MaaDurgaLibrary!", Toast.LENGTH_LONG).show()
                    return true
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "MaaDurgaLibrary")
                if (!appDir.exists()) appDir.mkdirs()
                val imageFile = File(appDir, filename)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    imageFile.absolutePath,
                    filename,
                    "Entrance Gate QR Code - Maa Durga Digital Library"
                )
                Toast.makeText(context, "QR Code saved to Gallery!", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            // Fallback: cache & share
            try {
                val cachePath = File(context.cacheDir, "qr_codes")
                cachePath.mkdirs()
                val file = File(cachePath, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Save or Print Gate QR Code"))
                Toast.makeText(context, "QR Code ready to save or share", Toast.LENGTH_SHORT).show()
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "Error saving QR: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    fun shareQrCode(context: Context, payload: String, title: String = "Entrance Gate QR") {
        val bitmap = generateQrBitmap(payload, 1024) ?: return
        try {
            val cachePath = File(context.cacheDir, "qr_codes")
            cachePath.mkdirs()
            val file = File(cachePath, "Gate_QR_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Official Entrance Gate QR Code for Maa Durga Digital Library ($payload)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Gate QR via WhatsApp / Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
