import SwiftUI

/// One license family: a header with the tone tile and the "N de M" pill,
/// then a package row per dependency, expandable in place.
struct LicenseGroupCard: View {
    let group: LicenseGroup
    var expandedID: LicensePackage.ID?
    var copiedID: LicensePackage.ID?
    var onToggle: (LicensePackage.ID) -> Void
    var onHomepage: (LicensePackage.ID) -> Void
    var onCopy: (LicensePackage.ID) -> Void
    var onLicenseText: (LicenseFamily) -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
                .overlay(alignment: .bottom) {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(height: 0.5)
                }

            ForEach(Array(group.packages.enumerated()), id: \.element.id) { position, package in
                LicenseRow(
                    package: package,
                    expanded: expandedID == package.id,
                    copied: copiedID == package.id,
                    onToggle: { onToggle(package.id) },
                    onHomepage: { onHomepage(package.id) },
                    onCopy: { onCopy(package.id) },
                    onLicenseText: { onLicenseText(package.family) }
                )
                .overlay(alignment: .bottom) {
                    if position < group.packages.count - 1 {
                        Rectangle()
                            .fill(UNESColor.line)
                            .frame(height: 0.5)
                    }
                }
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "doc.plaintext")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color(light: .white, dark: UNESColor.darkBg))
                .frame(width: 32, height: 32)
                .background(group.family.tone, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                .shadow(color: group.family.tone.opacity(0.33), radius: 6, y: 4)

            VStack(alignment: .leading, spacing: 1) {
                Text(group.family.rawValue)
                    .font(.system(size: 15.5, weight: .bold, design: .monospaced))
                    .tracking(-0.31)
                    .foregroundStyle(UNESColor.ink)
                Text(group.family.blurb)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(.licensesGroupCountOfTotal(group.packages.count, group.familyCount))
                .font(.system(size: 11.5, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
                .padding(EdgeInsets(top: 3, leading: 10, bottom: 3, trailing: 10))
                .background(UNESColor.surface2, in: Capsule())
        }
        .padding(EdgeInsets(top: 13, leading: 14, bottom: 11, trailing: 14))
    }
}

/// One dependency: the collapsed row, plus the copyright blurb and action
/// pills while expanded.
private struct LicenseRow: View {
    let package: LicensePackage
    var expanded: Bool
    var copied: Bool
    var onToggle: () -> Void
    var onHomepage: () -> Void
    var onCopy: () -> Void
    var onLicenseText: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: onToggle) {
                row
                    .contentShape(Rectangle())
            }
            .buttonStyle(RowPressStyle())

            if expanded {
                details
                    .transition(.opacity.combined(with: .offset(y: 10)))
            }
        }
    }

    private var row: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(package.family.tone)
                .frame(width: 8, height: 8)

            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .lastTextBaseline, spacing: 6) {
                    Text(package.name)
                        .font(.system(size: 13.5, weight: .semibold, design: .monospaced))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Text(package.version)
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundStyle(UNESColor.ink4)
                        .layoutPriority(1)
                }
                Text(package.author)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.down")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
                .rotationEffect(.degrees(expanded ? 180 : 0))
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: 9) {
            copyrightBlurb
                .padding(EdgeInsets(top: 12, leading: 13, bottom: 12, trailing: 13))
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))

            FlowLayout(spacing: 7, lineSpacing: 7) {
                LicensePill(icon: "arrow.up.right", label: package.homepage, primary: true, action: onHomepage)
                LicensePill(
                    icon: copied ? "checkmark" : "doc.on.doc",
                    label: copied ? String.localized(.licensesCopiedFeedback) : package.pin,
                    action: onCopy
                )
                LicensePill(
                    icon: "doc.plaintext",
                    label: String.localized(.licensesLicenseTextLabel(package.family.rawValue)),
                    action: onLicenseText
                )
            }

            Text(.licensesCategoryLabel(String.localized(package.category)))
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 1)
        }
        .padding(EdgeInsets(top: 0, leading: 14, bottom: 14, trailing: 14))
        .padding(.leading, 20)
    }

    private var copyrightBlurb: some View {
        (
            Text(.licensesCopyrightPrefix)
                + Text(package.author)
                .fontWeight(.semibold)
                .foregroundStyle(UNESColor.ink)
                + Text(.licensesCopyrightMiddle)
                + Text(package.family.rawValue)
                .fontWeight(.semibold)
                .foregroundStyle(UNESColor.ink)
                + Text(.licensesCopyrightSuffix)
        )
        .font(.system(size: 13.5, weight: .medium))
        .tracking(-0.14)
        .lineSpacing(3)
        .foregroundStyle(UNESColor.ink2)
    }
}

/// The expanded-row actions: accent-filled for the repository link,
/// outlined for copy and license text.
private struct LicensePill: View {
    var icon: String
    var label: String
    var primary = false
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .semibold))
                Text(label)
                    .font(.system(size: 12.5, weight: .semibold))
                    .tracking(-0.13)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .foregroundStyle(primary ? .white : UNESColor.ink2)
            .padding(EdgeInsets(
                top: primary ? 7 : 6,
                leading: primary ? 12 : 11,
                bottom: primary ? 7 : 6,
                trailing: primary ? 12 : 11
            ))
            .background(primary ? UNESColor.accent : UNESColor.card, in: Capsule())
            .overlay {
                if !primary {
                    Capsule().strokeBorder(UNESColor.line)
                }
            }
        }
        .buttonStyle(TilePressStyle())
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 14) {
            LicenseGroupCard(
                group: LicenseGroup(
                    family: .mit,
                    packages: Array(LicenseCatalog.packages.filter { $0.family == .mit }.prefix(4)),
                    familyCount: 13
                ),
                expandedID: "GRDB.swift",
                onToggle: { _ in },
                onHomepage: { _ in },
                onCopy: { _ in },
                onLicenseText: { _ in }
            )
            LicenseGroupCard(
                group: LicenseGroup(
                    family: .apache2,
                    packages: LicenseCatalog.packages.filter { $0.family == .apache2 },
                    familyCount: 2
                ),
                onToggle: { _ in },
                onHomepage: { _ in },
                onCopy: { _ in },
                onLicenseText: { _ in }
            )
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
