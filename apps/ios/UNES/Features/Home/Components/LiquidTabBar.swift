import SwiftUI

struct LiquidTabBar: View {
    @Binding var active: HomeTabKey
    @Namespace private var pill

    var body: some View {
        HStack(spacing: 0) {
            ForEach(HomeTabKey.allCases, id: \.rawValue) { tab in
                TabButton(
                    tab: tab,
                    isActive: active == tab,
                    pillNamespace: pill
                ) {
                    withAnimation(.spring(response: 0.45, dampingFraction: 0.78)) {
                        active = tab
                    }
                }
            }
        }
        .padding(6)
        .frame(height: 62)
        .modifier(TabBarChrome())
        .padding(.horizontal, 14)
    }
}

private struct TabButton: View {
    let tab: HomeTabKey
    let isActive: Bool
    let pillNamespace: Namespace.ID
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                if isActive {
                    Capsule(style: .continuous)
                        .fill(UNESColor.ink)
                        .matchedGeometryEffect(id: "pill", in: pillNamespace)
                }

                VStack(spacing: 2) {
                    Image(systemName: tab.icon)
                        .font(.system(size: 19, weight: isActive ? .semibold : .medium))
                    if isActive {
                        Text(tab.label.uppercased())
                            .font(UNESFont.sans(9, weight: .semibold))
                            .tracking(0.36)
                    }
                }
                .foregroundStyle(isActive ? UNESColor.surface : UNESColor.ink3)

                if let badge = tab.badge, !isActive {
                    Text("\(badge)")
                        .font(UNESFont.sans(9, weight: .bold))
                        .foregroundStyle(UNESColor.surfaceLight)
                        .frame(minWidth: 14, minHeight: 14)
                        .padding(.horizontal, 4)
                        .background(
                            Capsule().fill(UNESColor.accent)
                                .overlay(Capsule().strokeBorder(UNESColor.surface, lineWidth: 1.5))
                        )
                        .offset(x: 12, y: -12)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Floating pill chrome with Liquid Glass (iOS 26+) or blurred material fallback.
private struct TabBarChrome: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(
                    .regular.tint(UNESColor.surface.opacity(0.15)),
                    in: Capsule(style: .continuous)
                )
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(Color.white.opacity(0.6), lineWidth: 1)
                )
                .shadow(color: UNESColor.ink.opacity(0.14), radius: 20, y: 16)
                .shadow(color: UNESColor.ink.opacity(0.06), radius: 3, y: 2)
        } else {
            content
                .background(
                    Capsule(style: .continuous)
                        .fill(UNESColor.card.opacity(0.72))
                        .background(
                            .ultraThinMaterial,
                            in: Capsule(style: .continuous)
                        )
                        .overlay(
                            Capsule(style: .continuous)
                                .strokeBorder(Color.white.opacity(0.5), lineWidth: 1)
                        )
                )
                .shadow(color: UNESColor.ink.opacity(0.14), radius: 20, y: 16)
                .shadow(color: UNESColor.ink.opacity(0.06), radius: 3, y: 2)
        }
    }
}

#Preview {
    @Previewable @State var tab: HomeTabKey = .home
    return ZStack {
        UNESColor.surface.ignoresSafeArea()
        VStack {
            Spacer()
            LiquidTabBar(active: $tab)
                .padding(.bottom, 22)
        }
    }
}
