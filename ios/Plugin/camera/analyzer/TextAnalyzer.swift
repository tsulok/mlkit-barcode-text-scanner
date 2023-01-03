//
//  TextAnalyzer.swift
//  Plugin
//
//  Created by David Timar on 2022. 12. 16..
//

import Foundation
import MLKitTextRecognition
import MLKitVision
import MLImage

final class TextAnalyzer: AnalyzerProtocol {
  
  
  private let recognizer: TextRecognizer
  
  init() {
    self.recognizer = TextRecognizer.textRecognizer(options: TextRecognizerOptions())
  }
  
  func recognizeText(on image: VisionImage, foundResultCallback: (String, AnalyzerType) -> Void) {
    do {
      let recognizedText: Text = try recognizer.results(in: image)
      recognizedText.blocks.forEach { textBlock in
        textBlock.lines.forEach { line in
          foundResultCallback(line.text, AnalyzerType.imageText)
        }
      }
    } catch let error {
      Logger.error("Failed to recognize text with error: \(error.localizedDescription).")
    }
  }
}
