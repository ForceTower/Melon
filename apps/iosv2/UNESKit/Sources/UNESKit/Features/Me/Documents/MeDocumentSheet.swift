import ComposableArchitecture
import SwiftUI
#if os(iOS)
import QuickLook
#endif

/// The Comprovante / Histórico bottom sheet: summary rows, then the captcha
/// gate when one is required, the generating spinner, and the ready card
/// with the PDF preview.
struct MeDocumentSheet: View {
    @Bindable var store: StoreOf<MeDocumentFeature>

    /// Measured content height, driven through the detent *selection* so the
    /// sheet resizes in both directions — the stages differ a lot (captcha is
    /// more than twice the intro) and a plain `.height` detent won't shrink
    /// once presented.
    @State private var detent = PresentationDetent.height(340)
    @State private var previewURL: URL?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            summaryRows
                .padding(.top, 16)
            stageArea
                .padding(.top, 16)
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 16, trailing: 18))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            detent = .height(measured)
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([detent], selection: $detent)
        .presentationDragIndicator(.visible)
        .pdfPreview($previewURL)
    }

    private var document: AcademicDocument { store.document }

    // MARK: Header

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: document.icon)
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(document.tone, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .shadow(color: document.tone.opacity(0.33), radius: 7, y: 6)

            VStack(alignment: .leading, spacing: 2) {
                Text(document.title)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                Text(document.subtitle)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                store.send(.closeTapped)
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: Summary rows

    private var summaryRows: some View {
        VStack(spacing: 0) {
            row(label: .meDocumentRowStudent, value: store.studentName)
            divider
            row(label: .meDocumentRowCourse, value: store.course)
            divider
            switch document {
            case .enrollmentCertificate:
                row(label: .meDocumentRowStatus, value: .localized(.meDocumentRowStatusActive))
            case .academicHistory:
                row(label: .meStatScore, value: formatGrade(store.score))
            }
        }
        .padding(.horizontal, 14)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }

    private func row(label: LocalizedStringResource, value: String?) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.system(size: 11.5, weight: .semibold))
                .foregroundStyle(document.tone)
                .frame(width: 82, alignment: .leading)
            Text(value ?? "—")
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 11)
    }

    private var divider: some View {
        Rectangle()
            .fill(UNESColor.line)
            .frame(height: 0.5)
    }

    // MARK: Stages

    @ViewBuilder
    private var stageArea: some View {
        switch store.stage {
        case .intro:
            intro
        case .saved:
            documentCard(.saved)
        case .captcha:
            captcha
        case .generating:
            generating
        case .fresh:
            documentCard(.fresh)
        case let .stale(savedAt):
            documentCard(.stale(savedAt: savedAt))
        case .failed:
            failed
        }
    }

    /// How the offline copy is being offered — drives the card's badge, tint,
    /// and buttons.
    private enum CardStatus {
        case saved
        case fresh
        case stale(savedAt: Date)

        var isStale: Bool {
            if case .stale = self { return true }
            return false
        }
    }

    private var intro: some View {
        VStack(spacing: 9) {
            actionButton(.meDocumentDownload, icon: "arrow.down.circle") {
                store.send(.downloadTapped)
            }
            Text(.meDocumentOfflineFootnote)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
        }
    }

    @ViewBuilder
    private var captcha: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(.meDocumentCaptchaPrompt)
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
            #if os(iOS)
            RecaptchaView(siteKey: store.captchaSiteKey, origin: URL(string: store.captchaBaseURL)) { token in
                store.send(.captchaSolved(token: token))
            }
            .frame(height: 480)
            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
            #endif
            if store.stored != nil {
                ghostButton(.meDocumentCancel, icon: nil) {
                    store.send(.captchaCanceled)
                }
            }
        }
    }

    private var generating: some View {
        VStack(spacing: 0) {
            SpinnerRing(
                size: 40,
                lineWidth: 3,
                color: document.tone,
                trackColor: document.tone.opacity(0.19)
            )
            Text(store.stored == nil ? .meDocumentGenerating : .meDocumentRefreshing)
                .font(.system(size: 15, weight: .semibold))
                .tracking(-0.15)
                .foregroundStyle(UNESColor.ink)
                .padding(.top, 18)
            Text(.meDocumentGeneratingSub)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 22, leading: 0, bottom: 8, trailing: 0))
    }

    @ViewBuilder
    private func documentCard(_ status: CardStatus) -> some View {
        if let stored = store.stored {
            VStack(spacing: 14) {
                fileCard(stored, status: status)
                actionButton(status.isStale ? .meDocumentOpenSaved : .meDocumentOpen, icon: "arrow.up.forward.square") {
                    previewURL = stored.fileURL
                }
                ghostButton(status.isStale ? .meDocumentRefreshRetry : .meDocumentRefresh) {
                    store.send(.downloadTapped)
                }
            }
        }
    }

    private func fileCard(_ stored: StoredAcademicDocument, status: CardStatus) -> some View {
        HStack(spacing: 13) {
            pdfChip(edge: status.isStale ? UNESColor.caution : document.tone)
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 7) {
                    Text(stored.fileURL.lastPathComponent)
                        .font(.system(size: 13.5, weight: .semibold))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    Text(verbatim: "v\(stored.version)")
                        .font(.system(size: 9.5, weight: .bold))
                        .tracking(0.3)
                        .foregroundStyle(UNESColor.ink4)
                        .padding(EdgeInsets(top: 2, leading: 5, bottom: 2, trailing: 5))
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 5, style: .continuous))
                }
                HStack(spacing: 5) {
                    Circle()
                        .fill(status.isStale ? UNESColor.caution : UNESColor.successGreen)
                        .frame(width: 6, height: 6)
                    statusText(stored, status: status)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(status.isStale ? UNESColor.caution : UNESColor.ink4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 14, leading: 15, bottom: 14, trailing: 15))
        .background(status.isStale ? UNESColor.caution.opacity(0.08) : UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(status.isStale ? UNESColor.caution.opacity(0.27) : UNESColor.cardLine)
        }
    }

    private func statusText(_ stored: StoredAcademicDocument, status: CardStatus) -> Text {
        switch status {
        case .saved:
            Text(.meDocumentSavedLine(format(stored.savedAt)))
        case .fresh:
            Text(.meDocumentFreshLine)
        case let .stale(savedAt):
            Text(.meDocumentStaleLine(format(savedAt)))
        }
    }

    private func format(_ date: Date) -> String {
        date.formatted(date: .abbreviated, time: .shortened)
    }

    private func pdfChip(edge: Color) -> some View {
        VStack {
            Spacer()
            Text(verbatim: "PDF")
                .font(.system(size: 7.5, weight: .heavy))
                .tracking(0.5)
                .foregroundStyle(edge)
                .padding(.bottom, 6)
        }
        .frame(width: 42, height: 52)
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(edge, lineWidth: 1.5)
        }
    }

    private var failed: some View {
        VStack(spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                Text(verbatim: "!")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(width: 22, height: 22)
                    .background(UNESColor.alertRed, in: Circle())
                VStack(alignment: .leading, spacing: 3) {
                    Text(.meDocumentErrorTitle)
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                    Text(.meDocumentErrorBody)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(15)
            .background(UNESColor.alertRed.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.alertRed.opacity(0.27))
            }

            actionButton(.meDocumentRetry, icon: "arrow.clockwise") {
                store.send(.downloadTapped)
            }
        }
    }

    private func ghostButton(
        _ title: LocalizedStringResource,
        icon: String? = "arrow.triangle.2.circlepath",
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 7) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 12, weight: .semibold))
                }
                Text(title)
                    .font(.system(size: 13.5, weight: .semibold))
                    .tracking(-0.14)
            }
            .foregroundStyle(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
        }
        .buttonStyle(TilePressStyle())
    }

    private func actionButton(
        _ title: LocalizedStringResource,
        icon: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .semibold))
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.14)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(document.tone, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: document.tone.opacity(0.27), radius: 12, y: 10)
        }
        .buttonStyle(TilePressStyle())
    }
}

