import SwiftUI

/// Small eyebrow-style section label used between the hero card and each
/// service block on the Me screen. Optional trailing action reproduces the
/// "gerenciar →" affordance from the prototype.
struct MeSectionLabel: View {
    let label: String
    var actionLabel: String? = nil
    var action: () -> Void = {}

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("◦ \(label)")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)

            Spacer()

            if let actionLabel {
                Button(action: action) {
                    HStack(spacing: 3) {
                        Text(actionLabel)
                            .font(UNESFont.sans(11, weight: .medium))
                            .tracking(-0.06)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 9, weight: .semibold))
                    }
                    .foregroundStyle(UNESColor.ink3)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 8)
        .padding(.top, 6)
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        VStack {
            MeSectionLabel(label: "atalhos fixados", actionLabel: "gerenciar")
            MeSectionLabel(label: "definições")
            Spacer()
        }
        .padding()
    }
}
