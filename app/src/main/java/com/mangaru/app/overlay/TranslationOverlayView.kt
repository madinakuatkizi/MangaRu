package com.mangaru.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

data class TranslatedBlockUI(
    val translatedText: String,
    val rect: Rect
)

class TranslationOverlayView(context: Context) : View(context) {

    private val items = mutableListOf<TranslatedBlockUI>()
    
    var textSizePx: Float = 36f
        set(value) {
            field = value
            textPaint.textSize = value
            invalidate()
        }

    var showBackground: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 230
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun updateTranslations(newItems: List<TranslatedBlockUI>) {
        items.clear()
        items.addAll(newItems)
        invalidate()
    }

    fun clear() {
        items.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (item in items) {
            val rectF = RectF(item.rect)

            if (showBackground) {
                canvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                canvas.drawRoundRect(rectF, 12f, 12f, borderPaint)
            }

            drawWrappedText(canvas, item.translatedText, rectF)
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, rect: RectF) {
        val words = text.split(" ")
        val padding = 8f
        val availableWidth = rect.width() - (padding * 2)
        
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val measure = textPaint.measureText(testLine)
            if (measure <= availableWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        var y = rect.top + padding + textPaint.textSize
        for (line in lines) {
            if (y > rect.bottom - padding) break
            canvas.drawText(line, rect.left + padding, y, textPaint)
            y += textPaint.textSize + 4f
        }
    }
}
