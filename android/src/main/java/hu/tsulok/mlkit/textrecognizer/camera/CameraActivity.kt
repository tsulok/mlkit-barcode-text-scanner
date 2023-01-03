package hu.tsulok.mlkit.textrecognizer.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import hu.tsulok.mlkit.textrecognizer.camera.analyzer.BarCodeAnalyzer
import hu.tsulok.mlkit.textrecognizer.camera.analyzer.TextAnalyzer
import hu.tsulok.mlkit.textrecognizer.domain.eventbus.ObjectRecognizerEventBus
import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerConfig
import hu.tsulok.mlkit.textrecognizer.utils.PluginLogger
import hu.tsulok.mlkit.textscanner.R

internal class CameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONFIG_KEY = "config"
    }

    private val previewView: PreviewView by lazy { findViewById(R.id.cameraViewFinder) }
    private val TAG = CameraActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_layout)
        startCamera(intent.getSerializableExtra(EXTRA_CONFIG_KEY) as? RecognizerConfig)
    }

    private fun startCamera(config: RecognizerConfig?) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val useCaseBuilder = UseCaseGroup.Builder()
            config?.barcodeScanner?.isAllowed?.let { isAllowed ->
                if (!isAllowed) {
                    return@let
                }
                useCaseBuilder.addUseCase(
                    ImageAnalysis.Builder().build().apply {
                        setAnalyzer(
                            ContextCompat.getMainExecutor(this@CameraActivity),
                            BarCodeAnalyzer(this@CameraActivity,
                                onBarCodeRecognized = {
                                    PluginLogger.debug(TAG, "Barcode results are: $it")
                                    ObjectRecognizerEventBus.notify(it)
                                }, onError = {
                                    PluginLogger.error(TAG, "Exception happened ${it.message}", it)
                                })
                        )
                    })
            }

            config?.textRecognizerConfig?.isAllowed?.let { isAllowed ->
                if (!isAllowed) {
                    return@let
                }

                useCaseBuilder.addUseCase(
                    ImageAnalysis.Builder().build().apply {
                        setAnalyzer(
                            ContextCompat.getMainExecutor(this@CameraActivity),
                            TextAnalyzer(this@CameraActivity,
                                onTextRecognized = {
                                    ObjectRecognizerEventBus.notify(it)
                                }, onError = {

                                })
                        )
                    })
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            useCaseBuilder.addUseCase(preview)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseBuilder.build()
                )

            } catch (exc: Exception) {
                PluginLogger.error("Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}