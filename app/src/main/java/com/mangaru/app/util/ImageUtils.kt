package com.mangaru.app.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object ImageUtils {

    fun calculateBitmapHash(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 40, stream)
        val bytes = stream.toByteArray()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
