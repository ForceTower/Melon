import SwiftUI

/// "Licenças open source" — full list of bundled third-party licenses, loaded
/// from `com.mono0926.LicensePlist.plist` (generated at build time by the
/// `license-plist` Run Script phase). Mirrors the Settings screen's editorial
/// chrome: warm mesh wash behind the header, eyebrow + serif title, and a
/// single grouped card holding the rows. Tap-through opens the full license
/// text in `LicenseDetailView`.
///
/// The handoff bundle (`UNES Licenses.html`) referenced a `screens-licenses.jsx`
/// that didn't ship in the export, so we reuse the layout vocabulary already
/// established by the sibling Settings screen instead of inventing new
/// primitives.
struct LicensesView: View {
    /// Snapshot of the bundled licenses, taken once on view creation. The
    /// plist is a build artifact — it can't change between view appearances
    /// inside a single launch — so there's no flow/observer to subscribe to.
    private let entries: [LicenseEntry]

    init(entries: [LicenseEntry]? = nil) {
        self.entries = entries ?? LicensePlistLoader.load()
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Same warm mesh treatment Settings/FinalCountdown use — pinned
            // behind the header, fading into the surface so the editorial
            // type reads cleanly below.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.25)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.5),
                            .init(color: UNESColor.surface, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 300)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    LicensesHeader(totalPackages: entries.count)
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    VStack(spacing: 22) {
                        listSection
                            .fadeUpOnAppear(delay: 0.12, distance: 12, duration: 0.55)

                        emptyStateOrFootnote
                            .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        // Match Settings: keep the system back chevron and pop gesture, only
        // hide the bar's background so the warm mesh continues behind the
        // header.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var listSection: some View {
        VStack(spacing: 0) {
            SettingsSectionHeader(
                eyebrow: "índice",
                title: "Bibliotecas",
                meta: "ordem alfabética"
            )

            if entries.isEmpty {
                emptyCard
            } else {
                groupedCard
            }
        }
    }

    /// Single grouped card holding every row, separator-style — same chrome
    /// as `SettingsCard`. We render rows in alphabetical order rather than
    /// the plist's native order so the index tile feels stable across builds
    /// (license-plist's order is dependency-graph order, which jitters when
    /// the resolved file changes).
    private var groupedCard: some View {
        let ordered = entries.sorted { lhs, rhs in
            lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }

        return VStack(spacing: 0) {
            ForEach(Array(ordered.enumerated()), id: \.element.id) { index, entry in
                NavigationLink(value: entry) {
                    LicenseRowLabel(index: index + 1, entry: entry)
                }
                .buttonStyle(LicensePressStyle())

                if index < ordered.count - 1 {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(height: 1)
                }
            }
        }
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .navigationDestination(for: LicenseEntry.self) { entry in
            LicenseDetailView(entry: entry)
        }
    }

    /// Card shown before the build artifact has been generated (e.g. a fresh
    /// checkout that hasn't run the Run Script phase yet). Keeps the screen
    /// from looking broken in that window.
    private var emptyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Sem licenças bundladas")
                .font(UNESFont.serif(18))
                .tracking(-0.18)
                .foregroundStyle(UNESColor.ink)

            Text("Rode o build uma vez para gerar com.mono0926.LicensePlist.plist em UNES/Resources/Licenses/.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    /// Closing footnote that sits below the list — same vibe as the
    /// SettingsFooter signature line, but scoped to this screen.
    private var emptyStateOrFootnote: some View {
        VStack(spacing: 6) {
            HStack(spacing: 6) {
                Circle().fill(UNESColor.accent).frame(width: 5, height: 5)
                Text("obrigado")
                    .font(UNESFont.serif(16, italic: true))
                    .tracking(-0.16)
                    .foregroundStyle(UNESColor.ink2)
                Circle().fill(UNESColor.accent).frame(width: 5, height: 5)
            }

            Text("GERADO POR LICENSE-PLIST · ATUALIZADO A CADA BUILD")
                .font(UNESFont.mono(9))
                .tracking(1.62)
                .foregroundStyle(UNESColor.ink4)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }
}

/// `LicenseRow` renders a tappable button itself; the grouped-card list above
/// needs a `NavigationLink` instead, so we extract the label-only content
/// into its own view rather than nesting buttons.
private struct LicenseRowLabel: View {
    let index: Int
    let entry: LicenseEntry

    var body: some View {
        HStack(spacing: 13) {
            ZStack {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(UNESColor.surface2)
                Text(String(format: "%02d", index))
                    .font(UNESFont.mono(11, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(width: 30, height: 30)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.title)
                    .font(UNESFont.sans(14, weight: .medium))
                    .tracking(-0.07)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)

                Text(entry.identifier ?? "◦ ARQUIVO DE LICENÇA")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.38)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
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
}

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
        NavigationStack {
            LicensesView(entries: [
                LicenseEntry(title: "Firebase", identifier: "Apache-2.0",
                             body: "Apache License\nVersion 2.0"),
                LicenseEntry(title: "leveldb", identifier: "BSD-3-Clause",
                             body: "Copyright (c) 2011 The LevelDB Authors."),
                LicenseEntry(title: "Promises", identifier: "Apache-2.0",
                             body: "Apache License\nVersion 2.0"),
            ])
        }
    }
#endif
