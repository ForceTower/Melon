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

        init(course: String? = nil, progress: CourseProgress? = nil) {
            self.course = course
            self.progress = progress
        }
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case refreshPulled
        case progressUpdated(CourseProgress?)
        case refreshFinished(succeeded: Bool)
        case flowchartTapped
        case complementaryExplainerTapped
        case complementaryExplainerDismissed
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openFlowchart(CourseProgress)
        }
    }

    @Dependency(\.courseProgressRepository) var courseProgressRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("CourseProgressFeature")

    private enum CancelID { case observation, refresh }

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

            case .delegate:
                return .none
            }
        }
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
