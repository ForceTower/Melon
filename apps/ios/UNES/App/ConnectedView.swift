import SwiftUI

/// Authenticated shell shown after onboarding completes. Hosts the tab bar
/// and routes to each feature. Named for the "◦ conectado" moment in the
/// onboarding flow.
struct ConnectedView: View {
    @State private var activeTab: ConnectedTab = .overview

    var body: some View {
        TabView(selection: $activeTab) {
            Tab(ConnectedTab.overview.label, systemImage: ConnectedTab.overview.icon, value: .overview) {
                OverviewView()
            }
            Tab(ConnectedTab.schedule.label, systemImage: ConnectedTab.schedule.icon, value: .schedule) {
                ScheduleGridView()
            }
            Tab(ConnectedTab.classes.label, systemImage: ConnectedTab.classes.icon, value: .classes) {
                PlaceholderTab(title: ConnectedTab.classes.label)
            }
            Tab(ConnectedTab.messages.label, systemImage: ConnectedTab.messages.icon, value: .messages) {
                PlaceholderTab(title: ConnectedTab.messages.label)
            }
            .badge(ConnectedTab.messages.badge ?? 0)
            Tab(ConnectedTab.me.label, systemImage: ConnectedTab.me.icon, value: .me) {
                PlaceholderTab(title: ConnectedTab.me.label)
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
        case .classes:  return "Turmas"
        case .messages: return "Recados"
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

    var badge: Int? {
        switch self {
        case .messages: return 2
        default:        return nil
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

#Preview {
    ConnectedView()
}
