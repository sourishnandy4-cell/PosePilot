import SwiftUI

struct ShutterButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .stroke(.white, lineWidth: 5)
                    .frame(width: 78, height: 78)
                Circle()
                    .fill(.white)
                    .frame(width: 62, height: 62)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Capture photo")
    }
}

struct PoseToggleButton: View {
    @Binding var isOn: Bool

    var body: some View {
        Button {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                isOn.toggle()
            }
        } label: {
            Label(isOn ? "AI Pose On" : "AI Pose", systemImage: isOn ? "figure.stand.line.dotted.figure.stand" : "figure.stand")
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(.white)
                .padding(.horizontal, 18)
                .padding(.vertical, 11)
                .background(isOn ? Color(red: 0.31, green: 0.67, blue: 1.0) : .black.opacity(0.58), in: Capsule())
                .overlay(Capsule().stroke(.white.opacity(0.18), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isOn ? "Disable AI Pose" : "Enable AI Pose")
    }
}

struct CameraControlsBar: View {
    let timerMode: Int
    let flipAction: () -> Void
    let timerAction: () -> Void

    var body: some View {
        HStack {
            Button(action: timerAction) {
                Label(timerMode == 0 ? "Timer" : "\(timerMode)s", systemImage: "timer")
                    .labelStyle(.iconOnly)
            }
            .accessibilityLabel(timerMode == 0 ? "Timer off" : "\(timerMode) second timer")

            Spacer()

            Button(action: flipAction) {
                Image(systemName: "camera.rotate")
            }
            .accessibilityLabel("Flip camera")
        }
        .font(.system(size: 19, weight: .semibold))
        .foregroundStyle(.white)
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(.black.opacity(0.28))
    }
}

struct PoseSelectorCarousel: View {
    let poses: [PoseDefinition]
    let selectedPose: PoseDefinition?
    let select: (PoseDefinition) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(poses) { pose in
                    Button {
                        select(pose)
                    } label: {
                        VStack(spacing: 6) {
                            Image(systemName: iconName(for: pose))
                                .font(.system(size: 22, weight: .semibold))
                                .frame(width: 58, height: 42)
                                .foregroundStyle(.white)
                            Text(pose.name)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                                .minimumScaleFactor(0.75)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .frame(width: 118, height: 82)
                        .background(selectedPose?.id == pose.id ? Color(red: 0.31, green: 0.67, blue: 1.0).opacity(0.82) : .black.opacity(0.56))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(.white.opacity(selectedPose?.id == pose.id ? 0.65 : 0.16), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private func iconName(for pose: PoseDefinition) -> String {
        if pose.sceneTags.contains("group") {
            return "person.2"
        }
        if pose.sceneTags.contains("action") {
            return "figure.run"
        }
        if pose.sceneTags.contains("formal") {
            return "person.crop.rectangle"
        }
        return "figure.stand"
    }
}

struct CountdownOverlay: View {
    let value: Int?

    var body: some View {
        if let value {
            Text("\(value)")
                .font(.system(size: 92, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .shadow(radius: 12)
                .transition(.scale.combined(with: .opacity))
                .accessibilityLabel("\(value)")
        }
    }
}

struct PoseHistoryGridView: View {
    let items: [PoseHistoryItem]
    let close: () -> Void

    var body: some View {
        NavigationStack {
            Group {
                if items.isEmpty {
                    ContentUnavailableView("No Poses Yet", systemImage: "photo.on.rectangle", description: Text("Captured pose sessions will appear here."))
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
                            ForEach(items) { item in
                                VStack(alignment: .leading, spacing: 8) {
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .fill(.linearGradient(colors: [.blue.opacity(0.65), .purple.opacity(0.5)], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .aspectRatio(1, contentMode: .fit)
                                        .overlay(Image(systemName: "figure.stand").font(.largeTitle).foregroundStyle(.white))
                                    Text(item.poseName)
                                        .font(.headline)
                                    Text("\(Int(item.score * 100))% match")
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(10)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("My Poses")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done", action: close)
                }
            }
        }
    }
}

struct OnboardingView: View {
    let finish: () -> Void

    var body: some View {
        TabView {
            onboardingPage("AI Pose", "Turn on AI Pose to get a live target silhouette for your scene.", "figure.stand.line.dotted.figure.stand")
            onboardingPage("Align", "Match your skeleton to the guide and follow the instruction banner.", "scope")
            onboardingPage("Capture", "Use the shutter, timer, or auto-capture when the match is perfect.", "camera")
                .overlay(alignment: .bottom) {
                    Button("Start") {
                        finish()
                    }
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 34)
                    .padding(.vertical, 13)
                    .background(Color(red: 0.31, green: 0.67, blue: 1.0), in: Capsule())
                    .padding(.bottom, 52)
                }
        }
        .tabViewStyle(.page)
        .background(.black)
    }

    private func onboardingPage(_ title: String, _ text: String, _ systemImage: String) -> some View {
        VStack(spacing: 24) {
            Image(systemName: systemImage)
                .font(.system(size: 82, weight: .thin))
                .foregroundStyle(Color(red: 0.31, green: 0.67, blue: 1.0))
            Text(title)
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
            Text(text)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.76))
                .padding(.horizontal, 32)
        }
    }
}
