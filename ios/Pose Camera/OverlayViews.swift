import SwiftUI
import Vision

enum PoseDrawing {
    static let connections: [(String, String)] = [
        ("nose", "neck"),
        ("neck", "leftShoulder"), ("neck", "rightShoulder"),
        ("leftShoulder", "leftElbow"), ("leftElbow", "leftWrist"),
        ("rightShoulder", "rightElbow"), ("rightElbow", "rightWrist"),
        ("neck", "leftHip"), ("neck", "rightHip"),
        ("leftHip", "rightHip"),
        ("leftHip", "leftKnee"), ("leftKnee", "leftAnkle"),
        ("rightHip", "rightKnee"), ("rightKnee", "rightAnkle")
    ]

    static func point(_ normalized: CGPoint, in size: CGSize) -> CGPoint {
        CGPoint(x: normalized.x * size.width, y: normalized.y * size.height)
    }
}

struct SilhouetteOverlayView: View {
    let targetPose: PoseDefinition
    let mirrored: Bool
    let isMatched: Bool

    var body: some View {
        Canvas { context, size in
            var points: [String: CGPoint] = [:]

            for (key, joint) in targetPose.targetJoints {
                let normalized = CGPoint(x: mirrored ? 1.0 - joint.x : joint.x, y: joint.y)
                points[key] = PoseDrawing.point(normalized, in: size)
            }

            context.addFilter(.shadow(color: .cyan.opacity(0.55), radius: isMatched ? 12 : 7))

            for connection in PoseDrawing.connections {
                guard let start = points[connection.0], let end = points[connection.1] else { continue }
                var path = Path()
                path.move(to: start)
                path.addQuadCurve(to: end, control: CGPoint(x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 - 6))
                context.stroke(path, with: .color(.white.opacity(0.58)), lineWidth: 4)
            }

            for point in points.values {
                let rect = CGRect(x: point.x - 5, y: point.y - 5, width: 10, height: 10)
                context.fill(Path(ellipseIn: rect), with: .color(.cyan.opacity(0.75)))
            }
        }
        .opacity(0.95)
        .scaleEffect(isMatched ? 1.025 : 1.0)
        .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true), value: isMatched)
        .transition(.opacity.animation(.easeIn(duration: 0.4)))
    }
}

struct SkeletonOverlayView: View {
    let skeleton: DetectedSkeleton?

    var body: some View {
        Canvas { context, size in
            guard let skeleton else { return }

            let pointsByName = Dictionary(uniqueKeysWithValues: skeleton.joints.map { key, value in
                (key.rawValue.rawValue, PoseDrawing.point(value, in: size))
            })

            for connection in PoseDrawing.connections {
                guard let start = pointsByName[connection.0], let end = pointsByName[connection.1] else { continue }
                var path = Path()
                path.move(to: start)
                path.addLine(to: end)
                context.stroke(path, with: .color(.green.opacity(0.75)), lineWidth: 2)
            }

            for point in pointsByName.values {
                context.fill(Path(ellipseIn: CGRect(x: point.x - 3, y: point.y - 3, width: 6, height: 6)), with: .color(.green))
            }
        }
        .allowsHitTesting(false)
    }
}

struct MatchScoreIndicator: View {
    let result: PoseMatchResult?

    private var score: Float {
        result?.score ?? 0
    }

    private var color: Color {
        switch score {
        case 0.7...:
            return Color(red: 0.20, green: 0.83, blue: 0.60)
        case 0.4..<0.7:
            return Color(red: 0.98, green: 0.75, blue: 0.14)
        default:
            return Color(red: 0.97, green: 0.44, blue: 0.44)
        }
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(.white.opacity(0.18), lineWidth: 8)
            Circle()
                .trim(from: 0, to: CGFloat(score))
                .stroke(color, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 0.3), value: score)
            Text("\(Int(score * 100))")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .shadow(radius: 1)
        }
        .frame(width: 72, height: 72)
        .accessibilityLabel("Pose match \(Int(score * 100)) percent")
    }
}

struct PoseInstructionBanner: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .medium, design: .rounded))
            .foregroundStyle(.white)
            .lineLimit(2)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(.black.opacity(0.62), in: Capsule())
            .overlay(Capsule().stroke(.white.opacity(0.18), lineWidth: 1))
            .shadow(radius: 8)
            .transition(.move(edge: .bottom).combined(with: .opacity))
            .accessibilityAddTraits(.updatesFrequently)
    }
}

struct PerfectMatchBurstView: View {
    let isActive: Bool

    var body: some View {
        TimelineView(.animation) { timeline in
            Canvas { context, size in
                guard isActive else { return }
                let time = timeline.date.timeIntervalSinceReferenceDate
                let center = CGPoint(x: size.width / 2, y: size.height / 2)

                for index in 0..<18 {
                    let angle = Double(index) / 18.0 * .pi * 2.0
                    let radius = 70 + CGFloat(sin(time * 4 + Double(index)) * 18)
                    let point = CGPoint(x: center.x + cos(angle) * radius, y: center.y + sin(angle) * radius)
                    let alpha = 0.35 + 0.35 * sin(time * 3 + Double(index))
                    context.fill(
                        Path(ellipseIn: CGRect(x: point.x - 3, y: point.y - 3, width: 6, height: 6)),
                        with: .color(.white.opacity(alpha))
                    )
                }
            }
        }
        .allowsHitTesting(false)
    }
}
