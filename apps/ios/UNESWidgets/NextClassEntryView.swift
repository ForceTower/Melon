import SwiftUI
import WidgetKit

/// Top-level view that branches on widget family + entry state to pick the
/// right layout, and resolves the theme from the system `colorScheme` (per
/// the handoff: widgets follow system appearance, not the app's theme
/// override). Lock-screen complications stay monochrome and let WidgetKit
/// handle tinting.
struct NextClassEntryView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var colorScheme

    let entry: NextClassEntry

    private var theme: WidgetTheme { WidgetTheme.resolve(colorScheme) }

    var body: some View {
        switch family {
        case .systemSmall:
            homeScreen(NextClassSmallView(entry: entry, theme: theme), mesh: true, padding: 14)
        case .systemMedium:
            homeScreenMedium()
        case .systemLarge:
            homeScreen(NextClassLargeView(entry: entry, theme: theme), mesh: true, padding: 18)
        case .accessoryRectangular:
            LockRectangularView(entry: entry)
        case .accessoryCircular:
            LockCircularView(entry: entry)
        case .accessoryInline:
            LockInlineView(entry: entry)
        default:
            homeScreen(NextClassSmallView(entry: entry, theme: theme), mesh: true, padding: 14)
        }
    }

    @ViewBuilder
    private func homeScreenMedium() -> some View {
        switch entry.state {
        case .upcoming:
            homeScreen(NextClassMediumView(entry: entry, theme: theme), mesh: true, padding: 14)
        case .inClass:
            homeScreen(InClassMediumView(entry: entry, theme: theme), mesh: true, padding: 14)
        case .dayDone:
            // Day-done is intentionally flat (no mesh) per the design.
            homeScreen(DayDoneMediumView(entry: entry, theme: theme), mesh: false, padding: 14)
        }
    }

    @ViewBuilder
    private func homeScreen(_ content: some View, mesh: Bool, padding: CGFloat) -> some View {
        content
            .padding(padding)
            .containerBackground(for: .widget) {
                WidgetCardBackground(theme: theme, mesh: mesh)
            }
            .foregroundStyle(theme.ink)
    }
}
