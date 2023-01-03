package hu.tsulok.mlkit.textrecognizer.domain.eventbus

import hu.tsulok.mlkit.textrecognizer.domain.model.RecognizerResult
import hu.tsulok.mlkit.textrecognizer.utils.PluginLogger

interface IRecognizerEvent {
    fun onEventReceived(result: RecognizerResult)
}

object ObjectRecognizerEventBus {
    private val recognizedEventListeners = mutableListOf<IRecognizerEvent>()
    private const val TAG = "ObjectRecognizerEventBus"

    fun addListener(event: IRecognizerEvent) {
        recognizedEventListeners.add(event)
        PluginLogger.debug(TAG, "Listener added")
    }

    fun removeListener(event: IRecognizerEvent) {
        recognizedEventListeners.remove(event)
        PluginLogger.debug(TAG, "Listener removed")
    }

    fun notify(result: RecognizerResult) {
        PluginLogger.debug(TAG, "Event notified $result")
        recognizedEventListeners.forEach {
            it.onEventReceived(result)
        }
    }
}