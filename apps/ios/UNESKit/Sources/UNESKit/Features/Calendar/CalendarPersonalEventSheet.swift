import ComposableArchitecture
import SwiftUI

/// The "Novo evento" / "Editar evento" composer: title, kind, dates, an
/// optional class tag, a reminder and notes, on inset-grouped cards.
struct CalendarPersonalEventSheet: View {
    @Bindable var store: StoreOf<CalendarPersonalEventFeature>
    @FocusState private var isTitleFocused: Bool

    private var category: CalendarCategory { store.category.calendarCategory }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    titleCard
                    categoryPicker
                        .padding(.top, 10)
                    whenGroup
                    disciplineGroup
                    reminderGroup
                    notesGroup
                    if !store.isNew {
                        deleteButton
                            .padding(.top, 18)
                    }
                    privacyNote
                }
                .padding(EdgeInsets(top: 14, leading: 16, bottom: 28, trailing: 16))
            }
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.interactively)
            .background(UNESColor.surface)
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .principalCompat) {
                    Text(store.isNew ? .calendarPersonalNewTitle : .calendarPersonalEditTitle)
                        .font(.system(size: 16, weight: .bold))
                        .tracking(-0.32)
                        .foregroundStyle(UNESColor.ink)
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button(String.localized(.commonCancel)) {
                        store.send(.cancelTapped)
                    }
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String.localized(store.isNew ? .calendarPersonalAddAction : .commonSave)) {
                        store.send(.saveTapped)
                    }
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(store.canSave ? UNESColor.accent : UNESColor.ink4)
                    .disabled(!store.canSave)
                }
            }
        }
        .tint(UNESColor.accent)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .confirmationDialog($store.scope(state: \.confirmDelete, action: \.confirmDelete))
        .task {
            guard store.isNew else { return }
            try? await Task.sleep(for: .milliseconds(420))
            isTitleFocused = true
        }
    }

    // MARK: Title + kind

    /// The tile hugs the first line as the title wraps, which needs a baseline
    /// alignment — but a `TextField` showing only its placeholder reports no
    /// first-line baseline, so the empty state centres instead. Both render the
    /// same for one line, so typing the first character doesn't shift anything.
    private var titleCard: some View {
        HStack(alignment: store.title.isEmpty ? .center : .firstTextBaseline, spacing: 12) {
            Image(systemName: category.icon)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .background(category.color, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .shadow(color: category.color.opacity(0.3), radius: 6, y: 5)
                .alignmentGuide(.firstTextBaseline) { $0[VerticalAlignment.center] + 6 }

            TextField(
                String.localized(.calendarPersonalTitlePlaceholder),
                text: $store.title,
                axis: .vertical
            )
            .focused($isTitleFocused)
            .font(.system(size: 17, weight: .semibold))
            .tracking(-0.34)
            .foregroundStyle(UNESColor.ink)
            .lineLimit(1 ... 4)
            .submitLabel(.done)
        }
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        .cardSurface(cornerRadius: 18)
    }

    private var categoryPicker: some View {
        HStack(spacing: 7) {
            ForEach(PersonalEvent.Category.allCases, id: \.self) { option in
                categoryChip(option)
            }
        }
    }

    private func categoryChip(_ option: PersonalEvent.Category) -> some View {
        let color = option.calendarCategory.color
        let isOn = store.category == option
        return Button {
            store.send(.binding(.set(\.category, option)), animation: .easeOut(duration: 0.15))
        } label: {
            VStack(spacing: 5) {
                Image(systemName: option.calendarCategory.icon)
                    .font(.system(size: 17, weight: .medium))
                Text(option.label)
                    .font(.system(size: 11.5, weight: .semibold))
                    .tracking(-0.11)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            .foregroundStyle(isOn ? color : UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(
                isOn ? color.opacity(0.12) : UNESColor.card,
                in: RoundedRectangle(cornerRadius: 15, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .strokeBorder(isOn ? color : UNESColor.cardLine)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: Dates

    private var whenGroup: some View {
        CalendarFormGroup(label: .calendarPersonalSectionWhen) {
            CalendarFormRow(icon: "calendar", tint: UNESColor.coral, label: .calendarPersonalRowDate) {
                DatePicker(
                    "",
                    selection: Binding(get: { store.start }, set: { store.send(.startPicked($0)) }),
                    displayedComponents: .date
                )
                .labelsHidden()
            }
            Divider().overlay(UNESColor.cardLine)
            CalendarFormRow(icon: "clock", tint: UNESColor.tangerine, label: .calendarPersonalRowPeriod) {
                Toggle(
                    "",
                    isOn: Binding(get: { store.end != nil }, set: { store.send(.periodToggled($0)) })
                )
                .labelsHidden()
            }
            if let end = store.end {
                Divider().overlay(UNESColor.cardLine)
                CalendarFormRow(
                    icon: "arrow.right",
                    tint: UNESColor.magenta,
                    label: .calendarPersonalRowEnds
                ) {
                    DatePicker(
                        "",
                        selection: Binding(get: { end }, set: { store.send(.endPicked($0)) }),
                        in: dayAfterStart...,
                        displayedComponents: .date
                    )
                    .labelsHidden()
                }
            }
        }
    }

    private var dayAfterStart: Date {
        Calendar.current.date(byAdding: .day, value: 1, to: store.start) ?? store.start
    }

    // MARK: Class tag

    private var disciplineGroup: some View {
        CalendarFormGroup(label: .calendarPersonalSectionDiscipline) {
            VStack(alignment: .leading, spacing: 10) {
                if store.disciplines.isEmpty {
                    Text(.calendarPersonalDisciplineEmpty)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                } else {
                    FlowLayout(spacing: 6, lineSpacing: 6) {
                        noneChip
                        ForEach(store.disciplines) { option in
                            disciplineChip(option)
                        }
                    }
                    Text(.calendarPersonalDisciplineHint)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 11, leading: 12, bottom: 12, trailing: 12))
        }
    }

    private var noneChip: some View {
        let isOn = store.discipline == nil
        return Button {
            store.send(.binding(.set(\.discipline, nil)), animation: .easeOut(duration: 0.15))
        } label: {
            Text(.calendarPersonalDisciplineNone)
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(isOn ? UNESColor.surface : UNESColor.ink3)
                .padding(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
                .background(
                    isOn ? UNESColor.ink : UNESColor.surface2,
                    in: Capsule()
                )
                .overlay { Capsule().strokeBorder(isOn ? UNESColor.ink : UNESColor.cardLine) }
        }
        .buttonStyle(.plain)
    }

    private func disciplineChip(_ option: PersonalEventDisciplineOption) -> some View {
        let color = UNESColor.disciplineReadableColor(option.colorIndex)
        let isOn = store.discipline?.id == option.tag.id
        return Button {
            store.send(.binding(.set(\.discipline, option.tag)), animation: .easeOut(duration: 0.15))
        } label: {
            HStack(spacing: 6) {
                Circle()
                    .fill(isOn ? Color.white.opacity(0.85) : color)
                    .frame(width: 7, height: 7)
                Text(option.tag.code)
                    .font(.system(size: 12.5, weight: .semibold))
            }
            .foregroundStyle(isOn ? .white : UNESColor.ink2)
            .padding(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
            .background(isOn ? color : UNESColor.surface2, in: Capsule())
            .overlay { Capsule().strokeBorder(isOn ? color : UNESColor.cardLine) }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(option.tag.name)
    }

    // MARK: Reminder + notes

    private var reminderGroup: some View {
        CalendarFormGroup(label: .calendarPersonalSectionReminder) {
            Picker("", selection: $store.reminder) {
                ForEach(PersonalEvent.Reminder.allCases, id: \.self) { option in
                    Text(option.label).tag(option)
                }
            }
            .segmentedPickerCompat()
            .labelsHidden()
            .padding(10)
        }
    }

    private var notesGroup: some View {
        CalendarFormGroup(label: .calendarPersonalSectionNotes) {
            HStack(alignment: store.notes.isEmpty ? .center : .firstTextBaseline, spacing: 11) {
                Image(systemName: "text.alignleft")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 26, height: 26)
                    .background(UNESColor.teal, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .alignmentGuide(.firstTextBaseline) { $0[VerticalAlignment.center] + 5 }
                TextField(
                    String.localized(.calendarPersonalNotesPlaceholder),
                    text: $store.notes,
                    axis: .vertical
                )
                .font(.system(size: 14.5, weight: .medium))
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1 ... 6)
            }
            .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        }
    }

    private var privacyNote: some View {
        Text(.calendarPersonalPrivacyNote)
            .font(.system(size: 11.5, weight: .medium))
            .foregroundStyle(UNESColor.ink4)
            .multilineTextAlignment(.center)
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 0, trailing: 20))
    }

    private var deleteButton: some View {
        Button {
            store.send(.deleteTapped)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                Text(.calendarPersonalDelete)
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundStyle(UNESColor.alertRed)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .cardSurface(cornerRadius: 18)
        }
        .buttonStyle(.pressableCard)
    }

}

// MARK: - Form primitives

/// An inset-grouped section: uppercase caption over one rounded card.
struct CalendarFormGroup<Content: View>: View {
    let label: LocalizedStringResource
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .semibold))
                .tracking(0.5)
                .foregroundStyle(UNESColor.ink4)
                .padding(.horizontal, 14)
            VStack(spacing: 0) { content }
                .cardSurface(cornerRadius: 18)
        }
        .padding(.top, 16)
    }
}

/// One labelled row: tinted glyph tile, title, trailing control.
struct CalendarFormRow<Control: View>: View {
    let icon: String
    let tint: Color
    let label: LocalizedStringResource
    @ViewBuilder var control: Control

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 26, height: 26)
                .background(tint, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            Text(label)
                .font(.system(size: 14.5, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink)
            Spacer(minLength: 8)
            control
        }
        .padding(EdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14))
        .frame(minHeight: 48)
    }
}

extension View {
    /// The card fill + hairline the calendar's grouped rows share.
    func cardSurface(cornerRadius: CGFloat) -> some View {
        background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        CalendarPersonalEventSheet(
            store: Store(
                initialState: CalendarPersonalEventFeature.State(
                    day: .now,
                    disciplines: .preview
                )
            ) {
                CalendarPersonalEventFeature()
            }
        )
    }
}
