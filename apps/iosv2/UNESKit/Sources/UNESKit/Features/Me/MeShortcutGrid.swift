import SwiftUI

/// The pinned shortcut tiles. Three equal-width columns wrapping into rows,
/// and every tile stretches to match the tallest in its row so each row
/// reads as a uniform grid.
struct MeShortcutGrid: View {
    var shortcuts: [MeShortcut] = MeShortcut.allCases
    var onOpen: (MeShortcut) -> Void

    private static let columns = 3

    var body: some View {
        VStack(spacing: 10) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(alignment: .top, spacing: 10) {
                    ForEach(row) { shortcut in
                        tile(shortcut)
                    }
                    ForEach(0..<(Self.columns - row.count), id: \.self) { _ in
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var rows: [[MeShortcut]] {
        stride(from: 0, to: shortcuts.count, by: Self.columns).map {
            Array(shortcuts[$0..<min($0 + Self.columns, shortcuts.count)])
        }
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
        case .certificate: .localized(.meShortcutCertificate)
        case .history: .localized(.meShortcutHistory)
        }
    }

    var hint: String? {
        switch self {
        case .enrollment: .localized(.meShortcutEnrollmentHint)
        case .calendar: .localized(.meShortcutCalendarHint)
        case .countdown: nil
        case .certificate: .localized(.meShortcutCertificateHint)
        case .history: .localized(.meShortcutHistoryHint)
        }
    }

    var icon: String {
        switch self {
        case .enrollment: "checklist"
        case .calendar: "calendar"
        case .countdown: "timer"
        case .certificate: "doc.text"
        case .history: "chart.bar.doc.horizontal"
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
        case .certificate: UNESColor.readable(0x0A84FF)
        case .history: UNESColor.readable(0x7A5AD0)
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
