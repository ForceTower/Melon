import SwiftUI

/// What the sandbox is simulating: a discipline (color badge, name,
/// professor + semester) or "modo livre" hypotheticals, plus "Trocar" when
/// there's somewhere else to go.
struct FCDisciplineRow: View {
    let discipline: FCDiscipline?
    var canChange: Bool
    var onChange: () -> Void

    var body: some View {
        HStack(spacing: 13) {
            badge

            VStack(alignment: .leading, spacing: 1) {
                Text(discipline?.name ?? String.localized(.finalCountdownDisciplineFreeMode))
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .lineLimit(1)
                    .foregroundStyle(UNESColor.ink)
                Text(subtitle)
                    .font(.system(size: 12.5, weight: .medium))
                    .lineLimit(1)
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if canChange {
                Button(action: onChange) {
                    Text(.finalCountdownDisciplineChange)
                        .font(.system(size: 13, weight: .semibold))
                        .tracking(-0.13)
                        .foregroundStyle(UNESColor.accent)
                        .padding(.horizontal, 13)
                        .padding(.vertical, 7)
                        .background(UNESColor.surface2, in: Capsule())
                }
                .buttonStyle(.plain)
            }
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

    @ViewBuilder
    private var badge: some View {
        if let discipline {
            let color = UNESColor.disciplineReadableColor(discipline.colorIndex)
            Text(discipline.shortLabel)
                .font(.system(size: 13, weight: .bold))
                .tracking(-0.13)
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(color, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .shadow(color: color.opacity(0.27), radius: 7, y: 6)
        } else {
            Image(systemName: "sparkles")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(UNESColor.slate, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .shadow(color: UNESColor.slate.opacity(0.27), radius: 7, y: 6)
        }
    }

    private var subtitle: String {
        guard let discipline else { return String.localized(.finalCountdownDisciplineFreeModeSubtitle) }
        return [
            discipline.teacherName,
            discipline.semesterCode.map(DisciplinesFormat.semesterLabel),
        ]
        .compactMap(\.self)
        .joined(separator: " · ")
    }
}

/// The "Trocar" sheet: modo livre on top, then every current-semester
/// discipline with its released count, checkmark on what's being simulated.
struct FCDisciplinePicker: View {
    var choices: [DisciplineSummary]
    var selectedId: String?
    /// Nil is "modo livre".
    var onPick: (String?) -> Void
    var onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                VStack(spacing: 0) {
                    freeModeRow
                        .overlay(alignment: .bottom) {
                            if !choices.isEmpty {
                                Rectangle().fill(UNESColor.line).frame(height: 0.5)
                            }
                        }
                    ForEach(Array(choices.enumerated()), id: \.element.id) { position, choice in
                        row(choice)
                            .overlay(alignment: .bottom) {
                                if position < choices.count - 1 {
                                    Rectangle().fill(UNESColor.line).frame(height: 0.5)
                                }
                            }
                    }
                }
                .padding(.top, 14)
            }
            .padding(EdgeInsets(top: 24, leading: 18, bottom: 24, trailing: 18))
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(.finalCountdownPickerTitle)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                Text(.finalCountdownPickerSubtitle)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    /// A blank slate — hypotheticals with no discipline attached.
    private var freeModeRow: some View {
        Button {
            onPick(nil)
        } label: {
            HStack(spacing: 13) {
                Image(systemName: "sparkles")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(UNESColor.slate, in: RoundedRectangle(cornerRadius: 10, style: .continuous))

                VStack(alignment: .leading, spacing: 1) {
                    Text(.finalCountdownDisciplineFreeMode)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.23)
                        .foregroundStyle(UNESColor.ink)
                    Text(.finalCountdownPickerFreeModeDescription)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if selectedId == nil {
                    Image(systemName: "checkmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.accent)
                }
            }
            .padding(.vertical, 11)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func row(_ choice: DisciplineSummary) -> some View {
        let badge = FCDiscipline(
            id: choice.id,
            name: choice.name,
            teacherName: choice.teacherName,
            colorIndex: choice.colorIndex
        )
        return Button {
            onPick(choice.id)
        } label: {
            HStack(spacing: 13) {
                Text(badge.shortLabel)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(
                        UNESColor.disciplineReadableColor(choice.colorIndex),
                        in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                    )

                VStack(alignment: .leading, spacing: 1) {
                    Text(choice.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.23)
                        .lineLimit(1)
                        .foregroundStyle(UNESColor.ink)
                    Text(.finalCountdownPickerReleasedCount(choice.releasedCount, choice.grades.count))
                        .font(.system(size: 12, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if choice.id == selectedId {
                    Image(systemName: "checkmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.accent)
                }
            }
            .padding(.vertical, 11)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack {
        FCDisciplineRow(
            discipline: FCDiscipline(
                id: "d2",
                name: "Cálculo Diferencial II",
                teacherName: "Adriana Matos",
                colorIndex: 1,
                semesterCode: "20261"
            ),
            canChange: true,
            onChange: {}
        )
        .padding(16)
    }
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
    .sheet(isPresented: .constant(true)) {
        FCDisciplinePicker(
            choices: DisciplinesOverview.preview().current!.disciplines,
            selectedId: "d2",
            onPick: { _ in },
            onClose: {}
        )
    }
}
