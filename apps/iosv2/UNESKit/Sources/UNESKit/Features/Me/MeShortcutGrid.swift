import SwiftUI

/// The pinned shortcut tiles. Sized as a three-column grid so tiles keep
/// their proportions as more shortcuts land.
struct MeShortcutGrid: View {
    var countdown: SemesterCountdown?
    var onOpen: (MeShortcut) -> Void

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
            ForEach(MeShortcut.allCases) { shortcut in
                tile(shortcut)
            }
        }
    }

    private func tile(_ shortcut: MeShortcut) -> some View {
        Button {
            onOpen(shortcut)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: shortcut.icon)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 40, height: 40)
                    .background(shortcut.tone, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: shortcut.tone.opacity(0.33), radius: 6, y: 5)
                    .padding(.bottom, 11)
                Text(shortcut.label)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
                Text(hint(for: shortcut))
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 3)
            }
            .padding(EdgeInsets(top: 13, leading: 12, bottom: 12, trailing: 12))
            .frame(maxWidth: .infinity, minHeight: 100, alignment: .topLeading)
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

    private func hint(for shortcut: MeShortcut) -> String {
        switch shortcut {
        case .enrollment:
            "montar proposta"
        case .calendar:
            "datas acadêmicas"
        case .countdown:
            countdown.map { "semestre · \($0.weeksLeft) sem" } ?? "fim do semestre"
        }
    }
}

extension MeShortcut {
    var label: String {
        switch self {
        case .enrollment: "Matrícula"
        case .calendar: "Calendário"
        case .countdown: "Final Countdown"
        }
    }

    var icon: String {
        switch self {
        case .enrollment: "checklist"
        case .calendar: "calendar"
        case .countdown: "timer"
        }
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
    MeShortcutGrid(countdown: MeOverview.preview.countdown, onOpen: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
