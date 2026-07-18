import ComposableArchitecture
import SwiftUI
#if os(iOS)
import QuickLook
#endif

struct MaterialsDetailView: View {
    @Bindable var store: StoreOf<MaterialsDetailFeature>

    private var material: Material { store.material }
    private var typeTone: Color { material.type.tone }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            if store.showsModerationStatus {
                MaterialsModerationStatusView(material: material) {
                    store.send(.reuploadTapped)
                }
            } else {
                publicDetail
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task { await store.send(.task).finish() }
        .toolbar {
            if !store.showsModerationStatus {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        store.send(.reportTapped)
                    } label: {
                        Image(systemName: "flag")
                    }
                    .tint(UNESColor.ink4)
                }
            }
        }
        .sheet(isPresented: $store.isReportPresented) {
            MaterialsReportSheet(reason: $store.reportReason) {
                store.send(.reportConfirmed)
            }
        }
        .sheet(item: $store.scope(state: \.upload, action: \.upload)) { uploadStore in
            MaterialsUploadSheet(store: uploadStore)
        }
        .filePreview(previewBinding)
        .overlay(alignment: .bottom) {
            if let toast = store.toast {
                MaterialsToast(icon: toast.icon, tone: toast.tone, text: toast.text)
                    .padding(.bottom, 88)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .animation(UNESMotion.ease(0.3), value: store.toast)
    }

    // MARK: Public detail

    private var publicDetail: some View {
        ZStack(alignment: .top) {
            ambientWash
            ScrollView {
                VStack(spacing: 0) {
                    hero
                        .padding(.bottom, 18)
                    if material.isSaved {
                        savedBanner
                            .fadeUp(delay: 0.1)
                            .padding(.bottom, 18)
                    }
                    actionRow
                        .fadeUp(delay: 0.12)
                        .padding(.bottom, 22)
                    if let note = material.note {
                        noteSection(note)
                            .fadeUp(delay: 0.16)
                            .padding(.bottom, 22)
                    }
                    metadataSection
                        .fadeUp(delay: 0.2)
                        .padding(.bottom, 22)
                    uploaderCard
                        .fadeUp(delay: 0.24)
                        .padding(.bottom, 16)
                    Text(.materialsDetailFooter)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .multilineTextAlignment(.center)
                        .lineSpacing(2)
                        .padding(.horizontal, 16)
                        .fadeUp(delay: 0.28)
                }
                .padding(EdgeInsets(top: 10, leading: 16, bottom: 110, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .overlay(alignment: .bottom) {
            openBar
        }
    }

    private var hero: some View {
        HStack(alignment: .top, spacing: 18) {
            MaterialDocPreview(material: material)
                .scaleIn(delay: 0.06, duration: 0.6)
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 5) {
                    Image(systemName: material.type.icon)
                        .font(.system(size: 10, weight: .bold))
                    Text(material.type.label)
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .bold))
                        .tracking(0.33)
                }
                .foregroundStyle(typeTone)
                .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                .background(typeTone.opacity(0.13), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                Text(material.title)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .lineSpacing(1)
                    .foregroundStyle(UNESColor.ink)
                    .padding(.top, 10)

                HStack(spacing: 8) {
                    MaterialUsefulSignal(
                        count: material.usefulCount,
                        isActive: material.isUseful,
                        large: true
                    )
                    Text(verbatim: "·")
                        .foregroundStyle(UNESColor.ink4)
                        .opacity(0.6)
                    HStack(spacing: 5) {
                        Image(systemName: "arrow.down.circle")
                            .font(.system(size: 12, weight: .semibold))
                        Text(MaterialsFormat.count(material.downloadCount))
                            .font(.system(size: 13, weight: .medium))
                            .monospacedDigit()
                    }
                    .foregroundStyle(UNESColor.ink4)
                }
                .padding(.top, 12)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 4)
            .fadeUp(delay: 0.06)
        }
        .padding(.horizontal, 4)
    }

    private var savedBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: "bookmark.fill")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(UNESColor.successGreen)
            Text(.materialsDetailSavedBanner)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
        .background(UNESColor.successGreen.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.successGreen.opacity(0.26))
        }
    }

    /// Útil / Salvar pills; denunciar lives on the nav flag only.
    private var actionRow: some View {
        HStack(spacing: 8) {
            MaterialsPillAction(
                icon: material.isUseful ? "hand.thumbsup.fill" : "hand.thumbsup",
                label: .materialsDetailActionUseful,
                tone: UNESColor.readable(0x2F9E5E),
                isActive: material.isUseful
            ) {
                store.send(.usefulTapped)
            }
            MaterialsPillAction(
                icon: material.isSaved ? "bookmark.fill" : "bookmark",
                label: material.isSaved ? .materialsDetailActionSaved : .materialsDetailActionSave,
                tone: UNESColor.readable(0x2F9E5E),
                isActive: material.isSaved
            ) {
                store.send(.saveTapped)
            }
        }
    }

    private func noteSection(_ note: String) -> some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.materialsDetailSectionAbout)
            HStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(typeTone)
                    .frame(width: 4)
                Text(note)
                    .font(.system(size: 14.5, weight: .regular))
                    .lineSpacing(4)
                    .foregroundStyle(UNESColor.ink2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 12)
            }
            .padding(EdgeInsets(top: 15, leading: 17, bottom: 15, trailing: 17))
            .materialsCard()
        }
    }

    private var metadataSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.materialsDetailSectionDetails)
            VStack(spacing: 0) {
                metaRow(icon: "square.grid.2x2", label: .materialsDetailMetaDiscipline, value: material.discipline.code)
                divider
                metaRow(icon: "clock", label: .materialsDetailMetaSemester, value: material.semester)
                divider
                metaRow(
                    icon: "person",
                    label: .materialsDetailMetaTeacher,
                    value: material.teacherName ?? .localized(.materialsDetailMetaTeacherUnknown)
                )
                divider
                metaRow(
                    icon: material.fileKind.icon,
                    label: .materialsDetailMetaFile,
                    value: fileValue
                )
            }
            .materialsCard()
        }
    }

    private var fileValue: String {
        let pages = String.localized(material.pages == 1
            ? .materialsDetailPagesLongOne(material.pages)
            : .materialsDetailPagesLongOther(material.pages))
        return "\(String.localized(material.fileKind.label)) · \(pages)"
    }

    private var divider: some View {
        Divider()
            .overlay(UNESColor.line)
            .padding(.leading, 43)
    }

    private func metaRow(icon: String, label: LocalizedStringResource, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 18)
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            Spacer(minLength: 12)
            Text(value)
                .font(.system(size: 14.5, weight: .semibold))
                .tracking(-0.15)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.trailing)
        }
        .padding(EdgeInsets(top: 11, leading: 15, bottom: 11, trailing: 15))
    }

    /// Semi-anonymous attribution — course + entry year, never a name.
    private var uploaderCard: some View {
        let tone = UNESColor.disciplineReadableColor(material.discipline.colorIndex)
        return HStack(spacing: 13) {
            Image(systemName: "person.fill")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(
                    LinearGradient.css(
                        stops: [
                            .init(color: tone, location: 0),
                            .init(color: UNESColor.plum, location: 1),
                        ],
                        angle: 135
                    ),
                    in: Circle()
                )
            VStack(alignment: .leading, spacing: 1) {
                Text(.materialsDetailUploaderTitle(material.uploader.course))
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(.materialsDetailUploaderSubtitle(String(material.uploader.entryYear)))
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
        .materialsCard()
    }

    // MARK: Open CTA

    private var openBar: some View {
        Button {
            store.send(.openTapped)
        } label: {
            HStack(spacing: 8) {
                if store.isOpening {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: "arrow.up.forward.square")
                        .font(.system(size: 15, weight: .semibold))
                }
                Text(material.fileKind == .photo ? .materialsDetailOpenPhoto : .materialsDetailOpenPdf)
                    .tracking(-0.17)
            }
        }
        .buttonStyle(.unesAccent)
        .disabled(store.isOpening)
        .padding(EdgeInsets(top: 34, leading: 20, bottom: 12, trailing: 20))
        .background {
            LinearGradient(
                stops: [
                    .init(color: UNESColor.surface.opacity(0), location: 0),
                    .init(color: UNESColor.surface, location: 0.42),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .bottom)
            .allowsHitTesting(false)
        }
    }

    private var ambientWash: some View {
        RadialGradient(
            colors: [typeTone.opacity(0.26), .clear],
            center: .top,
            startRadius: 0,
            endRadius: 250
        )
        .frame(height: 280)
        .offset(y: -60)
        .ignoresSafeArea()
    }

    private var previewBinding: Binding<URL?> {
        Binding(
            get: { store.previewURL },
            set: { value in
                if value == nil, store.previewURL != nil { store.send(.previewDismissed) }
            }
        )
    }
}

extension MaterialsDetailFeature.State.Toast {
    var icon: String {
        switch self {
        case .saved: "bookmark.fill"
        case .unsaved: "bookmark.slash"
        case .reported: "checkmark.shield"
        case .syncFailed, .openFailed: "wifi.exclamationmark"
        }
    }

    var tone: Color {
        switch self {
        case .saved, .unsaved, .reported: UNESColor.successGreen
        case .syncFailed, .openFailed: UNESColor.coral
        }
    }

    var text: LocalizedStringResource {
        switch self {
        case .saved: .materialsToastSaved
        case .unsaved: .materialsToastUnsaved
        case .reported: .materialsToastReported
        case .syncFailed: .materialsToastSyncFailed
        case .openFailed: .materialsToastOpenFailed
        }
    }
}

extension View {
    /// QuickLook on iOS; inert elsewhere (the screen is never mounted there).
    @ViewBuilder
    fileprivate func filePreview(_ url: Binding<URL?>) -> some View {
        #if os(iOS)
        quickLookPreview(url)
        #else
        self
        #endif
    }
}

// MARK: - Pill action

/// One slot of the útil / salvar row — inline pill, tinted while active.
private struct MaterialsPillAction: View {
    var icon: String
    var label: LocalizedStringResource
    var tone: Color
    var isActive: Bool
    var onTap: () -> Void

    private var color: Color { isActive ? tone : UNESColor.ink2 }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 7) {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                Text(label)
                    .font(.system(size: 13.5, weight: .semibold))
                    .tracking(-0.14)
            }
            .foregroundStyle(color)
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(
                isActive ? color.opacity(0.09) : UNESColor.surface2,
                in: RoundedRectangle(cornerRadius: 13, style: .continuous)
            )
            .overlay {
                if isActive {
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .strokeBorder(color.opacity(0.33))
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
    }
}

// MARK: - Document preview

/// Striped placeholder "first page" — a hint of the file, not a render of it.
struct MaterialDocPreview: View {
    var material: Material

    @Environment(\.colorScheme) private var colorScheme

    private var tone: Color { material.type.tone }

    var body: some View {
        ZStack(alignment: .topLeading) {
            stripes
            fauxLines
            Image(systemName: material.type.icon)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(tone, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .padding(8)
        }
        .frame(width: 150, height: 200)
        .overlay(alignment: .bottom) { ribbon }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.16), radius: 13, y: 7)
    }

    private var stripes: some View {
        let light = colorScheme == .dark ? Color(hex: 0x221B2A) : Color(hex: 0xFBF9F5)
        let dark = colorScheme == .dark ? Color(hex: 0x1C1624) : Color(hex: 0xF1ECE4)
        return Canvas { context, size in
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(light))
            let step: CGFloat = 14
            var x: CGFloat = -size.height
            while x < size.width {
                var path = Path()
                path.move(to: CGPoint(x: x, y: size.height))
                path.addLine(to: CGPoint(x: x + size.height, y: 0))
                path.addLine(to: CGPoint(x: x + size.height + step / 2, y: 0))
                path.addLine(to: CGPoint(x: x + step / 2, y: size.height))
                path.closeSubpath()
                context.fill(path, with: .color(dark))
                x += step
            }
        }
    }

    private var fauxLines: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoundedRectangle(cornerRadius: 3)
                .fill(tone.opacity(0.85))
                .frame(width: 84, height: 7)
            ForEach(Array([0.88, 0.96, 0.72, 0.9, 0.6, 0.84, 0.94, 0.68, 0.8].enumerated()), id: \.offset) { _, width in
                RoundedRectangle(cornerRadius: 2)
                    .fill(UNESColor.ink.opacity(0.14))
                    .frame(width: 126 * width, height: 4)
            }
        }
        .padding(EdgeInsets(top: 42, leading: 12, bottom: 14, trailing: 12))
    }

    private var ribbon: some View {
        HStack(spacing: 5) {
            Image(systemName: material.fileKind.icon)
                .font(.system(size: 10, weight: .semibold))
            Text("\(String.localized(material.fileKind.label).uppercased()) · \(material.pages)")
                .font(.system(size: 10, weight: .bold))
                .tracking(0.4)
                .monospacedDigit()
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 5, leading: 9, bottom: 5, trailing: 9))
        .background(UNESColor.scrim.opacity(0.5))
    }
}

