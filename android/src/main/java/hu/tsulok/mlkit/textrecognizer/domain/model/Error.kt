package hu.tsulok.mlkit.textrecognizer.domain.model

sealed class PluginError(val errorMessage: String) : Exception(errorMessage) {
    object PermissionDeniedError : PluginError("User did not enable camera permission")
    object PermissionDeniedCompletely :
        PluginError("Permission has been denied. Manual enable needed")
}