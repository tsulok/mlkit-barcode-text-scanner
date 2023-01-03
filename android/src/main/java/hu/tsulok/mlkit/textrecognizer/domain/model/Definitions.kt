package hu.tsulok.mlkit.textrecognizer.domain.model

import java.io.Serializable

data class BarCodeScannerConfig(val isAllowed: Boolean) : Serializable
data class TextRecognizerConfig(val isAllowed: Boolean) : Serializable

data class RecognizerConfig(
    val isLoggingEnabled: Boolean,
    val barcodeScanner: BarCodeScannerConfig,
    val textRecognizerConfig: TextRecognizerConfig
) : Serializable