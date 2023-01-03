//
//  Copyright (c) 2021 Google Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import AVFoundation
import CoreVideo
import MLKitCommon
import MLKitVision
import UIKit

extension Bundle {
    
    static func loadView<T>(fromNib name: String, withType type: T.Type) -> T {
        if let view = Bundle.main.loadNibNamed(name, owner: nil, options: nil)?.first as? T {
            return view
        }
        
        fatalError("Could not load view with type " + String(describing: type))
    }
}

/// Defines UI-related utilitiy methods for text recognition.
public class UIUtilities {
  
    public static func imageOrientation(
        fromDevicePosition devicePosition: AVCaptureDevice.Position = .back
    ) -> UIImage.Orientation {
        var deviceOrientation = UIDevice.current.orientation
        if deviceOrientation == .faceDown || deviceOrientation == .faceUp
            || deviceOrientation
            == .unknown
        {
            deviceOrientation = currentUIOrientation()
        }
        switch deviceOrientation {
        case .portrait:
            return devicePosition == .front ? .leftMirrored : .right
        case .landscapeLeft:
            return devicePosition == .front ? .downMirrored : .up
        case .portraitUpsideDown:
            return devicePosition == .front ? .rightMirrored : .left
        case .landscapeRight:
            return devicePosition == .front ? .upMirrored : .down
        case .faceDown, .faceUp, .unknown:
            return .up
        @unknown default:
            fatalError()
        }
    }
    
    /// Converts an image buffer to a `UIImage`.
    ///
    /// @param imageBuffer The image buffer which should be converted.
    /// @param orientation The orientation already applied to the image.
    /// @return A new `UIImage` instance.
    public static func createUIImage(
        from imageBuffer: CVImageBuffer,
        orientation: UIImage.Orientation
    ) -> UIImage? {
        let ciImage = CIImage(cvPixelBuffer: imageBuffer)
        let context = CIContext(options: nil)
        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else { return nil }
        return UIImage(cgImage: cgImage, scale: Constants.originalScale, orientation: orientation)
    }
    
    // MARK: - Private
    
    private static func currentUIOrientation() -> UIDeviceOrientation {
        let deviceOrientation = { () -> UIDeviceOrientation in
            switch UIApplication.shared.statusBarOrientation {
            case .landscapeLeft:
                return .landscapeRight
            case .landscapeRight:
                return .landscapeLeft
            case .portraitUpsideDown:
                return .portraitUpsideDown
            case .portrait, .unknown:
                return .portrait
            @unknown default:
                fatalError()
            }
        }
        guard Thread.isMainThread else {
            var currentOrientation: UIDeviceOrientation = .portrait
            DispatchQueue.main.sync {
                currentOrientation = deviceOrientation()
            }
            return currentOrientation
        }
        return deviceOrientation()
    }
}

// MARK: - Constants
private enum Constants {
    static let originalScale: CGFloat = 1.0
}
