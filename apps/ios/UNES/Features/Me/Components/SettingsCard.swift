import SwiftUI

/// Quiet settings list that lives below the shortcut grid. Each row is an
/// icon tile + label/hint + optional status pill + chevron. Mirrors the
/// native iOS "grouped" list visually while keeping the app's custom type
/// stack and mesh-aware surfaces.
struct SettingsCard: View {
    let rows: [MeSettingsRow]
    var onTap: (MeSettingsRow) -> Void = { _ in }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                SettingsRowButton(row: row, onTap: { onTap(row) })

                if index < rows.count - 1 {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(height: 1)
                }
            }
        }
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct SettingsRowButton: View {
    let row: MeSettingsRow
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 13) {
                iconTile

                VStack(alignment: .leading, spacing: 2) {
                    Text(row.label)
                        .font(UNESFont.sans(14, weight: .medium))
                        .tracking(-0.07)
                        .foregroundStyle(UNESColor.ink)

                    Text(row.hint)
                        .font(UNESFont.mono(9.5))
                        .tracking(0.38)
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if row.statusOK {
                    statusPill
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressHighlightStyle())
    }

    private var iconTile: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(UNESColor.surface2)
            Image(systemName: row.systemImage)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
        }
        .frame(width: 30, height: 30)
    }

    private var statusPill: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(MeColors.okGreen)
                .frame(width: 6, height: 6)
            Text("OK")
                .font(UNESFont.mono(9, weight: .medium))
                .tracking(0.72)
                .foregroundStyle(MeColors.okGreen)
        }
    }
}

/// List-row press feedback: a soft surface-2 highlight instead of the default
/// blue tint, so it matches the warm neutral palette.
private struct PressHighlightStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed
                    ? UNESColor.surface2.opacity(0.6)
                    : Color.clear
            )
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        SettingsCard(rows: MeFixtures.settingsRows(syncHint: "última: há 2 min"))
            .padding(14)
    }
}
