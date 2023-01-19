package hu.tsulok.mlkit.textrecognizer

import android.Manifest
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import hu.tsulok.mlkit.textrecognizer.domain.PermissionHandler
import hu.tsulok.mlkit.textrecognizer.domain.eventbus.IRecognizerEvent
import hu.tsulok.mlkit.textrecognizer.domain.eventbus.ObjectRecognizerEventBus
import hu.tsulok.mlkit.textrecognizer.domain.model.*
import hu.tsulok.mlkit.textrecognizer.utils.PluginLogger

@CapacitorPlugin(
    name = "MLKitTextRecognizer",
    permissions = [
        Permission(strings = arrayOf(Manifest.permission.CAMERA), alias = "camera")
    ]
)
class MLKitTextRecognizerPlugin : Plugin(), IRecognizerEvent {

    private lateinit var recognizer: MLKitTextRecognizer
    private var call: PluginCall? = null

    override fun load() {
        recognizer = MLKitTextRecognizerImpl(context)
        ObjectRecognizerEventBus.addListener(this)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    fun startRecognizer(call: PluginCall) {
        Logger.debug("App", "Plugin method called")
        checkPermissionsInternal(call)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    fun killPlugin(killCall: PluginCall) {
        Logger.debug("App", "Plugin kill call invoked")
        if (this.call == null) {
            Logger.error("App - Plugin call can't be killed, saved call is not found")
            return
        }

        bridge.releaseCall(this.call)
        this.call = null
        Logger.debug("App", "Existing plugin released")
        recognizer.killCamera()
        killCall.resolve()
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        ObjectRecognizerEventBus.removeListener(this)
    }

    private fun checkPermissionsInternal(
        call: PluginCall,
        isAfterPermissionRequest: Boolean = false
    ) {
        PermissionHandler(this, call).checkPermission(
            permissionEnabledCallback = {
                startCamera(call)
            }, requestPermissionCallback = {
                if (isAfterPermissionRequest) {
                    call.reject(
                        PluginError.PermissionDeniedError.errorMessage,
                        PluginError.PermissionDeniedError
                    )
                } else {
                    requestPermissionForAlias("camera", call, "cameraPermissionCallback")
                }
            })
    }

    private fun startCamera(call: PluginCall) {
        val config = call.getObject("config", JSObject())
        if (config == null) {
            call.reject("Configuration were not provided")
            return
        }

        // Save the call to be accessible for data stream
        call.setKeepAlive(true)
        this.call = call

        val barcodeScannerConfig = config.getJSObject("barcodeScanner")
        val textRecognizerConfig = config.getJSObject("textRecognizer")
        recognizer.startCamera(
            RecognizerConfig(
                isLoggingEnabled = config.getBoolean("isLoggingEnabled"),
                barcodeScanner = BarCodeScannerConfig(
                    isAllowed = barcodeScannerConfig?.getBool("allow") ?: false
                ),
                textRecognizerConfig = TextRecognizerConfig(
                    isAllowed = textRecognizerConfig?.getBool(
                        "allow"
                    ) ?: false
                )
            )
        )
    }

    @PluginMethod
    override fun checkPermissions(pluginCall: PluginCall?) {
        super.checkPermissions(pluginCall)
    }

    @PluginMethod
    override fun requestPermissions(call: PluginCall?) {
        super.requestPermissions(call)
    }

    @PermissionCallback
    private fun cameraPermissionCallback(call: PluginCall) {
        checkPermissionsInternal(call, true)
    }

    override fun onEventReceived(result: RecognizerResult) {
        val resultObject = JSObject()
        when (result) {
            is RecognizerResult.Text -> {
                JSObject().apply {
                    this.put("content", result.content)
                    this.put("confidencePercentage", result.confidence)
                }.also {
                    resultObject.put("text", it)
                }
            }
            is RecognizerResult.Barcode -> {
                JSObject().apply {
                    this.put("content", result.content)
                }.also {
                    resultObject.put("barcode", it)
                }
            }
        }
        PluginLogger.debug("Plugin", "Call should be resolved with $resultObject")
        call?.resolve(resultObject)
    }
}