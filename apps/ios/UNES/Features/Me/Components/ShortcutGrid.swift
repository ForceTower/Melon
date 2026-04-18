import SwiftUI

/// 3-column constellation of pinned shortcuts. The Me tab surfaces whatever
/// subset the user has pinned (managed via the Tweak panel in the design; on
/// iOS this will eventually hang off a "gerenciar" sheet).
struct ShortcutGrid: View {
    let shortcuts: [Shortcut]

    private let columns: [GridItem] = Array(
        repeating: GridItem(.flexible(), spacing: 8, alignment: .topLeading),
        count: 3
    )

    var body: some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(shortcuts) { shortcut in
                ShortcutTile(shortcut: shortcut)
            }
        }
    }
}

struct ShortcutTile: View {
    let shortcut: Shortcut

    var body: some View {
        Button {
            // Wiring to individual destinations will happen per-shortcut;
            // for now each tile is a press target with no action.
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                iconBadge
                    .padding(.bottom, 10)

                Text(shortcut.label)
                    .font(UNESFont.sans(12, weight: .semibold))
                    .tracking(-0.06)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .padding(.bottom, 3)

                Text(shortcut.hint)
                    .font(UNESFont.mono(9))
                    .tracking(0.36)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.top, 12)
            .padding(.bottom, 11)
            .frame(maxWidth: .infinity, minHeight: 94, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(UNESColor.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                    )
            )
        }
        .buttonStyle(PressScaleStyle())
    }

    private var iconBadge: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(shortcut.tone.background)
            Image(systemName: shortcut.systemImage)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(shortcut.tone.foreground)
        }
        .frame(width: 30, height: 30)
        .shadow(color: shortcut.tone.background.opacity(0.2), radius: 6, x: 0, y: 4)
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        ShortcutGrid(shortcuts: MeFixtures.pinned(from: MeFixtures.defaultPinned))
            .padding(14)
    }
}
