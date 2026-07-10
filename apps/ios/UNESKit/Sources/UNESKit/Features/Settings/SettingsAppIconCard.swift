import SwiftUI

/// The "Ícone do app" card: the current icon up top, a horizontally
/// scrolling tile row below, and locked tiles for the secret icons until
/// their easter eggs are found.
struct SettingsAppIconCard: View {
    var selected: AppIcon
    var unlocked: AppIconSet
    var onSelect: (AppIcon) -> Void
    var onLockedTap: (AppIcon) -> Void

    private var lockedCount: Int {
        AppIcon.secrets.count { !unlocked.contains($0) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
                .padding(.horizontal, 16)
            iconRow
        }
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var header: some View {
        HStack(spacing: 11) {
            SettingsAppIconArt(icon: selected, size: 30)

            VStack(alignment: .leading, spacing: 1) {
                Text(.settingsAppIconTitle)
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(.settingsAppIconSubtitle)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if lockedCount > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "lock")
                        .font(.system(size: 10, weight: .semibold))
                    Text(.settingsAppIconSecretCount(lockedCount))
                        .font(.system(size: 11.5, weight: .semibold))
                        .monospacedDigit()
                }
                .foregroundStyle(UNESColor.ink4)
                .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                .background(UNESColor.surface2, in: Capsule())
            }
        }
    }

    private var iconRow: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 6) {
                ForEach(AppIcon.allCases) { icon in
                    tile(icon)
                }
            }
            .padding(EdgeInsets(top: 2, leading: 13, bottom: 4, trailing: 13))
        }
        .scrollIndicators(.hidden)
    }

    private func tile(_ icon: AppIcon) -> some View {
        let locked = icon.isSecret && !unlocked.contains(icon)
        let active = icon == selected && !locked
        return Button {
            locked ? onLockedTap(icon) : onSelect(icon)
        } label: {
            VStack(spacing: 8) {
                ZStack(alignment: .topTrailing) {
                    Group {
                        if locked {
                            lockedTile
                        } else {
                            SettingsAppIconArt(icon: icon, size: 60)
                                .shadow(color: Color(hex: 0x141020, opacity: 0.18), radius: 6, y: 4)
                        }
                    }
                    .padding(3)
                    .background {
                        if active {
                            RoundedRectangle(cornerRadius: 19, style: .continuous)
                                .fill(UNESColor.accent)
                        }
                    }

                    if active {
                        checkBadge
                            .offset(x: 1, y: -1)
                    }
                }

                Text(locked ? String.localized(.settingsAppIconLocked) : icon.label)
                    .font(.system(size: 11.5, weight: active ? .semibold : .medium))
                    .tracking(-0.12)
                    .foregroundStyle(active ? UNESColor.ink : UNESColor.ink3)
                    .lineLimit(1)
            }
            .frame(width: 72)
        }
        .buttonStyle(.plain)
    }

    private var lockedTile: some View {
        RoundedRectangle(cornerRadius: 15, style: .continuous)
            .fill(UNESColor.surface3)
            .frame(width: 60, height: 60)
            .overlay {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .strokeBorder(UNESColor.line, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            }
            .overlay {
                Image(systemName: "lock")
                    .font(.system(size: 19, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
    }

    private var checkBadge: some View {
        Image(systemName: "checkmark")
            .font(.system(size: 9, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 20, height: 20)
            .background(UNESColor.accent, in: Circle())
            .overlay {
                Circle().strokeBorder(UNESColor.card, lineWidth: 2)
            }
    }
}

extension AppIcon {
    var label: String {
        switch self {
        case .aurora: String.localized(.settingsAppIconAurora)
        case .ocean: String.localized(.settingsAppIconOcean)
        case .stripped: String.localized(.settingsAppIconStripped)
        case .baseSans: String.localized(.settingsAppIconBaseSans)
        case .paper: String.localized(.settingsAppIconPaper)
        case .nowShip: String.localized(.settingsAppIconNowShip)
        }
    }

    /// The unlock-sheet blurb — only secret icons carry one.
    var secretDescription: String? {
        switch self {
        case .aurora, .ocean, .stripped: nil
        case .baseSans: String.localized(.settingsAppIconBaseSansDesc)
        case .paper: String.localized(.settingsAppIconPaperDesc)
        case .nowShip: String.localized(.settingsAppIconNowShipDesc)
        }
    }

    /// The cryptic toast when a locked tile is tapped — nudges toward the
    /// icon's own discovery path without giving it away.
    var lockedHint: String {
        switch self {
        case .aurora, .ocean, .stripped, .baseSans:
            String.localized(.settingsAppIconHintBaseSans)
        case .paper:
            String.localized(.settingsAppIconHintPaper)
        case .nowShip:
            String.localized(.settingsAppIconHintNowShip)
        }
    }
}

#Preview {
    @Previewable @State var selected = AppIcon.aurora
    VStack(spacing: 12) {
        SettingsAppIconCard(
            selected: selected,
            unlocked: AppIconSet(),
            onSelect: { selected = $0 },
            onLockedTap: { _ in }
        )
        SettingsAppIconCard(
            selected: selected,
            unlocked: AppIconSet(Set(AppIcon.secrets)),
            onSelect: { selected = $0 },
            onLockedTap: { _ in }
        )
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
