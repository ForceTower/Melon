import SwiftUI

/// Editable row in the "suas avaliações" list. Label (editable, 5-char cap),
/// serif score field that accepts a comma-decimal 0–10, optional weight
/// stepper (weighted mode only), star toggle for the wildcard flag, and a
/// trailing delete button when more than one row exists.
struct FCGradeRow: View {
    @Binding var row: FCRow
    let weighted: Bool
    let canRemove: Bool
    let onRemove: () -> Void

    @FocusState private var scoreFocused: Bool

    /// Decimal-comma display binding. Parses input back into `row.score`,
    /// clamps to 0–10, swaps commas for dots on the way in.
    private var scoreText: Binding<String> {
        Binding(
            get: {
                guard let score = row.score else { return "" }
                return FinalCountdownMath.formatGrade(score)
            },
            set: { newValue in
                let cleaned = newValue
                    .replacingOccurrences(of: ",", with: ".")
                    .filter { "0123456789.".contains($0) }
                if cleaned.isEmpty {
                    row.score = nil
                    return
                }
                if let parsed = Double(cleaned) {
                    row.score = max(0, min(10, parsed))
                }
            }
        )
    }

    var body: some View {
        HStack(spacing: 8) {
            labelBlock

            TextField("—,—", text: scoreText)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(UNESFont.serif(20))
                .tracking(-0.2)
                .foregroundStyle(UNESColor.ink)
                .focused($scoreFocused)
                .frame(minWidth: 0)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .padding(.horizontal, 8)
                .background(
                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                        .fill(UNESColor.surface2)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                        .strokeBorder(scoreFocused ? UNESColor.accent : UNESColor.line, lineWidth: 1)
                )
                .animation(.easeOut(duration: 0.15), value: scoreFocused)

            if weighted {
                weightStepper
            }

            wildcardToggle

            if canRemove {
                Button(action: onRemove) {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .frame(width: 22, height: 28)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(10)
        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - Pieces

    private var labelBlock: some View {
        VStack(alignment: .leading, spacing: 1) {
            TextField("VA", text: Binding(
                get: { row.label },
                set: { row.label = String($0.prefix(5)) }
            ))
            .font(UNESFont.mono(11, weight: .semibold))
            .tracking(0.22)
            .foregroundStyle(UNESColor.ink)
            .textFieldStyle(.plain)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.characters)

            Text("AVAL.")
                .font(UNESFont.mono(8))
                .tracking(0.64)
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(width: 42, alignment: .leading)
    }

    private var weightStepper: some View {
        HStack(spacing: 0) {
            Button {
                row.weight = max(1, row.weight - 1)
            } label: {
                Image(systemName: "minus")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 18, height: 20)
            }
            .buttonStyle(.plain)

            Text("×\(row.weight)")
                .font(UNESFont.mono(10, weight: .semibold))
                .foregroundStyle(UNESColor.ink)
                .frame(minWidth: 16)

            Button {
                row.weight = min(9, row.weight + 1)
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 18, height: 20)
            }
            .buttonStyle(.plain)
        }
        .padding(1)
        .background(
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(UNESColor.surface2)
                .overlay(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .strokeBorder(UNESColor.line, lineWidth: 1)
                )
        )
    }

    private var wildcardToggle: some View {
        Button {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                row.wildcard.toggle()
            }
        } label: {
            Image(systemName: row.wildcard ? "star.fill" : "star")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(row.wildcard
                                 ? Color(red: 0x2D/255, green: 0x1B/255, blue: 0x4E/255)
                                 : UNESColor.ink4)
                .frame(width: 28, height: 28)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(row.wildcard
                              ? Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255)
                              : UNESColor.surface2)
                )
                .shadow(
                    color: row.wildcard
                        ? Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255).opacity(0.35)
                        : .clear,
                    radius: 6, x: 0, y: 3
                )
        }
        .buttonStyle(.plain)
    }
}
