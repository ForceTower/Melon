import WidgetKit

/// Timeline provider for the "Próxima aula" widget.
///
/// Today this returns the same fixture the design system uses, refreshed every
/// 5 minutes per the spec. Once the KMP layer exposes a query for the next
/// class on the widget surface, this is where it gets called from. The widget
/// extension can't link the umbrella framework yet (the umbrella build script
/// targets only the host app), so the real data path will likely flow through
/// an App Group + shared cache the app writes to.
struct NextClassProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextClassEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> Void) {
        completion(.placeholder)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextClassEntry>) -> Void) {
        let entry = NextClassEntry.placeholder
        let next = Calendar.current.date(byAdding: .minute, value: 5, to: entry.date) ?? entry.date
        completion(Timeline(entries: [entry], policy: .after(next)))
    }
}
