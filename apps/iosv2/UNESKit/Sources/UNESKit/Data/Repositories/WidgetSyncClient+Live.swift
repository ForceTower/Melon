import ComposableArchitecture
import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

extension WidgetSyncClient: DependencyKey {
    static let liveValue = WidgetSyncClient(
        run: {
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)
            // Observation only fails if the database itself is gone; there
            // is nothing left to keep in sync then.
            do {
                for try await schedule in mirror.widgetScheduleUpdates(now: { date.now }) {
                    // The first emission replays whatever is already
                    // published — skip the no-op reload.
                    guard schedule != WidgetSnapshotStore.load() else { continue }
                    if let schedule {
                        WidgetSnapshotStore.save(schedule)
                    } else {
                        WidgetSnapshotStore.clear()
                    }
                    #if canImport(WidgetKit)
                    WidgetCenter.shared.reloadTimelines(ofKind: UNESWidgetKind.nextClass)
                    #endif
                }
            } catch {}
        }
    )
}
