import Foundation

// Wire shapes of `api/enrollment/*`. Dates and times arrive as SAGRES sends
// them — offset datetimes that may omit seconds, "HH:mm" or "HH:mm:ss"
// clock times — so both parse leniently here instead of trusting the
// envelope's ISO decoder.

/// `GET api/enrollment/window`.
struct EnrollmentWindowResponseDTO: Decodable {
    let available: Bool
    let window: EnrollmentWindowDTO?

    var domain: EnrollmentWindow? {
        guard available, let window else { return nil }
        return window.domain
    }
}

struct EnrollmentWindowDTO: Decodable {
    let semester: String
    let state: String
    let startDate: String
    let endDate: String
    let minHours: Int
    let maxHours: Int
    let useQueue: Bool

    var domain: EnrollmentWindow {
        EnrollmentWindow(
            semester: semester,
            state: EnrollmentWindowState(rawValue: state) ?? .closed,
            startDate: parseOffsetDate(startDate),
            endDate: parseOffsetDate(endDate),
            minHours: minHours,
            maxHours: maxHours,
            useQueue: useQueue
        )
    }
}

/// `GET api/enrollment/offers`.
struct EnrollmentOffersResponseDTO: Decodable {
    let disciplines: [EnrollmentDisciplineDTO]

    /// Tints follow the catalogue's code order so a discipline keeps its
    /// color across filters and screens. Codes can repeat across departments
    /// upstream — duplicates share the first slot rather than trapping.
    var domain: [EnrollmentDiscipline] {
        let ordered = disciplines.sorted { $0.code < $1.code }
        let colorIndexByCode = Dictionary(
            ordered.enumerated().map { ($0.element.code, $0.offset) },
            uniquingKeysWith: { first, _ in first }
        )
        return disciplines.map { $0.domain(colorIndex: colorIndexByCode[$0.code] ?? 0) }
    }
}

struct EnrollmentDisciplineDTO: Decodable {
    let id: Int64
    let code: String
    let name: String
    let workload: Int
    let mandatory: Bool
    let gradePeriod: Int
    let suggestion: Bool
    let prereqs: [EnrollmentPrerequisiteDTO]
    let sections: [EnrollmentSectionDTO]

    func domain(colorIndex: Int) -> EnrollmentDiscipline {
        EnrollmentDiscipline(
            id: id,
            code: code,
            name: name,
            workload: workload,
            mandatory: mandatory,
            gradePeriod: gradePeriod,
            suggestion: suggestion,
            prereqs: prereqs.map(\.domain),
            sections: sections.map(\.domain),
            colorIndex: colorIndex
        )
    }
}

struct EnrollmentPrerequisiteDTO: Decodable {
    let code: String
    let name: String
    let met: Bool

    var domain: EnrollmentPrerequisite {
        EnrollmentPrerequisite(code: code, name: name, met: met)
    }
}

struct EnrollmentSectionDTO: Decodable {
    let id: Int64
    let label: String
    let coursePreferential: Bool
    let suggestion: Bool
    let vacancies: Int
    let proposalsCount: Int
    let allowsOtherDefault: Bool
    let waitlistCount: Int
    let selected: Bool
    let meetings: [EnrollmentMeetingDTO]

    var domain: EnrollmentSection {
        EnrollmentSection(
            id: id,
            label: label,
            coursePreferential: coursePreferential,
            suggestion: suggestion,
            vacancies: vacancies,
            proposalsCount: proposalsCount,
            allowsOtherDefault: allowsOtherDefault,
            waitlistCount: waitlistCount,
            selected: selected,
            meetings: meetings.map(\.domain)
        )
    }
}

struct EnrollmentMeetingDTO: Decodable {
    let kind: String
    let shift: String
    let professors: [String]
    let room: String?
    let slots: [EnrollmentSlotDTO]

    var domain: EnrollmentMeeting {
        EnrollmentMeeting(
            kind: kind,
            shift: EnrollmentShift(rawValue: shift) ?? .undefined,
            professors: professors,
            room: room,
            slots: slots.compactMap(\.domain)
        )
    }
}

struct EnrollmentSlotDTO: Decodable {
    let day: Int
    let start: String
    let end: String

    var domain: EnrollmentSlot? {
        guard let start = parseClockMinutes(start), let end = parseClockMinutes(end) else { return nil }
        return EnrollmentSlot(day: day, startMinute: start, endMinute: end)
    }
}

/// `POST api/enrollment/submit`.
struct EnrollmentSubmitRequestDTO: Encodable {
    let selections: [EnrollmentSelectionDTO]

    init(_ selections: [EnrollmentSelection]) {
        self.selections = selections.map {
            EnrollmentSelectionDTO(sectionId: $0.sectionId, allowsOther: $0.allowsOther, waitlist: $0.waitlist)
        }
    }
}

struct EnrollmentSelectionDTO: Encodable {
    let sectionId: Int64
    let allowsOther: Bool
    let waitlist: Bool
}

/// Submit succeeds with an empty `data: {}` payload.
struct EnrollmentSubmitResponseDTO: Decodable {}

// MARK: - Lenient parsing

/// "2026-06-22T23:59-03:00" (SAGRES usually omits seconds) or the full
/// "2026-06-22T23:59:00-03:00".
private func parseOffsetDate(_ raw: String) -> Date? {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    for format in ["yyyy-MM-dd'T'HH:mm:ssZZZZZ", "yyyy-MM-dd'T'HH:mmZZZZZ"] {
        formatter.dateFormat = format
        if let date = formatter.date(from: raw) { return date }
    }
    return nil
}

/// "13:30" or "13:30:00" → minutes since midnight.
private func parseClockMinutes(_ raw: String) -> Int? {
    let parts = raw.split(separator: ":")
    guard parts.count >= 2, let hours = Int(parts[0]), let minutes = Int(parts[1].prefix(2)) else { return nil }
    return hours * 60 + minutes
}
