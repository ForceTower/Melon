import SwiftUI

/// Top chrome of the Licenças screen. Mirrors the editorial header treatment
/// `SettingsHeader` uses: meta strip on top, eyebrow, then a serif title with
/// the second half italicised in the accent color, and a short body
/// paragraph below. The system nav bar's chevron is kept for the back gesture
/// — there's no custom back pill, only the right-aligned package count.
struct LicensesHeader: View {
    let totalPackages: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Spacer()

                Text("◦ \(totalPackages) PACOTES")
                    .font(UNESFont.mono(9.5))
                    .tracking(1.33)
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.bottom, 10)

            Text("◦ ATRIBUIÇÃO · OPEN SOURCE")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 8)

            (
                Text("Lic").foregroundStyle(UNESColor.ink)
                + Text("enças").font(UNESFont.serif(40, italic: true)).foregroundStyle(UNESColor.accent)
            )
            .font(UNESFont.serif(40))
            .tracking(-0.8)

            Text("Pequenas obras de gente que não conhecemos pessoalmente, mas que tornam o UNES possível. Toque para ler a licença completa.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
                .padding(.top, 10)
                .frame(maxWidth: 290, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 4)
        .padding(.bottom, 18)
    }
}

#if DEBUG
    #Preview {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            LicensesHeader(totalPackages: 126)
        }
    }
#endif
