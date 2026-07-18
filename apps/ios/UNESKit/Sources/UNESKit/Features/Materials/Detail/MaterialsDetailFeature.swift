import ComposableArchitecture
import Foundation

/// One material: preview, actions (útil / salvar / denunciar), metadata and
/// the semi-anonymous uploader. The student's own pending/rejected uploads
/// render the moderation status screen instead.
@Reducer
struct MaterialsDetailFeature {
    @ObservableState
    struct State: Equatable {
        var material: Material
        /// Streaming the file into the temp slot for QuickLook.
        var isOpening = false
        /// Non-nil hands the temp file to QuickLook.
        var previewURL: URL?
        var isReportPresented = false
        var reportReason: MaterialReportReason?
        var toast: Toast?
        @Presents var upload: MaterialsUploadFeature.State?

        init(material: Material) {
            self.material = material
        }

        enum Toast: Equatable {
            case saved
            case unsaved
            case reported
            case syncFailed
            case openFailed
        }

        /// Own uploads outside the public shelf show the status screen.
        var showsModerationStatus: Bool {
            material.isMine && material.status != .published
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case usefulTapped
        case usefulSyncFailed(previousCount: Int, wasUseful: Bool)
        case saveTapped
        case saveSyncFailed(wasSaved: Bool)
        case openTapped
        case openReady(URL)
        case openFailed
        case previewDismissed
        case reportTapped
        case reportConfirmed
        case reportFailed
        case reportSent
        case reuploadTapped
        case toastExpired
        case upload(PresentationAction<MaterialsUploadFeature.Action>)
        case binding(BindingAction<State>)
    }

    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("MaterialsDetailFeature")

    private enum CancelID { case open, toast }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.materialsDetail, properties: ["material_id": state.material.id])
                return .none

            case .usefulTapped:
                // Optimistic: flip locally, reconcile with the server count,
                // roll back if the vote never landed.
                let wasUseful = state.material.isUseful
                let previousCount = state.material.usefulCount
                state.material.isUseful = !wasUseful
                state.material.usefulCount += wasUseful ? -1 : 1
                log.info("useful toggle id=\(state.material.id) useful=\(!wasUseful)")
                analytics.selectContent(
                    contentType: ContentTypes.material,
                    itemId: state.material.id,
                    properties: ["action": wasUseful ? "not_useful" : "useful"]
                )
                return .run { [id = state.material.id] _ in
                    _ = try await materialsRepository.setUseful(id, !wasUseful)
                } catch: { _, send in
                    await send(.usefulSyncFailed(previousCount: previousCount, wasUseful: wasUseful))
                }

            case let .usefulSyncFailed(previousCount, wasUseful):
                state.material.isUseful = wasUseful
                state.material.usefulCount = previousCount
                return show(.syncFailed, in: &state)

            case .saveTapped:
                let wasSaved = state.material.isSaved
                state.material.isSaved = !wasSaved
                log.info("save toggle id=\(state.material.id) saved=\(!wasSaved)")
                analytics.selectContent(
                    contentType: ContentTypes.material,
                    itemId: state.material.id,
                    properties: ["action": wasSaved ? "unsave" : "save"]
                )
                return .merge(
                    show(wasSaved ? .unsaved : .saved, in: &state),
                    .run { [id = state.material.id] _ in
                        try await materialsRepository.setSaved(id, !wasSaved)
                    } catch: { _, send in
                        await send(.saveSyncFailed(wasSaved: wasSaved))
                    }
                )

            case let .saveSyncFailed(wasSaved):
                state.material.isSaved = wasSaved
                return show(.syncFailed, in: &state)

            case .openTapped:
                guard !state.isOpening else { return .none }
                state.isOpening = true
                log.info("open file id=\(state.material.id)")
                analytics.selectContent(
                    contentType: ContentTypes.material,
                    itemId: state.material.id,
                    properties: ["action": "open"]
                )
                return .run { [material = state.material] send in
                    do {
                        let url = try await materialsRepository.open(material)
                        await send(.openReady(url))
                    } catch {
                        await send(.openFailed)
                    }
                }
                .cancellable(id: CancelID.open, cancelInFlight: true)

            case let .openReady(url):
                state.isOpening = false
                state.material.downloadCount += 1
                state.previewURL = url
                return .none

            case .openFailed:
                state.isOpening = false
                return show(.openFailed, in: &state)

            case .previewDismissed:
                state.previewURL = nil
                return .none

            case .reportTapped:
                state.reportReason = nil
                state.isReportPresented = true
                return .none

            case .reportConfirmed:
                guard let reason = state.reportReason else { return .none }
                state.isReportPresented = false
                analytics.selectContent(
                    contentType: ContentTypes.material,
                    itemId: state.material.id,
                    properties: ["action": "report", "reason": reason.rawValue]
                )
                return .run { [id = state.material.id] send in
                    try await materialsRepository.report(id, reason)
                    await send(.reportSent)
                } catch: { _, send in
                    await send(.reportFailed)
                }

            case .reportSent:
                return show(.reported, in: &state)

            case .reportFailed:
                return show(.syncFailed, in: &state)

            case .reuploadTapped:
                log.info("reupload id=\(state.material.id)")
                let ref = state.material.discipline
                let discipline = MaterialsDiscipline(
                    id: ref.id, code: ref.code, name: ref.name,
                    teacherName: state.material.teacherName,
                    colorIndex: ref.colorIndex, counts: [:]
                )
                state.upload = MaterialsUploadFeature.State(
                    disciplines: [discipline],
                    semester: nil,
                    locked: discipline
                )
                return .none

            case .toastExpired:
                state.toast = nil
                return .none

            case .upload(.presented(.delegate(.finished))):
                state.upload = nil
                return .none

            case .upload, .binding:
                return .none
            }
        }
        .ifLet(\.$upload, action: \.upload) {
            MaterialsUploadFeature()
        }
    }

    private func show(_ toast: State.Toast, in state: inout State) -> Effect<Action> {
        state.toast = toast
        return .run { send in
            try await clock.sleep(for: .milliseconds(2200))
            await send(.toastExpired)
        }
        .cancellable(id: CancelID.toast, cancelInFlight: true)
    }
}
