import SwiftUI
import UNESKit
import WidgetKit

@main
struct UNESWatchWidgetsBundle: WidgetBundle {
    var body: some Widget {
        WatchNextClassWidget()
        WatchCoefficientWidget()
        WatchNextExamWidget()
        WatchAttendanceWidget()
    }
}
