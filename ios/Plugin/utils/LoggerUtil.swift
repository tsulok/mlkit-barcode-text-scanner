//
//  LoggerUtil.swift
//  Plugin
//
//  Created by David Timar on 2023. 01. 02..
//

import Foundation

final class Logger {
  
  static var minimumLogLvl: Level = Level.debug
  
  static func debug(_ message: String) {
    log(message: message, lvl: .debug)
  }
  
  static func error(_ message: String) {
    log(message: message, lvl: .error)
  }
  
  static func info(_ message: String) {
    log(message: message, lvl: .info)
  }
  
  static func log(message: String, lvl: Level = .debug) {
    guard lvl >= minimumLogLvl else { return }
    print(message)
  }
  
  enum Level: String, CaseIterable {
    
    /// Appropriate for messages that contain information normally of use only when
    /// debugging a program.
    case debug
    
    /// Appropriate for informational messages.
    case info
    
    /// For error messages
    case error
  }
}


extension Logger.Level: Comparable {
  public static func < (lhs: Logger.Level, rhs: Logger.Level) -> Bool {
    return lhs.naturalIntegralValue < rhs.naturalIntegralValue
  }
  
  internal var naturalIntegralValue: Int {
    switch self {
    case .debug:
      return 1
    case .info:
      return 2
    case .error:
      return 3
    }
  }
}
