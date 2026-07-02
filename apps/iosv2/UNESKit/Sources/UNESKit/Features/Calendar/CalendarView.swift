import ComposableArchitecture
import SwiftUI

/// The Calendário screen: collapsing large title, the next-deadline hero,
/// category + scope filters, and the agenda / month-grid body.
struct CalendarView: View {
    @Bindable var store: StoreOf<CalendarFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(.calendarTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
            ToolbarItem(placement: .trailingCompat) {
                viewModeButton
            }
        }
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
        .sheet(item: detailBinding) { event in
            CalendarEventSheet(event: event, today: store.today) {
                store.send(.detailDismissed)
            }
        }
    }

    // MARK: Content

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                if let hero = store.hero {
                    CalendarHeroCard(event: hero, today: store.today) {
                        store.send(.eventTapped($0))
                    }
                    .fadeUp(delay: 0.1)
                    .padding(EdgeInsets(top: 0, leading: 16, bottom: 20, trailing: 16))
                }

                CalendarCategorySegments(selected: store.category) {
                    store.send(.categorySelected($0))
                }
                .fadeUp(delay: 0.18)
                .padding(EdgeInsets(top: 0, leading: 16, bottom: 10, trailing: 16))

                CalendarScopePills(selected: store.scopeFilter) {
                    store.send(.scopeSelected($0))
                }
                .fadeUp(delay: 0.24)
                .padding(.bottom, 10)

                bodySection
                    .fadeUp(delay: 0.32)

                footer
                    .fadeUp(delay: 0.4)
            }
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .scrollIndicators(.hidden)
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(.calendarTitle)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(.calendarSubtitle)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    @ViewBuilder
    private var bodySection: some View {
        if store.fetchedAt == nil {
            SpinnerRing(size: 24, color: UNESColor.accent, trackColor: UNESColor.line)
                .padding(.vertical, 64)
        } else {
            switch store.viewMode {
            case .agenda:
                CalendarAgendaList(groups: store.agendaGroups, today: store.today) {
                    store.send(.eventTapped($0))
                }
                .transition(.opacity)
            case .grid:
                CalendarMonthGridSection(
                    events: store.filtered,
                    selectedDay: store.selectedDay,
                    today: store.today,
                    onSelectDay: { store.send(.daySelected($0)) },
                    onOpen: { store.send(.eventTapped($0)) }
                )
                .transition(.opacity)
            }
        }
    }

    @ViewBuilder
    private var footer: some View {
        if let fetchedAt = store.fetchedAt {
            TimelineView(.everyMinute) { context in
                Text(CalendarFormat.syncLabel(fetchedAt: fetchedAt, now: context.date))
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity)
            .padding(EdgeInsets(top: 10, leading: 24, bottom: 4, trailing: 24))
        }
    }

    // MARK: Chrome

    private var viewModeButton: some View {
        Button {
            store.send(.viewModeToggled, animation: UNESMotion.ease(0.4))
        } label: {
            Image(systemName: store.viewMode == .agenda ? "calendar" : "list.bullet")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(UNESColor.ink)
                .contentTransition(.symbolEffect(.replace))
        }
        .accessibilityLabel(store.viewMode == .agenda ? Text(.calendarActionViewMonth) : Text(.calendarActionViewAgenda))
    }

    /// Faint rose mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .rose, intensity: 0.55)
            .frame(height: 320)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.32)
            .offset(y: -80)
            .ignoresSafeArea()
            .allowsHitTesting(false)
    }

    private var detailBinding: Binding<CalendarEvent?> {
        Binding(
            get: { store.detail },
            set: { value in
                if value == nil { store.send(.detailDismissed) }
            }
        )
    }
}

#Preview {
    NavigationStack {
        CalendarView(
            store: Store(initialState: CalendarFeature.State()) {
                CalendarFeature()
            }
        )
    }
}
