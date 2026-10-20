import ComposableArchitecture
import SwiftUI

/// Setup, last step per direction: the lines the student really takes at that
/// stop and how long the walk there is. Leaving campus has a single boarding
/// stop, so that direction comes straight here.
@Reducer
struct CommuteLinesFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.commutePlan) var plan
        var draft: CommuteSetupDraft
        var direction: TransitDirection
        var stop: TransitStop?
        var network: TransitNetwork?
        var loadFailed = false
        var selected: [String] = []
        var walkMinutes: Int

        init(draft: CommuteSetupDraft, direction: TransitDirection, stop: TransitStop? = nil) {
            self.draft = draft
            self.direction = direction
            self.stop = stop
            walkMinutes = draft.existing.leg(for: direction)?.walkMinutes ?? (direction == .toCampus ? 5 : 3)
        }

        var serving: [String] {
            guard let network, let stop else { return [] }
            return network.lines(serving: stop.id, direction: direction)
        }

        var isLastStep: Bool { draft.direction(after: direction) == nil }
    }

    enum Action: Equatable {
        case task
        case networkLoaded(TransitNetwork)
        case networkFailed
        case lineToggled(String)
        case walkChanged(Int)
        case saveTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case next(CommuteSetupDraft)
            case finished(TransitDirection)
        }
    }

    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.analytics) var analytics

    static let walkRange = 0...30

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.network == nil else { return .none }
                state.loadFailed = false
                return .run { send in
                    do {
                        await send(.networkLoaded(try await transitRepository.network()))
                    } catch {
                        await send(.networkFailed)
                    }
                }

            case let .networkLoaded(network):
                state.network = network
                if state.stop == nil {
                    state.stop = network.campusDepartureStop
                }
                let serving = state.serving
                var previous: [String] = []
                if let leg = state.draft.existing.leg(for: state.direction), leg.stopId == state.stop?.id {
                    previous = leg.lineNumbers.filter(serving.contains)
                }
                state.selected = previous.isEmpty ? serving : previous
                return .none

            case .networkFailed:
                state.loadFailed = true
                return .none

            case let .lineToggled(line):
                guard state.serving.contains(line) else { return .none }
                if let index = state.selected.firstIndex(of: line) {
                    state.selected.remove(at: index)
                } else {
                    state.selected = state.serving.filter { $0 == line || state.selected.contains($0) }
                }
                return .none

            case let .walkChanged(minutes):
                state.walkMinutes = min(max(minutes, Self.walkRange.lowerBound), Self.walkRange.upperBound)
                return .none

            case .saveTapped:
                guard let stop = state.stop, !state.selected.isEmpty else { return .none }
                state.draft.plan.setLeg(
                    CommuteLeg(
                        stopId: stop.id,
                        stopName: stop.name,
                        stopArea: stop.area,
                        lineNumbers: state.selected,
                        walkMinutes: state.walkMinutes
                    ),
                    for: state.direction
                )
                guard state.isLastStep else { return .send(.delegate(.next(state.draft))) }
                let plan = state.draft.plan
                state.$plan.withLock { $0 = plan }
                analytics.selectContent(
                    contentType: ContentTypes.setting,
                    itemId: "commute_saved",
                    properties: ["directions": plan.directions.map(\.rawValue).joined(separator: ",")]
                )
                return .send(.delegate(.finished(state.draft.directions.first ?? state.direction)))

            case .delegate:
                return .none
            }
        }
    }
}

struct CommuteLinesView: View {
    @Bindable var store: StoreOf<CommuteLinesFeature>

