import Foundation
import Observation
@preconcurrency import Umbrella

// KMP type aliases — the Umbrella framework module-prefixes every generated
// class. Local aliases keep the mapping readable against the iOS presentation
// structs in `EnrollmentModels.swift`.
private typealias KmpAvailability = EnrollmentEnrollmentAvailability
private typealias KmpOffers = EnrollmentEnrollmentOffers
private typealias KmpWindow = EnrollmentEnrollmentWindow
private typealias KmpDiscipline = EnrollmentEnrollmentDiscipline
private typealias KmpSection = EnrollmentEnrollmentSection
private typealias KmpMeeting = EnrollmentEnrollmentMeeting
private typealias KmpShift = EnrollmentEnrollmentShift
private typealias KmpWindowState = EnrollmentEnrollmentWindowState
private typealias KmpSelection = EnrollmentEnrollmentSelection
private typealias KmpError = EnrollmentEnrollmentError

// Drives the matrícula flow. Loads the window status (cheap) then the full
// offers tree (heavy) live from SAGRES, maps each into the iOS presentation
// structs the screens already read, and owns the in-progress proposal. With
// `useCases == nil` it falls back to `EnrollmentFixtures` so `#Preview` and the
// factory-less `MeView()` keep rendering.
@MainActor
@Observable
final class EnrollmentViewModel {
    private(set) var window: EnrollmentWindow?
    private(set) var windowState: EnrollmentWindowState = .upcoming
    private(set) var disciplines: [OfferedDiscipline] = []
    private(set) var offersLoading = true
    private(set) var loadError: String?
    private(set) var submitting = false

    /// The in-progress proposal, shared by reference across every flow screen.
    let enroll = EnrollmentState()

    @ObservationIgnored private let useCases: EnrollmentUseCases?
    @ObservationIgnored private var didStart = false
    @ObservationIgnored private let log = Log.scoped("EnrollmentViewModel")

    init(useCases: EnrollmentUseCases?) {
        self.useCases = useCases
    }

    convenience init() {
        self.init(useCases: nil)
    }

    func loadIfNeeded() async {
        guard !didStart else { return }
        didStart = true
        guard let useCases else {
            loadFixtures()
            return
        }
        await loadWindow(useCases)
        // No point fetching the heavy offers tree when there's no open window.
        if window != nil { await loadOffers(useCases) }
    }

    func retry() async {
        didStart = false
        loadError = nil
        await loadIfNeeded()
    }

    // Submit the complete proposal. Returns nil on success or a message to
    // surface in the review dock. Fixture mode reports success without a call.
    func submit() async -> String? {
        guard let useCases else { return nil }
        guard !submitting else { return nil }
        submitting = true
        defer { submitting = false }

        // Snapshot the picks to Sendable primitives on the actor, then build the
        // KMP selection objects in a nonisolated helper that hands them back as a
        // `sending` (disconnected) region — otherwise the freshly-built array
        // counts as main-actor-isolated and can't cross to the @concurrent
        // submit use case.
        let inputs = enroll.picks.map { (id: $0.section.id, allowsOther: $0.allowsOther, waitlist: $0.waitlist) }
        let selections = Self.makeSelections(inputs)
        do {
            let outcome = try await useCases.submit.invoke(selections: selections)
            switch onEnum(of: outcome) {
            case .ok:
                log.info("enrollment submit ok picks=\(selections.count)")
                return nil
            case .err(let wrapper):
                log.warn("enrollment submit failed err=\(String(describing: wrapper.error))")
                return wrapper.error.map(Self.describe) ?? Self.genericSubmitError
            }
        } catch {
            log.error("enrollment submit threw", error: error)
            return Self.genericSubmitError
        }
    }

    // Builds the KMP selection objects off the actor from Sendable primitives,
    // returning them as a `sending` (disconnected) region so they can be passed
    // to the @concurrent submit use case.
    nonisolated private static func makeSelections(
        _ inputs: [(id: Int64, allowsOther: Bool, waitlist: Bool)]
    ) -> sending [KmpSelection] {
        inputs.map { KmpSelection(sectionId: $0.id, allowsOther: $0.allowsOther, waitlist: $0.waitlist) }
    }

    // MARK: - Loading

    private func loadFixtures() {
        window = EnrollmentFixtures.window
        windowState = .open
        disciplines = EnrollmentFixtures.disciplines
        offersLoading = false
    }

    private func loadWindow(_ useCases: EnrollmentUseCases) async {
        do {
            let outcome = try await useCases.window.invoke()
            switch onEnum(of: outcome) {
            case .ok(let ok):
                if let value = ok.value {
                    apply(availability: value)
                } else {
                    loadError = Self.genericLoadError
                }
            case .err(let wrapper):
                loadError = wrapper.error.map(Self.describe) ?? Self.genericLoadError
                log.warn("window load failed err=\(String(describing: wrapper.error))")
            }
        } catch {
            loadError = Self.genericLoadError
            log.error("window load threw", error: error)
        }
    }

