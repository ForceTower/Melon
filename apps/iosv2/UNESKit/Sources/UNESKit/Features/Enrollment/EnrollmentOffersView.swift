import ComposableArchitecture
import SwiftUI

/// The offers catalogue: search + segment filter, disciplines grouped by
/// curriculum period, and the running dock into the grade and the review.
struct EnrollmentOffersView: View {
    @Bindable var store: StoreOf<EnrollmentOffersFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            EnrollmentAmbientWash(variant: .cool, opacity: 0.24)
            content
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(.enrollmentOffersNavTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .hiddenTabBar()
        .safeAreaInset(edge: .bottom) {
            EnrollmentDock(
                session: store.session,
                primaryLabel: .localized(.enrollmentActionReview),
                onPrimary: { store.send(.reviewTapped) },
                secondaryLabel: String.localized(.enrollmentActionGrid),
                onSecondary: { store.send(.timetableTapped) }
            )
        }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 12) {
                    SearchField(placeholder: .enrollmentOffersSearch, query: store.query) {
                        store.send(.queryChanged($0))
                    }
                    .fadeUp(delay: 0.06)

                    Picker(String.localized(.commonFilter), selection: $store.filter.sending(\.filterChanged)) {
                        ForEach(EnrollmentOfferFilter.allCases) { filter in
                            Text(filter.label).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .fadeUp(delay: 0.1)
                }
                .padding(.horizontal, 16)

                groups
                    .padding(EdgeInsets(top: 18, leading: 16, bottom: 0, trailing: 16))
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(.enrollmentOffersTitle)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(.enrollmentOffersAvailableCount(store.session.disciplines.count))
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    @ViewBuilder
    private var groups: some View {
        if store.groups.isEmpty {
            emptyState
        } else {
            VStack(spacing: 0) {
                ForEach(Array(store.groups.enumerated()), id: \.element.id) { index, group in
                    VStack(spacing: 0) {
                        EnrollmentSectionHeader(title: group.period == 0 ? .enrollmentOffersFilterOptional : .enrollmentOffersPeriodTitle(group.period))
                        VStack(spacing: 10) {
                            ForEach(group.disciplines) { discipline in
                                EnrollmentOfferRow(
                                    discipline: discipline,
                                    pickedSection: pickedSection(of: discipline)
                                ) {
                                    store.send(.disciplineTapped(discipline.id))
                                }
                            }
                        }
                    }
                    .fadeUp(delay: 0.14 + Double(index) * 0.05)
                    .padding(.bottom, 22)
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 4) {
            Text(.enrollmentOffersEmptyTitle)
                .font(.system(size: 17, weight: .bold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(.enrollmentOffersEmptyBody(store.query.trimmingCharacters(in: .whitespaces)))
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 40, leading: 16, bottom: 40, trailing: 16))
        .enrollmentCard()
    }

    private func pickedSection(of discipline: EnrollmentDiscipline) -> EnrollmentSection? {
        guard let pick = store.session.pick(for: discipline.id) else { return nil }
        return discipline.section(pick.sectionId)
    }

}

/// One discipline card of the catalogue: code chip, badges, name, and the
/// hours/turmas meta line. Picking a turma raises the tinted rail.
struct EnrollmentOfferRow: View {
    var discipline: EnrollmentDiscipline
    var pickedSection: EnrollmentSection?
    var onTap: () -> Void

    var body: some View {
        Button {
            onTap()
        } label: {
            VStack(alignment: .leading, spacing: 9) {
                HStack(spacing: 8) {
                    EnrollmentCodeChip(code: discipline.code, color: discipline.tint)
                    if discipline.suggestion {
                        EnrollmentBadge(kind: .suggested, text: .localized(.enrollmentBadgeSuggested))
                    }
                    Spacer()
                    if let pickedSection {
                        EnrollmentBadge(kind: .selected, text: pickedSection.label)
                    } else {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }

                Text(discipline.name)
                    .font(.system(size: 17, weight: .bold))
                    .tracking(-0.43)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)

                HStack(spacing: 8) {
                    Text("\(discipline.workload)h")
                        .monospacedDigit()
                    Text("·").opacity(0.4)
                    Text(EnrollmentFormat.sectionCountLabel(discipline.sections.count))
                        .monospacedDigit()
                    Spacer()
                    EnrollmentBadge(
                        kind: discipline.mandatory ? .mandatory : .optional,
                        text: .localized(discipline.mandatory ? .enrollmentBadgeMandatory : .enrollmentBadgeOptional)
                    )
                    if discipline.hasUnmetPrereq {
                        EnrollmentBadge(kind: .prereq, text: .localized(.enrollmentBadgePrereq))
                    }
                }
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 13, trailing: 16))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.card)
            .overlay(alignment: .leading) {
                if pickedSection != nil {
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(discipline.tint)
                        .frame(width: 3.5)
                        .padding(.vertical, 14)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(pickedSection != nil ? discipline.tint.opacity(0.33) : UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(.pressableCard)
        .animation(UNESMotion.ease(0.25), value: pickedSection != nil)
    }
}

#Preview {
    NavigationStack {
        EnrollmentOffersView(
            store: Store(initialState: EnrollmentOffersFeature.State(session: .preview)) {
                EnrollmentOffersFeature()
            }
        )
    }
}
