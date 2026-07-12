import Foundation

enum PoseLibrary {
    static let all: [PoseDefinition] = [
        pose("crossedArms", "Crossed Arms", ["solo", "formal", "indoor", "urban"], "Easy", [
            "leftShoulder": joint(0.38, 0.36, 1.2), "rightShoulder": joint(0.62, 0.36, 1.2),
            "leftElbow": joint(0.55, 0.49, 1), "rightElbow": joint(0.45, 0.49, 1),
            "leftWrist": joint(0.63, 0.47, 0.9), "rightWrist": joint(0.37, 0.47, 0.9),
            "leftHip": joint(0.42, 0.63, 1), "rightHip": joint(0.58, 0.63, 1)
        ], ["Cross your arms higher", "Keep shoulders level"], 95),
        pose("handOnHip", "Hand on Hip", ["solo", "casual", "beach", "urban"], "Easy", [
            "leftShoulder": joint(0.40, 0.35, 1.2), "rightShoulder": joint(0.62, 0.36, 1.2),
            "leftElbow": joint(0.32, 0.50, 1), "leftWrist": joint(0.43, 0.60, 1),
            "rightElbow": joint(0.72, 0.50, 0.8), "rightWrist": joint(0.74, 0.67, 0.8),
            "leftHip": joint(0.42, 0.62, 1), "rightHip": joint(0.58, 0.63, 1)
        ], ["Put one hand on your hip", "Relax the other arm"], 100),
        pose("armsOut", "Arms Out", ["solo", "beach", "park", "action"], "Easy", [
            "leftShoulder": joint(0.39, 0.36, 1.2), "rightShoulder": joint(0.61, 0.36, 1.2),
            "leftElbow": joint(0.25, 0.35, 1), "rightElbow": joint(0.75, 0.35, 1),
            "leftWrist": joint(0.13, 0.34, 1), "rightWrist": joint(0.87, 0.34, 1),
            "leftHip": joint(0.42, 0.62, 1), "rightHip": joint(0.58, 0.62, 1)
        ], ["Stretch both arms outward", "Open your chest"], 90),
        pose("lookingAway", "Looking Away", ["solo", "casual", "urban", "restaurant"], "Easy", [
            "nose": joint(0.44, 0.25, 0.8), "neck": joint(0.50, 0.33, 1.2),
            "leftShoulder": joint(0.39, 0.37, 1.1), "rightShoulder": joint(0.61, 0.36, 1.1),
            "leftElbow": joint(0.34, 0.52, 0.8), "rightElbow": joint(0.66, 0.52, 0.8),
            "leftHip": joint(0.42, 0.64, 1), "rightHip": joint(0.58, 0.63, 1)
        ], ["Turn your face slightly left", "Keep your torso forward"], 85),
        pose("casualLean", "Casual Lean", ["solo", "casual", "urban", "indoor"], "Medium", [
            "nose": joint(0.46, 0.25, 0.7), "neck": joint(0.48, 0.34, 1.1),
            "leftShoulder": joint(0.35, 0.38, 1.2), "rightShoulder": joint(0.58, 0.34, 1.2),
            "leftHip": joint(0.39, 0.64, 1.2), "rightHip": joint(0.61, 0.60, 1.2),
            "leftKnee": joint(0.43, 0.80, 0.8), "rightKnee": joint(0.66, 0.76, 0.8)
        ], ["Lean your shoulders gently", "Shift weight onto one leg"], 88),
        pose("sitCrossLegged", "Cross-Leg Sit", ["solo", "park", "beach", "casual"], "Medium", [
            "leftShoulder": joint(0.39, 0.34, 1), "rightShoulder": joint(0.61, 0.34, 1),
            "leftHip": joint(0.43, 0.57, 1.2), "rightHip": joint(0.57, 0.57, 1.2),
            "leftKnee": joint(0.35, 0.74, 1), "rightKnee": joint(0.65, 0.74, 1),
            "leftAnkle": joint(0.58, 0.82, 0.9), "rightAnkle": joint(0.42, 0.82, 0.9)
        ], ["Sit lower in frame", "Cross your ankles"], 72),
        pose("overShoulder", "Over Shoulder", ["solo", "urban", "formal", "restaurant"], "Medium", [
            "nose": joint(0.57, 0.26, 0.9), "neck": joint(0.51, 0.35, 1.1),
            "leftShoulder": joint(0.44, 0.39, 1), "rightShoulder": joint(0.65, 0.36, 1),
            "leftHip": joint(0.45, 0.63, 1), "rightHip": joint(0.62, 0.61, 1)
        ], ["Look back over your shoulder", "Angle your body slightly"], 82),
        pose("frameHands", "Frame Hands", ["solo", "casual", "indoor", "urban"], "Medium", [
            "nose": joint(0.50, 0.25, 0.8), "leftShoulder": joint(0.39, 0.36, 1), "rightShoulder": joint(0.61, 0.36, 1),
            "leftElbow": joint(0.31, 0.28, 1), "rightElbow": joint(0.69, 0.28, 1),
            "leftWrist": joint(0.41, 0.20, 1), "rightWrist": joint(0.59, 0.20, 1)
        ], ["Raise both hands near your face", "Frame your face with your hands"], 70),
        pose("fingerGuns", "Finger Guns", ["solo", "casual", "urban"], "Easy", [
            "leftShoulder": joint(0.40, 0.36, 1), "rightShoulder": joint(0.62, 0.36, 1),
            "leftElbow": joint(0.29, 0.42, 1), "rightElbow": joint(0.74, 0.42, 1),
            "leftWrist": joint(0.20, 0.36, 1), "rightWrist": joint(0.83, 0.36, 1)
        ], ["Point both hands outward", "Lift your elbows"], 65),
        pose("candid", "Candid Walk", ["solo", "casual", "urban", "park"], "Easy", [
            "leftShoulder": joint(0.40, 0.36, 1), "rightShoulder": joint(0.61, 0.36, 1),
            "leftHip": joint(0.43, 0.62, 1), "rightHip": joint(0.58, 0.62, 1),
            "leftKnee": joint(0.38, 0.78, 0.8), "rightKnee": joint(0.63, 0.77, 0.8),
            "leftAnkle": joint(0.35, 0.93, 0.8), "rightAnkle": joint(0.68, 0.90, 0.8)
        ], ["Take a small step", "Look relaxed"], 92),
        pose("backToBack", "Back to Back", ["group", "urban", "formal"], "Easy", baseStanding(), ["Stand close, shoulders aligned"], 74),
        pose("shoulderHold", "Shoulder Hold", ["group", "casual", "park"], "Easy", baseStanding(), ["Place a hand on their shoulder"], 78),
        pose("pileUp", "Pile Up", ["group", "casual", "indoor"], "Advanced", baseStanding(), ["Move closer together"], 58),
        pose("leanOnShoulder", "Lean on Shoulder", ["group", "casual", "restaurant"], "Medium", baseStanding(), ["Lean gently toward the group"], 76),
        pose("jump", "Jump", ["solo", "group", "action", "beach", "park"], "Advanced", [
            "leftShoulder": joint(0.38, 0.28, 1), "rightShoulder": joint(0.62, 0.28, 1),
            "leftWrist": joint(0.24, 0.12, 1), "rightWrist": joint(0.76, 0.12, 1),
            "leftHip": joint(0.43, 0.52, 1), "rightHip": joint(0.57, 0.52, 1),
            "leftAnkle": joint(0.34, 0.78, 0.8), "rightAnkle": joint(0.68, 0.77, 0.8)
        ], ["Raise your arms and jump"], 68),
        pose("spin", "Spin", ["solo", "action", "beach", "park"], "Advanced", baseStanding(), ["Turn your shoulders into the spin"], 61),
        pose("walking", "Walking", ["solo", "action", "urban", "park"], "Easy", baseStanding(), ["Take one natural step"], 84),
        pose("runningLaugh", "Running Laugh", ["solo", "group", "action", "park", "beach"], "Medium", baseStanding(), ["Lean forward and lift your step"], 75),
        pose("standStraight", "Stand Straight", ["solo", "formal", "indoor", "restaurant"], "Easy", baseStanding(), ["Stand tall", "Level your shoulders"], 98),
        pose("handsClasped", "Hands Clasped", ["solo", "formal", "indoor", "restaurant"], "Easy", [
            "leftShoulder": joint(0.39, 0.36, 1.2), "rightShoulder": joint(0.61, 0.36, 1.2),
            "leftElbow": joint(0.44, 0.52, 1), "rightElbow": joint(0.56, 0.52, 1),
            "leftWrist": joint(0.49, 0.61, 1), "rightWrist": joint(0.51, 0.61, 1),
            "leftHip": joint(0.43, 0.63, 1), "rightHip": joint(0.57, 0.63, 1)
        ], ["Clasp your hands in front", "Stand tall"], 93),
        pose("sideProfile", "Side Profile", ["solo", "formal", "urban", "restaurant"], "Medium", [
            "nose": joint(0.55, 0.25, 0.9), "neck": joint(0.52, 0.34, 1),
            "leftShoulder": joint(0.46, 0.36, 1.2), "rightShoulder": joint(0.57, 0.36, 1.2),
            "leftHip": joint(0.47, 0.63, 1.1), "rightHip": joint(0.57, 0.63, 1.1),
            "leftAnkle": joint(0.48, 0.92, 0.7), "rightAnkle": joint(0.59, 0.92, 0.7)
        ], ["Turn to your side", "Look toward the camera"], 80)
    ]

