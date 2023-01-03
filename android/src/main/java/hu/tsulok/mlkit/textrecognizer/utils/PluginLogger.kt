package hu.tsulok.mlkit.textrecognizer.utils

import com.getcapacitor.Logger

object PluginLogger {

    var enableLogging = true

    fun debug(tag: String? = null, message: String?) {
        if (!enableLogging) {
            return
        }
        Logger.debug(tag, message ?: "")
    }

    fun error(tag: String? = null, message: String?, throwable: Throwable?) {
        Logger.error(tag, message, throwable)
    }

    fun error(tag: String? = null, throwable: Throwable?) {
        if (!enableLogging) {
            return
        }
        Logger.error(tag, throwable)
    }
}