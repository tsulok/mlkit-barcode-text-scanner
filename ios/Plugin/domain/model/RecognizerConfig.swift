//
//  RecognizerConfig.swift
//  Plugin
//
//  Created by David Timar on 2023. 01. 03..
//

import Foundation

struct BarcodeScannerConfig {
  let isAllowed: Bool
}

struct TextRecognizerConfig {
  let isAllowed: Bool
}

struct RecoginzerConfig {
  let isLoggingEnabled: Bool
  let barcodeScannerConfig: BarcodeScannerConfig
  let textRecognizerConfig: TextRecognizerConfig
}
