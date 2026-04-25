import SwiftUI

/// Single library row in the Licenças list. Card-row pattern shared with
/// `SettingsCard` — leading mono-numbered index tile, title + license-id
/// subtitle in the middle, right-aligned chevron. Composes into a grouped
/// card by the parent.
struct LicenseRow: View {
    /// 1-based position in the visible list. Rendered in the icon tile to
    /// give the column some rhythm without inventing per-library colors.
    let index: Int
    let entry: LicenseEntry
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 13) {
                indexTile

                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.title)
                        .font(UNESFont.sans(14, weight: .medium))
                        .tracking(-0.07)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)

                    if let identifier = entry.identifier {
                        Text(identifier)
                            .font(UNESFont.mono(9.5))
                            .tracking(0.38)
                            .foregroundStyle(UNESColor.ink4)
                            .lineLimit(1)
                    } else {
                        Text("◦ ARQUIVO DE LICENÇA")
                            .font(UNESFont.mono(9.5))
                            .tracking(0.38)
                            .foregroundStyle(UNESColor.ink4)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(LicensePressStyle())
    }

    private var indexTile: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(UNESColor.surface2)
            Text(String(format: "%02d", index))
                .font(UNESFont.mono(11, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(width: 30, height: 30)
    }
}

/// Same warm-neutral press feedback `SettingsCard` uses, kept private so it
/// doesn't leak as a generic style.
private struct LicensePressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed
                    ? UNESColor.surface2.opacity(0.6)
                    : Color.clear
            )
    }
}

#if DEBUG
    #Preview {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            VStack(spacing: 0) {
                LicenseRow(
                    index: 1,
                    entry: LicenseEntry(
                        title: "Firebase",
                        identifier: "Apache-2.0",
                        body: "Apache License Version 2.0"
                    ),
                    onTap: {}
                )
                Rectangle().fill(UNESColor.line).frame(height: 1)
                LicenseRow(
                    index: 2,
                    entry: LicenseEntry(
                        title: "leveldb",
                        identifier: nil,
                        body: "BSD-style license."
                    ),
                    onTap: {}
                )
            }
            .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .padding(16)
        }
    }
#endif
