//
//  AnalyzerProtocol.swift
//  Plugin
//
//  Created by David Timar on 2023. 01. 02..

import MLKitVision
import MLImage

typealias AnalyzerResultFoundCallback = ((String, AnalyzerType) -> Void)

enum AnalyzerType {
  case barCode
  case imageText
}

protocol AnalyzerProtocol {
  func recognizeText(
    on image: VisionImage,
    foundResultCallback: @escaping AnalyzerResultFoundCallback)
}
