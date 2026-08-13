package com.mangaru.app.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.mangaru.app.MangaRuApp
import com.mangaru.app.R
import com.mangaru.app.ocr.MLKitOcrEngine
import com.mangaru.app.overlay.FloatingControlView
import com.mangaru.app.overlay.TranslatedBlockUI
import com.mangaru.app.overlay.TranslationOverlayView
import com.mangaru.app.translator.GoogleTranslatorEngine
import com.mangaru.app.ui.MainActivity
import com.mangaru.app.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var windowManager: WindowManager? = null
    private var overlayView: TranslationOverlayView? = null
    private var floatingControl: FloatingControlView? = null

    private val ocrEngine = MLKitOcrEngine()
    private val translatorEngine = GoogleTranslatorEngine()

    private var isPaused = false
    private var lastBitmapHash = ""
    private var sourceLanguage = "ja"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initOverlayViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        sourceLanguage = intent?.getStringExtra(EXTRA_LANGUAGE) ?: "ja"

        if (resultCode == Activity.RESULT_OK && data != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startScreenCapture()
            startProcessingLoop()
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, MangaRuApp.CHANNEL_ID)
            .setContentTitle("MangaRu активен")
            .setContentText("Автоматический перевод экрана выполняется...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun initOverlayViews() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = TranslationOverlayView(this)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(overlayView, overlayParams)

        floatingControl = FloatingControlView(
            context = this,
            onStopClick = { stopSelf() },
            onPauseToggle = { paused -> isPaused = paused },
            onTextSizeChange = { size -> overlayView?.textSizePx = size }
        )
        floatingControl?.show()
    }

    private fun startScreenCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MangaRuCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun startProcessingLoop() {
        serviceScope.launch {
            while (true) {
                delay(800)

                if (isPaused) continue

                val bitmap = acquireLatestBitmap() ?: continue
                val currentHash = ImageUtils.calculateBitmapHash(bitmap)

                if (currentHash == lastBitmapHash) {
                    bitmap.recycle()
                    continue
                }
                lastBitmapHash = currentHash

                val recognizedBlocks = ocrEngine.recognizeText(bitmap, sourceLanguage)
                bitmap.recycle()

                if (recognizedBlocks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        overlayView?.clear()
                    }
                    continue
                }

                val uiList = mutableListOf<TranslatedBlockUI>()
                for (block in recognizedBlocks) {
                    val translated = translatorEngine.translate(block.originalText, sourceLanguage, "ru")
                    uiList.add(TranslatedBlockUI(translated, block.boundingBox))
                }

                withContext(Dispatchers.Main) {
                    overlayView?.updateTranslations(uiList)
                }
            }
        }
    }

    private fun acquireLatestBitmap(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingControl?.dismiss()
        overlayView?.let { windowManager?.removeView(it) }
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_LANGUAGE = "extra_language"
    }
}
