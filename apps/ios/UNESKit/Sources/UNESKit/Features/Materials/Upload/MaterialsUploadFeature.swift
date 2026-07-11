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

        init(
            disciplines: [MaterialsDiscipline],
            semester: String?,
            locked: MaterialsDiscipline? = nil,
            now: Date = .now
        ) {
            let resolved = locked ?? (disciplines.count == 1 ? disciplines.first : nil)
            let options = MaterialsFormat.uploadSemesters(
                from: semester ?? MaterialsFormat.currentSemester(now: now)
            )
            self.disciplines = disciplines
            isDisciplineLocked = locked != nil
            discipline = resolved
            semesterOptions = options
            self.semester = options.first ?? ""
            root = resolved == nil ? .pickDiscipline : .source
        }

        var canContinue: Bool {
            title.trimmingCharacters(in: .whitespaces).count > 1 && file != nil
        }
    }

    enum Action: Equatable, BindableAction {
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

    private let log = Log.scoped("MaterialsUploadFeature")

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
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
            semester: state.semester,
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

extension MaterialsFormat {
    /// The Brazilian academic semester a date falls in — "2026.1" through
    /// June, "2026.2" after.
    static func currentSemester(now: Date) -> String {
        let components = Calendar(identifier: .gregorian).dateComponents([.year, .month], from: now)
        let year = components.year ?? 2026
        let term = (components.month ?? 1) <= 6 ? 1 : 2
        return "\(year).\(term)"
    }
}