// MARK: - Report sheet

/// Anonymous report: one reason, then send.
struct MaterialsReportSheet: View {
    @Binding var reason: MaterialReportReason?
    var onConfirm: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.materialsReportTitle)
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.6)
                .foregroundStyle(UNESColor.ink)
            Text(.materialsReportSubtitle)
                .font(.system(size: 13.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 8)

            VStack(spacing: 0) {
                ForEach(Array(MaterialReportReason.allCases.enumerated()), id: \.element) { index, option in
                    if index > 0 {
                        Divider()
                            .overlay(UNESColor.line)
                            .padding(.leading, 15)
                    }
                    Button {
                        reason = option
                    } label: {
                        HStack(spacing: 12) {
                            Text(option.label)
                                .font(.system(size: 15, weight: .medium))
                                .tracking(-0.15)
                                .foregroundStyle(UNESColor.ink)
                                .multilineTextAlignment(.leading)
                            Spacer(minLength: 8)
                            Image(systemName: reason == option ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 20, weight: .medium))
                                .foregroundStyle(reason == option ? UNESColor.accent : UNESColor.surface3)
                        }
                        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(CardPressStyle())
                }
            }
            .materialsCard()
            .padding(.top, 16)

            Button(action: onConfirm) {
                Text(.materialsReportSend)
                    .tracking(-0.17)
            }
            .buttonStyle(.unesAccent)
            .disabled(reason == nil)
            .padding(.top, 20)
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 16, trailing: 18))
        .frame(maxHeight: .infinity, alignment: .top)
        .presentationDetents([.height(480)])
        .presentationDragIndicator(.visible)
        .presentationBackground(UNESColor.surface)
    }
}

