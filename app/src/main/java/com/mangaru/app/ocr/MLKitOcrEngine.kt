package com.mangaru.app.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class RecognizedBlock(
    val originalText: String,
    val boundingBox: Rect
)

class MLKitOcrEngine {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(bitmap: Bitmap, languageMode: String): List<RecognizedBlock> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        
        val recognizer = when (languageMode.lowercase()) {
            "ja", "japanese" -> japaneseRecognizer
            "ko", "korean" -> koreanRecognizer
            "zh", "chinese" -> chineseRecognizer
            else -> latinRecognizer
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = mutableListOf<RecognizedBlock>()
                for (block in visionText.textBlocks) {
                    val rect = block.boundingBox
                    val text = block.text.replace("\n", " ")
                    if (rect != null && text.isNotBlank()) {
                        blocks.add(RecognizedBlock(text, rect))
                    }
                }
                continuation.resume(blocks)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }
}
