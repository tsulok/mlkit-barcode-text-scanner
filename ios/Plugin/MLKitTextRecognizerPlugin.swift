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
  
  public override func checkPermissions(_ call: CAPPluginCall) {
    
  }
  
  public override func requestPermissions(_ call: CAPPluginCall) {
    
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