// MARK: - Moderation status (pending / rejected uploads)

/// Full-screen state for the student's own non-public upload.
struct MaterialsModerationStatusView: View {
    var material: Material
    var onReupload: () -> Void

    private var pending: Bool { material.status == .pending }
    private var tone: Color { material.status.tone }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Image(systemName: material.status.icon)
                    .font(.system(size: 34, weight: .medium))
                    .foregroundStyle(tone)
                    .frame(width: 78, height: 78)
                    .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 26, style: .continuous))
                    .scaleIn(delay: 0.06, duration: 0.6)

                Text(pending ? .materialsPendingTitle : .materialsRejectedTitle)
                    .font(.system(size: 26, weight: .bold))
                    .tracking(-0.78)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20)
                    .fadeUp(delay: 0.1)

                Text(pending ? .materialsPendingSubtitle : .materialsRejectedSubtitle)
                    .font(.system(size: 15, weight: .medium))
                    .lineSpacing(4)
                    .foregroundStyle(UNESColor.ink3)
                    .multilineTextAlignment(.center)
                    .padding(.top, 10)
                    .padding(.horizontal, 12)
                    .fadeUp(delay: 0.14)

                fileCard
                    .padding(.top, 24)
                    .fadeUp(delay: 0.18)

                if pending {
                    timeline
                        .padding(.top, 16)
                        .fadeUp(delay: 0.22)
                } else {
                    if let reason = material.rejectionReason {
                        reasonCard(reason)
                            .padding(.top, 16)
                            .fadeUp(delay: 0.22)
                    }
                    Button(action: onReupload) {
                        HStack(spacing: 8) {
                            Image(systemName: "plus")
                                .font(.system(size: 15, weight: .bold))
                            Text(.materialsRejectedResubmit)
                                .tracking(-0.17)
                        }
                    }
                    .buttonStyle(.unesAccent)
                    .padding(.top, 20)
                    .fadeUp(delay: 0.26)
                }
            }
            .padding(EdgeInsets(top: 24, leading: 20, bottom: 40, trailing: 20))
        }
        .scrollIndicators(.hidden)
        .navigationTitle(Text(pending ? .materialsStatusPending : .materialsStatusRejected))
    }

    private var fileCard: some View {
        HStack(spacing: 14) {
            MaterialTypeBadge(type: material.type)
            VStack(alignment: .leading, spacing: 3) {
                Text(material.title)
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Text(fileMeta)
                    .font(.system(size: 12.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .materialsCard()
    }

    private var fileMeta: String {
        let pages = String.localized(material.pages == 1
            ? .materialsListPagesOne(material.pages)
            : .materialsListPagesOther(material.pages))
        return "\(String.localized(material.type.label)) · \(material.semester) · \(pages)"
    }

    private func reasonCard(_ reason: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 24, height: 24)
                .background(tone, in: Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text(.materialsRejectedReasonTitle)
                    .font(.system(size: 13.5, weight: .bold))
                    .foregroundStyle(UNESColor.ink)
                Text(reason)
                    .font(.system(size: 13, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink2)
            }
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
        .background(tone.opacity(0.08), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(tone.opacity(0.2))
        }
    }

    /// Enviado → em análise → publicado.
    private var timeline: some View {
        VStack(alignment: .leading, spacing: 0) {
            timelineStep(
                state: .done,
                title: .materialsTimelineSent,
                note: .materialsTimelineSentNote,
                isLast: false
            )
            timelineStep(
                state: .active,
                title: .materialsTimelineReviewing,
                note: .materialsTimelineReviewingNote,
                isLast: false
            )
            timelineStep(
                state: .upcoming,
                title: .materialsTimelinePublished,
                note: .materialsTimelinePublishedNote,
                isLast: true
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
        .materialsCard()
    }

    private enum StepState { case done, active, upcoming }

    private func timelineStep(
        state: StepState,
        title: LocalizedStringResource,
        note: LocalizedStringResource,
        isLast: Bool
    ) -> some View {
        HStack(alignment: .top, spacing: 13) {
            VStack(spacing: 2) {
                ZStack {
                    Circle()
                        .fill(state == .done
                            ? UNESColor.successGreen
                            : state == .active ? tone : UNESColor.surface3)
                        .frame(width: 20, height: 20)
                    if state == .done {
                        Image(systemName: "checkmark")
                            .font(.system(size: 9, weight: .heavy))
                            .foregroundStyle(.white)
                    }
                }
                if !isLast {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(UNESColor.surface3)
                        .frame(width: 2, height: 26)
                }
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(state == .upcoming ? UNESColor.ink4 : UNESColor.ink)
                Text(note)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.top, 1)
        }
    }
}

#Preview("Material") {
    NavigationStack {
        MaterialsDetailView(
            store: Store(
                initialState: MaterialsDetailFeature.State(material: Material.preview()[4])
            ) {
                MaterialsDetailFeature()
            }
        )
    }
}

#Preview("Em análise") {
    NavigationStack {
        MaterialsDetailView(
            store: Store(
                initialState: MaterialsDetailFeature.State(material: Material.preview()[6])
            ) {
                MaterialsDetailFeature()
            }
        )
    }
}

#Preview("Rejeitado") {
    NavigationStack {
        MaterialsDetailView(
            store: Store(
                initialState: MaterialsDetailFeature.State(material: Material.preview()[7])
            ) {
                MaterialsDetailFeature()
            }
        )
    }
}
