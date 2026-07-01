import ComposableArchitecture
import SwiftUI

struct IntroView: View {
    @Bindable var store: StoreOf<IntroFeature>

    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()

            TabView(selection: $store.slide.sending(\.slideChanged)) {
                ForEach(IntroSlide.all) { slide in
                    IntroSlidePage(
                        slide: slide,
                        isLast: slide.id == IntroSlide.all.count - 1,
                        onContinue: { store.send(.continueTapped, animation: UNESMotion.ease(0.45)) }
                    )
                    .tag(slide.id)
                }
            }
            .pagedTabViewStyle()
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                pageDots
            }
            ToolbarItem(placement: .trailingCompat) {
                Button("Pular") { store.send(.skipTapped) }
                    .font(.system(size: 15, weight: .semibold))
                    .tint(UNESColor.ink3)
            }
        }
        .bareNavigationBar()
    }

    private var pageDots: some View {
        HStack(spacing: 6) {
            ForEach(0..<IntroFeature.slideCount, id: \.self) { index in
                Capsule()
                    .fill(index == store.slide ? UNESColor.accent : UNESColor.ink4.opacity(0.4))
                    .frame(width: index == store.slide ? 22 : 6, height: 6)
            }
        }
        .animation(UNESMotion.ease(0.4, overshoot: 1.2), value: store.slide)
    }
}

private struct IntroSlidePage: View {
    let slide: IntroSlide
    let isLast: Bool
    let onContinue: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            illustrationArea
                .frame(height: 344)

            Spacer(minLength: 0)

            Eyebrow(text: slide.eyebrow, color: slide.accent)
                .fadeUp(delay: 0.08, duration: 0.5)

            title
                .padding(.top, 12)
                .fadeUp(delay: 0.16, duration: 0.6)

            Text(slide.body)
                .font(.system(size: 16))
                .tracking(-0.08)
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 14)
                .fadeUp(delay: 0.26, duration: 0.6)

            Button(action: onContinue) {
                UNESButtonLabel(text: isLast ? "Entrar na conta" : "Continuar")
            }
            .buttonStyle(.unesDark)
            .padding(.top, 26)
            .fadeUp(delay: 0.36, duration: 0.6)
        }
        .padding(.horizontal, 28)
        .padding(.bottom, 12)
    }

    private var illustrationArea: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 34, style: .continuous)
                .fill(.clear)
                .overlay {
                    MeshView(variant: slide.variant, intensity: 0.62)
                        .overlay(UNESColor.surface.opacity(0.84))
                }
                .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
                .padding(26)
                .scaleIn(duration: 0.55)

            illustration
        }
    }

    @ViewBuilder
    private var illustration: some View {
        switch slide.illustration {
        case .schedule: ScheduleIllustration()
        case .grades: GradesIllustration()
        case .messages: MessagesIllustration()
        case .notifications: NotificationsIllustration()
        }
    }

    private var title: some View {
        VStack(alignment: .leading, spacing: -7) {
            ForEach(Array(slide.titleLines.enumerated()), id: \.offset) { _, line in
                titleLine(line)
            }
        }
    }

    private func titleLine(_ segments: [IntroSlide.Segment]) -> some View {
        segments.reduce(Text("")) { text, segment in
            text + Text(segment.text)
                .foregroundStyle(segment.accented ? slide.accent : UNESColor.ink)
        }
        .font(.system(size: 42, weight: .heavy))
        .tracking(-1.68)
    }
}

#Preview {
    NavigationStack {
        IntroView(
            store: Store(initialState: IntroFeature.State()) {
                IntroFeature()
            }
        )
    }
}
