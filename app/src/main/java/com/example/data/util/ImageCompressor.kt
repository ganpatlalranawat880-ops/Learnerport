package com.example.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageCompressor {
  private const val TAG = "ImageCompressor"

  /**
   * Validates whether the given byte array represents an authentic PDF document
   * by verifying the standard magic header (%PDF).
   */
  fun isValidPdfBytes(bytes: ByteArray?): Boolean {
    if (bytes == null || bytes.size < 4) return false
    return bytes[0] == 0x25.toByte() && // '%'
      bytes[1] == 0x50.toByte() && // 'P'
      bytes[2] == 0x44.toByte() && // 'D'
      bytes[3] == 0x46.toByte()    // 'F'
  }

  /**
   * Compresses and optimizes a book or notes cover image.
   * Enforces standard portrait 3:4 aspect ratio and compresses to high-quality JPEG.
   */
  fun compressCover(bytes: ByteArray, targetWidth: Int = 768, targetHeight: Int = 1024): ByteArray {
    return try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

      // Calculate inSampleSize to avoid OOM
      options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
      options.inJustDecodeBounds = false

      val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: return bytes

      // Center-crop to 3:4 aspect ratio if needed
      val currentRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
      val targetRatio = 3f / 4f

      val croppedBitmap = if (Math.abs(currentRatio - targetRatio) > 0.05f) {
        if (currentRatio > targetRatio) {
          // Wider than 3:4: crop width
          val newWidth = (originalBitmap.height * targetRatio).toInt().coerceAtMost(originalBitmap.width)
          val startX = (originalBitmap.width - newWidth) / 2
          Bitmap.createBitmap(originalBitmap, startX, 0, newWidth, originalBitmap.height)
        } else {
          // Taller than 3:4: crop height
          val newHeight = (originalBitmap.width / targetRatio).toInt().coerceAtMost(originalBitmap.height)
          val startY = (originalBitmap.height - newHeight) / 2
          Bitmap.createBitmap(originalBitmap, 0, startY, originalBitmap.width, newHeight)
        }
      } else {
        originalBitmap
      }

      // Scale to target dimensions
      val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)

      val outputStream = ByteArrayOutputStream()
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
      val compressedBytes = outputStream.toByteArray()

      if (croppedBitmap != originalBitmap) croppedBitmap.recycle()
      if (scaledBitmap != croppedBitmap && scaledBitmap != originalBitmap) scaledBitmap.recycle()
      originalBitmap.recycle()

      Log.i(TAG, "Cover compressed from ${bytes.size / 1024} KB to ${compressedBytes.size / 1024} KB")
      compressedBytes
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to compress cover image, using raw bytes", e)
      bytes
    }
  }

  /**
   * Compresses a user profile avatar image.
   * Resizes large images to max 600x600 and compresses with 85% quality.
   */
  fun compressAvatar(bytes: ByteArray, maxDimension: Int = 600): ByteArray {
    return try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

      options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
      options.inJustDecodeBounds = false

      val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: return bytes

      val width = original.width
      val height = original.height

      val scale = if (width > maxDimension || height > maxDimension) {
        val maxCurrent = maxOf(width, height)
        maxDimension.toFloat() / maxCurrent.toFloat()
      } else {
        1.0f
      }

      val finalBitmap = if (scale < 1.0f) {
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(original, targetW, targetH, true)
      } else {
        original
      }

      val outputStream = ByteArrayOutputStream()
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
      val compressedBytes = outputStream.toByteArray()

      if (finalBitmap != original) finalBitmap.recycle()
      original.recycle()

      Log.i(TAG, "Avatar compressed from ${bytes.size / 1024} KB to ${compressedBytes.size / 1024} KB")
      compressedBytes
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to compress avatar image, using raw bytes", e)
      bytes
    }
  }

  private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
      val halfHeight = height / 2
      val halfWidth = width / 2
      while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }
}
