import ComposableArchitecture
import SwiftUI

struct CampusEventView: View {
    @Bindable var store: StoreOf<CampusEventFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            hub

            if store.isShowingWelcome {
                CampusEventWelcomeView(event: store.event) {
                    store.send(.welcomeContinueTapped, animation: UNESMotion.ease(0.5))
                }
                .transition(.opacity)
                .zIndex(1)
            }
        }
        .navigationTitle(store.event.name)
        .modifier(WelcomeChrome(isActive: store.isShowingWelcome))
        .task { await store.send(.task).finish() }
    }

    // MARK: Hub

    private var hub: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 18)

                TimelineView(.periodic(from: .now, by: 1)) { context in
                    CampusEventHubHero(event: store.event, filter: store.filter, now: context.date)
                }
                .scaleIn(delay: 0.1, duration: 0.62)
                .padding(.horizontal, 16)
                .padding(.bottom, 22)

                quickLinks
                    .fadeUp(delay: 0.18)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 26)

                TimelineView(.everyMinute) { context in
                    schedule(now: context.date)
                }
                .fadeUp(delay: 0.26)

                if let credit = store.event.credit {
                    Text(credit)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .multilineTextAlignment(.center)
                        .padding(EdgeInsets(top: 22, leading: 24, bottom: 8, trailing: 24))
                        .fadeUp(delay: 0.32)
                }
            }
            .padding(.vertical, 8)
        }
        .scrollIndicators(.hidden)
        .refreshable { await store.send(.refreshPulled).finish() }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Eyebrow(text: headerEyebrow)
            if let tagline = store.event.tagline {
                Text(tagline)
                    .font(.system(size: 15, weight: .medium))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
    }

    private var headerEyebrow: String {
        let range = CampusEventFormat.dateRange(from: store.event.startsAt, to: store.event.endsAt, in: store.event.timeZone)
        guard let edition = store.event.edition else { return range }
        return "\(edition) · \(range)"
    }

    // MARK: Quick links

    private var quickLinks: some View {
        let event = store.event
        return LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
            if !event.speakers.isEmpty {
                CampusEventQuickTile(
                    icon: "mic", tone: UNESColor.violet,
                    label: .campusEventHubQuickSpeakers,
                    note: .campusEventHubQuickSpeakersCount(event.speakers.count)
                ) { store.send(.speakersTapped) }
            }
            if !event.workshops.isEmpty {
                CampusEventQuickTile(
                    icon: "wrench.and.screwdriver", tone: UNESColor.teal,
                    label: .campusEventHubQuickWorkshops,
                    note: .campusEventHubQuickWorkshopsCount(event.workshops.count)
                ) { store.send(.workshopsTapped) }
            }
            if !event.venues.isEmpty {
                CampusEventQuickTile(
                    icon: "map", tone: UNESColor.amber,
                    label: .campusEventHubQuickVenues,
                    note: .campusEventHubQuickVenuesCount(event.venues.count)
                ) { store.send(.venuesTapped) }
            }
            if !event.organizations.isEmpty {
                CampusEventQuickTile(
                    icon: "person.3", tone: UNESColor.magenta,
                    label: .campusEventHubQuickOrganizations,
                    note: .campusEventHubQuickOrganizationsCount(event.organizations.count)
                ) { store.send(.organizationsTapped) }
            }
        }
    }

    // MARK: Schedule

    private func schedule(now: Date) -> some View {
        let event = store.event
        let days = event.days()
        let isLive = event.phase(now: now) == .live
        let selected = store.selectedDay ?? days.first?.date

        return VStack(spacing: 0) {
            CampusEventSectionHeader(.campusEventHubSchedule, note: .campusEventHubScheduleNote)
                .padding(.horizontal, 20)

            dayTabs(days: days, selected: selected, now: isLive ? now : nil)
                .padding(.bottom, 14)

            if event.hasAudienceSplit {
                filterPicker
                    .padding(.horizontal, 16)
                    .padding(.bottom, 14)
            }

            if let selected {
                dayList(for: selected, now: now)
                    .padding(.horizontal, 16)
            }
        }
    }

    private func dayTabs(days: [CampusEventDay], selected: Date?, now: Date?) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                ForEach(days) { day in
                    CampusEventDayTab(
                        day: day,
                        timeZone: store.event.timeZone,
                        isSelected: day.date == selected,
                        isToday: now.map { store.event.isDate($0, onDay: day.date) } ?? false,
                        hasActivities: store.event.activityCount(on: day.date, matching: store.filter) > 0
                    ) {
                        store.send(.dayTapped(day.date), animation: UNESMotion.ease(0.3))
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
        .scrollIndicators(.hidden)
        // The selected pill's shadow reaches past the scroll bounds.
        .scrollClipDisabled()
    }

    private var filterPicker: some View {
        Picker(
            selection: $store.filter.sending(\.filterChanged),
            content: {
                ForEach(CampusEventAudience.allCases, id: \.self) { audience in
                    Text(audience.label).tag(audience)
                }
            },
            label: { Text(.campusEventHubSchedule) }
        )
        .segmentedPickerCompat()
    }

    @ViewBuilder
    private func dayList(for day: Date, now: Date) -> some View {
        let activities = store.event.activities(on: day, matching: store.filter)

        HStack(spacing: 0) {
            Text(CampusEventFormat.weekdayLong(for: day, in: store.event.timeZone))
            Text(" · ")
            Text(.campusEventHubActivityCount(activities.count))
        }
        .font(.system(size: 13, weight: .semibold))
        .foregroundStyle(UNESColor.ink3)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 4, bottom: 10, trailing: 4))

        if activities.isEmpty {
            Text(.campusEventHubEmptyDay)
                .font(.system(size: 14.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
                .campusEventCard()
        } else {
            VStack(spacing: 0) {
                ForEach(Array(activities.enumerated()), id: \.element.id) { index, activity in
                    CampusEventActivityRow(
                        activity: activity,
                        timeZone: store.event.timeZone,
                        state: activity.state(now: now),
                        isLast: index == activities.count - 1
                    ) {
                        store.send(.activityTapped(activity))
                    }
                }
            }
            .campusEventCard()
        }
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 300)
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
            .opacity(0.28)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

/// Hides the navigation chrome while the fullscreen welcome owns the screen.
private struct WelcomeChrome: ViewModifier {
    var isActive: Bool

    func body(content: Content) -> some View {
        if isActive {
            content
                .hiddenNavigationBar()
                .hiddenTabBar()
        } else {
            content
        }
    }
}

// MARK: - Hero

/// Phase-aware dark hero: countdown before the event, day progress and the
/// live/next activity while it runs, thanks + stats after.
struct CampusEventHubHero: View {
    let event: CampusEvent
    let filter: CampusEventAudience
    let now: Date

    var body: some View {
        let phase = event.phase(now: now)
        ZStack {
            UNESColor.darkBg
            MeshView(variant: phase == .ended ? .fresh : .warm)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.14), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.64), location: 1),
                ],
                angle: 160
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 7) {
                    if phase != .ended { LiveDot() }
                    Text(eyebrow(for: phase))
                        .textCase(.uppercase)
                        .font(.system(size: 11.5, weight: .bold))
                        .tracking(0.81)
                }
                .foregroundStyle(.white.opacity(0.9))

                phaseBody(for: phase)
                    .padding(.top, 16)

                footer(for: phase)
                    .padding(.top, 14)
                    .overlay(alignment: .top) {
                        Rectangle()
                            .fill(.white.opacity(0.14))
                            .frame(height: 1)
                    }
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.3), radius: 20, y: 18)
    }

    private func eyebrow(for phase: CampusEventPhase) -> LocalizedStringResource {
        switch phase {
        case .upcoming:
            return .campusEventPhaseUpcoming
        case .live:
            let liveCount = event.liveActivityCount(matching: filter, now: now)
            return liveCount > 0 ? .campusEventPhaseLiveCount(liveCount) : .campusEventPhaseLive
        case .ended:
            return .campusEventPhaseEnded
        }
    }

    @ViewBuilder
    private func phaseBody(for phase: CampusEventPhase) -> some View {
        switch phase {
        case .upcoming:
            CampusEventCountdownRow(target: event.startsAt, now: now)
                .frame(maxWidth: .infinity)

        case .live:
            VStack(spacing: 8) {
                HStack {
                    Text(.campusEventDayOf(event.dayNumber(now: now), event.dayCount))
                        .font(.system(size: 13, weight: .bold))
                        .tracking(-0.13)
                        .foregroundStyle(.white)
                    Spacer()
                    Text(CampusEventFormat.fullDate(for: now, in: event.timeZone))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.6))
                }
                CampusEventDayProgress(dayCount: event.dayCount, currentIndex: event.dayNumber(now: now) - 1)
            }

        case .ended:
            HStack(spacing: 14) {
                Image(systemName: "checkmark")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(UNESColor.successOnDark)
                    .frame(width: 54, height: 54)
                    .background(
                        UNESColor.liveGreen.opacity(0.22),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                    )
                VStack(alignment: .leading, spacing: 6) {
                    Text(.campusEventHubEndedTitle)
                        .font(.system(size: 26, weight: .heavy))
                        .tracking(-0.78)
                        .foregroundStyle(.white)
                    Text(.campusEventHubEndedBody)
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundStyle(.white.opacity(0.78))
                }
            }
        }
    }

    @ViewBuilder
    private func footer(for phase: CampusEventPhase) -> some View {
        switch phase {
        case .upcoming:
            if let opener = event.opener {
                CampusEventHeroActivityBlock(activity: opener, tag: .campusEventHubOpening, timeZone: event.timeZone)
            }
        case .live:
            if let current = event.liveOrNextActivity(matching: filter, now: now) {
                CampusEventHeroActivityBlock(
                    activity: current.activity,
                    tag: current.isLive ? .campusEventHubNow : .campusEventHubNext,
                    tagTone: current.isLive ? UNESColor.successOnDark : .white.opacity(0.55),
                    timeZone: event.timeZone
                )
            }
        case .ended:
            HStack(spacing: 20) {
                CampusEventHeroStat(value: event.activities.count, label: .campusEventHubStatActivities)
                if !event.speakers.isEmpty {
                    CampusEventHeroStat(value: event.speakers.count, label: .campusEventHubStatSpeakers)
                }
                CampusEventHeroStat(value: event.dayCount, label: .campusEventHubStatDays)
                Spacer()
            }
        }
    }
}

