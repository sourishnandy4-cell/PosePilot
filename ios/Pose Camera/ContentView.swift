import AVFoundation
import SwiftUI

struct ContentView: View {
    @StateObject private var camera = CameraManager()
    @StateObject private var poseDetector = PoseDetector()
    @StateObject private var engine = PoseRecommendationEngine()

    private let sceneAnalyzer = SceneAnalyzer()
    private let matcher = PoseMatcher()

    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false
    @State private var isPoseActive = false
    @State private var selectedPose: PoseDefinition?
    @State private var matchResult: PoseMatchResult?
    @State private var timerMode = 0
    @State private var countdownValue: Int?
    @State private var showHistory = false
    @State private var historyItems: [PoseHistoryItem] = []
    @State private var stablePerfectStart: Date?
    @State private var isAutoCaptureRunning = false

    var body: some View {
        ZStack {
            CameraPreviewView(session: camera.session, isMirrored: camera.cameraPosition == .front)
                .ignoresSafeArea()

            if camera.authorizationStatus != .authorized {
                permissionView
            }

            if isPoseActive, let selectedPose {
                SilhouetteOverlayView(
                    targetPose: selectedPose,
                    mirrored: camera.cameraPosition == .front,
                    isMatched: (matchResult?.score ?? 0) > 0.8
                )
                .ignoresSafeArea()
            }

            SkeletonOverlayView(skeleton: poseDetector.detectedSkeleton)
                .ignoresSafeArea()

            PerfectMatchBurstView(isActive: (matchResult?.score ?? 0) > 0.9)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                CameraControlsBar(
                    timerMode: timerMode,
                    flipAction: camera.toggleCamera,
                    timerAction: advanceTimerMode
                )
                .padding(.top, 4)

                HStack {
                    Spacer()
                    MatchScoreIndicator(result: matchResult)
                        .padding(.top, 18)
                        .padding(.trailing, 18)
                }

                Spacer()

                if isPoseActive {
                    PoseInstructionBanner(text: matchResult?.topInstruction ?? selectedPose?.instructions.first ?? "Step into frame")
                        .padding(.bottom, 10)

                    PoseSelectorCarousel(
                        poses: engine.recommendations,
                        selectedPose: selectedPose,
                        select: { pose in
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                selectedPose = pose
                            }
                        }
                    )
                    .frame(height: 92)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                PoseToggleButton(isOn: $isPoseActive)
                    .padding(.top, 8)
                    .onChange(of: isPoseActive) { isActive in
                        handlePoseToggle(isActive)
                    }

                HStack {
                    Button {
                        loadHistory()
                        showHistory = true
                    } label: {
                        Image(systemName: "photo.on.rectangle")
                            .font(.system(size: 24, weight: .semibold))
                            .frame(width: 58, height: 58)
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("My Poses")

                    Spacer()

                    ShutterButton {
                        captureWithTimerIfNeeded()
                    }

                    Spacer()

                    Button {
                        isPoseActive.toggle()
                    } label: {
                        Image(systemName: "sparkles")
                            .font(.system(size: 24, weight: .semibold))
                            .frame(width: 58, height: 58)
                            .foregroundStyle(isPoseActive ? Color(red: 0.31, green: 0.67, blue: 1.0) : .white)
                    }
                    .accessibilityLabel("AI Pose")
                }
                .padding(.horizontal, 34)
                .padding(.top, 12)
                .padding(.bottom, 22)
            }

            CountdownOverlay(value: countdownValue)
        }
        .background(.black)
        .task {
            camera.startSession()
            loadHistory()
            if engine.recommendations.isEmpty {
                let recommendations = engine.recommend(for: .unknown, peopleCount: 1)
                selectedPose = recommendations.first
            }
        }
        .onDisappear {
            camera.stopSession()
        }
        .onChange(of: camera.currentPixelBuffer) { pixelBuffer in
            guard let pixelBuffer else { return }
            poseDetector.processFrame(pixelBuffer, mirrored: camera.cameraPosition == .front)
        }
        .onChange(of: poseDetector.detectedSkeleton?.confidence ?? 0) { confidence in
            if confidence > 0.25 {
                HapticsManager.shared.skeletonDetected()
            }
            updateMatchResult()
        }
        .onChange(of: selectedPose) { _ in
            updateMatchResult()
        }
        .onChange(of: matchResult?.score ?? 0) { score in
            HapticsManager.shared.updateScore(score)
            updateAutoCapture(score: score)
        }
        .sheet(isPresented: $showHistory) {
            PoseHistoryGridView(items: historyItems) {
                showHistory = false
            }
        }
        .fullScreenCover(isPresented: Binding(get: { !hasSeenOnboarding }, set: { hasSeenOnboarding = !$0 })) {
            OnboardingView {
                hasSeenOnboarding = true
            }
        }
    }

