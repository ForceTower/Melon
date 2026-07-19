import ComposableArchitecture
import SwiftUI

/// The story player: auto-advancing cards with animated
/// segment bars, tap right/left to move, press-and-hold to pause, glass
/// chrome, and the once-per-semester announce reveal on top.
struct RetrospectiveView: View {
    @Bindable var store: StoreOf<RetrospectiveFeature>
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var pressing = false

    var body: some View {
        ZStack {
            Color(hex: 0x0B0712).ignoresSafeArea()
            if let deck = store.deck {
                card(deck)
                    .id(store.index)
                    .transition(.opacity.combined(with: .scale(scale: 1.04)))
                pressLayer
                chrome
                if store.isShowingAnnounce {
                    RetroAnnounceView(deck: deck) {
                        store.send(.announceEnterTapped, animation: UNESMotion.ease(0.5))
                    }
                    .transition(.opacity)
                    .zIndex(2)
                }
            } else {
                emptyState
            }
        }
        .animation(UNESMotion.ease(0.5), value: store.index)
        .environment(\.colorScheme, .dark)
        #if !os(watchOS)
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(.hidden, for: .tabBar)
        #endif
        .sheet(
            isPresented: Binding(
                get: { store.isSharePresented },
                set: { if !$0 { store.send(.shareDismissed) } }
            )
        ) {
            if let deck = store.deck {
                RetroShareSheet(deck: deck, card: store.currentCard, firstName: store.firstName)
            }
        }
        .onAppear { store.send(.reduceMotionChanged(reduceMotion)) }
        .task { await store.send(.task).finish() }
    }

    // MARK: Card

    @ViewBuilder
    private func card(_ deck: RetrospectiveDeck) -> some View {
        switch store.currentCard {
        case .abertura:
            RetroCardAbertura(deck: deck)
        case .notas:
            if let grades = deck.grades { RetroCardNotas(grades: grades) }
        case .frequencia:
            if let attendance = deck.attendance { RetroCardFrequencia(attendance: attendance) }
        case .conquista:
            if let victory = deck.victory { RetroCardConquista(victory: victory) }
        case .score:
            if let score = deck.score { RetroCardScore(score: score) }
        case .turma:
            if let turma = deck.turma { RetroCardTurma(turma: turma) }
        case .encerramento:
            RetroCardEncerramento(deck: deck) { store.send(.shareTapped) }
        }
    }

    // MARK: Tap / hold navigation

    /// Leaves the top chrome and the bottom action band free: 96pt below
    /// the top, 116pt above the bottom.
    private var pressLayer: some View {
        GeometryReader { proxy in
            Color.clear
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { _ in
                            guard !pressing else { return }
                            pressing = true
                            store.send(.pressBegan)
                        }
                        .onEnded { value in
                            pressing = false
                            store.send(.pressEnded(
                                fraction: value.location.x / max(1, proxy.size.width),
                                moved: abs(value.translation.width) > 12
                            ))
                        }
                )
                .padding(.top, 96)
                // The closing card's share button sits higher than the
                // usual action band — leave it reachable.
                .padding(.bottom, store.currentCard == .encerramento ? 220 : 116)
        }
    }

    // MARK: Chrome

    private var chrome: some View {
        VStack(spacing: 12) {
            TimelineView(.animation(paused: store.isPaused)) { context in
                HStack(spacing: 5) {
                    ForEach(store.cards.indices, id: \.self) { index in
                        GeometryReader { proxy in
                            ZStack(alignment: .leading) {
                                Capsule().fill(.white.opacity(0.28))
                                Capsule()
                                    .fill(.white)
                                    .frame(width: proxy.size.width * fill(for: index, at: context.date))
                            }
                        }
                        .frame(height: 3)
                    }
                }
            }
            HStack {
                RetroGlassButton(systemName: "xmark") { store.send(.closeTapped) }
                Spacer()
                if store.isPaused, !store.isSharePresented, !store.isShowingAnnounce {
                    Text(String.localized(.retroPaused).uppercased())
                        .font(.system(size: 12, weight: .bold))
                        .tracking(0.7)
                        .foregroundStyle(.white.opacity(0.7))
                        .padding(.horizontal, 11)
                        .padding(.vertical, 6)
                        .background(.white.opacity(0.14), in: Capsule())
                        .transition(.opacity)
                }
                RetroGlassButton(systemName: "square.and.arrow.up") { store.send(.shareTapped) }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .frame(maxHeight: .infinity, alignment: .top)
        .animation(UNESMotion.ease(0.3), value: store.isPaused)
    }

    private func fill(for index: Int, at now: Date) -> Double {
        if index < store.index { return 1 }
        guard index == store.index else { return 0 }
        if store.reduceMotion { return 1 }
        var elapsed = store.elapsedBeforePause
        if !store.isPaused, let startedAt = store.cardStartedAt {
            elapsed += now.timeIntervalSince(startedAt)
        }
        return min(1, elapsed / RetrospectiveFeature.cardDuration)
    }

    private var emptyState: some View {
        VStack(spacing: 10) {
            Text(.retroEmptyTitle)
                .font(.system(size: 26, weight: .bold))
                .foregroundStyle(.white)
            Text(.retroEmptySubtitle)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white.opacity(0.6))
                .multilineTextAlignment(.center)
            Button { store.send(.closeTapped) } label: {
                Text(.retroEmptyClose)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 22)
                    .padding(.vertical, 10)
                    .background(.white.opacity(0.14), in: Capsule())
            }
            .padding(.top, 10)
        }
        .padding(.horizontal, 32)
    }
}

