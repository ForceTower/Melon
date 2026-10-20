import ComposableArchitecture
import SwiftUI

/// Setup, step 1: which trips the student wants help with. Also the way back
/// into setup from "Meu trajeto", where it offers removing the plan.
@Reducer
struct CommuteDirectionFeature {
    @ObservableState
    struct State: Equatable {
        /// The saved plan being edited; empty on a first setup.
        var plan: CommutePlan
        var selected: Set<TransitDirection>
        var isRemoveConfirmationPresented = false
        var isRemoving = false
        var removeFailed = false

        init(plan: CommutePlan) {
            self.plan = plan
            let saved = Set(plan.directions)
            selected = saved.isEmpty ? [.toCampus] : saved
        }

        var isEditing: Bool { !plan.isEmpty }
    }

    enum Action: Equatable {
        case toggled(TransitDirection)
        case continueTapped
        case removeTapped
        case removeConfirmationChanged(Bool)
        case removeConfirmed
        case removeFinished(succeeded: Bool)
        case removeFailureDismissed
        case delegate(Delegate)

        enum Delegate: Equatable {
            case next(CommuteSetupDraft)
            case removed
        }
    }

    @Dependency(\.commutePlanRepository) var commutePlanRepository
    @Dependency(\.analytics) var analytics

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .toggled(direction):
                if state.selected.contains(direction) {
                    guard state.selected.count > 1 else { return .none }
                    state.selected.remove(direction)
                } else {
                    state.selected.insert(direction)
                }
                return .none

            case .continueTapped:
                let directions = TransitDirection.allCases.filter(state.selected.contains)
                return .send(.delegate(.next(CommuteSetupDraft(directions: directions, existing: state.plan))))

            case .removeTapped:
                state.isRemoveConfirmationPresented = true
                return .none

            case let .removeConfirmationChanged(isPresented):
                state.isRemoveConfirmationPresented = isPresented
                return .none

            case .removeConfirmed:
                state.isRemoveConfirmationPresented = false
                state.isRemoving = true
                return .run { send in
                    do {
                        try await commutePlanRepository.remove()
                        await send(.removeFinished(succeeded: true))
                    } catch {
                        await send(.removeFinished(succeeded: false))
                    }
                }

            case let .removeFinished(succeeded):
                state.isRemoving = false
                guard succeeded else {
                    state.removeFailed = true
                    return .none
                }
                analytics.selectContent(contentType: ContentTypes.setting, itemId: "commute_removed")
                return .send(.delegate(.removed))

            case .removeFailureDismissed:
                state.removeFailed = false
                return .none

            case .delegate:
                return .none
            }
        }
    }
}

struct CommuteDirectionView: View {
    @Bindable var store: StoreOf<CommuteDirectionFeature>

    var body: some View {
        CommuteSetupScaffold(
            step: 1,
            total: totalSteps,
            title: .commuteSetupDirectionTitle,
            subtitle: .commuteSetupDirectionSubtitle
        ) {
            VStack(spacing: 10) {
                option(.toCampus, symbol: "figure.walk.arrival", title: .commuteDirectionToCampus, body: .commuteSetupToCampusBody)
                option(.fromCampus, symbol: "figure.walk.departure", title: .commuteDirectionFromCampus, body: .commuteSetupFromCampusBody)
            }
            UNESBanner(tone: .neutral, symbol: "location.slash") {
                Text(.commuteSetupNoLocationNeeded)
            }
            .padding(.top, 18)
            if store.isEditing {
                Button(role: .destructive) {
                    store.send(.removeTapped)
                } label: {
                    Group {
                        if store.isRemoving {
                            ProgressView()
                        } else {
                            Text(.commuteSetupRemove)
                        }
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }
                .disabled(store.isRemoving)
                .padding(.top, 22)
            }
        } footer: {
            Button {
                store.send(.continueTapped)
            } label: {
                Text(.commonContinue)
            }
            .buttonStyle(.unesDark)
        }
        .navigationTitle(Text(.commuteTitle))
        .confirmationDialog(
            Text(.commuteSetupRemoveConfirmTitle),
            isPresented: $store.isRemoveConfirmationPresented.sending(\.removeConfirmationChanged),
            titleVisibility: .visible
        ) {
            Button(role: .destructive) {
                store.send(.removeConfirmed)
            } label: {
                Text(.commuteSetupRemove)
            }
        } message: {
            Text(.commuteSetupRemoveConfirmBody)
        }
        .alert(
            Text(.commuteSetupRemoveFailed),
            isPresented: Binding(
                get: { store.removeFailed },
                set: { if !$0 { store.send(.removeFailureDismissed) } }
            )
        ) {
            Button(role: .cancel) {} label: { Text(.commonOk) }
        } message: {
            Text(.commuteSetupTryAgainBody)
        }
    }

    private var totalSteps: Int {
        CommuteSetupDraft(directions: Array(store.selected), existing: CommutePlan()).totalSteps
    }

    private func option(
        _ direction: TransitDirection,
        symbol: String,
        title: LocalizedStringResource,
        body: LocalizedStringResource
    ) -> some View {
        let isOn = store.selected.contains(direction)
        return Button {
            store.send(.toggled(direction), animation: UNESMotion.ease(0.25))
        } label: {
            HStack(spacing: 14) {
                Image(systemName: symbol)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(isOn ? UNESColor.surface : UNESColor.ink2)
                    .frame(width: 44, height: 44)
                    .background(isOn ? UNESColor.ink : UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 17, weight: .semibold))
                        .tracking(-0.26)
                        .foregroundStyle(UNESColor.ink)
                    Text(body)
                        .font(.system(size: 13))
                        .foregroundStyle(UNESColor.ink3)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                CommuteCheckmark(isOn: isOn)
            }
            .padding(16)
            .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(isOn ? UNESColor.ink : UNESColor.cardLine, lineWidth: 1.5)
            }
        }
        .buttonStyle(.pressableCard)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

#Preview {
    NavigationStack {
        CommuteDirectionView(store: Store(initialState: CommuteDirectionFeature.State(plan: TransitFixtures.plan)) {
            CommuteDirectionFeature()
        })
    }
}