    var body: some View {
        CommuteSetupScaffold(
            step: store.draft.linesStep(for: store.direction),
            total: store.draft.totalSteps,
            title: store.direction == .toCampus ? .commuteSetupLinesTitle : .commuteSetupLinesTitleLeaving,
            subtitle: .commuteSetupLinesSubtitle
        ) {
            if let network = store.network {
                lines(network)
                walk
                    .padding(.top, 22)
            } else if store.loadFailed {
                UNESBanner(
                    tone: .neutral,
                    title: .localized(.commuteHeroUnavailable),
                    action: .localized(.commonTryAgain),
                    onAction: { store.send(.task) },
                    symbol: "icloud.slash"
                ) {
                    Text(.commuteSetupStopLoadFailed)
                }
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            }
        } footer: {
            Button {
                store.send(.saveTapped)
            } label: {
                Text(store.isLastStep ? .commuteSetupSave : .commonContinue)
            }
            .buttonStyle(.unesDark)
            .disabled(store.selected.isEmpty || store.stop == nil)
        }
        .navigationTitle(Text(verbatim: store.stop?.name ?? ""))
        .task { await store.send(.task).finish() }
    }

    private func lines(_ network: TransitNetwork) -> some View {
        let serving = store.serving
        let ordered = network.lines.filter { serving.contains($0.number) } + network.lines.filter { !serving.contains($0.number) }
        return VStack(spacing: 0) {
            ForEach(ordered) { line in
                row(line, network: network)
                if line.id != ordered.last?.id {
                    Divider().overlay(UNESColor.line)
                }
            }
        }
        .transitCard()
    }

    private func row(_ line: TransitLine, network: TransitNetwork) -> some View {
        let serves = store.serving.contains(line.number)
        let isOn = store.selected.contains(line.number)
        let typical = network.route(line: line.number, direction: store.direction)?.typicalMinutes
        return Button {
            store.send(.lineToggled(line.number), animation: UNESMotion.ease(0.25))
        } label: {
            HStack(spacing: 12) {
                TransitLineBadge(number: line.number, size: .large, ghost: !isOn)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: line.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                    Text(verbatim: detail(serves: serves, typical: typical))
                        .font(.system(size: 12.5))
                        .foregroundStyle(UNESColor.ink3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if serves {
                    CommuteCheckmark(isOn: isOn)
                }
            }
            .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 14))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!serves)
        .opacity(serves ? 1 : 0.45)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    private func detail(serves: Bool, typical: ClosedRange<Int>?) -> String {
        guard serves else { return .localized(.commuteSetupLineDoesNotServe) }
        guard let typical else { return .localized(.commuteSetupLineServes) }
        return typical.lowerBound == typical.upperBound
            ? .localized(.commuteSetupLineDuration(typical.lowerBound))
            : .localized(.commuteSetupLineDurationRange(typical.lowerBound, typical.upperBound))
    }

    private var walk: some View {
        VStack(alignment: .leading, spacing: 0) {
            CommuteSectionHeader(
                title: .localized(.commuteSetupWalkTitle),
                hint: .localized(store.direction == .toCampus ? .commuteSetupWalkHint : .commuteSetupWalkHintLeaving)
            )
            Stepper(
                value: Binding(get: { store.walkMinutes }, set: { store.send(.walkChanged($0)) }),
                in: CommuteLinesFeature.walkRange
            ) {
                Label {
                    HStack(spacing: 6) {
                        Text(.commuteSetupWalkLabel)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(UNESColor.ink)
                        Spacer(minLength: 0)
                        Text(.commuteWalkMinutesValue(store.walkMinutes))
                            .font(.system(size: 17, weight: .bold))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink)
                            .contentTransition(.numericText())
                    }
                } icon: {
                    Image(systemName: "figure.walk")
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            .transitCard(padding: EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 14))
        }
    }
}

#Preview {
    NavigationStack {
        CommuteLinesView(store: Store(initialState: CommuteLinesFeature.State(
            draft: CommuteSetupDraft(directions: [.toCampus], existing: CommutePlan()),
            direction: .toCampus,
            stop: TransitFixtures.network.route(line: "087", direction: .toCampus)?.stops[4]
        )) {
            CommuteLinesFeature()
        } withDependencies: {
            $0.transitRepository = .mock(mode: .normal)
        })
    }
}
