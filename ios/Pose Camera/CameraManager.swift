import AVFoundation
import Combine
import Photos
import UIKit

final class CameraManager: NSObject, ObservableObject {
    @Published var capturedImage: UIImage?
    @Published var currentPixelBuffer: CVPixelBuffer?
    @Published var cameraPosition: AVCaptureDevice.Position = .back
    @Published var isSessionRunning = false
    @Published var authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

    let session = AVCaptureSession()

    private let sessionQueue = DispatchQueue(label: "com.posesnap.camera")
    private let videoOutput = AVCaptureVideoDataOutput()
    private let photoOutput = AVCapturePhotoOutput()
    private var currentInput: AVCaptureDeviceInput?

    func startSession() {
        requestCameraAccess { [weak self] granted in
            guard granted else { return }

            self?.sessionQueue.async {
                self?.configureSessionIfNeeded()
                if self?.session.isRunning == false {
                    self?.session.startRunning()
                    DispatchQueue.main.async {
                        self?.isSessionRunning = true
                    }
                }
            }
        }
    }

    func stopSession() {
        sessionQueue.async {
            guard self.session.isRunning else { return }
            self.session.stopRunning()
            DispatchQueue.main.async {
                self.isSessionRunning = false
            }
        }
    }

    func toggleCamera() {
        sessionQueue.async {
            let nextPosition: AVCaptureDevice.Position = self.cameraPosition == .back ? .front : .back
            self.setCamera(position: nextPosition)
        }
    }

    func capturePhoto() {
        sessionQueue.async {
            let settings = AVCapturePhotoSettings()
            settings.flashMode = .off
            self.photoOutput.capturePhoto(with: settings, delegate: self)
        }
    }

    private func requestCameraAccess(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    self.authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
                }
                completion(granted)
            }
        default:
            DispatchQueue.main.async {
                self.authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
            }
            completion(false)
        }
    }

    private func configureSessionIfNeeded() {
        guard session.inputs.isEmpty else { return }

        session.beginConfiguration()
        session.sessionPreset = .photo
        defer { session.commitConfiguration() }

        setCamera(position: cameraPosition, commitsConfiguration: false)

        if session.canAddOutput(videoOutput) {
            videoOutput.alwaysDiscardsLateVideoFrames = true
            videoOutput.videoSettings = [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
            ]
            videoOutput.setSampleBufferDelegate(self, queue: sessionQueue)
            session.addOutput(videoOutput)
        }

        if session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
            photoOutput.isHighResolutionCaptureEnabled = true
        }

        updateConnections()
    }

    private func setCamera(position: AVCaptureDevice.Position, commitsConfiguration: Bool = true) {
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
              let input = try? AVCaptureDeviceInput(device: device) else {
            return
        }

        if commitsConfiguration {
            session.beginConfiguration()
        }

        if let currentInput {
            session.removeInput(currentInput)
        }

        if session.canAddInput(input) {
            session.addInput(input)
            currentInput = input
            DispatchQueue.main.async {
                self.cameraPosition = position
            }
        }

        updateConnections()

        if commitsConfiguration {
            session.commitConfiguration()
        }
    }

    private func updateConnections() {
        [videoOutput.connection(with: .video), photoOutput.connection(with: .video)].forEach { connection in
            guard let connection else { return }
            if connection.isVideoOrientationSupported {
                connection.videoOrientation = .portrait
            }
            if connection.isVideoMirroringSupported {
                connection.isVideoMirrored = cameraPosition == .front
            }
        }
    }

    private func saveToPhotoLibrary(_ image: UIImage) {
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else { return }
            PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            }
        }
    }
}

extension CameraManager: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        DispatchQueue.main.async {
            self.currentPixelBuffer = pixelBuffer
        }
    }
}

extension CameraManager: AVCapturePhotoCaptureDelegate {
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        guard error == nil,
              let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else {
            return
        }

        DispatchQueue.main.async {
            self.capturedImage = image
        }
        saveToPhotoLibrary(image)
    }
}