// MARK: - Day tab

/// One pill of the day strip: weekday, day number and a today/dot marker.
struct CampusEventDayTab: View {
    let day: CampusEventDay
    var timeZone: TimeZone
    var isSelected: Bool
    var isToday: Bool
    var hasActivities: Bool
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 2) {
                Text(CampusEventFormat.weekdayShort(for: day.date, in: timeZone))
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.44)
                    .foregroundStyle(isSelected ? UNESColor.surface.opacity(0.65) : UNESColor.ink3)
                Text(CampusEventFormat.dayNumber(for: day.date, in: timeZone))
                    .font(.system(size: 20, weight: .bold))
                    .tracking(-0.6)
                    .monospacedDigit()
                    .foregroundStyle(isSelected ? UNESColor.surface : UNESColor.ink)
                marker
                    .frame(height: 9)
                    .padding(.top, 2)
            }
            .frame(width: 60)
            .padding(EdgeInsets(top: 10, leading: 0, bottom: 8, trailing: 0))
            .background(
                isSelected ? UNESColor.ink : UNESColor.card,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(
                        isToday && !isSelected ? UNESColor.accent : UNESColor.cardLine,
                        lineWidth: isToday && !isSelected ? 1.5 : 1
                    )
            }
            .shadow(
                color: Color(hex: 0x141020, opacity: isSelected ? 0.22 : 0.04),
                radius: isSelected ? 10 : 6,
                y: isSelected ? 8 : 4
            )
        }
        .buttonStyle(.pressableCard)
    }

    @ViewBuilder
    private var marker: some View {
        if isToday {
            Text(.campusEventHubToday)
                .textCase(.uppercase)
                .font(.system(size: 8.5, weight: .heavy))
                .tracking(0.51)
                .foregroundStyle(isSelected ? UNESColor.surface : UNESColor.accent)
        } else {
            Circle()
                .fill(
                    isSelected
                        ? UNESColor.surface.opacity(0.7)
                        : hasActivities ? UNESColor.accent : .clear
                )
                .frame(width: 5, height: 5)
        }
    }
}

