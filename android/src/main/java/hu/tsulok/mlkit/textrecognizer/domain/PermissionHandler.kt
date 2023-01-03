package hu.tsulok.mlkit.textrecognizer.domain

import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import hu.tsulok.mlkit.textrecognizer.domain.model.PluginError

internal class PermissionHandler(
    private val plugin: Plugin,
    private val call: PluginCall
) {
    fun checkPermission(
        permissionEnabledCallback: () -> Unit,
        requestPermissionCallback: () -> Unit
    ) {
        when (plugin.getPermissionState("camera")) {
            PermissionState.GRANTED -> {
                permissionEnabledCallback()
                return
            }
            PermissionState.PROMPT, PermissionState.PROMPT_WITH_RATIONALE -> {
                requestPermissionCallback()
                return
            }
            PermissionState.DENIED -> {
                call.reject(
                    PluginError.PermissionDeniedCompletely.errorMessage,
                    PluginError.PermissionDeniedCompletely
                )
                return
            }
            else -> {
                call.reject(
                    "Unknown state",
                    IllegalStateException("Android OS did not return camera state")
                )
                return
            }
        }
    }
}