import Foundation

// Preview/test fixtures for the matrícula flow — the design project's sample
// curriculum (Ciência da Computação · UEFS), exercising every interesting
// state: theory+practice bundles, a full section with a waitlist, a schedule
// clash, an "a definir" section, and an unmet prerequisite.

extension EnrollmentWindow {
    static let preview = EnrollmentWindow(
        semester: "2026.2",
        state: .open,
        startDate: fixtureDate(2026, 6, 15, 0, 0),
        endDate: fixtureDate(2026, 6, 22, 23, 59),
        minHours: 240,
        maxHours: 420,
        useQueue: true
    )
}

extension EnrollmentSession {
    /// The seeded 240h starting point: a valid proposal one edit away from
    /// every warning state.
    static let preview: EnrollmentSession = {
        var session = EnrollmentSession(window: .preview, disciplines: .previewCatalogue)
        session.picks = [
            EnrollmentPick(disciplineId: 201, sectionId: 30101, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 204, sectionId: 30402, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 205, sectionId: 30501, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 203, sectionId: 30302, allowsOther: false, waitlist: false),
        ]
        return session
    }()
}

extension [EnrollmentDiscipline] {
    static var previewCatalogue: [EnrollmentDiscipline] {
        [
            previewDataStructures, previewDigitalSystems, previewProbability,
            previewConcurrency, previewDatabases,
            previewEnglish, previewPsychology, previewAI,
        ]
    }
}

private let previewDataStructures = EnrollmentDiscipline(
    id: 201, code: "EXA427", name: "Estruturas de Dados",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [EnrollmentPrerequisite(code: "EXA418", name: "Algoritmos e Programação II", met: true)],
    sections: [
        EnrollmentSection(
            id: 30101, label: "T01", coursePreferential: true, suggestion: true,
            vacancies: 50, proposalsCount: 31, allowsOtherDefault: true,
            waitlistCount: 0, selected: true,
            meetings: [fixtureMeeting("Teórica", .afternoon, ["Matheus Andrade"], "PAT76 · UEFS", [(1, "13:30", "15:30"), (3, "13:30", "15:30")])]
        ),
        EnrollmentSection(
            id: 30102, label: "T02", coursePreferential: false, suggestion: false,
            vacancies: 50, proposalsCount: 47, allowsOtherDefault: true,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .night, ["Cláudia Ribeiro"], "MA09 · UEFS", [(2, "18:50", "20:50"), (4, "18:50", "20:50")])]
        ),
    ],
    colorIndex: 1
)

private let previewDigitalSystems = EnrollmentDiscipline(
    id: 202, code: "TEC499", name: "Sistemas Digitais",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [],
    sections: [
        EnrollmentSection(
            id: 30201, label: "T01P01", coursePreferential: true, suggestion: true,
            vacancies: 40, proposalsCount: 22, allowsOtherDefault: true,
            waitlistCount: 0, selected: false,
            meetings: [
                fixtureMeeting("Teórica", .afternoon, ["Roberto Sales"], "PAT54 · UEFS", [(1, "13:30", "15:30")]),
                fixtureMeeting("Prática", .afternoon, ["Roberto Sales"], "Lab. Hardware · UEFS", [(5, "15:30", "17:30")]),
            ]
        ),
        EnrollmentSection(
            id: 30202, label: "T02P02", coursePreferential: false, suggestion: false,
            vacancies: 40, proposalsCount: 12, allowsOtherDefault: true,
            waitlistCount: 0, selected: false,
            meetings: [
                fixtureMeeting("Teórica", .morning, ["Roberto Sales"], "PAT55 · UEFS", [(2, "07:30", "09:30")]),
                fixtureMeeting("Prática", .morning, ["Helena Past"], "Lab. Hardware · UEFS", [(4, "09:30", "11:30")]),
            ]
        ),
    ],
    colorIndex: 0
)

private let previewProbability = EnrollmentDiscipline(
    id: 203, code: "EXA866", name: "Probabilidade e Estatística",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [EnrollmentPrerequisite(code: "EXA412", name: "Cálculo Diferencial e Integral I", met: true)],
    sections: [
        // Full → the waitlist path.
        EnrollmentSection(
            id: 30301, label: "T01", coursePreferential: true, suggestion: true,
            vacancies: 45, proposalsCount: 45, allowsOtherDefault: true,
            waitlistCount: 6, selected: false,
            meetings: [fixtureMeeting("Teórica", .morning, ["Sônia Vasconcelos"], "MA12 · UEFS", [(2, "07:30", "09:30"), (4, "07:30", "09:30")])]
        ),
        EnrollmentSection(
            id: 30302, label: "T02", coursePreferential: false, suggestion: false,
            vacancies: 45, proposalsCount: 39, allowsOtherDefault: true,
            waitlistCount: 0, selected: true,
            meetings: [fixtureMeeting("Teórica", .night, ["Ivan Coutinho"], "MA08 · UEFS", [(3, "20:50", "22:50"), (5, "20:50", "22:50")])]
        ),
    ],
    colorIndex: 3
)

