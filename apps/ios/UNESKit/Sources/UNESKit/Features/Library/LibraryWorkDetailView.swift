import ComposableArchitecture
import SwiftUI

struct LibraryWorkDetailView: View {
    @Bindable var store: StoreOf<LibraryWorkDetailFeature>

    private var work: LibraryWork { store.work }
    private var availability: LibraryAvailability { work.availability(now: store.now) }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(work.parsedTitle.title)
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
        .overlay(alignment: .bottom) {
            if let toast = store.toast {
                MaterialsToast(icon: "doc.on.doc", tone: UNESColor.successGreen, text: toast.text)
                    .padding(.bottom, 24)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .animation(UNESMotion.ease(0.3), value: store.toast)
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                hero
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 12)
                authorsAndMeta
                    .fadeUp(delay: 0.06)
                    .padding(.bottom, 18)
                availabilityCard
                    .scaleIn(delay: 0.1, duration: 0.62)
                    .padding(.bottom, 20)
                whereSection
                    .fadeUp(delay: 0.16)
                    .padding(.bottom, 20)
                copiesSection
                    .fadeUp(delay: 0.2)
                    .padding(.bottom, 22)
                recordSection
                    .fadeUp(delay: 0.24)
                    .padding(.bottom, 22)
                if !work.subjects.isEmpty {
                    subjectsSection
                        .fadeUp(delay: 0.26)
                        .padding(.bottom, 22)
                }
                referenceSection
                    .fadeUp(delay: 0.28)
                    .padding(.bottom, 16)
                identifiers
                    .fadeUp(delay: 0.3)
                    .padding(.bottom, 12)
                footer
                    .fadeUp(delay: 0.32)
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 32, trailing: 16))
        }
        .scrollIndicators(.hidden)
    }

    // MARK: Hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 7) {
            LibraryTypeTag(type: work.type, size: 11)
            Text(work.parsedTitle.title)
                .font(.system(size: 26, weight: .bold))
                .tracking(-1.04)
                .lineSpacing(0.5)
                .foregroundStyle(UNESColor.ink)
            if let subtitle = work.parsedTitle.subtitle {
                Text(subtitle)
                    .font(.system(size: 15, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var authorsAndMeta: some View {
        VStack(alignment: .leading, spacing: 7) {
            if work.authors.isEmpty {
                Text(.libraryAuthorUnknown)
                    .font(.system(size: 14.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            } else {
                FlowLayout(spacing: 8, lineSpacing: 4) {
                    ForEach(work.authors, id: \.self) { author in
                        Button {
                            store.send(.authorTapped(author))
                        } label: {
                            Text(author)
                                .font(.system(size: 14.5, weight: .semibold))
                                .tracking(-0.29)
                                .foregroundStyle(UNESColor.accent)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            Text(metaLine)
                .font(.system(size: 13, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var metaLine: String {
        var parts: [String] = []
        switch work.year {
        case let .year(text):
            parts.append(text)
        case let .illegible(raw):
            parts.append(.localized(.libraryDetailYearRegisteredAs(raw)))
        case .none:
            break
        }
        if let edition = work.edition { parts.append(edition) }
        if let language = work.language { parts.append(language) }
        if let volumes = work.volumes { parts.append(volumes) }
        return parts.joined(separator: " · ")
    }

    // MARK: The answer

    /// The screen's answer: how many copies are free right now, and how much
    /// to trust that number.
    private var availabilityCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            switch store.reading {
            case nil:
                VStack(alignment: .leading, spacing: 11) {
                    Capsule()
                        .fill(UNESColor.surface3)
                        .frame(width: 172, height: 22)
                    Capsule()
                        .fill(UNESColor.surface3)
                        .frame(width: 118, height: 12)
                }
            case .unavailable:
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 9) {
                        Image(systemName: "wifi.slash")
                            .font(.system(size: 16, weight: .semibold))
                        Text(.libraryDetailDownTitle)
                            .font(.system(size: 19, weight: .bold))
                            .tracking(-0.57)
                    }
                    .foregroundStyle(LibraryTone.none)
                    Text(.libraryDetailDownBody)
                        .font(.system(size: 13.5, weight: .medium))
                        .lineSpacing(3)
                        .foregroundStyle(UNESColor.ink3)
                }
            case .fresh, .stale:
                verdict(isStale: {
                    if case .stale = store.reading { true } else { false }
                }())
            }
            Divider()
                .overlay(UNESColor.line)
                .padding(.vertical, 12)
            LibraryFreshnessStamp(reading: store.reading, now: store.now) {
                store.send(.refreshTapped)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .materialsCard()
    }

    private func verdict(isStale: Bool) -> some View {
        let tone = isStale ? LibraryTone.loan : availability.tone
        return VStack(alignment: .leading, spacing: 7) {
            HStack(spacing: 10) {
                Circle()
                    .fill(tone)
                    .frame(width: 11, height: 11)
                Text(verdictHeadline)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.77)
                    .monospacedDigit()
                    .foregroundStyle(tone)
            }
            Text(verdictHint)
                .font(.system(size: 13.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
            if isStale, case let .stale(checkedAt) = store.reading {
                LibraryNote(
                    icon: "hourglass",
                    tone: LibraryTone.loan,
                    text: Text(.libraryDetailStaleNote(LibraryFormat.ago(from: checkedAt, to: store.now)))
                )
                .padding(.top, 6)
            }
        }
    }

    private var verdictHeadline: String {
        switch availability.verdict {
        case .available:
            .localized(availability.available == 1
                ? .libraryDetailFreeOne(availability.available, availability.total)
                : .libraryDetailFreeOther(availability.available, availability.total))
        case .allOnLoan:
            .localized(.libraryAvailabilityNone)
        case .localUseOnly:
            .localized(.libraryAvailabilityLocalOnly)
        }
    }

    private var verdictHint: String {
        switch availability.verdict {
        case .available:
            if availability.hasNearAvailable {
                return .localized(.libraryDetailNearHint)
            }
            let withFree = availability.branches.first { $0.available > 0 }?.branch
            let campus = withFree?.campus ?? withFree?.name ?? ""
            return .localized(.libraryDetailFarHint(campus))
        case .localUseOnly:
            return .localized(.libraryDetailLocalHint)
        case .allOnLoan:
            guard let nextDue = availability.nextDue else {
                return .localized(.libraryDetailLoanNoForecastHint)
            }
            return .localized(.libraryDetailLoanHint(LibraryFormat.shortDate(nextDue)))
        }
    }

    // MARK: Where on the shelf

    private var whereSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.libraryDetailSectionWhere)
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(.libraryDetailCallNumberLabel)
                        .textCase(.uppercase)
                        .font(.system(size: 10.5, weight: .bold))
                        .tracking(0.74)
                        .foregroundStyle(UNESColor.ink4)
                    Button {
                        store.send(.copyCallNumberTapped)
                    } label: {
                        HStack(spacing: 10) {
                            Text(work.callNumber)
                                .font(.system(size: 27, weight: .bold))
                                .tracking(-0.94)
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink)
                            Image(systemName: "doc.on.doc")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(UNESColor.ink4)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                VStack(alignment: .leading, spacing: 9) {
                    ForEach(availability.branches, id: \.branch) { entry in
                        HStack(alignment: .top, spacing: 9) {
                            Image(systemName: entry.branch.isNear ? "books.vertical" : "mappin.and.ellipse")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(entry.branch.isNear ? UNESColor.ink3 : LibraryTone.loan)
                                .frame(width: 16)
                                .padding(.top, 2)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(entry.areas.joined(separator: " · "))
                                    .font(.system(size: 13.5, weight: .semibold))
                                    .tracking(-0.27)
                                    .foregroundStyle(UNESColor.ink)
                                Text(branchLine(entry.branch))
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(UNESColor.ink4)
                            }
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 15, leading: 16, bottom: 15, trailing: 16))
            .materialsCard()
        }
    }

    private func branchLine(_ branch: LibraryBranch) -> String {
        let base = branch.campus.map { "\(branch.name) · \($0)" } ?? branch.name
        return branch.isNear ? base : "\(base) \(String.localized(.libraryDetailOtherCampus))"
    }

    // MARK: Copies

    private var copiesSection: some View {
        let isDown = store.reading == .unavailable
        return VStack(spacing: 0) {
            MaterialsSectionHeader(title: .libraryDetailSectionCopies) {
                Text(isDown
                    ? .libraryDetailCopiesRegistered(work.copies.count)
                    : .libraryDetailCopiesCounted(availability.total))
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            VStack(spacing: 10) {
                ForEach(Array(copyGroups.enumerated()), id: \.offset) { _, group in
                    LibraryVolumeCard(group: group, isDown: isDown, now: store.now)
                }
            }
            if availability.missing > 0, !isDown {
                LibraryNote(
                    icon: "exclamationmark.triangle",
                    text: Text(.libraryDetailMissingNote(
                        work.copies.count,
                        availability.missing,
                        availability.available,
                        availability.total
                    ))
                )
                .padding(.top, 11)
            }
        }
    }

    /// Copies aggregated by branch + per-copy call number + shelf area — a
    /// title can carry a hundred physical copies, and a flat list is useless.
    private var copyGroups: [LibraryCopyGroup] {
        var order: [String] = []
        var groups: [String: LibraryCopyGroup] = [:]
        for copy in work.copies {
            let key = "\(copy.branch.id)|\(copy.callNumber)|\(copy.area)"
            if groups[key] == nil {
                order.append(key)
                groups[key] = LibraryCopyGroup(
                    branch: copy.branch, callNumber: copy.callNumber, area: copy.area
                )
            }
            groups[key]?.add(copy.status)
        }
        return order.compactMap { groups[$0] }
    }

    // MARK: Catalogue record

    private var recordSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(title: .libraryDetailSectionRecord) {
                Button {
                    store.send(.recordToggled)
                } label: {
                    Text(store.isRecordShown ? .libraryDetailRecordHide : .libraryDetailRecordShow)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.accent)
                }
                .buttonStyle(.plain)
            }
            if store.isRecordShown {
                VStack(spacing: 0) {
                    ForEach(Array(work.record.enumerated()), id: \.offset) { index, field in
                        if index > 0 {
                            Divider()
                                .overlay(UNESColor.line)
                                .padding(.leading, 15)
                        }
                        VStack(alignment: .leading, spacing: 3) {
                            Text(field.label)
                                .textCase(.uppercase)
                                .font(.system(size: 10.5, weight: .bold))
                                .tracking(0.63)
                                .foregroundStyle(UNESColor.ink4)
                            Text(field.value)
                                .font(.system(size: 14, weight: .medium))
                                .tracking(-0.21)
                                .lineSpacing(2)
                                .foregroundStyle(UNESColor.ink)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 11, leading: 15, bottom: 11, trailing: 15))
                    }
                }
                .materialsCard()
            }
        }
    }

    // MARK: Subjects

    private var subjectsSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.libraryDetailSectionSubjects)
            LibraryChipRow {
                ForEach(work.subjects, id: \.self) { subject in
                    LibraryChip(onTap: { store.send(.subjectTapped(subject)) }) {
                        Text(subject)
                    }
                }
            }
        }
    }

    // MARK: Reference

    private var referenceSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.libraryDetailSectionReference)
            if let reference = work.reference {
                VStack(alignment: .leading, spacing: 12) {
                    Text(referenceText(reference))
                        .font(.system(size: 13.5, weight: .regular))
                        .lineSpacing(4)
                        .foregroundStyle(UNESColor.ink2)
                        .textSelectionCompat()
                    Button {
                        store.send(.copyReferenceTapped)
                    } label: {
                        HStack(spacing: 7) {
                            Image(systemName: "doc.on.doc")
                                .font(.system(size: 12, weight: .medium))
                            Text(.libraryDetailReferenceCopy)
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .foregroundStyle(UNESColor.ink)
                        .padding(EdgeInsets(top: 8, leading: 12, bottom: 8, trailing: 12))
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(TilePressStyle())
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
                .materialsCard()
            } else {
                LibraryNote(.libraryDetailReferenceMissing)
            }
        }
    }

    private func referenceText(_ markdown: String) -> AttributedString {
        (try? AttributedString(
            markdown: markdown,
            options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        )) ?? AttributedString(markdown.replacingOccurrences(of: "**", with: ""))
    }

    // MARK: Identifiers + footer

    private var identifiers: some View {
        FlowLayout(spacing: 9, lineSpacing: 9) {
            if let isbn = work.isbn {
                if let value = isbn.value {
                    identifierChip(
                        label: value.count == 13 ? .libraryDetailIsbnLabel : .libraryDetailIsbn10Label,
                        value: isbn.pretty ?? value
                    ) {
                        store.send(.copyISBNTapped)
                    }
                } else if let note = isbn.note {
                    identifierChip(label: .libraryDetailRegistryLabel, value: note, onTap: nil)
                }
            }
            identifierChip(label: .libraryDetailIdLabel, value: work.id) {
                store.send(.copyIdTapped)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private func identifierChip(
        label: LocalizedStringResource,
        value: String,
        onTap: (() -> Void)?
    ) -> some View {
        Button {
            onTap?()
        } label: {
            HStack(spacing: 8) {
                Text(label)
                    .textCase(.uppercase)
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.6)
                    .foregroundStyle(UNESColor.ink4)
                Text(value)
                    .font(.system(size: 13, weight: .bold))
                    .tracking(-0.13)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                if onTap != nil {
                    Image(systemName: "doc.on.doc")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(EdgeInsets(top: 7, leading: 10, bottom: 7, trailing: 10))
            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
        .disabled(onTap == nil)
    }

    /// Honest small print: what the catalogue doesn't provide, and the raw
    /// title string as registered — junk suffix and all.
    private var footer: some View {
        Text(footerText)
            .font(.system(size: 11.5, weight: .medium))
            .lineSpacing(3)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 4)
    }

    private var footerText: String {
        var text = String.localized(.libraryDetailFooterRaw(work.rawTitle))
        if let junk = work.parsedTitle.junkYear {
            text += " \(String.localized(.libraryDetailFooterJunk(junk)))"
        }
        return text
    }

    private var ambientWash: some View {
        RadialGradient(
            colors: [work.type.tone.opacity(0.22), .clear],
            center: .top,
            startRadius: 0,
            endRadius: 250
        )
        .frame(height: 280)
        .offset(y: -60)
        .ignoresSafeArea()
    }
}

extension LibraryWorkDetailFeature.State.Toast {
    var text: LocalizedStringResource {
        switch self {
        case .callNumberCopied: .libraryToastCallNumberCopied
        case .isbnCopied: .libraryToastIsbnCopied
        case .referenceCopied: .libraryToastReferenceCopied
        case .idCopied: .libraryToastIdCopied
        }
    }
}

// MARK: - Volume card

/// One branch + call-number bundle of physical copies.
struct LibraryCopyGroup {
    var branch: LibraryBranch
    var callNumber: String
    var area: String

    init(branch: LibraryBranch, callNumber: String, area: String) {
        self.branch = branch
        self.callNumber = callNumber
        self.area = area
    }

    var available = 0
    var missing = 0
    var futureDues: [Date] = []
    /// Loans with no credible return: a nil due (the backend already dropped
    /// a decades-old date) or one that lapsed while cached.
    var staleLoans = 0
    /// Earliest lapsed due still on record, when any survived — feeds the
    /// "vencido desde" sub-line.
    var staleSince: Date?
    var localUseNotes: [String] = []
    private var pendingLoans: [Date?] = []

    mutating func add(_ status: LibraryCopyStatus) {
        switch status {
        case .available: available += 1
        case .missing: missing += 1
        case let .onLoan(due): pendingLoans.append(due)
        case let .localUse(note): localUseNotes.append(note)
        }
    }

    /// Splits loans into "coming back" and "record never closed" — a due
    /// date years in the past is a stale record, not a forecast.
    mutating func settle(now: Date) {
        futureDues = pendingLoans.compactMap { $0 }.filter { $0 > now }.sorted()
        staleLoans = pendingLoans.count - futureDues.count
        staleSince = pendingLoans.compactMap { $0 }.filter { $0 <= now }.min()
        pendingLoans = []
    }

    var total: Int { available + futureDues.count + staleLoans + localUseNotes.count }
}

struct LibraryVolumeCard: View {
    var group: LibraryCopyGroup
    var isDown: Bool
    var now: Date

    private var settled: LibraryCopyGroup {
        var copy = group
        copy.settle(now: now)
        return copy
    }

    var body: some View {
        let group = settled
        let tone = group.available > 0
            ? LibraryTone.available
            : group.localUseNotes.isEmpty ? LibraryTone.none : LibraryTone.localUse
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 7) {
                        Text(group.branch.sigla)
                            .font(.system(size: 10.5, weight: .bold))
                            .tracking(0.53)
                            .foregroundStyle(UNESColor.ink3)
                            .padding(EdgeInsets(top: 2.5, leading: 7, bottom: 2.5, trailing: 7))
                            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                        if !group.branch.isNear, let campus = group.branch.campus {
                            HStack(spacing: 3) {
                                Image(systemName: "mappin.and.ellipse")
                                    .font(.system(size: 9.5, weight: .bold))
                                Text(campus)
                                    .font(.system(size: 10.5, weight: .bold))
                            }
                            .foregroundStyle(LibraryTone.loan)
                        }
                    }
                    Text(group.callNumber)
                        .font(.system(size: 14.5, weight: .semibold))
                        .tracking(-0.29)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    Text("\(group.area) · \(group.branch.name)")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                Spacer(minLength: 8)
                if isDown {
                    Text(verbatim: "—")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                } else {
                    VStack(alignment: .trailing, spacing: 3) {
                        Text(String(group.available))
                            .font(.system(size: 19, weight: .bold))
                            .tracking(-0.57)
                            .monospacedDigit()
                            .foregroundStyle(tone)
                        Text(.libraryDetailOfTotal(group.total))
                            .font(.system(size: 10.5, weight: .semibold))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
            }
            if !isDown {
                Divider()
                    .overlay(UNESColor.line)
                    .padding(.top, 10)
                    .padding(.bottom, 4)
                statusRows(group)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 11, trailing: 15))
        .materialsCard()
    }

    @ViewBuilder
    private func statusRows(_ group: LibraryCopyGroup) -> some View {
        if group.available > 0 {
            statusRow(
                icon: "checkmark",
                tone: LibraryTone.available,
                label: .localized(.libraryDetailCopyOnShelf(group.available)),
                sub: nil
            )
        }
        if !group.futureDues.isEmpty {
            statusRow(
                icon: "clock",
                tone: LibraryTone.loan,
                label: .localized(group.futureDues.count == 1
                    ? .libraryDetailCopyLoanOne(group.futureDues.count)
                    : .libraryDetailCopyLoanOther(group.futureDues.count)),
                sub: group.futureDues.first.map {
                    .localized(.libraryDetailCopyLoanNext(LibraryFormat.shortDate($0)))
                }
            )
        }
        if group.staleLoans > 0 {
            statusRow(
                icon: "hourglass",
                tone: UNESColor.ink3,
                label: .localized(.libraryDetailCopyStale(group.staleLoans)),
                sub: group.staleSince.map {
                    .localized(.libraryDetailCopyStaleSince(LibraryFormat.year($0)))
                },
                faded: true
            )
        }
        if !group.localUseNotes.isEmpty {
            statusRow(
                icon: "info.circle",
                tone: LibraryTone.localUse,
                label: .localized(.libraryDetailCopyLocal(group.localUseNotes.count)),
                sub: group.localUseNotes.first { !$0.isEmpty }
            )
        }
        if group.missing > 0 {
            statusRow(
                icon: "exclamationmark.triangle",
                tone: UNESColor.ink4,
                label: .localized(group.missing == 1
                    ? .libraryDetailCopyMissingOne(group.missing)
                    : .libraryDetailCopyMissingOther(group.missing)),
                sub: .localized(.libraryDetailCopyMissingHint),
                faded: true
            )
        }
    }

    private func statusRow(
        icon: String,
        tone: Color,
        label: String,
        sub: String?,
        faded: Bool = false
    ) -> some View {
        HStack(alignment: .top, spacing: 9) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(tone)
                .frame(width: 14)
                .padding(.top, 2)
            VStack(alignment: .leading, spacing: 1.5) {
                Text(label)
                    .font(.system(size: 13.5, weight: .semibold))
                    .tracking(-0.2)
                    .monospacedDigit()
                    .foregroundStyle(tone)
                if let sub {
                    Text(sub)
                        .font(.system(size: 12, weight: .medium))
                        .lineSpacing(2)
                        .foregroundStyle(UNESColor.ink4)
                }
            }
        }
        .padding(.vertical, 7)
        .opacity(faded ? 0.68 : 1)
    }
}

#Preview("Obra · disponível") {
    NavigationStack {
        LibraryWorkDetailView(
            store: Store(
                initialState: LibraryWorkDetailFeature.State(
                    work: LibraryFixtures.works(now: Date())[0]
                )
            ) {
                LibraryWorkDetailFeature()
            }
        )
    }
}

#Preview("Obra · nada livre") {
    NavigationStack {
        LibraryWorkDetailView(
            store: Store(
                initialState: LibraryWorkDetailFeature.State(
                    work: LibraryFixtures.works(now: Date())[2]
                )
            ) {
                LibraryWorkDetailFeature()
            }
        )
    }
}

#Preview("Obra · consulta local") {
    NavigationStack {
        LibraryWorkDetailView(
            store: Store(
                initialState: LibraryWorkDetailFeature.State(
                    work: LibraryFixtures.works(now: Date())[6]
                )
            ) {
                LibraryWorkDetailFeature()
            }
        )
    }
}

#Preview("Obra · fora do ar") {
    NavigationStack {
        LibraryWorkDetailView(
            store: Store(
                initialState: LibraryWorkDetailFeature.State(
                    work: LibraryFixtures.works(now: Date())[0]
                )
            ) {
                LibraryWorkDetailFeature()
            } withDependencies: {
                $0.libraryRepository.checkAvailability = { _ in
                    LibraryAvailabilitySnapshot(reading: .unavailable, copies: [])
                }
            }
        )
    }
}
