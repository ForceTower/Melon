import ComposableArchitecture
import Foundation

/// A file picked or scanned for upload, already flattened to PDF data.
struct MaterialPickedFile: Equatable, Sendable {
    var fileName: String
    var byteCount: Int
    var pages: Int
    var data: Data
    /// Came from the camera scanner rather than the file picker.
    var isScan: Bool
}

/// The contribution wizard: (discipline →) source → details → guidelines →
/// success. The guidelines step runs once — after the first acknowledged
/// submission it's skipped and "Continuar" submits directly.
@Reducer
struct MaterialsUploadFeature {
    @ObservableState
    struct State: Equatable {
        enum Step: Equatable, Hashable {
            case pickDiscipline
            case source
            case details
            case guidelines
            case success
        }

        /// Choices for the discipline picker; a single locked entry when the
        /// flow starts inside a discipline.
        var disciplines: [MaterialsDiscipline]
        /// Set → the picker is skipped and back never returns to it.
        var isDisciplineLocked: Bool
        var discipline: MaterialsDiscipline?
        /// Quick-picks only; `semester` is free text and may hold anything.
        var semesterOptions: [String]
        /// First screen of the sheet's own NavigationStack.
        let root: Step
        /// Steps pushed past the root — value-routed so the native pop
        /// gesture works between them.
        var path: [Step] = []
        var file: MaterialPickedFile?
        var type: MaterialType = .exam
        var title = ""
        var semester: String
        var teacherName = ""
        var isGuidelinesAccepted = false
        var isSubmitting = false
        var submitFailed = false
        var submitted: Material?
        var isScannerPresented = false
        var isFileImporterPresented = false
        var filePickFailed = false
        @Shared(.appStorage("materials_guidelines_acknowledged")) var hasAcknowledgedGuidelines = false

        /// `initialSemester` pre-fills the field for a re-upload, which should
        /// keep the semester of the material it replaces. Otherwise the field
        /// adopts the newest mirrored semester once `.task` loads the chips.
        init(
            disciplines: [MaterialsDiscipline],
            initialSemester: String? = nil,
            locked: MaterialsDiscipline? = nil
        ) {
            let resolved = locked ?? (disciplines.count == 1 ? disciplines.first : nil)
            self.disciplines = disciplines
            isDisciplineLocked = locked != nil
            discipline = resolved
            semesterOptions = []
            semester = initialSemester ?? ""
            root = resolved == nil ? .pickDiscipline : .source
        }

        var canContinue: Bool {
            title.trimmingCharacters(in: .whitespaces).count > 1 && file != nil && isSemesterValid
        }

        /// Non-blank and within the server's length cap. No shape check — the
        /// server takes any label and every upload is moderated, so a student
        /// on a term we have never seen ("26.2PGM") can still tag it.
        var isSemesterValid: Bool {
            let trimmed = semester.trimmingCharacters(in: .whitespaces)
            return !trimmed.isEmpty && trimmed.count <= MaterialsFormat.semesterMaxLength
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case semesterOptionsLoaded([String])
        case disciplinePicked(MaterialsDiscipline)
        case sourceFileTapped
        case sourceScanTapped
        case fileImported(URL)
        case filePicked(MaterialPickedFile)
        case filePickFailed
        case closeTapped
        case continueTapped
        case submitTapped
        case submitted(Material)
        case submitFailed
        case trackTapped
        case doneTapped
        case binding(BindingAction<State>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case finished(Material, track: Bool)
        }
    }

    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.dismiss) var dismiss
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("MaterialsUploadFeature")

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
                guard state.semesterOptions.isEmpty else { return .none }
                return .run { send in
                    guard let options = try? await materialsRepository.uploadSemesters() else { return }
                    await send(.semesterOptionsLoaded(options))
                }

            case let .semesterOptionsLoaded(options):
                guard !options.isEmpty else { return .none }
                let current = state.semester.trimmingCharacters(in: .whitespaces)
                // A pre-filled semester the mirror doesn't know (an old
                // re-upload) still deserves a chip rather than looking unset.
                state.semesterOptions =
                    current.isEmpty || options.contains(current) ? options : [current] + options
                if current.isEmpty {
                    state.semester = options[0]
                }
                return .none

            case let .disciplinePicked(discipline):
                log.info("upload discipline picked id=\(discipline.id)")
                state.discipline = discipline
                state.path.append(.source)
                return .none

            case .sourceFileTapped:
                state.filePickFailed = false
                state.isFileImporterPresented = true
                return .none

            case .sourceScanTapped:
                state.filePickFailed = false
                state.isScannerPresented = true
                return .none

            case let .fileImported(url):
                // Reading + page counting happens off the main actor; the
                // picker URL is security-scoped.
                return .run { send in
                    do {
                        let file = try MaterialFileReader.read(url)
                        await send(.filePicked(file))
                    } catch {
                        await send(.filePickFailed)
                    }
                }

            case let .filePicked(file):
                log.info("upload file ready scan=\(file.isScan) pages=\(file.pages) bytes=\(file.byteCount)")
                state.file = file
                state.isScannerPresented = false
                state.path.append(.details)
                return .none

            case .filePickFailed:
                log.warn("upload file pick failed")
                state.isScannerPresented = false
                state.filePickFailed = true
                return .none

            case .closeTapped:
                return .run { _ in await dismiss() }

            case .continueTapped:
                guard state.canContinue else { return .none }
                if state.hasAcknowledgedGuidelines {
                    return submit(&state)
                }
                state.isGuidelinesAccepted = false
                state.path.append(.guidelines)
                return .none

            case .submitTapped:
                guard state.isGuidelinesAccepted else { return .none }
                return submit(&state)

            case let .submitted(material):
                log.info("upload submitted id=\(material.id)")
                analytics.selectContent(
                    contentType: ContentTypes.material,
                    itemId: material.id,
                    properties: ["action": "submit"]
                )
                state.isSubmitting = false
                state.submitted = material
                state.$hasAcknowledgedGuidelines.withLock { $0 = true }
                state.path.append(.success)
                return .none

            case .submitFailed:
                state.isSubmitting = false
                state.submitFailed = true
                return .none

            case .trackTapped:
                guard let material = state.submitted else { return .none }
                return .send(.delegate(.finished(material, track: true)))

            case .doneTapped:
                guard let material = state.submitted else {
                    return .run { _ in await dismiss() }
                }
                return .send(.delegate(.finished(material, track: false)))

            case .binding(\.path):
                // Popping back past the source step (gesture or chevron)
                // returns to the picker — the choice no longer stands.
                if state.path.isEmpty, state.root == .pickDiscipline {
                    state.discipline = nil
                }
                return .none

            case .binding, .delegate:
                return .none
            }
        }
    }

    private func submit(_ state: inout State) -> Effect<Action> {
        guard let discipline = state.discipline, let file = state.file, !state.isSubmitting else {
            return .none
        }
        state.isSubmitting = true
        state.submitFailed = false
        let teacher = state.teacherName.trimmingCharacters(in: .whitespaces)
        let submission = MaterialSubmission(
            disciplineId: discipline.id,
            type: state.type,
            title: state.title.trimmingCharacters(in: .whitespaces),
            semester: state.semester.trimmingCharacters(in: .whitespaces),
            teacherName: teacher.isEmpty ? nil : teacher,
            fileKind: .pdf,
            pages: file.pages,
            fileName: file.fileName,
            data: file.data
        )
        return .run { send in
            do {
                let material = try await materialsRepository.submit(submission)
                await send(.submitted(material))
            } catch {
                await send(.submitFailed)
            }
        }
    }
}
