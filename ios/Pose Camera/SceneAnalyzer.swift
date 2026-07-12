import Foundation
import Vision

final class SceneAnalyzer {
    func analyzeScene(from pixelBuffer: CVPixelBuffer) async -> SceneContext {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                let request = VNClassifyImageRequest()
                let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .right)

                do {
                    try handler.perform([request])
                    let identifiers = request.results?
                        .prefix(5)
                        .map { $0.identifier.lowercased() } ?? []
                    continuation.resume(returning: self.mapClassifications(identifiers))
                } catch {
                    continuation.resume(returning: .unknown)
                }
            }
        }
    }

    private func mapClassifications(_ identifiers: [String]) -> SceneContext {
        let text = identifiers.joined(separator: " ")

        if text.contains("beach") || text.contains("seashore") || text.contains("coast") {
            return .beach
        }
        if text.contains("park") || text.contains("garden") || text.contains("field") {
            return .park
        }
        if text.contains("street") || text.contains("city") || text.contains("building") {
            return .urbanStreet
        }
        if text.contains("restaurant") || text.contains("dining") || text.contains("cafe") {
            return .restaurant
        }
        if text.contains("mountain") || text.contains("valley") || text.contains("cliff") {
            return .mountain
        }
        if text.contains("room") || text.contains("indoor") || text.contains("studio") {
            return .indoorCasual
        }

        return .unknown
    }
}
