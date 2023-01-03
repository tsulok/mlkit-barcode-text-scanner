//
//  BarcodeAnalyzer.swift
//  Plugin
//
//  Created by David Timar on 2023. 01. 02..
//

import MLKitBarcodeScanning
import MLKitVision
import MLImage

final class BarcodeAnalyzer: AnalyzerProtocol {
  
  
  private let recognizer: BarcodeScanner
  
  init() {
    self.recognizer = BarcodeScanner.barcodeScanner(
      options: BarcodeScannerOptions(formats: .all))
  }
  
  func recognizeText(on image: VisionImage, foundResultCallback: @escaping (String, AnalyzerType) -> Void) {
    
    recognizer.process(image, completion: { barcodes, error in
      barcodes?.forEach({ barcode in
        guard let barcodeValue = barcode.rawValue else { return }
        foundResultCallback(barcodeValue, AnalyzerType.barCode)
      })
    })
  }
}
