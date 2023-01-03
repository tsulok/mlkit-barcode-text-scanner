package hu.tsulok.mlkit.textrecognizer.camera.analyzer

import android.content.Context
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import hu.tsulok.mlkit.textrecognizer.camera.vision.VisionProcessorBase
import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerResult

internal class TextAnalyzer(
    context: Context,
    private val onTextRecognized: (RecognizerResult) -> Unit,
    private val onError: (Exception) -> Unit
) :
    VisionProcessorBase<Text>(context),
    ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun stop() {
        super.stop()
        recognizer.close()
    }

    override fun detectInImage(image: InputImage): Task<Text> {
        return recognizer.process(image)
    }

    override fun onSuccess(results: Text) {
        results.textBlocks.forEach { textBlock ->
            textBlock.lines.forEach { line ->
                onTextRecognized(RecognizerResult.Text(line.text, line.confidence * 100.0))
            }
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