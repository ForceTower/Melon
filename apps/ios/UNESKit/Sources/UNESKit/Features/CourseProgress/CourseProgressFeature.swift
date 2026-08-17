import ComposableArchitecture
import Foundation

/// "Progresso do curso": completed hours against the curriculum, broken down
/// by hour type, plus the door into the fluxograma. Renders from the mirror
/// (so it works offline) and re-pulls `api/curriculum` on every entry.
@Reducer
struct CourseProgressFeature {
    @ObservableState
    struct State: Equatable {
        /// The course name from the profile — the payload carries the
        /// curriculum, not the course.
        var course: String?
        var progress: CourseProgress?
        /// A refresh is in flight and nothing is mirrored yet.
        var isLoading = false
        /// The refresh failed and nothing is mirrored to fall back on.
        var loadFailed = false
        var isComplementaryExplainerPresented = false
        var isVersionPickerPresented = false
        /// The version whose PUT (or the DELETE back to automatic, as
        /// `automaticVersionSwitch`) is in flight — the picker locks and
        /// spins on it meanwhile.
        var switchingVersionId: String?
        @Presents var alert: AlertState<Never>?

        var isSwitchingVersion: Bool { switchingVersionId != nil }

        init(course: String? = nil, progress: CourseProgress? = nil) {
            self.course = course
            self.progress = progress
        }
    }

    /// Sentinel for `switchingVersionId` while resetting to the server's
    /// resolution rather than picking a version.
    static let automaticVersionSwitch = "automatic"

    enum Action: Equatable {
        case task
        case retryTapped
        case refreshPulled
        case progressUpdated(CourseProgress?)
        case refreshFinished(succeeded: Bool)
        case flowchartTapped
        case complementaryExplainerTapped
        case complementaryExplainerDismissed
        case versionPickerTapped
        case versionPickerDismissed
        case versionSelected(String)
        case automaticVersionTapped
        case versionSwitchFinished(succeeded: Bool)
        case alert(PresentationAction<Never>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openFlowchart(CourseProgress)
        }
    }

    @Dependency(\.courseProgressRepository) var courseProgressRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("CourseProgressFeature")

    private enum CancelID { case observation, refresh, versionSwitch }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.courseProgress)
                // The observation replays the mirror on subscription, so the
                // screen paints offline; the refresh lands through it. Only
                // the very first fetch shows a spinner.
                if state.progress == nil {
                    state.isLoading = true
                }
                state.loadFailed = false
                return .merge(observeMirror(), refresh())

            case .retryTapped:
                log.info("retry refresh")
                state.isLoading = true
                state.loadFailed = false
                return refresh()

            case .refreshPulled:
                return refresh()

            case let .progressUpdated(progress):
                if let progress {
                    state.progress = progress
                    state.isLoading = false
                    state.loadFailed = false
                }
                return .none

            case let .refreshFinished(succeeded):
                state.isLoading = false
                // A stale screen beats an error screen.
                state.loadFailed = !succeeded && state.progress == nil
                return .none

            case .flowchartTapped:
                guard let progress = state.progress, progress.hasCurriculum else { return .none }
                log.info("open flowchart curriculum=\(progress.curriculum?.code ?? "<none>")")
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "curriculum_flow")
                return .send(.delegate(.openFlowchart(progress)))

            case .complementaryExplainerTapped:
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "complementary_hours_explainer")
                state.isComplementaryExplainerPresented = true
                return .none

            case .complementaryExplainerDismissed:
                state.isComplementaryExplainerPresented = false
                return .none

            case .versionPickerTapped:
                guard let progress = state.progress, progress.canPickVersion else { return .none }
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "curriculum_version_picker")
                state.isVersionPickerPresented = true
                return .none

            case .versionPickerDismissed:
                state.isVersionPickerPresented = false
                return .none

            case let .versionSelected(curriculumId):
                guard !state.isSwitchingVersion else { return .none }
                // Re-picking the bound version is a no-op: nothing to send,
                // just close.
                if state.progress?.curriculum?.id == curriculumId {
                    state.isVersionPickerPresented = false
                    return .none
                }
                log.info("select curriculum version id=\(curriculumId)")
                analytics.selectContent(contentType: ContentTypes.curriculumVersion, itemId: curriculumId)
                state.switchingVersionId = curriculumId
                return .run { send in
                    do {
                        try await courseProgressRepository.selectVersion(curriculumId)
                        await send(.versionSwitchFinished(succeeded: true))
                    } catch {
                        await send(.versionSwitchFinished(succeeded: false))
                    }
                }
                .cancellable(id: CancelID.versionSwitch, cancelInFlight: true)

            case .automaticVersionTapped:
                guard !state.isSwitchingVersion, state.progress?.curriculum?.isManualPick == true else { return .none }
                log.info("reset curriculum version to automatic")
                analytics.selectContent(contentType: ContentTypes.curriculumVersion, itemId: Self.automaticVersionSwitch)
                state.switchingVersionId = Self.automaticVersionSwitch
                return .run { send in
                    do {
                        try await courseProgressRepository.resetVersion()
                        await send(.versionSwitchFinished(succeeded: true))
                    } catch {
                        await send(.versionSwitchFinished(succeeded: false))
                    }
                }
                .cancellable(id: CancelID.versionSwitch, cancelInFlight: true)

            case let .versionSwitchFinished(succeeded):
                state.switchingVersionId = nil
                // The rebuilt payload already landed through the observation;
                // the sheet closes over the new numbers.
                if succeeded {
                    state.isVersionPickerPresented = false
                } else {
                    state.alert = AlertState {
                        TextState(String.localized(.courseProgressVersionSwitchFailedTitle))
                    } message: {
                        TextState(String.localized(.courseProgressVersionSwitchFailedBody))
                    }
                }
                return .none

            case .alert:
                return .none

            case .delegate:
                return .none
            }
        }
        .ifLet(\.$alert, action: \.alert)
    }

    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await progress in courseProgressRepository.observe() {
                await send(.progressUpdated(progress))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                try await courseProgressRepository.refresh()
                await send(.refreshFinished(succeeded: true))
            } catch {
                await send(.refreshFinished(succeeded: false))
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }
}
