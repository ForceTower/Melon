import SwiftUI

/// Full-text view of a single bundled license. Pushed from `LicensesView` via
/// the standard navigation stack so the system back chevron and pop gesture
/// stay native. Header keeps the editorial vocabulary (eyebrow + serif title);
/// the license body itself sits in a single card with monospaced text so the
/// fixed-width formatting common in license headers (rule lines, indented
/// "TERMS AND CONDITIONS" blocks) survives the transfer.
struct LicenseDetailView: View {
    let entry: LicenseEntry

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Same warm wash as the list — keeps the screens tonally
            // related when the user pushes between them.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.2)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.5),
                            .init(color: UNESColor.surface, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 240)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 18) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    licenseCard
                        .fadeUpOnAppear(delay: 0.12, distance: 12, duration: 0.55)
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
                .padding(.bottom, 32)
            }
        }
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Spacer()

                Text("◦ \(entry.identifier ?? "LICENÇA").")
                    .font(UNESFont.mono(9.5))
                    .tracking(1.33)
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.bottom, 10)

            Text("◦ BIBLIOTECA")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 8)

            Text(entry.title)
                .font(UNESFont.serif(34, italic: true))
                .tracking(-0.6)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var licenseCard: some View {
        Text(entry.body.isEmpty ? "Esta biblioteca não publicou um texto de licença." : entry.body)
            .font(UNESFont.mono(11))
            .lineSpacing(3)
            .foregroundStyle(UNESColor.ink2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .textSelection(.enabled)
            .padding(18)
            .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

#if DEBUG
    #Preview {
        NavigationStack {
            LicenseDetailView(entry: LicenseEntry(
                title: "Firebase",
                identifier: "Apache-2.0",
                body: """
                                       Apache License
                                  Version 2.0, January 2004
                               http://www.apache.org/licenses/

                  TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

                  1. Definitions.
                """
            ))
        }
    }
#endif
