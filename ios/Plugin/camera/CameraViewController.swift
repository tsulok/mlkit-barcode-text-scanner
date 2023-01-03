//
//  CameraViewController.swift
//  Plugin
//
//  Created by David Timar on 2022. 12. 16..
//

import UIKit
import AVFoundation
import CoreVideo
import MLKitBarcodeScanning
import MLKitTextRecognition
import MLKitVision
import MLKitCommon
import MLImage

protocol DataFoundDelegate: AnyObject {
  func onDataRead(_ text: String, type: AnalyzerType)
}

final class CameraViewController: UIViewController {
  
  private var isUsingFrontCamera = false
  private var previewLayer: AVCaptureVideoPreviewLayer!
  private lazy var captureSession = AVCaptureSession()
  private lazy var sessionQueue = DispatchQueue(label: Constant.sessionQueueLabel)
  private var lastFrame: CMSampleBuffer?
  
  weak var dataFoundDelegate: DataFoundDelegate?
  
  private var registeredAnalyzers: [AnalyzerProtocol] = []
  
  init(recognizerConfig: RecoginzerConfig) {
    if (recognizerConfig.barcodeScannerConfig.isAllowed) {
      registeredAnalyzers.append(BarcodeAnalyzer())
    }
    
    if (recognizerConfig.textRecognizerConfig.isAllowed) {
      registeredAnalyzers.append(TextAnalyzer())
    }
    
    super.init(nibName: nil, bundle: nil)
  }
  
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
  
  private lazy var previewOverlayView: UIImageView = {
    precondition(isViewLoaded)
    let previewOverlayView = UIImageView(frame: .zero)
    previewOverlayView.contentMode = UIView.ContentMode.scaleAspectFill
    previewOverlayView.translatesAutoresizingMaskIntoConstraints = false
    return previewOverlayView
  }()
  
  private var cameraView: UIView!
  
  override func viewDidLoad() {
    super.viewDidLoad()
    setupCameraBackground()
    setupPreviewOverlayView()
    setupCaptureSessionOutput()
    setupCaptureSessionInput()
  }
  
  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    startSession()
  }
  
  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)
    stopSession()
  }
  
  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    previewLayer.frame = cameraView.frame
  }
  
  // MARK: Detections
  private func recognizeText(in image: VisionImage, width: CGFloat, height: CGFloat) {
    registeredAnalyzers.forEach { analyzer in
      analyzer.recognizeText(on: image) { [weak self] foundText, analyzerType in
        self?.dataFoundDelegate?.onDataRead(foundText, type: analyzerType)
      }
    }
    self.updatePreviewOverlayViewWithLastFrame()
  }
}

// MARK: Camera handling
extension CameraViewController {
  private func setupCaptureSessionOutput() {
    sessionQueue.async { [weak self] in
      guard let `self` = self else {
        return
      }
      self.captureSession.beginConfiguration()
      // When performing latency tests to determine ideal capture settings,
      // run the app in 'release' mode to get accurate performance metrics
      self.captureSession.sessionPreset = AVCaptureSession.Preset.medium
      
      let output = AVCaptureVideoDataOutput()
      output.videoSettings = [
        (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA
      ]
      output.alwaysDiscardsLateVideoFrames = true
      let outputQueue = DispatchQueue(label: Constant.videoDataOutputQueueLabel)
      output.setSampleBufferDelegate(self, queue: outputQueue)
      guard self.captureSession.canAddOutput(output) else {
        Logger.error("Failed to add capture session output.")
        return
      }
      self.captureSession.addOutput(output)
      self.captureSession.commitConfiguration()
    }
  }
  
  private func setupCaptureSessionInput() {
    sessionQueue.async { [weak self] in
      guard let `self` = self else {
        return
      }
      let cameraPosition: AVCaptureDevice.Position = self.isUsingFrontCamera ? .front : .back
      guard let device = self.captureDevice(forPosition: cameraPosition) else {
        Logger.error("Failed to get capture device for camera position: \(cameraPosition)")
        return
      }
      do {
        self.captureSession.beginConfiguration()
        let currentInputs = self.captureSession.inputs
        for input in currentInputs {
          self.captureSession.removeInput(input)
        }
        
        let input = try AVCaptureDeviceInput(device: device)
        guard self.captureSession.canAddInput(input) else {
          Logger.error("Failed to add capture session input.")
          return
        }
        self.captureSession.addInput(input)
        self.captureSession.commitConfiguration()
      } catch {
        Logger.error("Failed to create capture device input: \(error.localizedDescription)")
      }
    }
  }
  
  private func startSession() {
    sessionQueue.async { [weak self] in
      guard let `self` = self else {
        return
      }
      self.captureSession.startRunning()
    }
  }
  
  private func stopSession() {
    sessionQueue.async { [weak self] in
      guard let `self` = self else {
        return
      }
      self.captureSession.stopRunning()
    }
  }
  
  private func captureDevice(forPosition position: AVCaptureDevice.Position) -> AVCaptureDevice? {
    if #available(iOS 10.0, *) {
      let discoverySession = AVCaptureDevice.DiscoverySession(
        deviceTypes: [.builtInWideAngleCamera],
        mediaType: .video,
        position: .unspecified
      )
      return discoverySession.devices.first { $0.position == position }
    }
    return nil
  }
  