// MARK: - Activity row

/// One schedule entry: time column, category rail, badges + title + venue.
struct CampusEventActivityRow: View {
    let activity: CampusEventActivity
    var timeZone: TimeZone
    var state: CampusEventActivityState
    var isLast: Bool
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(CampusEventFormat.time(for: activity.startsAt, in: timeZone))
                        .font(.system(size: 13.5, weight: .bold))
                        .foregroundStyle(UNESColor.ink)
                    if let end = activity.endsAt {
                        Text(CampusEventFormat.time(for: end, in: timeZone))
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                .monospacedDigit()
                .frame(width: 46, alignment: .trailing)
                .padding(.top, 2)

                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(activity.category.tone)
                    .frame(width: state == .live ? 4 : 3)
                    .shadow(color: state == .live ? activity.category.tone : .clear, radius: 4)

                VStack(alignment: .leading, spacing: 0) {
                    badges
                    Text(activity.title)
                        .font(.system(size: 15.5, weight: .semibold))
                        .tracking(-0.31)
                        .foregroundStyle(UNESColor.ink)
                        .strikethrough(state == .past, color: UNESColor.ink4)
                        .multilineTextAlignment(.leading)
                        .padding(.top, 7)
                    HStack(spacing: 6) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                        Text(activity.venueName)
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .lineLimit(1)
                    }
                    .padding(.top, 5)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
                    .frame(maxHeight: .infinity)
            }
            .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 12))
            .background(state == .live ? activity.category.tone.opacity(0.07) : .clear)
            .opacity(state == .past ? 0.55 : 1)
            .overlay(alignment: .bottom) {
                if !isLast {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(height: 0.5)
                        .padding(.leading, 74)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var badges: some View {
        HStack(spacing: 8) {
            CampusEventCategoryPill(category: activity.category)
            CampusEventAudienceChip(audience: activity.audience)
            switch state {
            case .live:
                HStack(spacing: 5) {
                    LiveDot(size: 6, color: UNESColor.successGreen)
                    Text(.campusEventHubNow)
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .bold))
                }
                .foregroundStyle(UNESColor.successGreen)
                .padding(EdgeInsets(top: 3, leading: 7, bottom: 3, trailing: 8))
                .background(UNESColor.successGreen.opacity(0.12), in: Capsule())
            case .past:
                HStack(spacing: 4) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 9, weight: .bold))
                    Text(.campusEventRowDone)
                        .font(.system(size: 11, weight: .bold))
                }
                .foregroundStyle(UNESColor.ink4)
            case .upcoming:
                EmptyView()
            }
        }
    }
}

