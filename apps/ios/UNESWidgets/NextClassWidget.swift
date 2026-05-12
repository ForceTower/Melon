import SwiftUI
import WidgetKit

/// "Próxima aula" widget — one widget exposed in three home-screen sizes plus
/// the three lock-screen complication families. The handoff calls for all six,
/// served from the same data, with the entry view branching internally.
struct NextClassWidget: Widget {
    static let kind = "dev.forcetower.unes.ios.widgets.nextClass"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: NextClassWidget.kind, provider: NextClassProvider()) { entry in
            NextClassEntryView(entry: entry)
        }
        .configurationDisplayName("Próxima aula")
        .description("A próxima aula do dia, com horário, sala e topo de prioridades.")
        .supportedFamilies([
            .systemSmall, .systemMedium, .systemLarge,
            .accessoryRectangular, .accessoryCircular, .accessoryInline,
        ])
        .contentMarginsDisabled()
    }
}

@main
struct UNESWidgetsBundle: WidgetBundle {
    var body: some Widget {
        NextClassWidget()
    }
}

// MARK: - Previews

#Preview("Small — Próxima", as: .systemSmall) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}

#Preview("Medium — Próxima", as: .systemMedium) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}

#Preview("Medium — Em aula", as: .systemMedium) {
    NextClassWidget()
} timeline: {
    NextClassEntry.inClassPlaceholder
}

#Preview("Medium — Dia concluído", as: .systemMedium) {
    NextClassWidget()
} timeline: {
    NextClassEntry.dayDonePlaceholder
}

#Preview("Large", as: .systemLarge) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}

#Preview("Lock · rectangular", as: .accessoryRectangular) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}

#Preview("Lock · circular", as: .accessoryCircular) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}

#Preview("Lock · inline", as: .accessoryInline) {
    NextClassWidget()
} timeline: {
    NextClassEntry.placeholder
}
