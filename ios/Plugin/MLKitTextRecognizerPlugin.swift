import Foundation
import Capacitor
import AVFoundation

@objc(MLKitTextRecognizerPlugin)
public class MLKitTextRecognizerPlugin: CAPPlugin {
  private let implementation = MLKitTextRecognizer()
  private var savedCall: CAPPluginCall?
  
  @objc func startRecognizer(_ call: CAPPluginCall) {
    call.keepAlive = true
    self.savedCall = call
    let config: JSObject = call.getObject("config", JSObject())
    implementation.startCamera(configuration: config, bridge: self.bridge, dataFoundDelegate: self)
  }
  
  @objc func killPlugin(_ call: CAPPluginCall) {
    Logger.debug("Plugin kill is being invoked")
    guard let savedCall = self.savedCall else {
      Logger.error("Saved call not found - can't kill plugin")
      return
    }
    bridge?.releaseCall(savedCall)
    self.savedCall = nil
    Logger.debug("Plugin is being released")
    implementation.removeCamera()
    call.resolve()
  }
}

extension MLKitTextRecognizerPlugin: DataFoundDelegate {
  func onDataRead(_ text: String, type: AnalyzerType) {
    var resultObject = JSObject()
    let contentObject = JSObject(dictionaryLiteral: ("content", text))
    switch type {
    case .barCode:
      resultObject["barcode"] = contentObject
    case .imageText:
      resultObject["text"] = contentObject
    }
    
    savedCall?.resolve(resultObject)
  }
}