    private func loadOffers(_ useCases: EnrollmentUseCases) async {
        offersLoading = true
        defer { offersLoading = false }
        do {
            let outcome = try await useCases.offers.invoke()
            switch onEnum(of: outcome) {
            case .ok(let ok):
                if let value = ok.value {
                    apply(offers: value.disciplines)
                }
            case .err(let wrapper):
                loadError = wrapper.error.map(Self.describe) ?? Self.genericLoadError
                log.warn("offers load failed err=\(String(describing: wrapper.error))")
            }
        } catch {
            loadError = Self.genericLoadError
            log.error("offers load threw", error: error)
        }
    }

    // MARK: - Apply

    private func apply(availability raw: KmpAvailability) {
        guard let w = raw.window else {
            window = nil
            windowState = .closed
            loadError = Self.noWindowMessage
            return
        }
        loadError = nil
        windowState = Self.map(state: w.state)
        window = EnrollmentWindow(
            semester: w.semester,
            startLabel: Self.dayLabel(w.startDate),
            endLabel: Self.endLabel(w.endDate),
            minHours: Int(w.minHours),
            maxHours: Int(w.maxHours),
            useQueue: w.useQueue
        )
    }

    private func apply(offers raw: [KmpDiscipline]) {
        let mapped = raw.map { Self.map(discipline: $0) }
        disciplines = mapped
        preseed(from: raw, mapped: mapped)
    }

    // Seed the proposal from sections the server already has saved, so a
    // returning student sees their current picks pre-selected.
    private func preseed(from raw: [KmpDiscipline], mapped: [OfferedDiscipline]) {
        for (di, discipline) in raw.enumerated() {
            for (si, section) in discipline.sections.enumerated() where section.selected {
                enroll.select(mapped[di], mapped[di].sections[si], waitlist: false)
            }
        }
    }

    // MARK: - Mapping (KMP → presentation structs)

    private static func map(discipline raw: KmpDiscipline) -> OfferedDiscipline {
        OfferedDiscipline(
            id: raw.id,
            code: raw.code,
            name: raw.name,
            workload: Int(raw.workload),
            mandatory: raw.mandatory,
            gradePeriod: Int(raw.gradePeriod),
            tone: EnrollmentTone.forCode(raw.code),
            suggestion: raw.suggestion,
            prereqs: raw.prerequisites.map { Prerequisite(code: $0.code, name: $0.name, met: $0.met) },
            sections: raw.sections.map { map(section: $0, code: raw.code) }
        )
    }

    private static func map(section raw: KmpSection, code: String) -> ClassSection {
        ClassSection(
            id: raw.id,
            label: raw.label,
            tone: EnrollmentTone.forCode(code),
            coursePreferential: raw.coursePreferential,
            suggestion: raw.suggestion,
            vacancies: Int(raw.vacancies),
            proposalsCount: Int(raw.proposalsCount),
            allowsOtherDefault: raw.allowsOtherDefault,
            waitlistCount: Int(raw.waitlistCount),
            meetings: raw.meetings.map { map(meeting: $0) }
        )
    }

    private static func map(meeting raw: KmpMeeting) -> SectionMeeting {
        SectionMeeting(
            kind: raw.kind,
            shift: map(shift: raw.shift),
            professors: raw.professors,
            room: raw.room,
            slots: raw.slots.map { MeetingSlot(day: Int($0.day), start: $0.start, end: $0.end) }
        )
    }

    private static func map(shift raw: KmpShift) -> ClassShift {
        if raw == .morning { return .morning }
        if raw == .afternoon { return .afternoon }
        if raw == .night { return .evening }
        return .undefined
    }

    private static func map(state raw: KmpWindowState) -> EnrollmentWindowState {
        if raw == .open { return .open }
        if raw == .upcoming { return .upcoming }
        return .closed // closed + unknown
    }

    // MARK: - Errors

    private static let genericLoadError = "Não foi possível carregar a matrícula. Tente novamente."
    private static let genericSubmitError = "Não foi possível enviar a matrícula. Tente novamente."
    private static let noWindowMessage = "Não há janela de matrícula aberta no momento."

    private static func describe(_ error: KmpError) -> String {
        let rendered = String(describing: error)
        if rendered.contains("Unauthorized") { return "Sessão expirada. Entre novamente para continuar." }
        if rendered.contains("NoConnection") { return "Sem conexão. Verifique a internet e tente de novo." }
        return genericLoadError
    }

    // MARK: - Date labels
    // SAGRES sends offset datetimes (e.g. `2026-06-22T23:59-03:00`); render the
    // labels in the institution's timezone so "23h59" stays "23h59".

    private static let institutionZone = TimeZone(identifier: "America/Bahia") ?? .current

    private static let offsetParser: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd'T'HH:mmXXXXX"
        return f
    }()

    private static let dayParser: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "d MMM"
        f.timeZone = institutionZone
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "HH'h'mm"
        f.timeZone = institutionZone
        return f
    }()

    private static func parse(_ iso: String) -> Date? {
        offsetParser.date(from: iso) ?? dayParser.date(from: String(iso.prefix(10)))
    }

    private static func dayLabel(_ iso: String) -> String {
        guard let date = parse(iso) else { return iso }
        return dayFormatter.string(from: date)
    }

    private static func endLabel(_ iso: String) -> String {
        guard let date = parse(iso) else { return iso }
        return "\(dayFormatter.string(from: date)) · \(timeFormatter.string(from: date))"
    }
}
