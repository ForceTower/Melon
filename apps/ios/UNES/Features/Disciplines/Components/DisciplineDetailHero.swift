import SwiftUI

/// Top section of the detail screen. Back button, code chip, department,
/// title, professor row (stacks for multi-group), and a segmented control
/// to switch between "Tudo / Teórica / Prática / …".
struct DisciplineDetailHero: View {
    let discipline: Discipline
    @Binding var selectedGroup: String?
    let onBack: () -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(
                stops: [
                    .init(color: discipline.color.opacity(0.13), location: 0),
                    .init(color: .clear, location: 0.8),
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                        .frame(width: 38, height: 38)
                        .background(
                            Circle()
                                .fill(UNESColor.card)
                                .overlay(Circle().strokeBorder(UNESColor.cardLine, lineWidth: 1))
                        )
                }
                .buttonStyle(.plain)
                .padding(.bottom, 18)

                HStack(spacing: 8) {
                    Text(discipline.fullCode)
                        .font(UNESFont.mono(10, weight: .bold))
                        .tracking(1)
                        .foregroundStyle(discipline.color)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: 5, style: .continuous)
                                .fill(discipline.color.opacity(0.13))
                        )
                    Text("Departamento de \(discipline.dept)")
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.ink3)
                }
                .padding(.bottom, 8)

                Text(discipline.title)
                    .font(UNESFont.serif(30))
                    .tracking(-0.6)
                    .foregroundStyle(UNESColor.ink)

                ProfessorRow(discipline: discipline, selectedGroup: selectedGroup)
                    .padding(.top, 8)

                if discipline.hasMultipleGroups {
                    GroupSegmented(
                        groups: discipline.groups,
                        selected: $selectedGroup,
                        accent: discipline.color
                    )
                    .padding(.top, 14)
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 60)
            .padding(.bottom, 20)
        }
    }
}

/// Shows the professor responsible for the current group — or the full
/// stack when "Tudo" is selected on a multi-group discipline.
private struct ProfessorRow: View {
    let discipline: Discipline
    let selectedGroup: String?

    var body: some View {
        if !discipline.hasMultipleGroups {
            singleRow(prof: discipline.prof, turma: nil)
        } else if let selected = selectedGroup,
                  let group = discipline.groups.first(where: { $0.code == selected }) {
            singleRow(prof: group.prof, turma: group.code)
        } else {
            VStack(alignment: .leading, spacing: 3) {
                ForEach(discipline.groups) { g in
                    HStack(spacing: 6) {
                        profIcon
                        Text(g.prof)
                            .font(UNESFont.sans(13))
                            .foregroundStyle(UNESColor.ink2)
                        turmaBadge(g.code)
                        Text("· \(g.kind)")
                            .font(UNESFont.sans(11))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
            }
        }
    }

    private func singleRow(prof: String, turma: String?) -> some View {
        HStack(spacing: 6) {
            profIcon
            Text(prof)
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink2)
            if let turma {
                turmaBadge(turma)
            }
        }
    }

    private var profIcon: some View {
        Image(systemName: "person.fill")
            .font(.system(size: 10))
            .foregroundStyle(UNESColor.ink3.opacity(0.6))
    }

    private func turmaBadge(_ code: String) -> some View {
        Text(code)
            .font(UNESFont.mono(9, weight: .semibold))
            .tracking(0.72)
            .foregroundStyle(UNESColor.ink4)
            .padding(.horizontal, 5)
            .padding(.vertical, 1)
            .background(
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(UNESColor.surface2)
            )
    }
}

/// "Tudo / Teórica / Prática" — each option shows its turma code as a caption.
private struct GroupSegmented: View {
    let groups: [DisciplineGroup]
    @Binding var selected: String?
    let accent: Color

    private struct Option: Identifiable {
        let id = UUID()
        let code: String?
        let kind: String
        let turma: String?
    }

    private var options: [Option] {
        [Option(code: nil, kind: "Tudo", turma: nil)] +
        groups.map { Option(code: $0.code, kind: $0.kind, turma: $0.code) }
    }

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options) { opt in
                let active = selected == opt.code
                Button {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                        selected = opt.code
                    }
                } label: {
                    VStack(spacing: 1) {
                        Text(opt.kind)
                            .font(UNESFont.sans(12, weight: .medium))
                        Text(opt.turma ?? "·")
                            .font(UNESFont.mono(8.5, weight: .semibold))
                            .tracking(0.5)
                            .foregroundStyle(active
                                             ? UNESColor.surface.opacity(0.72)
                                             : UNESColor.ink4)
                    }
                    .frame(minWidth: 62)
                    .foregroundStyle(active ? UNESColor.surface : UNESColor.ink2)
                    .padding(.horizontal, 12)
                    .padding(.top, 7)
                    .padding(.bottom, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .fill(active ? accent : Color.clear)
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(UNESColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                )
        )
        .fixedSize()
    }
}
