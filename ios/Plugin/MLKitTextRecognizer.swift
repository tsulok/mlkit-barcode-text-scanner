import Foundation
import Capacitor

class MLKitTextRecognizer: NSObject {
  
  private var cameraVC: CameraViewController?
  
  func startCamera(
    configuration: JSObject,
    bridge: CAPBridgeProtocol?,
    dataFoundDelegate: DataFoundDelegate?) {
      
      let barcodeConfig = configuration["barcodeScanner"] as? JSObject
      let textRecognizerConfig = configuration["textRecognizer"] as? JSObject

      let recognizerConfig = RecoginzerConfig(
        isLoggingEnabled: configuration["isLoggingEnabled"] as? Bool ?? false,
        barcodeScannerConfig: BarcodeScannerConfig(
          isAllowed: barcodeConfig?["allow"] as? Bool ?? false),
        textRecognizerConfig: TextRecognizerConfig(
          isAllowed: textRecognizerConfig?["allow"] as? Bool ?? false))
      
      if !recognizerConfig.isLoggingEnabled {
        Logger.minimumLogLvl = .error
      }
      
      DispatchQueue.main.async {
        let cameraVC = CameraViewController(recognizerConfig: recognizerConfig)
        cameraVC.dataFoundDelegate = dataFoundDelegate
        bridge?.viewController?.present(cameraVC, animated: true)
        self.cameraVC = cameraVC
      }
    }
}
