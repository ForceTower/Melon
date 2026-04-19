import SwiftUI

struct SyncView: View {
    let name: String
    @State var viewModel: SyncViewModel

    private var progress: Double {
        guard !SYNC_STEPS.isEmpty else { return 0 }
        return min(1, Double(viewModel.doneKeys.count) / Double(SYNC_STEPS.count))
    }

    var body: some View {
        ZStack {
            UNESColor.darkBg.ignoresSafeArea()
            MeshGradientView(variant: .warm, intensity: 0.9).ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Text("◦ preparando seu semestre")
                    .font(UNESFont.sans(11, weight: .medium))
                    .tracking(2)
                    .textCase(.uppercase)
                    .foregroundStyle(Color.white.opacity(0.55))
                    .fadeInOnAppear()

                headline
                    .padding(.top, 14)
                    .fadeUpOnAppear(delay: 0.1)

                progressRing
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 26)

                stepList

                Text("Isso levará alguns segundos na sua conexão.")
                    .font(UNESFont.mono(10))
                    .tracking(1)
                    .foregroundStyle(Color.white.opacity(0.4))
                    .frame(maxWidth: .infinity)
                    .padding(.top, 20)
            }
            .padding(.horizontal, 28)
            .padding(.top, 73)
            .padding(.bottom, 26)
        }
        .onAppear { viewModel.start() }
    }

    private var headline: Text {
        Text("\(Text("Quase lá,\n").foregroundStyle(UNESColor.surfaceLight))\(Text("\(name).").italic().foregroundStyle(UNESColor.amber))")
            .font(UNESFont.serif(44))
            .tracking(-1.1)
    }

    @ViewBuilder
    private var progressRing: some View {
        ZStack {
            // dashed spinning outer ring
            TimelineView(.animation) { ctx in
                let t = ctx.date.timeIntervalSince1970
                let angle = Angle.degrees(t * 45) // 8s for 360°
                Circle()
                    .stroke(
                        Color.white.opacity(0.1),
                        style: StrokeStyle(lineWidth: 1, dash: [2, 4])
                    )
                    .rotationEffect(angle)
            }

            // progress arc
            Circle()
                .trim(from: 0, to: CGFloat(progress))
                .stroke(UNESColor.amber,
                        style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.6), value: progress)

            VStack(spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 0) {
                    Text("\(Int(progress * 100))")
                        .font(UNESFont.serif(44))
                        .tracking(-1.3)
                        .foregroundStyle(UNESColor.surfaceLight)
                    Text("%")
                        .font(UNESFont.serif(20))
                        .foregroundStyle(Color.white.opacity(0.55))
                }
                Text("\(viewModel.doneKeys.count)/\(SYNC_STEPS.count)")
                    .font(UNESFont.mono(9))
                    .tracking(1.6)
                    .textCase(.uppercase)
                    .foregroundStyle(Color.white.opacity(0.4))
            }
        }
        .frame(width: 140, height: 140)
    }

    @ViewBuilder
    private var stepList: some View {
        VStack(spacing: 0) {
            ForEach(Array(SYNC_STEPS.enumerated()), id: \.element.id) { i, step in
                let isDone    = viewModel.doneKeys.contains(step.key)
                let isActive  = viewModel.currentStep == i
                let isPending = i > viewModel.currentStep

                HStack(spacing: 12) {
                    stepIndicator(isDone: isDone, isActive: isActive)

                    Text(step.label)
                        .font(UNESFont.sans(14, weight: isActive ? .medium : .regular))
                        .tracking(-0.07)
                        .foregroundStyle(
                            isDone ? Color.white.opacity(0.55) : UNESColor.surfaceLight
                        )
                        .strikethrough(isDone, color: Color.white.opacity(0.3))

                    Spacer()
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .opacity(isPending ? 0.35 : 1)
                .animation(.easeInOut(duration: 0.3), value: isDone)
                .animation(.easeInOut(duration: 0.3), value: viewModel.currentStep)
            }
        }
        .padding(.vertical, 6)
        .padding(.horizontal, 4)
        .background {
            // Liquid Glass on iOS 26+, ultra-thin material fallback below.
            // Inlined (rather than using `.cardSurface`) because this card
            // sits on the dark onboarding mesh and wants the material backdrop
            // on older runtimes — the shared card modifier targets the
            // on-light card pattern and doesn't carry a material.
            if #available(iOS 26.0, *) {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color.clear)
                    .glassEffect(
                        .regular.tint(Color.white.opacity(0.06)),
                        in: RoundedRectangle(cornerRadius: 24, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .strokeBorder(Color.white.opacity(0.08), lineWidth: 1)
                    )
            } else {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color.white.opacity(0.06))
                    .background(
                        .ultraThinMaterial.opacity(0.35),
                        in: RoundedRectangle(cornerRadius: 24, style: .continuous)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .strokeBorder(Color.white.opacity(0.08), lineWidth: 1)
                    )
            }
        }
    }

    @ViewBuilder
    private func stepIndicator(isDone: Bool, isActive: Bool) -> some View {
        ZStack {
            if isDone {
                Circle()
                    .fill(UNESColor.amber)
                    .frame(width: 20, height: 20)
                    .transition(.scale.combined(with: .opacity))
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(UNESColor.darkBg)
            } else if isActive {
                SpinnerView(color: UNESColor.amber)
                    .frame(width: 14, height: 14)
            } else {
                Circle()
                    .fill(Color.white.opacity(0.3))
                    .frame(width: 6, height: 6)
            }
        }
        .frame(width: 20, height: 20)
    }
}