// MARK: - Quick-link tile

struct CampusEventQuickTile: View {
    var icon: String
    var tone: Color
    var label: LocalizedStringResource
    var note: LocalizedStringResource
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(tone)
                    .frame(width: 34, height: 34)
                    .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                    .padding(.bottom, 12)
                Text(label)
                    .font(.system(size: 15, weight: .bold))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                Text(note)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(15)
            .background(alignment: .topTrailing) {
                Circle()
                    .fill(tone.opacity(0.13))
                    .frame(width: 74, height: 74)
                    .offset(x: 24, y: -24)
            }
            .campusEventCard()
        }
        .buttonStyle(.pressableCard)
    }
}

#Preview("Antes") {
    NavigationStack {
        CampusEventView(
            store: Store(initialState: CampusEventFeature.State(event: .preview(phase: .upcoming))) {
                CampusEventFeature()
            }
        )
    }
}

#Preview("Ao vivo") {
    NavigationStack {
        CampusEventView(
            store: Store(initialState: CampusEventFeature.State(event: .preview(phase: .live))) {
                CampusEventFeature()
            }
        )
    }
}

#Preview("Encerrado") {
    NavigationStack {
        CampusEventView(
            store: Store(initialState: CampusEventFeature.State(event: .preview(phase: .ended))) {
                CampusEventFeature()
            }
        )
    }
}
