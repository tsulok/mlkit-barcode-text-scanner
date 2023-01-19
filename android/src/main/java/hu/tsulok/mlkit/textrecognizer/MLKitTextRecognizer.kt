package hu.tsulok.mlkit.textrecognizer

import android.content.Context
import android.content.Intent
import hu.tsulok.mlkit.textrecognizer.camera.CameraActivity
import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerConfig
import hu.tsulok.mlkit.textrecognizer.utils.PluginLogger

interface MLKitTextRecognizer {
    fun startCamera(config: RecognizerConfig)
    fun killCamera()
}

internal class MLKitTextRecognizerImpl(private val context: Context) : MLKitTextRecognizer {

    override fun startCamera(config: RecognizerConfig) {

        PluginLogger.enableLogging = config.isLoggingEnabled

        context.startActivity(
            Intent(context, CameraActivity::class.java).putExtra(
                CameraActivity.EXTRA_CONFIG_KEY,
                config
            )
        )
    }

    override fun killCamera() {
        context.startActivity(
            Intent(context, CameraActivity::class.java).putExtra(
                CameraActivity.KILL_KEY,
                true
            )
        )
    }
}