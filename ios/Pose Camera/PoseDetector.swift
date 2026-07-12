import Foundation
import Combine
import QuartzCore
import Vision

final class PoseDetector: ObservableObject {
    @Published var detectedSkeleton: DetectedSkeleton?

    private let visionQueue = DispatchQueue(label: "com.posesnap.poseDetector")
    private let minimumFrameInterval: TimeInterval = 1.0 / 15.0
    private var lastProcessedAt: TimeInterval = 0

    func processFrame(_ pixelBuffer: CVPixelBuffer, mirrored: Bool) {
        let now = CACurrentMediaTime()
        guard now - lastProcessedAt >= minimumFrameInterval else { return }
        lastProcessedAt = now

        visionQueue.async {
            let request = VNDetectHumanBodyPoseRequest()
            let orientation: CGImagePropertyOrientation = mirrored ? .leftMirrored : .right
            let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: orientation)

            do {
                try handler.perform([request])
                guard let observation = request.results?.max(by: { $0.confidence < $1.confidence }) else {
                    DispatchQueue.main.async {
                        self.detectedSkeleton = nil
                    }
                    return
                }

                let skeleton = try self.makeSkeleton(from: observation)
                DispatchQueue.main.async {
                    self.detectedSkeleton = skeleton
                }
            } catch {
                DispatchQueue.main.async {
                    self.detectedSkeleton = nil
                }
            }
        }
    }

    private func makeSkeleton(from observation: VNHumanBodyPoseObservation) throws -> DetectedSkeleton {
        let recognizedPoints = try observation.recognizedPoints(.all)
        var joints: [VNHumanBodyPoseObservation.JointName: CGPoint] = [:]
        var confidenceTotal: Float = 0
        var confidenceCount: Float = 0

        for (jointName, point) in recognizedPoints where point.confidence > 0.25 {
            let normalizedPoint = CGPoint(x: CGFloat(point.location.x), y: 1.0 - CGFloat(point.location.y))
            joints[jointName] = normalizedPoint
            confidenceTotal += point.confidence
            confidenceCount += 1
        }

        return DetectedSkeleton(
            joints: joints,
            confidence: confidenceCount > 0 ? confidenceTotal / confidenceCount : 0,
            boundingBox: boundingBox(for: joints.values)
        )
    }

    private func boundingBox(for points: Dictionary<VNHumanBodyPoseObservation.JointName, CGPoint>.Values) -> CGRect {
        guard let first = points.first else { return .zero }
        var minX = first.x
        var minY = first.y
        var maxX = first.x
        var maxY = first.y

        for point in points {
            minX = min(minX, point.x)
            minY = min(minY, point.y)
            maxX = max(maxX, point.x)
            maxY = max(maxY, point.y)
        }

        return CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)
    }
}
