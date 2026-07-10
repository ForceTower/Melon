import SwiftUI

/// The rotating university-pulse hero: one curated fact at a time over the
/// living mesh, auto-advancing until the user picks a dot.
struct ParadoxoPulseHero: View {
    var facts: [ParadoxoPulseFact]
    var onOpen: (ParadoxoPulseFact) -> Void

    @State private var index = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let rotation: Duration = .milliseconds(4600)

    var body: some View {
        if let fact = facts[safe: index] ?? facts.first {
            Button {
                onOpen(fact)
            } label: {
                content(fact)
            }
            .buttonStyle(TilePressStyle())
            .task(id: facts.count) {
                guard facts.count > 1, !reduceMotion else { return }
                while !Task.isCancelled {
                    try? await Task.sleep(for: Self.rotation)
                    guard !Task.isCancelled else { return }
                    withAnimation(UNESMotion.ease(0.5)) {
                        index = (index + 1) % facts.count
                    }
                }
            }
        }
    }

    private func content(_ fact: ParadoxoPulseFact) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            header(fact)
            factBody(fact)
                .id(fact.id)
                .transition(.asymmetric(
                    insertion: .offset(y: 14).combined(with: .opacity),
                    removal: .opacity
                ))
        }
        .padding(EdgeInsets(top: 16, leading: 20, bottom: 18, trailing: 20))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            ZStack {
                UNESColor.darkBg
                MeshView(variant: fact.kind.mesh)
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.scrim.opacity(0.12), location: 0),
                        .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                    ],
                    angle: 155
                )
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.3), radius: 20, y: 12)
    }

    private func header(_ fact: ParadoxoPulseFact) -> some View {
        HStack {
            HStack(spacing: 7) {
                LiveDot(color: fact.kind.tone)
                Text(fact.kind.label)
                    .textCase(.uppercase)
                    .font(.system(size: 11.5, weight: .bold))
                    .tracking(0.69)
            }
            .foregroundStyle(.white.opacity(0.9))
            Spacer()
            HStack(spacing: 4) {
                ForEach(Array(facts.enumerated()), id: \.element.id) { dot, _ in
                    Capsule()
                        .fill(dot == index ? .white : .white.opacity(0.35))
                        .frame(width: dot == index ? 15 : 5, height: 5)
                        .onTapGesture {
                            withAnimation(UNESMotion.ease(0.4)) { index = dot }
                        }
                }
            }
        }
    }

    private func factBody(_ fact: ParadoxoPulseFact) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom, spacing: 14) {
                Text(ParadoxoFormat.metric(of: fact))
                    .font(.system(size: 66, weight: .bold))
                    .monospacedDigit()
                    .tracking(-3.3)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(fact.title)
                    .font(.system(size: 18, weight: .bold))
                    .tracking(-0.36)
                    .foregroundStyle(.white)
                    .lineLimit(3)
                    .padding(.bottom, 6)
            }
            .padding(.top, 16)

            HStack(spacing: 12) {
                Text(fact.subtitle)
                    .font(.system(size: 13.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 4) {
                    Text(.paradoxoPulseExplore)
                        .font(.system(size: 13, weight: .semibold))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 10, weight: .bold))
                }
                .foregroundStyle(.white)
            }
            .padding(.top, 13)
            .overlay(alignment: .top) {
                Rectangle()
                    .fill(.white.opacity(0.16))
                    .frame(height: 1)
            }
            .padding(.top, 16)
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#Preview("Pulso") {
    ParadoxoPulseHero(facts: ParadoxoOverview.preview().pulse, onOpen: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