private let previewConcurrency = EnrollmentDiscipline(
    id: 204, code: "TEC502", name: "Concorrência e Conectividade",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [],
    sections: [
        // Clashes with EXA427 T01 on Monday 13:30 — the conflict demo.
        EnrollmentSection(
            id: 30401, label: "T01", coursePreferential: true, suggestion: false,
            vacancies: 40, proposalsCount: 18, allowsOtherDefault: true,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .afternoon, ["Gustavo Lemos"], "PAT80 · UEFS", [(1, "13:30", "15:30"), (4, "13:30", "15:30")])]
        ),
        EnrollmentSection(
            id: 30402, label: "T02", coursePreferential: false, suggestion: false,
            vacancies: 40, proposalsCount: 25, allowsOtherDefault: true,
            waitlistCount: 0, selected: true,
            meetings: [fixtureMeeting("Teórica", .night, ["Gustavo Lemos"], "PAT81 · UEFS", [(1, "18:50", "20:50"), (3, "18:50", "20:50")])]
        ),
    ],
    colorIndex: 4
)

private let previewDatabases = EnrollmentDiscipline(
    id: 205, code: "TEC505", name: "Banco de Dados",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: false,
    prereqs: [],
    sections: [
        EnrollmentSection(
            id: 30501, label: "T01", coursePreferential: true, suggestion: false,
            vacancies: 45, proposalsCount: 20, allowsOtherDefault: true,
            waitlistCount: 0, selected: true,
            meetings: [fixtureMeeting("Teórica", .morning, ["Letícia Moura"], "LCC1 · UEFS", [(3, "09:30", "11:30"), (5, "09:30", "11:30")])]
        ),
        // Schedule and professor still "a definir" — never conflicts.
        EnrollmentSection(
            id: 30502, label: "T02", coursePreferential: false, suggestion: false,
            vacancies: 45, proposalsCount: 3, allowsOtherDefault: true,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .undefined, [], nil, [])]
        ),
    ],
    colorIndex: 2
)

private let previewEnglish = EnrollmentDiscipline(
    id: 301, code: "LET021", name: "Inglês Instrumental",
    workload: 30, mandatory: false, gradePeriod: 0, suggestion: false,
    prereqs: [],
    sections: [
        EnrollmentSection(
            id: 30601, label: "T01", coursePreferential: false, suggestion: false,
            vacancies: 35, proposalsCount: 14, allowsOtherDefault: false,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .afternoon, ["Marina Coelho"], "MOD7 · UEFS", [(5, "13:30", "15:30")])]
        ),
    ],
    colorIndex: 1
)

private let previewPsychology = EnrollmentDiscipline(
    id: 162, code: "CHF336", name: "Psicologia Social Comunitária",
    workload: 30, mandatory: false, gradePeriod: 0, suggestion: false,
    prereqs: [EnrollmentPrerequisite(code: "CHF330", name: "Psicologia Social", met: false)],
    sections: [
        EnrollmentSection(
            id: 30701, label: "T01", coursePreferential: false, suggestion: false,
            vacancies: 50, proposalsCount: 8, allowsOtherDefault: false,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .night, ["José Andrade"], "MOD3 · UEFS", [(2, "18:50", "20:50")])]
        ),
    ],
    colorIndex: 0
)

private let previewAI = EnrollmentDiscipline(
    id: 302, code: "TEC540", name: "Introdução à Inteligência Artificial",
    workload: 60, mandatory: false, gradePeriod: 0, suggestion: false,
    prereqs: [EnrollmentPrerequisite(code: "EXA427", name: "Estruturas de Dados", met: false)],
    sections: [
        // 27/30 → the "quase cheia" seat meter.
        EnrollmentSection(
            id: 30801, label: "T01", coursePreferential: false, suggestion: false,
            vacancies: 30, proposalsCount: 27, allowsOtherDefault: false,
            waitlistCount: 0, selected: false,
            meetings: [fixtureMeeting("Teórica", .afternoon, ["Daniel Prado"], "LCC2 · UEFS", [(2, "15:30", "17:30"), (4, "15:30", "17:30")])]
        ),
    ],
    colorIndex: 1
)

private func fixtureMeeting(
    _ kind: String,
    _ shift: EnrollmentShift,
    _ professors: [String],
    _ room: String?,
    _ slots: [(day: Int, start: String, end: String)]
) -> EnrollmentMeeting {
    EnrollmentMeeting(
        kind: kind,
        shift: shift,
        professors: professors,
        room: room,
        slots: slots.map { slot in
            EnrollmentSlot(
                day: slot.day,
                startMinute: fixtureMinutes(slot.start),
                endMinute: fixtureMinutes(slot.end)
            )
        }
    )
}

private func fixtureMinutes(_ time: String) -> Int {
    let parts = time.split(separator: ":").compactMap { Int($0) }
    return parts[0] * 60 + parts[1]
}

private func fixtureDate(_ year: Int, _ month: Int, _ day: Int, _ hour: Int, _ minute: Int) -> Date {
    var components = DateComponents(year: year, month: month, day: day, hour: hour, minute: minute)
    components.timeZone = TimeZone(secondsFromGMT: -3 * 3600)
    return Calendar(identifier: .gregorian).date(from: components)!
}
