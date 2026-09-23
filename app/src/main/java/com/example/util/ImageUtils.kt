package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Converts an image URI into a compact Base64 encoded JPEG string.
     * Keeps width/height within [maxDimension] and compresses with [quality]
     * so it fits well within Firestore limits (~30-60KB).
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 800,
        quality: Int = 75
    ): String? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var stream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return null
            }

            var sampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            while (maxOriginal / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // Decode actual bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            stream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()

            if (bitmap == null) return null

            // Scale down if still larger than maxDimension
            val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
            val finalBitmap = if (scale < 1.0f) {
                val matrix = Matrix().apply { postScale(scale, scale) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert URI to Base64", e)
            null
        }
    }

    /**
     * Decodes a Base64 string into an Android Bitmap.
     */
    fun base64ToBitmap(dataStr: String?): Bitmap? {
        if (dataStr.isNullOrBlank()) return null
        return try {
            val cleanBase64 = if (dataStr.contains(",")) {
                dataStr.substringAfter(",")
            } else {
                dataStr
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode Base64 to Bitmap", e)
            null
        }
    }

    /**
     * Checks whether a string represents a Base64 image.
     */
    fun isBase64Image(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return str.startsWith("data:image", ignoreCase = true) ||
                (str.length > 100 && !str.startsWith("http", ignoreCase = true) && !str.startsWith("content:", ignoreCase = true) && !str.startsWith("file:", ignoreCase = true))
    }
}

/**
 * Universal Image Viewer that seamlessly supports Base64 strings,
 * Content URIs, and Web URLs.
 */
@Composable
fun AppImageViewer(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val modelStr = model as? String

    if (modelStr != null && ImageUtils.isBase64Image(modelStr)) {
        val bitmap = remember(modelStr) { ImageUtils.base64ToBitmap(modelStr) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            // Fallback
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
