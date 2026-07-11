import ComposableArchitecture
import SwiftUI
import UniformTypeIdentifiers

/// The contribution wizard sheet. Steps are value-routed on the sheet's own
/// NavigationStack, so the native pop gesture and back chevron work between
/// them; the trailing xmark closes the whole sheet from any step.
struct MaterialsUploadSheet: View {
    @Bindable var store: StoreOf<MaterialsUploadFeature>

    private typealias Step = MaterialsUploadFeature.State.Step

    var body: some View {
        NavigationStack(path: $store.path) {
            stepView(store.root)
                .navigationDestination(for: Step.self) { step in
                    stepView(step)
                }
        }
        .tint(UNESColor.accent)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .pdfImporter(isPresented: $store.isFileImporterPresented) { result in
            switch result {
            case let .success(url):
                store.send(.fileImported(url))
            case .failure:
                store.send(.filePickFailed)
            }
        }
        .scannerSheet(
            isPresented: $store.isScannerPresented,
            onComplete: { store.send(.filePicked($0)) },
            onCancel: { store.send(.binding(.set(\.isScannerPresented, false))) },
            onFail: { store.send(.filePickFailed) }
        )
        .interactiveDismissDisabled(store.isSubmitting)
    }

    // MARK: Steps

    @ViewBuilder
    private func stepView(_ step: Step) -> some View {
        switch step {
        case .pickDiscipline:
            scaffold(title: .materialsUploadTitleDiscipline, showsDiscipline: false) {
                disciplinePicker
            }
        case .source:
            scaffold(title: .materialsUploadTitleNew) {
                sourcePicker
            }
        case .details:
            scaffold(title: .materialsUploadTitleDetails, progressSegments: 1, cta: step) {
                detailsForm
            }
        case .guidelines:
            scaffold(title: .materialsUploadTitleGuidelines, progressSegments: 2, cta: step) {
                guidelines
            }
        case .success:
            // Terminal: no bar, no way back into the submitted form.
            ScrollView {
                success
                    .padding(EdgeInsets(top: 4, leading: 20, bottom: 24, trailing: 20))
            }
            .scrollIndicators(.hidden)
            .background(UNESColor.surface)
            .navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    /// Shared step chrome: inline nav title (+ discipline subtitle), close
    /// button, optional progress rail and pinned CTA.
    private func scaffold(
        title: LocalizedStringResource,
        showsDiscipline: Bool = true,
        progressSegments: Int? = nil,
        cta: Step? = nil,
        @ViewBuilder content: () -> some View
    ) -> some View {
        VStack(spacing: 0) {
            if let progressSegments {
                progress(filled: progressSegments)
            }
            ScrollView {
                content()
                    .padding(EdgeInsets(top: 4, leading: 20, bottom: 24, trailing: 20))
            }
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.interactively)
            if let cta {
                footer(for: cta)
            }
        }
        .background(UNESColor.surface)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principalCompat) {
                VStack(spacing: 1) {
                    Text(title)
                        .font(.system(size: 16, weight: .bold))
                        .tracking(-0.32)
                        .foregroundStyle(UNESColor.ink)
                    if showsDiscipline, let discipline = store.discipline {
                        Text("\(discipline.code) · \(discipline.name)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                            .lineLimit(1)
                    }
                }
            }
            ToolbarItem(placement: .trailingCompat) {
                Button {
                    store.send(.closeTapped)
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
    }

    /// Two segments — details, guidelines.
    private func progress(filled: Int) -> some View {
        HStack(spacing: 5) {
            ForEach(0..<2) { index in
                RoundedRectangle(cornerRadius: 2)
                    .fill(index < filled ? UNESColor.accent : UNESColor.surface3)
                    .frame(height: 4)
            }
        }
        .padding(EdgeInsets(top: 10, leading: 20, bottom: 10, trailing: 20))
    }

    // MARK: Discipline picker

    private var disciplinePicker: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(.materialsUploadPickDiscipline)
                .font(.system(size: 14, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
            VStack(spacing: 10) {
                ForEach(store.disciplines) { discipline in
                    disciplineOption(discipline)
                }
            }
        }
    }

    private func disciplineOption(_ discipline: MaterialsDiscipline) -> some View {
        let tone = UNESColor.disciplineReadableColor(discipline.colorIndex)
        return Button {
            store.send(.disciplinePicked(discipline))
        } label: {
            HStack(spacing: 13) {
                Text(discipline.code)
                    .font(.system(size: 11, weight: .heavy))
                    .tracking(0.2)
                    .foregroundStyle(tone)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                    .padding(.horizontal, 4)
                    .frame(width: 44, height: 44)
                    .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(discipline.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    Text(discipline.total == 1
                        ? .materialsCountOne(discipline.total)
                        : .materialsCountOther(discipline.total))
                        .font(.system(size: 12.5, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(14)
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
        .materialsCard(cornerRadius: 18)
    }

    // MARK: Source picker

    private var sourcePicker: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.materialsUploadSourceIntro)
                .font(.system(size: 14, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 18)

            VStack(spacing: 12) {
                sourceOption(
                    icon: "doc",
                    tone: UNESColor.readable(0x3B9EAE),
                    title: .materialsUploadSourceFile,
                    subtitle: .materialsUploadSourceFileHint
                ) {
                    store.send(.sourceFileTapped)
                }
                if scannerAvailable {
                    sourceOption(
                        icon: "camera",
                        tone: UNESColor.readable(0xB23A7A),
                        title: .materialsUploadSourceScan,
                        subtitle: .materialsUploadSourceScanHint
                    ) {
                        store.send(.sourceScanTapped)
                    }
                }
            }

            if store.filePickFailed {
                Text(.materialsUploadFilePickFailed)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.coral)
                    .padding(.top, 12)
            }

            HStack(alignment: .top, spacing: 9) {
                Image(systemName: "info.circle")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 1)
                Text(.materialsUploadSourceNotice)
                    .font(.system(size: 12.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink3)
            }
            .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .padding(.top, 18)
        }
    }

    private var scannerAvailable: Bool {
        #if os(iOS)
        MaterialScannerView.isSupported
        #else
        false
        #endif
    }

    private func sourceOption(
        icon: String,
        tone: Color,
        title: LocalizedStringResource,
        subtitle: LocalizedStringResource,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 15) {
                Image(systemName: icon)
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(tone)
                    .frame(width: 52, height: 52)
                    .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .tracking(-0.32)
                        .foregroundStyle(UNESColor.ink)
                    Text(subtitle)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(16)
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
        .materialsCard()
    }

    // MARK: Details form

    private var detailsForm: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let file = store.file {
                fileCard(file)
                    .padding(.bottom, 22)
            }

            field(.materialsUploadFieldType) {
                typeGrid
            }
            field(.materialsUploadFieldTitle) {
                textInput(
                    text: $store.title,
                    placeholder: titlePlaceholder
                )
            }
            field(.materialsUploadFieldSemester) {
                semesterChips
            }
            field(.materialsUploadFieldTeacher, optional: true) {
                textInput(
                    text: $store.teacherName,
                    placeholder: teacherPlaceholder
                )
            }
        }
    }

    private var titlePlaceholder: LocalizedStringResource {
        switch store.type {
        case .exam: .materialsUploadTitlePlaceholderExam
        case .solvedList: .materialsUploadTitlePlaceholderList
        case .summary, .formulaSheet: .materialsUploadTitlePlaceholderGeneric
        }
    }

    private var teacherPlaceholder: LocalizedStringResource {
        if let teacher = store.discipline?.teacherName {
            .materialsUploadTeacherPlaceholderExample(teacher)
        } else {
            .materialsUploadTeacherPlaceholder
        }
    }

    private func fileCard(_ file: MaterialPickedFile) -> some View {
        HStack(spacing: 12) {
            Image(systemName: file.isScan ? "camera" : "doc")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 40, height: 40)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 1) {
                Text(file.fileName)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text(fileMeta(file))
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            Spacer(minLength: 8)
            Image(systemName: "checkmark")
                .font(.system(size: 14, weight: .heavy))
                .foregroundStyle(UNESColor.successGreen)
        }
        .padding(13)
        .materialsCard(cornerRadius: 16)
    }

    private func fileMeta(_ file: MaterialPickedFile) -> String {
        let pages = String.localized(file.pages == 1
            ? .materialsDetailPagesLongOne(file.pages)
            : .materialsDetailPagesLongOther(file.pages))
        return "\(pages) · \(MaterialsFormat.byteCount(file.byteCount))"
    }

    private func field(
        _ label: LocalizedStringResource,
        optional: Bool = false,
        @ViewBuilder content: () -> some View
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(label)
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.66)
                    .foregroundStyle(UNESColor.ink3)
                if optional {
                    Text(.materialsUploadOptional)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 18)
    }

    private func textInput(text: Binding<String>, placeholder: LocalizedStringResource) -> some View {
        TextField(String.localized(placeholder), text: text)
            .font(.system(size: 16, weight: .medium))
            .tracking(-0.16)
            .foregroundStyle(UNESColor.ink)
            .padding(.horizontal, 15)
            .frame(height: 50)
            .materialsCard(cornerRadius: 14)
    }

    private var typeGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 9) {
            ForEach(MaterialType.allCases, id: \.self) { type in
                typeOption(type)
            }
        }
    }

    private func typeOption(_ type: MaterialType) -> some View {
        let isOn = store.type == type
        return Button {
            store.send(.binding(.set(\.type, type)))
        } label: {
            HStack(spacing: 9) {
                Image(systemName: type.icon)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
                    .background(type.tone, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                Text(type.label)
                    .font(.system(size: 13.5, weight: .semibold))
                    .tracking(-0.14)
                    .foregroundStyle(isOn ? type.tone : UNESColor.ink)
                Spacer(minLength: 0)
            }
            .padding(EdgeInsets(top: 11, leading: 12, bottom: 11, trailing: 12))
            .background(
                isOn ? AnyShapeStyle(type.tone.opacity(0.1)) : AnyShapeStyle(UNESColor.card),
                in: RoundedRectangle(cornerRadius: 14, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(isOn ? type.tone : UNESColor.cardLine, lineWidth: 1.5)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
    }

    private var semesterChips: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                ForEach(store.semesterOptions, id: \.self) { semester in
                    let isOn = store.semester == semester
                    Button {
                        store.send(.binding(.set(\.semester, semester)))
                    } label: {
                        Text(semester)
                            .font(.system(size: 13.5, weight: .semibold))
                            .monospacedDigit()
                            .foregroundStyle(isOn ? UNESColor.surface : UNESColor.ink2)
                            .padding(.horizontal, 14)
                            .frame(height: 34)
                            .background(
                                isOn ? AnyShapeStyle(UNESColor.ink) : AnyShapeStyle(UNESColor.card),
                                in: Capsule()
                            )
                            .overlay {
                                if !isOn {
                                    Capsule().strokeBorder(UNESColor.cardLine)
                                }
                            }
                    }
                    .buttonStyle(TilePressStyle())
                }
            }
            // Bleeds through the sheet inset so scrolled chips reach the
            // display edge instead of clipping at the content column.
            .padding(.horizontal, 20)
        }
        .scrollIndicators(.hidden)
        .padding(.horizontal, -20)
    }

    // MARK: Guidelines

    private var guidelines: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.materialsUploadGuidelinesIntro)
                .font(.system(size: 14, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 18)

            VStack(spacing: 12) {
                rule(
                    icon: "exclamationmark.triangle",
                    tone: UNESColor.readable(0xE85D4E),
                    title: .materialsUploadRuleSemesterTitle,
                    text: .materialsUploadRuleSemesterText
                )
                rule(
                    icon: "checkmark.shield",
                    tone: UNESColor.readable(0xD9852E),
                    title: .materialsUploadRuleTeacherTitle,
                    text: .materialsUploadRuleTeacherText
                )
                rule(
                    icon: "sparkles",
                    tone: UNESColor.readable(0x2F9E5E),
                    title: .materialsUploadRuleQualityTitle,
                    text: .materialsUploadRuleQualityText
                )
            }
            .padding(.bottom, 20)

            acknowledgeToggle
        }
    }

    private func rule(
        icon: String,
        tone: Color,
        title: LocalizedStringResource,
        text: LocalizedStringResource
    ) -> some View {
        HStack(alignment: .top, spacing: 13) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(tone)
                .frame(width: 34, height: 34)
                .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.22)
                    .foregroundStyle(UNESColor.ink)
                Text(text)
                    .font(.system(size: 13, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink3)
            }
            Spacer(minLength: 0)
        }
        .padding(15)
        .materialsCard(cornerRadius: 18)
    }

    private var acknowledgeToggle: some View {
        Button {
            store.send(.binding(.set(\.isGuidelinesAccepted, !store.isGuidelinesAccepted)))
        } label: {
            HStack(spacing: 12) {
                Image(systemName: store.isGuidelinesAccepted ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(store.isGuidelinesAccepted ? UNESColor.successGreen : UNESColor.surface3)
                Text(.materialsUploadAck)
                    .font(.system(size: 14, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
            .background(
                store.isGuidelinesAccepted ? UNESColor.successGreen.opacity(0.1) : UNESColor.surface2,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(
                        store.isGuidelinesAccepted ? UNESColor.successGreen.opacity(0.4) : .clear,
                        lineWidth: 1.5
                    )
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
    }

    // MARK: Success

    private var success: some View {
        VStack(spacing: 0) {
            Image(systemName: "clock")
                .font(.system(size: 34, weight: .medium))
                .foregroundStyle(UNESColor.readable(0xD9852E))
                .frame(width: 80, height: 80)
                .background(
                    UNESColor.readable(0xD9852E).opacity(0.12),
                    in: RoundedRectangle(cornerRadius: 28, style: .continuous)
                )
                .scaleIn(delay: 0.05, duration: 0.55)
                .padding(.top, 20)

            Text(.materialsUploadSuccessTitle)
                .font(.system(size: 23, weight: .bold))
                .tracking(-0.69)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.center)
                .padding(.top, 20)
                .fadeUp(delay: 0.1)

            Text(.materialsUploadSuccessSubtitle)
                .font(.system(size: 14.5, weight: .medium))
                .lineSpacing(4)
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
                .padding(.top, 10)
                .padding(.horizontal, 8)
                .fadeUp(delay: 0.14)

            submittedCard
                .padding(.top, 22)
                .fadeUp(delay: 0.18)

            VStack(spacing: 10) {
                Button {
                    store.send(.trackTapped)
                } label: {
                    Text(.materialsUploadSuccessTrack)
                        .tracking(-0.17)
                }
                .buttonStyle(.unesAccent)
                Button {
                    store.send(.doneTapped)
                } label: {
                    Text(.materialsUploadSuccessDone)
                        .tracking(-0.17)
                }
                .buttonStyle(.unesNeutral)
            }
            .padding(.top, 22)
            .fadeUp(delay: 0.22)
        }
        .padding(.horizontal, 4)
    }

    private var submittedCard: some View {
        HStack(spacing: 12) {
            MaterialTypeBadge(type: store.type, size: 42)
            VStack(alignment: .leading, spacing: 2) {
                Text(store.title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text("\(String.localized(store.type.label)) · \(store.semester)")
                    .font(.system(size: 12.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            Spacer(minLength: 8)
            MaterialStatusPill(status: .pending)
        }
        .padding(14)
        .materialsCard(cornerRadius: 16)
    }

    // MARK: Footer

    private func footer(for step: Step) -> some View {
        VStack(spacing: 10) {
            if store.submitFailed {
                Text(.materialsUploadFailed)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.coral)
                    .multilineTextAlignment(.center)
            }
            Button {
                store.send(step == .details ? .continueTapped : .submitTapped)
            } label: {
                HStack(spacing: 8) {
                    if store.isSubmitting {
                        ProgressView()
                            .tint(.white)
                    }
                    Text(footerLabel(for: step))
                        .tracking(-0.17)
                }
            }
            .buttonStyle(.unesAccent)
            .disabled(footerDisabled(for: step))
        }
        .padding(EdgeInsets(top: 12, leading: 20, bottom: 16, trailing: 20))
        .background(UNESColor.surface)
        .overlay(alignment: .top) {
            Divider().overlay(UNESColor.line)
        }
    }

    private func footerLabel(for step: Step) -> LocalizedStringResource {
        if store.isSubmitting {
            .materialsUploadUploading
        } else if step == .guidelines || store.hasAcknowledgedGuidelines {
            .materialsUploadSubmit
        } else {
            .materialsUploadContinue
        }
    }

    private func footerDisabled(for step: Step) -> Bool {
        if store.isSubmitting { return true }
        return switch step {
        case .details: !store.canContinue
        case .guidelines: !store.isGuidelinesAccepted
        default: false
        }
    }
}

extension View {
    /// The system PDF picker; absent on watchOS, where this sheet is never
    /// mounted.
    @ViewBuilder
    fileprivate func pdfImporter(
        isPresented: Binding<Bool>,
        onCompletion: @escaping (Result<URL, Error>) -> Void
    ) -> some View {
        #if os(watchOS)
        self
        #else
        fileImporter(
            isPresented: isPresented,
            allowedContentTypes: [.pdf],
            onCompletion: onCompletion
        )
        #endif
    }

    /// The VisionKit scanner rides a sheet on iOS; elsewhere it's never
    /// offered.
    @ViewBuilder
    fileprivate func scannerSheet(
        isPresented: Binding<Bool>,
        onComplete: @escaping (MaterialPickedFile) -> Void,
        onCancel: @escaping () -> Void,
        onFail: @escaping () -> Void
    ) -> some View {
        #if os(iOS)
        sheet(isPresented: isPresented) {
            MaterialScannerView(onComplete: onComplete, onCancel: onCancel, onFail: onFail)
                .ignoresSafeArea()
        }
        #else
        self
        #endif
    }
}

#Preview("Do início") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            MaterialsUploadSheet(
                store: Store(
                    initialState: MaterialsUploadFeature.State(
                        disciplines: MaterialsOverview.preview().disciplines,
                        semester: "2025.2"
                    )
                ) {
                    MaterialsUploadFeature()
                }
            )
        }
}

#Preview("Turma fixa") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            MaterialsUploadSheet(
                store: Store(
                    initialState: MaterialsUploadFeature.State(
                        disciplines: [MaterialsOverview.preview().disciplines[0]],
                        semester: "2025.2",
                        locked: MaterialsOverview.preview().disciplines[0]
                    )
                ) {
                    MaterialsUploadFeature()
                }
            )
        }
}
