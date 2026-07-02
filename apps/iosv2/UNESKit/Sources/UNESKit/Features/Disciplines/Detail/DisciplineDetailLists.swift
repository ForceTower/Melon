import SwiftUI

// MARK: - Aulas timeline

/// The lecture history as a dotted timeline: filled dots for given classes,
/// a halo on the next one, and a paperclip count when materials exist.
struct DisciplineClassesTimeline: View {
    let detail: DisciplineDetail
    let color: Color
    var selectedGroup: String?

    var body: some View {
        let lectures = detail.lectures(forGroup: selectedGroup)
        let nextId = lectures.first { !$0.isPast && $0.date != nil }?.id

        VStack(spacing: 0) {
            DisciplineSectionHeader(
                title: "Aulas",
                trailing: lectures.isEmpty ? nil : "\(lectures.count) aulas"
            )

            if lectures.isEmpty {
                DetailEmptyCard(message: "O professor ainda não registrou aulas.")
            } else {
                VStack(spacing: 8) {
                    ForEach(lectures) { lecture in
                        LectureRow(lecture: lecture, color: color, isNext: lecture.id == nextId)
                    }
                }
                .background(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(UNESColor.line)
                        .frame(width: 2)
                        .padding(.vertical, 12)
                        .offset(x: 6)
                }
            }
        }
    }
}

private struct LectureRow: View {
    let lecture: DisciplineLecture
    let color: Color
    var isNext: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            dot
                .padding(.top, 15)

            VStack(alignment: .leading, spacing: 5) {
                if isNext {
                    Text("próxima aula")
                        .textCase(.uppercase)
                        .font(.system(size: 10.5, weight: .bold))
                        .tracking(0.5)
                        .foregroundStyle(color)
                        .padding(.bottom, -1)
                }

                Text(lecture.subject ?? "Aula")
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.23)
                    .lineSpacing(2)
                    .foregroundStyle(lecture.isPast ? UNESColor.ink2 : UNESColor.ink)

                HStack(spacing: 10) {
                    Text(lecture.date.map(DisciplinesFormat.longDate) ?? "—")
                    if lecture.attachmentCount > 0 {
                        HStack(spacing: 4) {
                            Image(systemName: "link")
                                .font(.system(size: 10, weight: .medium))
                            Text("\(lecture.attachmentCount)")
                        }
                    }
                }
                .font(.system(size: 12, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 11, leading: 15, bottom: 11, trailing: 15))
            .background(isNext ? color.opacity(0.07) : UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(isNext ? color.opacity(0.33) : UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: isNext ? 0 : 0.04), radius: 6, y: 4)
        }
    }

    private var dot: some View {
        ZStack {
            if isNext {
                Circle()
                    .fill(color.opacity(0.13))
                    .frame(width: 22, height: 22)
            }
            Circle()
                .fill(lecture.isPast ? color : UNESColor.surface)
                .strokeBorder(lecture.isPast || isNext ? color : UNESColor.line, lineWidth: 2.5)
                .frame(width: 14, height: 14)
        }
        .frame(width: 14, height: 14)
    }
}

// MARK: - Anexos

/// Lecture materials, filterable by group, opening in the browser.
struct DisciplineAttachmentsBlock: View {
    let detail: DisciplineDetail
    let color: Color
    var selectedGroup: String?

    @Environment(\.openURL) private var openURL

    var body: some View {
        let attachments = detail.attachments(forGroup: selectedGroup)

        VStack(spacing: 0) {
            DisciplineSectionHeader(title: "Anexos", trailing: "\(attachments.count)")

            if attachments.isEmpty {
                DetailEmptyCard(message: "Sem anexos desta turma.")
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(attachments.enumerated()), id: \.element.id) { index, attachment in
                        AttachmentRow(
                            attachment: attachment,
                            color: color,
                            showsGroup: detail.hasMultipleGroups && selectedGroup == nil,
                            isLast: index == attachments.count - 1
                        ) {
                            if let url = URL(string: attachment.url) {
                                openURL(url)
                            }
                        }
                    }
                }
                .background(UNESColor.card)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(UNESColor.cardLine)
                }
                .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
            }
        }
    }
}

private struct AttachmentRow: View {
    let attachment: DisciplineAttachment
    let color: Color
    var showsGroup: Bool
    var isLast: Bool
    var onTap: () -> Void

    var body: some View {
        let kind = AttachmentKind(url: attachment.url)
        Button(action: onTap) {
            HStack(spacing: 13) {
                Image(systemName: kind.icon)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(color)
                    .frame(width: 38, height: 38)
                    .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 11, style: .continuous))

                VStack(alignment: .leading, spacing: 1) {
                    Text(attachment.name)
                        .font(.system(size: 14, weight: .semibold))
                        .tracking(-0.21)
                        .lineLimit(1)
                        .foregroundStyle(UNESColor.ink)

                    HStack(spacing: 7) {
                        Text(subtitle(kind: kind))
                            .monospacedDigit()
                        if showsGroup, let group = attachment.groupCode {
                            Text(group)
                                .fontWeight(.semibold)
                                .monospacedDigit()
                                .padding(.horizontal, 6)
                                .padding(.vertical, 1)
                                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 6))
                                .foregroundStyle(UNESColor.ink3)
                        }
                    }
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 12, leading: 15, bottom: 12, trailing: 15))
            .overlay(alignment: .bottom) {
                if !isLast {
                    Rectangle().fill(UNESColor.line).frame(height: 0.5)
                }
            }
        }
        .buttonStyle(AttachmentPressStyle())
    }

    private func subtitle(kind: AttachmentKind) -> String {
        guard let date = attachment.lectureDate else { return kind.label }
        return "\(kind.label) · \(DisciplinesFormat.shortDate(date))"
    }
}

/// Rows highlight to `surface2` while pressed — the `.dd-att:active` behavior.
private struct AttachmentPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? UNESColor.surface2 : .clear)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Icon + caption inferred from the material URL — SAGRES doesn't type them.
private enum AttachmentKind {
    case pdf, slides, notes, link

    init(url: String) {
        let lower = url.lowercased()
        self = if lower.hasSuffix(".pdf") {
            .pdf
        } else if lower.hasSuffix(".ppt") || lower.hasSuffix(".pptx") || lower.hasSuffix(".key") {
            .slides
        } else if lower.hasSuffix(".md") || lower.hasSuffix(".txt") || lower.hasSuffix(".doc") || lower.hasSuffix(".docx") {
            .notes
        } else {
            .link
        }
    }

    var icon: String {
        switch self {
        case .pdf: "doc.text"
        case .slides: "rectangle.on.rectangle"
        case .notes: "note.text"
        case .link: "link"
        }
    }

    var label: String {
        switch self {
        case .pdf: "PDF"
        case .slides: "SLIDES"
        case .notes: "NOTAS"
        case .link: "LINK"
        }
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 22) {
            DisciplineClassesTimeline(detail: .preview(), color: UNESColor.coral)
            DisciplineAttachmentsBlock(detail: .preview(), color: UNESColor.coral)
            DisciplineClassesTimeline(detail: .previewMultiGroup(), color: UNESColor.violet)
            DisciplineAttachmentsBlock(detail: .previewMultiGroup(), color: UNESColor.violet)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