    private static func pose(
        _ id: String,
        _ name: String,
        _ tags: [String],
        _ difficulty: String,
        _ joints: [String: JointTarget],
        _ instructions: [String],
        _ popularityScore: Int
    ) -> PoseDefinition {
        PoseDefinition(
            id: id,
            name: name,
            thumbnailName: id,
            sceneTags: tags,
            targetJoints: joints,
            instructions: instructions,
            difficulty: difficulty,
            popularityScore: popularityScore
        )
    }

    private static func joint(_ x: CGFloat, _ y: CGFloat, _ weight: Float) -> JointTarget {
        JointTarget(x: x, y: y, weight: weight)
    }

    private static func baseStanding() -> [String: JointTarget] {
        [
            "nose": joint(0.50, 0.25, 0.8), "neck": joint(0.50, 0.34, 1.1),
            "leftShoulder": joint(0.39, 0.36, 1.2), "rightShoulder": joint(0.61, 0.36, 1.2),
            "leftElbow": joint(0.35, 0.52, 0.9), "rightElbow": joint(0.65, 0.52, 0.9),
            "leftWrist": joint(0.33, 0.68, 0.8), "rightWrist": joint(0.67, 0.68, 0.8),
            "leftHip": joint(0.43, 0.63, 1.2), "rightHip": joint(0.57, 0.63, 1.2),
            "leftKnee": joint(0.43, 0.78, 0.7), "rightKnee": joint(0.57, 0.78, 0.7),
            "leftAnkle": joint(0.42, 0.93, 0.7), "rightAnkle": joint(0.58, 0.93, 0.7)
        ]
    }
}
