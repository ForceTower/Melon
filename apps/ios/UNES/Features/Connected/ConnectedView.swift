import SwiftUI
@preconcurrency import Umbrella

/// Authenticated shell shown after onboarding completes. Hosts the tab bar
/// and routes to each feature. Named for the "◦ conectado" moment in the
/// onboarding flow.
struct ConnectedView: View {
    let overview: OverviewFactory
    let scheduleFocused: ScheduleFocusedFactory
    let disciplines: DisciplinesFactory
    let messages: MessagesFactory
    let me: MeFactory
    let refreshSession: SyncRefreshSessionUseCase
    let backfillMirror: SyncBackfillMirrorUseCase
    var onLoggedOut: () -> Void = {}

    @State private var activeTab: ConnectedTab = .overview
    @AppStorage(ScheduleVariant.storageKey) private var scheduleVariantRaw: String = ScheduleVariant.default.rawValue

    private let log = Log.scoped("ConnectedView")

    private var scheduleVariant: ScheduleVariant {
        ScheduleVariant(rawValue: scheduleVariantRaw) ?? .default
    }

    var body: some View {
        TabView(selection: $activeTab) {
            Tab(ConnectedTab.overview.label, systemImage: ConnectedTab.overview.icon, value: .overview) {
                OverviewView(factory: overview, disciplinesFactory: disciplines)
            }
            Tab(ConnectedTab.schedule.label, systemImage: ConnectedTab.schedule.icon, value: .schedule) {
                switch scheduleVariant {
                case .grid:    ScheduleGridView()
                case .focused: ScheduleFocusedView(factory: scheduleFocused)
                }
            }
            Tab(ConnectedTab.classes.label, systemImage: ConnectedTab.classes.icon, value: .classes) {
                DisciplinesListView(factory: disciplines)
            }
            Tab(ConnectedTab.messages.label, systemImage: ConnectedTab.messages.icon, value: .messages) {
                MessagesListView(factory: messages)
            }
            Tab(ConnectedTab.me.label, systemImage: ConnectedTab.me.icon, value: .me) {
                MeView(factory: me, onLoggedOut: onLoggedOut)
            }
        }
        .tint(UNESColor.accent)
        .task {
            // Fires once per authenticated session entry (fresh launch or
            // logout→login). Profile + first-page messages are unthrottled
            // (cheap); per-semester payload pulls behind them are gated by
            // the 1h throttle inside the use case.
            log.info("connected entry — refreshing session")
            do {
                _ = try await refreshSession.invoke(force: false)
                log.info("session refresh ok")
            } catch is CancellationError {
                // view left before work completed
            } catch {
                log.warn("session refresh failed", error: error)
            }
            // Fills in historical semesters + full message archive once
            // server Phase 2 finalizes. No-op after first successful run
            // (flag persisted in SyncState, wiped on logout).
            do {
                _ = try await backfillMirror.invoke()
                log.info("mirror backfill ok")
            } catch is CancellationError {
                // view left before work completed
            } catch {
                log.warn("mirror backfill failed", error: error)
            }
        }
    }
}

enum ConnectedTab: String, CaseIterable {
    case overview, schedule, classes, messages, me

    var label: String {
        switch self {
        case .overview: return "Hoje"
        case .schedule: return "Horário"
        case .classes:  return "Disciplinas"
        case .messages: return "Mensagens"
        case .me:       return "Eu"
        }
    }

    /// SF Symbols mapping (closest to the custom SVGs in the design).
    var icon: String {
        switch self {
        case .overview: return "house"
        case .schedule: return "square.grid.2x2"
        case .classes:  return "square.stack.3d.up"
        case .messages: return "bubble.left"
        case .me:       return "person"
        }
    }

}

private struct PlaceholderTab: View {
    let title: String
    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            Text(title)
                .font(UNESFont.serif(32))
                .foregroundStyle(UNESColor.ink3)
        }
    }
}
