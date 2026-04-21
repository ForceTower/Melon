import SwiftUI

/// Authenticated shell shown after onboarding completes. Hosts the tab bar
/// and routes to each feature. Named for the "◦ conectado" moment in the
/// onboarding flow.
struct ConnectedView: View {
    let overview: OverviewFactory
    let scheduleFocused: ScheduleFocusedFactory
    let disciplines: DisciplinesFactory
    let messages: MessagesFactory
    let me: MeFactory

    @State private var activeTab: ConnectedTab = .overview
    @AppStorage(ScheduleVariant.storageKey) private var scheduleVariantRaw: String = ScheduleVariant.default.rawValue

    private var scheduleVariant: ScheduleVariant {
        ScheduleVariant(rawValue: scheduleVariantRaw) ?? .default
    }

    var body: some View {
        TabView(selection: $activeTab) {
            Tab(ConnectedTab.overview.label, systemImage: ConnectedTab.overview.icon, value: .overview) {
                OverviewView(factory: overview)
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
                MeView(factory: me)
            }
        }
        .tint(UNESColor.accent)
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