private extension View {
    @ViewBuilder
    func pdfPreview(_ url: Binding<URL?>) -> some View {
        #if os(iOS)
        quickLookPreview(url)
        #else
        self
        #endif
    }
}

extension AcademicDocument {
    var title: LocalizedStringResource {
        switch self {
        case .enrollmentCertificate: .meDocumentCertificateTitle
        case .academicHistory: .meDocumentHistoryTitle
        }
    }

    var subtitle: LocalizedStringResource {
        switch self {
        case .enrollmentCertificate: .meDocumentCertificateSubtitle
        case .academicHistory: .meDocumentHistorySubtitle
        }
    }

    var icon: String {
        switch self {
        case .enrollmentCertificate: "doc.text"
        case .academicHistory: "chart.bar.doc.horizontal"
        }
    }

    var tone: Color {
        switch self {
        case .enrollmentCertificate: UNESColor.readable(0x0A84FF)
        case .academicHistory: UNESColor.readable(0x7A5AD0)
        }
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeDocumentSheet(
            store: Store(
                initialState: MeDocumentFeature.State(
                    document: .enrollmentCertificate,
                    studentName: "Mariana Nogueira",
                    course: "Engenharia de Computação",
                    score: 8.5
                )
            ) {
                MeDocumentFeature()
            }
        )
    }
}
