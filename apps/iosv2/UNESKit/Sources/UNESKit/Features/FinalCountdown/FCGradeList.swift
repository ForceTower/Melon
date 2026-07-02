import SwiftUI

/// The "Modo ponderado" switch row above the grade list.
struct FCWeightedRow: View {
    var weighted: Bool
    var onChange: (Bool) -> Void

    var body: some View {
        HStack(spacing: 13) {
            Image(systemName: "scalemass")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 30, height: 30)
                .background(Color(hex: 0x2AA5B8), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

            VStack(alignment: .leading, spacing: 1) {
                Text("Modo ponderado")
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(weighted ? "pesos ativos · ajuste ×" : "média simples · todas valem igual")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle("Modo ponderado", isOn: Binding(get: { weighted }, set: { onChange($0) }))
                .labelsHidden()
                .tint(UNESColor.liveGreen)
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// The editable evaluation list: label + grade fields per row, an optional
/// weight stepper, remove buttons, and the trailing "Adicionar avaliação".
struct FCGradeList: View {
    var rows: [FCRow]
    var weighted: Bool
    var onScore: (FCRow.ID, String) -> Void
    var onLabel: (FCRow.ID, String) -> Void
    var onWeight: (FCRow.ID, Int) -> Void
    var onRemove: (FCRow.ID) -> Void
    var onAdd: () -> Void

    @FocusState private var focusedScore: FCRow.ID?

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { position, row in
                gradeRow(row)
                    .overlay(alignment: .bottom) {
                        if position < rows.count - 1 {
                            Rectangle().fill(UNESColor.line).frame(height: 0.5)
                        }
                    }
            }
            addButton
                .overlay(alignment: .top) {
                    Rectangle().fill(UNESColor.line).frame(height: 0.5)
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

    // MARK: Row

    private func gradeRow(_ row: FCRow) -> some View {
        HStack(spacing: 11) {
            TextField("Nome", text: Binding(get: { row.label }, set: { onLabel(row.id, $0) }))
                .textFieldStyle(.plain)
                .autocorrectionDisabled()
                .font(.system(size: 15, weight: .semibold))
                .tracking(-0.15)
                .foregroundStyle(UNESColor.ink)
                .frame(width: 46)

            scoreField(row)

            if weighted {
                weightStepper(row)
            }

            if rows.count > 1 {
                Button {
                    onRemove(row.id)
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .frame(width: 30, height: 30)
                        .contentShape(Rectangle().inset(by: -7))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Remover \(row.label)")
            }
        }
        .padding(EdgeInsets(top: 10, leading: 14, bottom: 10, trailing: 14))
    }

    private func scoreField(_ row: FCRow) -> some View {
        TextField("—", text: Binding(get: { row.scoreText }, set: { onScore(row.id, $0) }))
            .textFieldStyle(.plain)
            .decimalKeyboard()
            .focused($focusedScore, equals: row.id)
            .multilineTextAlignment(.center)
            .font(.system(size: 22, weight: .bold))
            .tracking(-0.44)
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink)
            .padding(EdgeInsets(top: 9, leading: 10, bottom: 9, trailing: 10))
            .frame(maxWidth: .infinity)
            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(
                        focusedScore == row.id ? UNESColor.accent : .clear,
                        lineWidth: 1.5
                    )
                    .animation(.easeOut(duration: 0.15), value: focusedScore == row.id)
            }
    }

    private func weightStepper(_ row: FCRow) -> some View {
        HStack(spacing: 2) {
            stepButton(icon: "minus", label: "Diminuir peso") { onWeight(row.id, -1) }
            Text("×\(row.weight)")
                .font(.system(size: 14, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
                .frame(minWidth: 24)
            stepButton(icon: "plus", label: "Aumentar peso") { onWeight(row.id, +1) }
        }
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func stepButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
                // 36pt keeps the pill the height the old 30 + 3 padding gave;
                // the inset stretches the touch target to ~44pt.
                .frame(width: 36, height: 36)
                .contentShape(Rectangle().inset(by: -4))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    // MARK: Add

    private var addButton: some View {
        Button(action: onAdd) {
            HStack(spacing: 9) {
                Image(systemName: "plus")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(UNESColor.accent)
                    .frame(width: 24, height: 24)
                    .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                Text("Adicionar avaliação")
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.accent)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 13, leading: 16, bottom: 13, trailing: 16))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: 12) {
        FCWeightedRow(weighted: true, onChange: { _ in })
        FCGradeList(
            rows: [
                FCRow(id: "a", label: "VA1", scoreText: "6,5"),
                FCRow(id: "b", label: "VA2", scoreText: "5,2", weight: 2),
                FCRow(id: "c", label: "Trab"),
            ],
            weighted: true,
            onScore: { _, _ in },
            onLabel: { _, _ in },
            onWeight: { _, _ in },
            onRemove: { _ in },
            onAdd: {}
        )
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
