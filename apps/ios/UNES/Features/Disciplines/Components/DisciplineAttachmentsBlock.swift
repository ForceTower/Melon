import SwiftUI

/// Attachments list. Icon per file type, truncation on long names, and a
/// turma badge appears for multi-group disciplines when "Tudo" is active.
struct DisciplineAttachmentsBlock: View {
    let discipline: Discipline
    let selectedGroup: String?

    private var visible: [Attachment] {
        discipline.attachments(for: selectedGroup)
    }

    var body: some View {
        if !discipline.attachments.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                DisciplineSectionHeader("Anexos") {
                    Text("\(visible.count)")
                        .font(UNESFont.mono(10))
                        .tracking(0.8)
                        .foregroundStyle(UNESColor.ink4)
                }

                if visible.isEmpty {
                    Text("Sem anexos desta turma.")
                        .font(UNESFont.sans(13))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(20)
                        .cardSurface(attachmentsShape)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(visible.enumerated()), id: \.element.id) { idx, att in
                            AttachmentRow(
                                attachment: att,
                                accent: discipline.color,
                                showGroupBadge: discipline.hasMultipleGroups && selectedGroup == nil && att.group != nil
                            )
                            if idx < visible.count - 1 {
                                Rectangle()
                                    .fill(UNESColor.line)
                                    .frame(height: 1)
                            }
                        }
                    }
                    .cardSurface(attachmentsShape)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
        }
    }

    private var attachmentsShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
    }
}

private struct AttachmentRow: View {
    let attachment: Attachment
    let accent: Color
    let showGroupBadge: Bool

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(accent.opacity(0.13))
                Image(systemName: iconName)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(accent)
            }
            .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(attachment.name)
                    .font(UNESFont.sans(13, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Text("\(attachment.kind.label) · adicionado em \(attachment.added)")
                        .font(UNESFont.mono(10))
                        .foregroundStyle(UNESColor.ink4)
                    if showGroupBadge, let group = attachment.group {
                        Text(group)
                            .font(UNESFont.mono(9, weight: .semibold))
                            .tracking(0.54)
                            .foregroundStyle(UNESColor.ink3)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 1)
                            .background(
                                RoundedRectangle(cornerRadius: 3, style: .continuous)
                                    .fill(UNESColor.surface2)
                            )
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    private var iconName: String {
        switch attachment.kind {
        case .pdf:    return "doc.text.fill"
        case .slides: return "rectangle.on.rectangle"
        case .link:   return "link"
        case .notes:  return "note.text"
        case .other:  return "doc"
        }
    }
}
