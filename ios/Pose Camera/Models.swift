import CoreGraphics
import Foundation
import Vision

struct DetectedSkeleton {
    let joints: [VNHumanBodyPoseObservation.JointName: CGPoint]
    let confidence: Float
    let boundingBox: CGRect
}

struct JointTarget: Codable, Hashable {
    let x: CGFloat
    let y: CGFloat
    let weight: Float

    var point: CGPoint {
        CGPoint(x: x, y: y)
    }
}

struct PoseDefinition: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let thumbnailName: String
    let sceneTags: [String]
    let targetJoints: [String: JointTarget]
    let instructions: [String]
    let difficulty: String
    let popularityScore: Int
}

enum SceneContext: String, CaseIterable, Codable {
    case beach
    case park
    case urbanStreet
    case indoorCasual
    case indoorFormal
    case restaurant
    case mountain
    case unknown

    var tags: [String] {
        switch self {
        case .beach:
            return ["beach", "outdoor", "casual"]
        case .park:
            return ["park", "outdoor", "casual"]
        case .urbanStreet:
            return ["urban", "outdoor", "street"]
        case .indoorCasual:
            return ["indoor", "casual"]
        case .indoorFormal:
            return ["indoor", "formal"]
        case .restaurant:
            return ["restaurant", "indoor", "formal"]
        case .mountain:
            return ["mountain", "outdoor", "action"]
        case .unknown:
            return ["casual", "solo"]
        }
    }
}

struct PoseMatchResult {
    let score: Float
    let topInstruction: String
    let jointDeltas: [VNHumanBodyPoseObservation.JointName: Float]

    static let empty = PoseMatchResult(score: 0, topInstruction: "Step into frame", jointDeltas: [:])
}

struct PoseHistoryItem: Codable, Identifiable {
    let id: UUID
    let poseName: String
    let score: Float
    let capturedAt: Date
}
