import SwiftUI

struct NowCard: View {
    let now: HomeNowClass

    private var countdownLabel: String {
        let h = now.startsIn / 60
        let m = now.startsIn % 60
        return h > 0 ? "\(h)h \(m)min" : "\(m)min"
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Always-dark base + animated mesh
            Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
            MeshGradientView(variant: now.meshVariant, intensity: 1)

            // Dim veil for contrast, matches the CSS gradient.
            LinearGradient(
                stops: [
                    .init(color: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.1),  location: 0),
                    .init(color: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.55), location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(UNESColor.amber)
                            .frame(width: 6, height: 6)
                            .pulseForever()
                        Text("próxima aula · em \(countdownLabel)")
                            .font(UNESFont.mono(10))
                            .tracking(1.8)
                            .textCase(.uppercase)
                            .foregroundStyle(Color.white.opacity(0.7))
                    }
                    Spacer()
                    Text(now.time)
                        .font(UNESFont.mono(10))
                        .tracking(0.8)
                        .foregroundStyle(Color.white.opacity(0.55))
                }

                Text(now.title)
                    .font(UNESFont.serif(30))
                    .tracking(-0.45)
                    .lineSpacing(1)
                    .foregroundStyle(UNESColor.surfaceLight)
                    .padding(.top, 14)

                if let topic = now.topic {
                    HStack(alignment: .center, spacing: 8) {
                        Image(systemName: "text.alignleft")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(Color.white.opacity(0.55))
                        Text(topic)
                            .font(UNESFont.sans(13))
                            .italic()
                            .foregroundStyle(Color.white.opacity(0.78))
                            .lineLimit(1)
                            .truncationMode(.tail)
                    }
                    .padding(.top, 8)
                }

                Divider()
                    .background(Color.white.opacity(0.15))
                    .padding(.top, 18)

                HStack(spacing: 16) {
                    NowMetaRow(systemIcon: "building.2", label: now.room, shrinks: false)

                    Rectangle()
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 1, height: 14)

                    NowMetaRow(
                        systemIcon: "person",
                        label: now.prof.replacingOccurrences(of: "Prof. ", with: ""),
                        shrinks: true
                    )

                    Spacer(minLength: 0)
                }
                .padding(.top, 14)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color.black.opacity(0.12), radius: 16, y: 12)
    }
}

private struct NowMetaRow: View {
    let systemIcon: String
    let label: String
    let shrinks: Bool

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: systemIcon)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.55))
            Text(label)
                .font(UNESFont.sans(12))
                .foregroundStyle(Color.white.opacity(0.85))
                .lineLimit(1)
                .truncationMode(.tail)
                .fixedSize(horizontal: !shrinks, vertical: false)
        }
        .layoutPriority(shrinks ? 0 : 1)
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        NowCard(now: HomeFixtures.nowClass)
            .padding(14)
    }
}
