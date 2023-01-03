package hu.tsulok.mlkit.textrecognizer.camera.analyzer

import android.content.Context
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import hu.tsulok.mlkit.textrecognizer.camera.vision.VisionProcessorBase
import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerResult

internal class BarCodeAnalyzer(
    context: Context,
    private val onBarCodeRecognized: (RecognizerResult) -> Unit,
    private val onError: (Exception) -> Unit
) : VisionProcessorBase<List<Barcode>>(context), ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        return scanner.process(image)
    }

    override fun onSuccess(results: List<Barcode>) {
        results.forEach {
            val value = it.rawValue ?: return@forEach
            onBarCodeRecognized(RecognizerResult.Barcode(value))
        }
    }

    override fun onFailure(e: Exception) {
        onError(e)
    }

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        processImageProxy(image)
    }
}