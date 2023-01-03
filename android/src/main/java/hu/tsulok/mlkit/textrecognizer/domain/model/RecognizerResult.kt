package hu.tsulok.mlkit.textrecognizer.domain.model

sealed class RecognizerResult {
    data class Text(val content: String, val confidence: Double) : RecognizerResult()
    data class Barcode(val content: String) : RecognizerResult()
}