/// The fullscreen staged reveal — shown once per semester before the
/// player starts.
struct RetroAnnounceView: View {
    var deck: RetrospectiveDeck
    var onEnter: () -> Void

    var body: some View {
        ZStack {
            Color(hex: 0x0A0612).ignoresSafeArea()
            MeshView(variant: .warm).ignoresSafeArea()
            RadialGradient(
                colors: [.clear, Color(hex: 0x06040C, opacity: 0.76)],
                center: UnitPoint(x: 0.5, y: 0.1),
                startRadius: 80,
                endRadius: 700
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                RetroUnesMark(size: 22)
                    .fadeUp(delay: 0.15)
                Text(.retroAnnounceLead)
                    .font(.system(size: 19, weight: .semibold))
                    .tracking(-0.2)
                    .foregroundStyle(.white.opacity(0.86))
                    .padding(.top, 22)
                    .fadeUp(delay: 0.34)
                Text(String.localized(.retroAnnounceKicker).uppercased())
                    .font(.system(size: 15, weight: .bold))
                    .tracking(4.8)
                    .foregroundStyle(.white.opacity(0.6))
                    .padding(.top, 12)
                    .fadeUp(delay: 0.52)
                Text(deck.semesterLabel)
                    .font(.system(size: 76, weight: .heavy))
                    .tracking(-4)
                    .foregroundStyle(.white)
                    .shadow(color: .black.opacity(0.4), radius: 20, y: 12)
                    .padding(.top, 6)
                    .fadeUp(delay: 0.68)
                Text(.retroAnnounceSub(deck.glance.disciplines, deck.glance.classHours))
                    .font(.system(size: 15.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 290)
                    .padding(.top, 20)
                    .fadeUp(delay: 0.9)
                Button(action: onEnter) {
                    HStack(spacing: 9) {
                        Text(.retroAnnounceEnter)
                            .font(.system(size: 16.5, weight: .bold))
                            .tracking(-0.2)
                        Image(systemName: "arrow.right")
                            .font(.system(size: 14, weight: .bold))
                    }
                    .foregroundStyle(Color(hex: 0x140F1C))
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(.white, in: Capsule())
                    .shadow(color: .black.opacity(0.36), radius: 20, y: 16)
                }
                .padding(.top, 34)
                .fadeUp(delay: 1.12)
                Text(.retroAnnounceHint)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.5))
                    .padding(.top, 16)
                    .fadeUp(delay: 1.28)
            }
            .padding(.horizontal, 32)

            Button(action: onEnter) {
                Text(.retroAnnounceSkip)
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.14), in: Capsule())
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            .padding(.top, 14)
            .padding(.trailing, 18)
        }
        .environment(\.colorScheme, .dark)
    }
}

#Preview {
    RetrospectiveView(
        store: Store(initialState: RetrospectiveFeature.State(semesterCode: "20261")) {
            RetrospectiveFeature()
        }
    )
}
