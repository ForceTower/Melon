import SwiftUI
import UserNotifications
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
    let pingActivity: SyncPingActivityUseCase
    let foregroundSignal: CommonForegroundSignal
    let widgetSnapshotPublisher: WidgetSnapshotPublisher
    var onLoggedOut: () -> Void = {}

    @State private var activeTab: ConnectedTab = .overview
    @State private var refreshInFlight = false
    @State private var pingInFlight = false
    @State private var wasBackgrounded = false
    @Environment(\.scenePhase) private var scenePhase
    @AppStorage(ScheduleVariant.storageKey) private var scheduleVariantRaw: String = ScheduleVariant.default.rawValue

    private let log = Log.scoped("ConnectedView")

    private var scheduleVariant: ScheduleVariant {
        ScheduleVariant(rawValue: scheduleVariantRaw) ?? .default
    }

    var body: some View {
        TabView(selection: $activeTab) {
            Tab(ConnectedTab.overview.label, systemImage: ConnectedTab.overview.icon, value: .overview) {
                OverviewView(
                    factory: overview,
                    disciplinesFactory: disciplines,
                    onOpenMessages: { activeTab = .messages },
                    onOpenSchedule: { activeTab = .schedule }
                )
            }
            Tab(ConnectedTab.schedule.label, systemImage: ConnectedTab.schedule.icon, value: .schedule) {
                switch scheduleVariant {
                case .grid:    ScheduleGridView()
                case .focused: ScheduleFocusedView(factory: scheduleFocused, disciplinesFactory: disciplines)
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
            // System dedupes after the first prompt — subsequent calls
            // return the existing status without re-showing UI, so no guard
            // needed here.
            _ = try? await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .badge, .sound])
        }
        .task {
            // Runs for the entire authenticated session — keeps the iOS
            // widget snapshot in the App Group container up to date so the
            // "Próxima aula" widget renders real data.
            await widgetSnapshotPublisher.start()
        }
        .task {
            // Fires once per authenticated session entry (fresh launch or
            // logout→login). Profile + first-page messages are unthrottled
            // (cheap); per-semester payload pulls behind them are gated by
            // the 1h throttle inside the use case.
            await runSessionRefresh(reason: "connected entry")
            // Bumps users.last_active_at server-side so the worker keeps this
            // student on the hourly sync cadence tier (cadenceFor in
            // apps/api/src/utils/cadence.ts). Without this the user falls to
            // the 24h tier 7 days after onboarding.
            await runPing(reason: "connected entry")
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
        .onChange(of: scenePhase) { _, newPhase in
            // SwiftUI's `.task` only fires the first time the view mounts, so a
            // backgrounded-then-resumed app would otherwise keep showing the day
            // it was suspended on. iOS resumes via .background → .inactive →
            // .active, so we can't key off the immediate previous phase — track
            // whether we truly reached .background instead. Transient .inactive
            // blips (control center, call banner, app-switcher peek) never do,
            // so they don't trigger work.
            switch newPhase {
            case .background:
                wasBackgrounded = true
            case .active:
                guard wasBackgrounded else { return }
                wasBackgrounded = false
                // Recompute wall-clock-derived UI (today/now class, schedule,
                // next test) immediately rather than waiting on the in-process
                // ticker, which doesn't reliably fire after a long suspension.
                foregroundSignal.pulse()
                Task { await runSessionRefresh(reason: "foreground") }
                Task { await runPing(reason: "foreground") }
            default:
                break
            }
        }
    }

    private func runSessionRefresh(reason: String) async {
        guard !refreshInFlight else {
            log.info("skip session refresh — already in flight reason=\(reason)")
            return
        }
        refreshInFlight = true
        defer { refreshInFlight = false }

        log.info("refreshing session reason=\(reason)")
        do {
            _ = try await refreshSession.invoke(force: false)
            log.info("session refresh ok reason=\(reason)")
        } catch is CancellationError {
            // view left before work completed
        } catch {
            log.warn("session refresh failed reason=\(reason)", error: error)
        }
    }

    private func runPing(reason: String) async {
        guard !pingInFlight else {
            log.info("skip ping — already in flight reason=\(reason)")
            return
        }
        pingInFlight = true
        defer { pingInFlight = false }

        do {
            _ = try await pingActivity.invoke()
            log.info("ping ok reason=\(reason)")
        } catch is CancellationError {
            // view left before work completed
        } catch {
            log.warn("ping failed reason=\(reason)", error: error)
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
