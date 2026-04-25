import SwiftUI

/// Academic-calendar screen — surfaces every UEFS-side date the student
/// should know about (deadlines, exams, holidays) in a single agenda. Pushed
/// from the "Calendário" shortcut in the Me hub.
///
/// Mirrors `CalendarScreen` (the timeline variant) in `screens-calendar.jsx`.
/// The HTML prototype offered a second "ribbon" variant in its tweak panel;
/// the timeline variant is the prototype's default and the only one shipped
/// in production for now.
struct CalendarView: View {
    @State private var category: CalendarCategoryFilter = .all
    @State private var scope: CalendarScopeFilter = .all

    private let allEvents: [CalendarEvent] = CalendarFixtures.events
    private let semesterLabel: String = CalendarFixtures.semesterLabel

    private var filtered: [CalendarEvent] {
        allEvents.filter { category.matches($0) && scope.matches($0) }
    }

    /// Hide past items from the body, but keep events that started in the past
    /// and are still active so the hero + agenda agree on what's happening.
    private var visible: [CalendarEvent] {
        filtered.filter { CalendarMath.status($0) != .past }
    }

    private var monthGroups: [CalendarMonthGroup] { visible.groupedByMonth() }
    private var hero: CalendarEvent? { CalendarMath.nextDeadline(in: filtered) }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient warm mesh wash pinned to the top, fading into the
            // surface — same pattern as Messages/FinalCountdown.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.3)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 0.92),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 280)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    if let hero {
                        CalHeroCard(event: hero)
                            .padding(.horizontal, 14)
                            .padding(.bottom, 14)
                            .fadeUpOnAppear(delay: 0.12, distance: 14, duration: 0.6)
                    }

                    CalCategoryFilterRow(active: $category)
                        .fadeUpOnAppear(delay: 0.2, distance: 10, duration: 0.55)

                    CalScopeFilterRow(active: $scope)
                        .padding(.top, 8)
                        .padding(.bottom, 10)
                        .fadeUpOnAppear(delay: 0.26, distance: 10, duration: 0.55)

                    if monthGroups.isEmpty {
                        emptyState
                    } else {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(monthGroups) { group in
                                CalMonthSection(group: group)
                            }
                        }
                        .padding(.horizontal, 14)
                        .fadeUpOnAppear(delay: 0.34, distance: 10, duration: 0.55)
                    }

                    syncFooter
                        .fadeUpOnAppear(delay: 0.44, distance: 10, duration: 0.55)
                }
                .padding(.bottom, 32)
            }
        }
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("◦ SEMESTRE \(semesterLabel)")
                .font(UNESFont.mono(10, weight: .medium))
                .tracking(1.2)
                .foregroundStyle(UNESColor.ink3)

            titleText

            Text("Prazos, provas e feriados da UEFS")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 4)
        .padding(.bottom, 18)
    }

    private var titleText: some View {
        (
            Text("Calendário ")
                .font(UNESFont.serif(32))
                .foregroundColor(UNESColor.ink)
            +
            Text("acadêmico")
                .font(UNESFont.serif(32, italic: true))
                .foregroundColor(UNESColor.accent)
        )
        .tracking(-0.64)
    }

    // MARK: - Footer / empty

    private var syncFooter: some View {
        Text("◦ SINCRONIZADO COM O SAGRES · HÁ 4 MIN ◦")
            .font(UNESFont.mono(9))
            .tracking(1.26)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(.top, 10)
            .padding(.bottom, 4)
    }

    private var emptyState: some View {
        Text("Nada por aqui com esses filtros.")
            .font(UNESFont.sans(14))
            .foregroundStyle(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 40)
            .padding(.vertical, 80)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        CalendarView()
    }
}
#endif
