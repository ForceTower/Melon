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

    /// Measured content height so the sheet hugs it. Same pattern as
    /// `MeAboutSheet`.
    @State private var height: CGFloat = 420
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
        .animation(UNESMotion.ease(0.32), value: store.stage)
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
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
        case .summary:
            actionButton(.meDocumentDownload, icon: "arrow.down.circle") {
                store.send(.downloadTapped)
            }
        case .captcha:
            captcha
        case .generating:
            generating
        case let .ready(url):
            ready(url)
        case .failed:
            failed
        }
    }

    @ViewBuilder
    private var captcha: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(.meDocumentCaptchaPrompt)
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
            #if os(iOS)
            RecaptchaView(siteKey: store.captchaSiteKey) { token in
                store.send(.captchaSolved(token: token))
            }
            .frame(height: 480)
            .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
            #endif
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
            Text(.meDocumentGenerating)
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

    private func ready(_ url: URL) -> some View {
        VStack(spacing: 14) {
            HStack(spacing: 13) {
                pdfChip
                VStack(alignment: .leading, spacing: 2) {
                    Text(url.lastPathComponent)
                        .font(.system(size: 13.5, weight: .semibold))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    HStack(spacing: 5) {
                        Circle()
                            .fill(UNESColor.successGreen)
                            .frame(width: 6, height: 6)
                        Text(.meDocumentReady)
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(EdgeInsets(top: 14, leading: 15, bottom: 14, trailing: 15))
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }

            actionButton(.meDocumentOpen, icon: "arrow.up.forward.square") {
                previewURL = url
            }
        }
    }

    private var pdfChip: some View {
        VStack {
            Spacer()
            Text(verbatim: "PDF")
                .font(.system(size: 7.5, weight: .heavy))
                .tracking(0.5)
                .foregroundStyle(document.tone)
                .padding(.bottom, 6)
        }
        .frame(width: 42, height: 52)
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(document.tone, lineWidth: 1.5)
        }
    }

    private var failed: some View {
        VStack(spacing: 12) {
            Text(.meDocumentFailed)
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(UNESColor.alertRed)
                .frame(maxWidth: .infinity)
            actionButton(.meDocumentRetry, icon: "arrow.clockwise") {
                store.send(.retryTapped)
            }
        }
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
