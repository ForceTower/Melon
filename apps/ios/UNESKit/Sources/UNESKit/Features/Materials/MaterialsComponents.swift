import SwiftUI

// MARK: - Type metadata

extension MaterialType {
    var label: LocalizedStringResource {
        switch self {
        case .exam: .materialsTypeExam
        case .solvedList: .materialsTypeList
        case .summary: .materialsTypeSummary
        case .formulaSheet: .materialsTypeFormula
        }
    }

    var pluralLabel: LocalizedStringResource {
        switch self {
        case .exam: .materialsTypeExamPlural
        case .solvedList: .materialsTypeListPlural
        case .summary: .materialsTypeSummaryPlural
        case .formulaSheet: .materialsTypeFormulaPlural
        }
    }

    /// What the type covers, sold on the empty state — "provas antigas".
    var hint: LocalizedStringResource {
        switch self {
        case .exam: .materialsTypeExamHint
        case .solvedList: .materialsTypeListHint
        case .summary: .materialsTypeSummaryHint
        case .formulaSheet: .materialsTypeFormulaHint
        }
    }

    var icon: String {
        switch self {
        case .exam: "doc.text"
        case .solvedList: "list.bullet.rectangle"
        case .summary: "doc.plaintext"
        case .formulaSheet: "function"
        }
    }

    var tone: Color {
        switch self {
        case .exam: UNESColor.readable(0xE85D4E)
        case .solvedList: UNESColor.readable(0x3B9EAE)
        case .summary: UNESColor.readable(0xB23A7A)
        case .formulaSheet: UNESColor.readable(0xD9852E)
        }
    }
}

extension MaterialFileKind {
    var label: LocalizedStringResource {
        switch self {
        case .pdf: .materialsFilePdf
        case .photo: .materialsFilePhoto
        }
    }

    var icon: String {
        switch self {
        case .pdf: "doc"
        case .photo: "camera"
        }
    }
}

extension MaterialStatus {
    /// Pill copy for the non-public states; `published` never shows one.
    var label: LocalizedStringResource {
        switch self {
        case .published, .pending: .materialsStatusPending
        case .rejected: .materialsStatusRejected
        }
    }

    var tone: Color {
        switch self {
        case .published, .pending: UNESColor.readable(0xD9852E)
        case .rejected: UNESColor.readable(0xE85D4E)
        }
    }

    var icon: String {
        switch self {
        case .published, .pending: "clock"
        case .rejected: "exclamationmark.triangle"
        }
    }
}

extension MaterialReportReason {
    var label: LocalizedStringResource {
        switch self {
        case .illegible: .materialsReportReasonIllegible
        case .ongoingExam: .materialsReportReasonOngoingExam
        case .restrictedByTeacher: .materialsReportReasonRestricted
        case .wrongDiscipline: .materialsReportReasonWrongDiscipline
        case .other: .materialsReportReasonOther
        }
    }
}

// MARK: - Card chrome

extension View {
    /// The standard v2 card: white/dark card fill, hairline border, soft
    /// shadow.
    func materialsCard(cornerRadius: CGFloat = 20) -> some View {
        background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// Section title with an optional trailing accessory, aligned with the cards.
struct MaterialsSectionHeader<Trailing: View>: View {
    var title: Text
    var trailing: Trailing

    init(title: LocalizedStringResource, @ViewBuilder trailing: () -> Trailing) {
        self.title = Text(title)
        self.trailing = trailing()
    }

    init(verbatim title: String, @ViewBuilder trailing: () -> Trailing) {
        self.title = Text(title)
        self.trailing = trailing()
    }

    var body: some View {
        HStack(alignment: .lastTextBaseline) {
            title
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.6)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
            Spacer(minLength: 8)
            trailing
        }
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 11, trailing: 4))
    }
}

extension MaterialsSectionHeader where Trailing == EmptyView {
    init(_ title: LocalizedStringResource) {
        self.init(title: title) { EmptyView() }
    }

    init(verbatim title: String) {
        self.init(verbatim: title) { EmptyView() }
    }
}

// MARK: - Type badge

/// Filled rounded square carrying the type glyph.
struct MaterialTypeBadge: View {
    var type: MaterialType
    var size: CGFloat = 44

    var body: some View {
        Image(systemName: type.icon)
            .font(.system(size: size * 0.42, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                LinearGradient.css(
                    stops: [
                        .init(color: type.tone, location: 0),
                        .init(color: type.tone.mix(with: .black, by: 0.18), location: 1),
                    ],
                    angle: 150
                ),
                in: RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
            )
            .shadow(color: type.tone.opacity(0.34), radius: 6, y: 4)
    }
}

// MARK: - Useful signal

/// The "útil" vote count — thumbs-up plus tally, green once the student
/// voted.
struct MaterialUsefulSignal: View {
    var count: Int
    var isActive = false
    var large = false

