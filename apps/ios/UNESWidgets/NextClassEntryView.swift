import SwiftUI
import WidgetKit

/// Top-level view that branches on widget family + entry state to pick the
/// right layout. Backgrounds are applied per-family because lock-screen
/// complications must be transparent (the system handles tinting), while
/// home-screen sizes get the dark plum mesh hero.
struct NextClassEntryView: View {
    @Environment(\.widgetFamily) private var family

    let entry: NextClassEntry

    var body: some View {
        switch family {
        case .systemSmall:
            homeScreen(NextClassSmallView(entry: entry), padding: 14)
        case .systemMedium:
            homeScreenMedium()
        case .systemLarge:
            homeScreen(NextClassLargeView(entry: entry), padding: 18)
        case .accessoryRectangular:
            LockRectangularView(entry: entry)
        case .accessoryCircular:
            LockCircularView(entry: entry)
        case .accessoryInline:
            LockInlineView(entry: entry)
        default:
            homeScreen(NextClassSmallView(entry: entry), padding: 14)
        }
    }

    @ViewBuilder
    private func homeScreenMedium() -> some View {
        switch entry.state {
        case .upcoming:
            homeScreen(NextClassMediumView(entry: entry), padding: 14)
        case .inClass:
            homeScreen(InClassMediumView(entry: entry), padding: 14)
        case .dayDone:
            homeScreenLight(DayDoneMediumView(entry: entry), padding: 14)
        }
    }

    /// Dark plum mesh hero — the canonical "next class" surface.
    @ViewBuilder
    private func homeScreen(_ content: some View, padding: CGFloat) -> some View {
        content
            .padding(padding)
            .containerBackground(for: .widget) {
                WidgetCardBackground(dark: true, mesh: true, meshVariant: .cool)
            }
            .foregroundStyle(WidgetColor.surfaceLight)
    }

    /// Light variant for "dia concluído". Surface follows the system theme.
    @ViewBuilder
    private func homeScreenLight(_ content: some View, padding: CGFloat) -> some View {
        content
            .padding(padding)
            .containerBackground(for: .widget) {
                WidgetColor.surface
            }
    }
}
