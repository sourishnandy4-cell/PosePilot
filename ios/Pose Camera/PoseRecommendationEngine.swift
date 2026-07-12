import Foundation
import Combine

final class PoseRecommendationEngine: ObservableObject {
    @Published var recommendations: [PoseDefinition] = []

    private let library: [PoseDefinition]
    private let defaults: UserDefaults
    private let recentKey = "recentPoseIDs"

    init(library: [PoseDefinition] = PoseLibrary.all, defaults: UserDefaults = .standard) {
        self.library = library
        self.defaults = defaults
    }

    @discardableResult
    func recommend(for scene: SceneContext, peopleCount: Int) -> [PoseDefinition] {
        let tags = Set(scene.tags)
        let groupTag = peopleCount > 1 ? "group" : "solo"
        let recentIDs = Set(defaults.stringArray(forKey: recentKey) ?? [])

        let filtered = library.filter { pose in
            let poseTags = Set(pose.sceneTags)
            return !poseTags.isDisjoint(with: tags) && (poseTags.contains(groupTag) || peopleCount == 1)
        }

        let sorted = (filtered.isEmpty ? library : filtered).sorted { lhs, rhs in
            let lhsPenalty = recentIDs.contains(lhs.id) ? 40 : 0
            let rhsPenalty = recentIDs.contains(rhs.id) ? 40 : 0
            return lhs.popularityScore - lhsPenalty > rhs.popularityScore - rhsPenalty
        }

        let result = Array(sorted.prefix(5))
        DispatchQueue.main.async {
            self.recommendations = result
        }
        return result
    }

    func markUsed(_ pose: PoseDefinition) {
        var ids = defaults.stringArray(forKey: recentKey) ?? []
        ids.removeAll { $0 == pose.id }
        ids.insert(pose.id, at: 0)
        defaults.set(Array(ids.prefix(5)), forKey: recentKey)
    }
}
