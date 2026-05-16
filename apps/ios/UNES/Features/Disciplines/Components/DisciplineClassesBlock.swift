import SwiftUI

/// Vertical timeline of registered classes. Past items are dimmed; the next
/// upcoming class is highlighted with a color wash and ring.
struct DisciplineClassesBlock: View {
    let discipline: Discipline

    private var classes: [ClassEntry] { discipline.classes }
    private var nextIndex: Int? { classes.firstIndex(where: \.isNext) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            DisciplineSectionHeader("Aulas") {
                if !classes.isEmpty {
                    Text("\(classes.count) aulas")
                        .font(UNESFont.mono(10))
                        .tracking(0.8)
                        .foregroundStyle(UNESColor.ink4)
                }
            }

            if classes.isEmpty {
                Text("O professor ainda não registrou aulas.")
                    .font(UNESFont.sans(13))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(20)
                    .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
            } else {
                timeline
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 18)
    }

    /// Width of the left "dot rail" column. The continuous spine passes
    /// through the horizontal center of this column, and each row's dot is
    /// also centered within it — keeping the line running exactly through
    /// the middle of every dot.
    private let railWidth: CGFloat = 22

    private var timeline: some View {
        ZStack(alignment: .topLeading) {
            // Continuous spine, centered in the dot rail.
            Rectangle()
                .fill(UNESColor.line)
                .frame(width: 1)
                .offset(x: railWidth / 2)
                .padding(.vertical, 14)

            VStack(spacing: 10) {
                ForEach(Array(classes.enumerated()), id: \.element.id) { idx, c in
                    ClassRow(entry: c,
                             accent: discipline.color,
                             isNext: idx == nextIndex,
                             railWidth: railWidth)
                }
            }
        }
    }
}

private struct ClassRow: View {
    let entry: ClassEntry
    let accent: Color
    let isNext: Bool
    let railWidth: CGFloat

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            ZStack {
                if isNext {
                    Circle()
                        .fill(accent.opacity(0.13))
                        .frame(width: 21, height: 21)
                }
                Circle()
                    .fill(entry.past ? accent : UNESColor.surface)
                    .overlay(
                        Circle().strokeBorder(entry.past || isNext ? accent : UNESColor.line, lineWidth: 2)
                    )
                    .frame(width: 13, height: 13)
            }
            .frame(width: railWidth, height: 22, alignment: .center)
            .padding(.top, 12)

            VStack(alignment: .leading, spacing: 3) {
                if isNext {
                    Text("PRÓXIMA AULA")
                        .font(UNESFont.mono(9, weight: .semibold))
                        .tracking(1.26)
                        .foregroundStyle(accent)
                }
                Text(entry.title)
                    .font(UNESFont.serif(15))
                    .tracking(-0.15)
                    .foregroundStyle(entry.past ? UNESColor.ink2 : UNESColor.ink)

                HStack(spacing: 10) {
                    Text(entry.date ?? "—")
                        .font(UNESFont.mono(10))
                        .foregroundStyle(UNESColor.ink4)
                    if let count = entry.attachments, count > 0 {
                        Text("·").foregroundStyle(UNESColor.ink4.opacity(0.5))
                        HStack(spacing: 3) {
                            Image(systemName: "paperclip")
                                .font(.system(size: 9, weight: .medium))
                            Text("\(count)")
                                .font(UNESFont.mono(10))
                        }
                        .foregroundStyle(UNESColor.ink4)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .cardSurface(
                RoundedRectangle(cornerRadius: 14, style: .continuous),
                fill: isNext ? accent.opacity(0.06) : UNESColor.card,
                stroke: isNext ? accent.opacity(0.33) : UNESColor.cardLine
            )
        }
    }
}
