import SwiftUI

/// Section header used inside the detail screen. Left side is the serif
/// label ("Notas", "Aulas", …); right side carries an optional monospaced
/// count / caption.
struct DisciplineSectionHeader<Trailing: View>: View {
    let title: String
    let trailing: () -> Trailing

    init(_ title: String, @ViewBuilder trailing: @escaping () -> Trailing = { EmptyView() }) {
        self.title = title
        self.trailing = trailing
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(UNESFont.serif(22).italic())
                .tracking(-0.22)
                .foregroundStyle(UNESColor.ink)
            Spacer(minLength: 10)
            trailing()
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 10)
    }
}

/// Small stat card used at the top of the detail screen.
struct DetailStatCard<Icon: View>: View {
    let label: String
    let value: String
    let sub: String?
    var color: Color = UNESColor.ink
    let icon: () -> Icon

    init(label: String, value: String, sub: String? = nil, color: Color = UNESColor.ink,
         @ViewBuilder icon: @escaping () -> Icon = { EmptyView() }) {
        self.label = label
        self.value = value
        self.sub = sub
        self.color = color
        self.icon = icon
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 6) {
                icon()
                Text(label)
                    .font(UNESFont.mono(9, weight: .semibold))
                    .tracking(1.08)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink4)
            }
            Text(value)
                .font(UNESFont.serif(26))
                .tracking(-0.52)
                .foregroundStyle(color)
                .padding(.top, 3)
            if let sub {
                Text(sub)
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
