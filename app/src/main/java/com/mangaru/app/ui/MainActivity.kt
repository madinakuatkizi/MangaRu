package com.mangaru.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.mangaru.app.service.ScreenCaptureService
import com.mangaru.app.ui.theme.MangaRuTheme
import com.mangaru.app.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)
    private var hasOverlayPermission by mutableStateOf(false)
    private var selectedLanguage by mutableStateOf("ja")

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
                putExtra(ScreenCaptureService.EXTRA_LANGUAGE, selectedLanguage)
            }
            startForegroundService(serviceIntent)
            isServiceRunning = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()

        setContent {
            MangaRuTheme {
                MainScreen(
                    isServiceRunning = isServiceRunning,
                    hasOverlayPermission = hasOverlayPermission,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { selectedLanguage = it },
                    onRequestOverlayPermission = {
                        PermissionUtils.requestOverlayPermission(this)
                    },
                    onStartClick = { startTranslationProcess() },
                    onStopClick = { stopTranslationProcess() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = PermissionUtils.hasOverlayPermission(this)
    }

    private fun startTranslationProcess() {
        if (!hasOverlayPermission) {
            PermissionUtils.requestOverlayPermission(this)
            return
        }
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun stopTranslationProcess() {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java)
        stopService(serviceIntent)
        isServiceRunning = false
    }
}
