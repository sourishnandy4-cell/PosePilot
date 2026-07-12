import CoreHaptics
import UIKit

final class HapticsManager {
    static let shared = HapticsManager()

    private var engine: CHHapticEngine?
    private var didDetectSkeleton = false
    private var crossedGood = false
    private var crossedPerfect = false

    private init() {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return }
        engine = try? CHHapticEngine()
        try? engine?.start()
    }

    func skeletonDetected() {
        guard !didDetectSkeleton else { return }
        didDetectSkeleton = true
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    func updateScore(_ score: Float) {
        if score > 0.7 && !crossedGood {
            crossedGood = true
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        }

        if score > 0.9 && !crossedPerfect {
            crossedPerfect = true
            playSuccess()
        }

        if score < 0.6 {
            crossedGood = false
            crossedPerfect = false
        }
    }

    private func playSuccess() {
        guard let engine else {
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            return
        }

        let events = [
            CHHapticEvent(eventType: .hapticTransient, parameters: [], relativeTime: 0),
            CHHapticEvent(eventType: .hapticTransient, parameters: [], relativeTime: 0.12),
            CHHapticEvent(eventType: .hapticTransient, parameters: [], relativeTime: 0.26)
        ]

        if let pattern = try? CHHapticPattern(events: events, parameters: []),
           let player = try? engine.makePlayer(with: pattern) {
            try? player.start(atTime: 0)
        }
    }
}
