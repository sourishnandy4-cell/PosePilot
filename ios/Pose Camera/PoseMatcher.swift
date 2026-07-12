import CoreGraphics
import Foundation
import Vision

final class PoseMatcher {
    func match(detected: DetectedSkeleton, target: PoseDefinition, mirrored: Bool) -> PoseMatchResult {
        var weightedScore: Float = 0
        var totalWeight: Float = 0
        var jointDeltas: [VNHumanBodyPoseObservation.JointName: Float] = [:]
        var worstInstruction = target.instructions.first ?? "Hold still"
        var worstDelta: Float = 0

        for (jointKey, targetJoint) in target.targetJoints {
            let jointName = VNHumanBodyPoseObservation.JointName(rawValue: VNRecognizedPointKey(rawValue: jointKey))
            guard let detectedPoint = detected.joints[jointName] else { continue }

            let targetPoint = CGPoint(
                x: mirrored ? 1.0 - targetJoint.x : targetJoint.x,
                y: targetJoint.y
            )
            let deltaX = Float(detectedPoint.x - targetPoint.x)
            let deltaY = Float(detectedPoint.y - targetPoint.y)
            let pointDistance = sqrt(deltaX * deltaX + deltaY * deltaY)
            let normalizedDelta = min(pointDistance / 0.45, 1.0)
            let jointScore = max(0, 1.0 - normalizedDelta)
            let weight = semanticWeight(for: jointName) * targetJoint.weight

            weightedScore += jointScore * weight
            totalWeight += weight
            jointDeltas[jointName] = normalizedDelta

            if normalizedDelta > worstDelta {
                worstDelta = normalizedDelta
                worstInstruction = instruction(for: jointName, detected: detectedPoint, target: targetPoint)
            }
        }

        guard totalWeight > 0 else { return .empty }
        let score = min(max(weightedScore / totalWeight, 0), 1)
        return PoseMatchResult(score: score, topInstruction: score > 0.9 ? "Perfect!" : worstInstruction, jointDeltas: jointDeltas)
    }

    private func semanticWeight(for joint: VNHumanBodyPoseObservation.JointName) -> Float {
        switch joint {
        case .neck, .leftShoulder, .rightShoulder, .leftHip, .rightHip:
            return 1.4
        case .leftElbow, .rightElbow, .leftWrist, .rightWrist:
            return 1.15
        case .nose, .leftEye, .rightEye, .leftEar, .rightEar:
            return 0.8
        default:
            return 0.65
        }
    }

    private func instruction(for joint: VNHumanBodyPoseObservation.JointName, detected: CGPoint, target: CGPoint) -> String {
        let horizontal = detected.x < target.x ? "right" : "left"
        let vertical = detected.y < target.y ? "down" : "up"

        switch joint {
        case .leftWrist:
            return "Move your left hand \(horizontal)"
        case .rightWrist:
            return "Move your right hand \(horizontal)"
        case .leftElbow:
            return "Adjust your left elbow \(vertical)"
        case .rightElbow:
            return "Adjust your right elbow \(vertical)"
        case .nose:
            return "Turn your face slightly \(horizontal)"
        case .leftShoulder, .rightShoulder:
            return "Level your shoulders"
        case .leftHip, .rightHip:
            return "Shift your weight \(horizontal)"
        default:
            return "Move \(vertical) and hold steady"
        }
    }
}