    private var permissionView: some View {
        VStack(spacing: 14) {
            Image(systemName: "camera.fill")
                .font(.largeTitle)
            Text("Camera Access Needed")
                .font(.headline)
            Text("Enable camera permission in Settings to use live pose guidance.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.white)
        .padding(28)
        .background(.black.opacity(0.72), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .padding()
    }

    private func handlePoseToggle(_ isActive: Bool) {
        guard isActive else {
            stablePerfectStart = nil
            matchResult = nil
            return
        }

        Task {
            let scene: SceneContext
            if let pixelBuffer = camera.currentPixelBuffer {
                scene = await sceneAnalyzer.analyzeScene(from: pixelBuffer)
            } else {
                scene = .unknown
            }

            let recommendations = engine.recommend(for: scene, peopleCount: 1)
            await MainActor.run {
                selectedPose = recommendations.first
                updateMatchResult()
            }
        }
    }

    private func updateMatchResult() {
        guard isPoseActive, let skeleton = poseDetector.detectedSkeleton, let selectedPose else {
            matchResult = nil
            return
        }

        let result = matcher.match(
            detected: skeleton,
            target: selectedPose,
            mirrored: camera.cameraPosition == .front
        )
        matchResult = result

        if UIAccessibility.isVoiceOverRunning, result.score < 0.9 {
            UIAccessibility.post(notification: .announcement, argument: result.topInstruction)
        }
    }

    private func updateAutoCapture(score: Float) {
        guard isPoseActive, score > 0.92, !isAutoCaptureRunning else {
            if score <= 0.92 {
                stablePerfectStart = nil
            }
            return
        }

        if stablePerfectStart == nil {
            stablePerfectStart = Date()
        }

        guard let stablePerfectStart,
              Date().timeIntervalSince(stablePerfectStart) >= 1.5 else {
            return
        }

        isAutoCaptureRunning = true
        Task {
            await runCountdown(seconds: 3)
            captureNow()
            await MainActor.run {
                isAutoCaptureRunning = false
                self.stablePerfectStart = nil
            }
        }
    }

    private func advanceTimerMode() {
        switch timerMode {
        case 0:
            timerMode = 3
        case 3:
            timerMode = 5
        case 5:
            timerMode = 10
        default:
            timerMode = 0
        }
    }

    private func captureWithTimerIfNeeded() {
        let seconds = timerMode
        Task {
            if seconds > 0 {
                await runCountdown(seconds: seconds)
            }
            captureNow()
        }
    }

    @MainActor
    private func runCountdown(seconds: Int) async {
        for value in stride(from: seconds, through: 1, by: -1) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                countdownValue = value
            }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }

        withAnimation(.easeOut(duration: 0.2)) {
            countdownValue = nil
        }
    }

    private func captureNow() {
        if let pose = selectedPose {
            engine.markUsed(pose)
            saveHistoryItem(poseName: pose.name, score: matchResult?.score ?? 0)
        }
        camera.capturePhoto()
    }

    private func saveHistoryItem(poseName: String, score: Float) {
        let item = PoseHistoryItem(id: UUID(), poseName: poseName, score: score, capturedAt: Date())
        historyItems.insert(item, at: 0)
        let encoded = try? JSONEncoder().encode(Array(historyItems.prefix(50)))
        UserDefaults.standard.set(encoded, forKey: "poseHistoryItems")
    }

    private func loadHistory() {
        guard let data = UserDefaults.standard.data(forKey: "poseHistoryItems"),
              let decoded = try? JSONDecoder().decode([PoseHistoryItem].self, from: data) else {
            historyItems = []
            return
        }
        historyItems = decoded
    }
}

#Preview {
    ContentView()
}