  private func updatePreviewOverlayViewWithLastFrame() {
    DispatchQueue.main.sync { [weak self] in
      guard let `self` = self else {
        return
      }
      
      guard let lastFrame = lastFrame,
            let imageBuffer = CMSampleBufferGetImageBuffer(lastFrame)
      else {
        return
      }
      self.updatePreviewOverlayViewWithImageBuffer(imageBuffer)
    }
  }
  
  private func updatePreviewOverlayViewWithImageBuffer(_ imageBuffer: CVImageBuffer?) {
    guard let imageBuffer = imageBuffer else {
      return
    }
    let orientation: UIImage.Orientation = isUsingFrontCamera ? .leftMirrored : .right
    let image = UIUtilities.createUIImage(from: imageBuffer, orientation: orientation)
    previewOverlayView.image = image
  }
}

// MARK: UI Config
extension CameraViewController {
  
  private func setupCameraBackground() {
    cameraView = UIView(frame: UIScreen.main.bounds)
    cameraView.translatesAutoresizingMaskIntoConstraints = false
    self.view.addSubview(cameraView)
    NSLayoutConstraint.activate([
      cameraView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
      cameraView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
      cameraView.topAnchor.constraint(equalTo: self.view.topAnchor),
      cameraView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor),
      
    ])
    
    previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
  }
  
  private func setupPreviewOverlayView() {
    cameraView.addSubview(previewOverlayView)
    NSLayoutConstraint.activate([
      previewOverlayView.leadingAnchor.constraint(equalTo: cameraView.leadingAnchor),
      previewOverlayView.trailingAnchor.constraint(equalTo: cameraView.trailingAnchor),
      previewOverlayView.topAnchor.constraint(equalTo: cameraView.topAnchor),
      previewOverlayView.bottomAnchor.constraint(equalTo: cameraView.bottomAnchor),
      
    ])
  }
}

// MARK: Camera delegates
extension CameraViewController: AVCaptureVideoDataOutputSampleBufferDelegate {
  
  func captureOutput(
    _ output: AVCaptureOutput,
    didOutput sampleBuffer: CMSampleBuffer,
    from connection: AVCaptureConnection
  ) {
    guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
      Logger.error("Failed to get image buffer from sample buffer.")
      return
    }
    lastFrame = sampleBuffer
    let visionImage = VisionImage(buffer: sampleBuffer)
    let orientation = UIUtilities.imageOrientation(
      fromDevicePosition: isUsingFrontCamera ? .front : .back
    )
    visionImage.orientation = orientation
    
    guard let inputImage = MLImage(sampleBuffer: sampleBuffer) else {
        Logger.error("Failed to create MLImage from sample buffer.")
      return
    }
    inputImage.orientation = orientation
    
    let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
    let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))
    recognizeText(in: visionImage, width: imageWidth, height: imageHeight)
  }
}


// MARK: - Constants
private enum Constant {
  static let videoDataOutputQueueLabel = "com.google.mlkit.textrecognizer.VideoDataOutputQueue"
  static let sessionQueueLabel = "com.google.mlkit.textrecognizer.SessionQueue"
}
