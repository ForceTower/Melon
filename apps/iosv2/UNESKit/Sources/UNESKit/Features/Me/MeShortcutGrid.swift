import SwiftUI

/// The pinned shortcut tiles. Equal-width columns, and every tile stretches
/// to match the tallest one so the row reads as a uniform grid.
struct MeShortcutGrid: View {
    var shortcuts: [MeShortcut] = MeShortcut.allCases
    var onOpen: (MeShortcut) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            ForEach(shortcuts) { shortcut in
                tile(shortcut)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func tile(_ shortcut: MeShortcut) -> some View {
        Button {
            onOpen(shortcut)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    Image(systemName: shortcut.icon)
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(.white)
                        .frame(width: 40, height: 40)
                        .background(shortcut.tone, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .shadow(color: shortcut.tone.opacity(0.33), radius: 6, y: 5)
                    if shortcut.isBeta {
                        Spacer(minLength: 6)
                        BetaTag()
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 11)
                Text(shortcut.label)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
                if let hint = shortcut.hint {
                    Text(hint)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 3)
                }
            }
            .padding(EdgeInsets(top: 13, leading: 12, bottom: 12, trailing: 12))
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .frame(minHeight: 100)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 8, y: 6)
        }
        .buttonStyle(TilePressStyle())
    }
}

extension MeShortcut {
    var label: String {
        switch self {
        case .enrollment: .localized(.meShortcutEnrollment)
        case .calendar: .localized(.meShortcutCalendar)
        case .countdown: .localized(.meShortcutCountdown)
        }
    }

    var hint: String? {
        switch self {
        case .enrollment: .localized(.meShortcutEnrollmentHint)
        case .calendar: .localized(.meShortcutCalendarHint)
        case .countdown: nil
        }
    }

    var icon: String {
        switch self {
        case .enrollment: "checklist"
        case .calendar: "calendar"
        case .countdown: "timer"
        }
    }

    var isBeta: Bool {
        self == .enrollment
    }

    var tone: Color {
        switch self {
        case .enrollment: UNESColor.readable(0x3B9EAE)
        case .calendar: UNESColor.readable(0xE85D4E)
        case .countdown: UNESColor.readable(0xB23A7A)
        }
    }
}

/// The v2 `.eu-press` treatment: tiles settle to 97%.
struct TilePressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

#Preview {
    MeShortcutGrid(onOpen: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
