import ComposableArchitecture
import SwiftUI

struct SyncView: View {
    let store: StoreOf<SyncFeature>

    var body: some View {
        ZStack {
            backdrop
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Eyebrow(text: String.localized(.onboardingSyncEyebrow), color: .white.opacity(0.92), live: true)
                    .fadeUp(duration: 0.5)

                title
                    .padding(.top, 12)
                    .fadeUp(delay: 0.1, duration: 0.6)

                Spacer()

                progressRing
                    .frame(maxWidth: .infinity)

                Spacer()

                stepsCard
            }
            .padding(.horizontal, 24)
            .padding(.top, 44)
            .padding(.bottom, 12)
        }
        .navigationBarBackButtonHidden()
        .bareNavigationBar()
        .task { await store.send(.task).finish() }
    }

    private var backdrop: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .warm, intensity: 0.95)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.25), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.68), location: 1),
                ],
                angle: 175
            )
        }
    }

    private var title: some View {
        VStack(alignment: .leading, spacing: -6) {
            Text(.onboardingSyncAlmostThere)
                .foregroundStyle(UNESColor.paper)
            (
                Text(store.greeting).foregroundStyle(UNESColor.accent)
                    + Text(verbatim: ".").foregroundStyle(UNESColor.accent)
            )
            .lineLimit(1)
        }
        .font(.system(size: 40, weight: .heavy))
        .tracking(-1.6)
    }

    // MARK: Ring

    private var progressRing: some View {
        ZStack {
            OrbitingDashes()

            Circle()
                .stroke(.white.opacity(0.14), lineWidth: 8)
                .frame(width: 120, height: 120)

            Circle()
                .trim(from: 0, to: store.progress)
                .stroke(UNESColor.accent, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .frame(width: 120, height: 120)
                .animation(UNESMotion.settle(0.6), value: store.progress)

            VStack(spacing: 4) {
                percentLabel
                Text("\(store.completedSteps)/\(SyncFeature.Step.allCases.count)")
                    .font(.system(size: 10, weight: .bold))
                    .monospacedDigit()
                    .tracking(0.8)
                    .foregroundStyle(.white.opacity(0.5))
            }
        }
        .frame(width: 128, height: 128)
    }

    private var percentLabel: some View {
        (
            Text("\(Int((store.progress * 100).rounded()))")
                + Text("%").font(.system(size: 20, weight: .bold)).foregroundStyle(.white.opacity(0.6))
        )
        .font(.system(size: 44, weight: .heavy))
        .monospacedDigit()
        .tracking(-1.76)
        .foregroundStyle(.white)
        .contentTransition(.numericText())
        .animation(UNESMotion.settle(0.6), value: store.completedSteps)
    }

    /// The slowly revolving dashed halo around the ring.
    private struct OrbitingDashes: View {
        @State private var spinning = false
        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        var body: some View {
            Circle()
                .stroke(.white.opacity(0.1), style: StrokeStyle(lineWidth: 1, dash: [2, 5]))
                .frame(width: 128, height: 128)
                .rotationEffect(.degrees(spinning ? 360 : 0))
                .onAppear {
                    guard !reduceMotion else { return }
                    withAnimation(.linear(duration: 9).repeatForever(autoreverses: false)) {
                        spinning = true
                    }
                }
        }
    }

    // MARK: Steps

    private var stepsCard: some View {
        VStack(spacing: 0) {
            ForEach(SyncFeature.Step.allCases, id: \.rawValue) { step in
                stepRow(step, phase: store.state.phase(of: step))
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 6)
        .background {
            Color.white.opacity(0.08).background(.ultraThinMaterial)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(.white.opacity(0.1))
        }
    }

    private func stepRow(_ step: SyncFeature.Step, phase: SyncFeature.State.StepPhase) -> some View {
        HStack(spacing: 12) {
            ZStack {
                switch phase {
                case .done:
                    DrawnCheck()
                case .active:
                    SpinnerRing(
                        size: 15,
                        color: UNESColor.accent,
                        trackColor: .white.opacity(0.3),
                        speed: 0.7
                    )
                case .pending:
                    Circle()
                        .fill(.white.opacity(0.35))
                        .frame(width: 6, height: 6)
                }
            }
            .frame(width: 20, height: 20)

            Text(step.label)
                .font(.system(size: 15, weight: phase == .active ? .semibold : .medium))
                .tracking(-0.15)
                .foregroundStyle(phase == .done ? .white.opacity(0.5) : .white)

            Spacer(minLength: 0)
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 14)
        .opacity(phase == .pending ? 0.4 : 1)
        .animation(.easeInOut(duration: 0.3), value: phase == .pending)
    }
}

/// Accent-filled circle whose checkmark draws itself on appear.
struct DrawnCheck: View {
    var size: CGFloat = 20
    var color: Color = UNESColor.accent
    var strokeColor: Color = UNESColor.darkBg

    @State private var drawn = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle().fill(color)
            CheckShape()
                .trim(from: 0, to: drawn ? 1 : 0)
                .stroke(strokeColor, style: StrokeStyle(lineWidth: size * 0.11, lineCap: .round, lineJoin: .round))
        }
        .frame(width: size, height: size)
        .onAppear {
            guard !drawn else { return }
            if reduceMotion {
                drawn = true
            } else {
                withAnimation(.easeOut(duration: 0.4)) {
                    drawn = true
                }
            }
        }
    }
}

/// The v2 checkmark glyph, normalized to its container.
struct CheckShape: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.width * 0.275, y: rect.height * 0.5))
            path.addLine(to: CGPoint(x: rect.width * 0.425, y: rect.height * 0.65))
            path.addLine(to: CGPoint(x: rect.width * 0.725, y: rect.height * 0.35))
        }
    }
}

#Preview {
    NavigationStack {
        SyncView(
            store: Store(initialState: SyncFeature.State(greeting: "mariana.souza")) {
                SyncFeature()
            } withDependencies: {
                $0.syncRepository = .previewValue
                $0.profileRepository = .previewValue
            }
        )
    }
}
