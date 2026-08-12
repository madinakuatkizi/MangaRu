package com.mangaru.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class FloatingControlView(
    private val context: Context,
    private val onStopClick: () -> Unit,
    private val onPauseToggle: (Boolean) -> Unit,
    private val onTextSizeChange: (Float) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var isPaused = false
    private var currentTextSize = 36f

    fun show() {
        if (floatingView != null) return

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(20, 10, 20, 10)
        }

        val statusTv = TextView(context).apply {
            text = "🟢 MangaRu"
            setTextColor(Color.WHITE)
            setPadding(10, 0, 20, 0)
        }

        val pauseBtn = Button(context).apply {
            text = "Пауза"
            setOnClickListener {
                isPaused = !isPaused
                text = if (isPaused) "Старт" else "Пауза"
                onPauseToggle(isPaused)
            }
        }

        val fontBtn = Button(context).apply {
            text = "A+"
            setOnClickListener {
                currentTextSize = if (currentTextSize >= 52f) 24f else currentTextSize + 6f
                onTextSizeChange(currentTextSize)
            }
        }

        val stopBtn = Button(context).apply {
            text = "❌"
            setOnClickListener { onStopClick() }
        }

        layout.addView(statusTv)
        layout.addView(pauseBtn)
        layout.addView(fontBtn)
        layout.addView(stopBtn)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        layout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(layout, params)
                        return true
                    }
                }
                return false
            }
        })

        floatingView = layout
        windowManager.addView(floatingView, params)
    }

    fun dismiss() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }
}