    private var tone: Color {
        isActive ? UNESColor.readable(0x2F9E5E) : UNESColor.ink4
    }

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: isActive ? "hand.thumbsup.fill" : "hand.thumbsup")
                .font(.system(size: large ? 14 : 12, weight: .semibold))
            Text(MaterialsFormat.count(count))
                .font(.system(size: large ? 14 : 12.5, weight: .semibold))
                .monospacedDigit()
        }
        .foregroundStyle(tone)
    }
}

// MARK: - Status pill

/// "Em análise" / "Não aprovado" pill on the student's own uploads.
struct MaterialStatusPill: View {
    var status: MaterialStatus

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: status.icon)
                .font(.system(size: 10, weight: .bold))
            Text(status.label)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.2)
        }
        .foregroundStyle(status.tone)
        .padding(EdgeInsets(top: 3.5, leading: 9, bottom: 3.5, trailing: 9))
        .background(status.tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

// MARK: - Material row

/// One material on a discipline shelf.
struct MaterialRow: View {
    var material: Material
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .center, spacing: 13) {
                MaterialTypeBadge(type: material.type)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 7) {
                        Text(material.type.label)
                            .textCase(.uppercase)
                            .font(.system(size: 10.5, weight: .bold))
                            .tracking(0.32)
                            .foregroundStyle(material.type.tone)
                        if material.isSaved {
                            Image(systemName: "bookmark.fill")
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundStyle(UNESColor.successGreen)
                        }
                    }
                    Text(material.title)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    HStack(spacing: 8) {
                        Text(material.semester)
                        Text(verbatim: "·")
                            .opacity(0.5)
                        Text(material.pages == 1
                            ? .materialsListPagesOne(material.pages)
                            : .materialsListPagesOther(material.pages))
                        if let teacher = material.teacherName?.split(separator: " ").first {
                            Text(verbatim: "·")
                                .opacity(0.5)
                            Text(teacher)
                                .lineLimit(1)
                        }
                    }
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 1)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 4) {
                    MaterialUsefulSignal(count: material.usefulCount, isActive: material.isUseful)
                    Image(systemName: material.fileKind.icon)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
    }
}

/// One of the student's own non-public uploads — status pill plus the
/// moderation reason when rejected.
struct MaterialMineRow: View {
    var material: Material
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .top, spacing: 13) {
                MaterialTypeBadge(type: material.type)
                VStack(alignment: .leading, spacing: 6) {
                    Text(material.title)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    MaterialStatusPill(status: material.status)
                    if material.status == .rejected, let reason = material.rejectionReason {
                        Text(reason)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .lineSpacing(2)
                            .multilineTextAlignment(.leading)
                    }
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 14)
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
    }
}

// MARK: - Loading / failure states

struct MaterialsLoadingView: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .tint(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 80)
    }
}

struct MaterialsFailureView: View {
    var onRetry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 52, height: 52)
                .background(UNESColor.surface2, in: Circle())
            Text(.materialsErrorTitle)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            Button {
                onRetry()
            } label: {
                Text(.materialsErrorRetry)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.accent)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 64)
    }
}

// MARK: - Toast

/// Transient confirmation pill floating over the bottom of the screen.
struct MaterialsToast: View {
    var icon: String
    var tone: Color
    var text: LocalizedStringResource

    var body: some View {
        HStack(spacing: 9) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(tone)
            Text(text)
                .font(.system(size: 14, weight: .semibold))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.surface)
        }
        .padding(EdgeInsets(top: 11, leading: 16, bottom: 11, trailing: 16))
        .background(UNESColor.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color(hex: 0x0A0710, opacity: 0.4), radius: 17, y: 8)
    }
}

#Preview("Componentes") {
    ScrollView {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 12) {
                ForEach(MaterialType.allCases, id: \.self) { type in
                    MaterialTypeBadge(type: type)
                }
            }
            HStack(spacing: 12) {
                MaterialUsefulSignal(count: 128)
                MaterialUsefulSignal(count: 1280, isActive: true)
                MaterialStatusPill(status: .pending)
                MaterialStatusPill(status: .rejected)
            }
            VStack(spacing: 0) {
                MaterialRow(material: Material.preview()[0]) {}
                Divider()
                MaterialRow(material: Material.preview()[4]) {}
                Divider()
                MaterialMineRow(material: Material.preview()[7]) {}
            }
            .materialsCard()
            MaterialsToast(icon: "bookmark.fill", tone: UNESColor.successGreen, text: .materialsToastSaved)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
