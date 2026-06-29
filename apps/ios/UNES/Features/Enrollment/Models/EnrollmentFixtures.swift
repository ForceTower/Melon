import Foundation

// UNES — Enrollment sample data. Ported faithfully from
// `screens-matricula-data.jsx` (Ciência da Computação · UEFS): five mandatory
// 4th-period disciplines plus three optativas, exercising every state the UI
// handles — suggestions, full sections (waitlist), schedule clashes, unmet
// prerequisites and "horário a definir" sections.

enum EnrollmentFixtures {
    static let window = EnrollmentWindow(
        semester: "2026.2",
        startLabel: "15 jun",
        endLabel: "22 jun · 23h59",
        minHours: 240,
        maxHours: 420,
        useQueue: true
    )

    static let student = EnrollmentStudent(
        name: "Mariana Alves",
        course: "Ciência da Computação",
        period: "4º período",
        avatarInitial: "M"
    )

    static let disciplines: [OfferedDiscipline] = [
        // ── Período 4 — obrigatórias (sugeridas) ──────────────────────────
        OfferedDiscipline(
            id: 8_000_000_201, code: "EXA427", name: "Estruturas de Dados",
            workload: 60, mandatory: true, gradePeriod: 4, tone: .teal, suggestion: true,
            prereqs: [Prerequisite(code: "EXA418", name: "Algoritmos e Programação II", met: true)],
            sections: [
                ClassSection(
                    id: 8_000_030_101, label: "T01", tone: .teal,
                    coursePreferential: true, suggestion: true,
                    vacancies: 50, proposalsCount: 31, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .afternoon,
                            professors: ["Matheus Andrade"], room: "PAT76 · UEFS",
                            slots: [
                                MeetingSlot(day: 1, start: "13:30", end: "15:30"),
                                MeetingSlot(day: 3, start: "13:30", end: "15:30"),
                            ]
                        )
                    ]
                ),
                ClassSection(
                    id: 8_000_030_102, label: "T02", tone: .teal,
                    coursePreferential: false, suggestion: false,
                    vacancies: 50, proposalsCount: 47, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .evening,
                            professors: ["Cláudia Ribeiro"], room: "MA09 · UEFS",
                            slots: [
                                MeetingSlot(day: 2, start: "18:50", end: "20:50"),
                                MeetingSlot(day: 4, start: "18:50", end: "20:50"),
                            ]
                        )
                    ]
                ),
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_202, code: "TEC499", name: "Sistemas Digitais",
            workload: 60, mandatory: true, gradePeriod: 4, tone: .coral, suggestion: true,
            prereqs: [],
            sections: [
                // theory + practice bundle (T01P01)
                ClassSection(
                    id: 8_000_030_201, label: "T01P01", tone: .coral,
                    coursePreferential: true, suggestion: true,
                    vacancies: 40, proposalsCount: 22, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .afternoon,
                            professors: ["Roberto Sales"], room: "PAT54 · UEFS",
                            slots: [MeetingSlot(day: 1, start: "13:30", end: "15:30")]
                        ),
                        SectionMeeting(
                            kind: "Prática", shift: .afternoon,
                            professors: ["Roberto Sales"], room: "Lab. Hardware · UEFS",
                            slots: [MeetingSlot(day: 5, start: "15:30", end: "17:30")]
                        ),
                    ]
                ),
                ClassSection(
                    id: 8_000_030_202, label: "T02P02", tone: .coral,
                    coursePreferential: false, suggestion: false,
                    vacancies: 40, proposalsCount: 12, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .morning,
                            professors: ["Roberto Sales"], room: "PAT55 · UEFS",
                            slots: [MeetingSlot(day: 2, start: "07:30", end: "09:30")]
                        ),
                        SectionMeeting(
                            kind: "Prática", shift: .morning,
                            professors: ["Helena Past"], room: "Lab. Hardware · UEFS",
                            slots: [MeetingSlot(day: 4, start: "09:30", end: "11:30")]
                        ),
                    ]
                ),
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_203, code: "EXA866", name: "Probabilidade e Estatística",
            workload: 60, mandatory: true, gradePeriod: 4, tone: .plum, suggestion: true,
            prereqs: [Prerequisite(code: "EXA412", name: "Cálculo Diferencial e Integral I", met: true)],
            sections: [
                // FULL section → waitlist
                ClassSection(
                    id: 8_000_030_301, label: "T01", tone: .plum,
                    coursePreferential: true, suggestion: true,
                    vacancies: 45, proposalsCount: 45, allowsOtherDefault: true, waitlistCount: 6,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .morning,
                            professors: ["Sônia Vasconcelos"], room: "MA12 · UEFS",
                            slots: [
                                MeetingSlot(day: 2, start: "07:30", end: "09:30"),
                                MeetingSlot(day: 4, start: "07:30", end: "09:30"),
                            ]
                        )
                    ]
                ),
                ClassSection(
                    id: 8_000_030_302, label: "T02", tone: .plum,
                    coursePreferential: false, suggestion: false,
                    vacancies: 45, proposalsCount: 39, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .evening,
                            professors: ["Ivan Coutinho"], room: "MA08 · UEFS",
                            slots: [
                                MeetingSlot(day: 3, start: "20:50", end: "22:50"),
                                MeetingSlot(day: 5, start: "20:50", end: "22:50"),
                            ]
                        )
                    ]
                ),
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_204, code: "TEC502", name: "Concorrência e Conectividade",
            workload: 60, mandatory: true, gradePeriod: 4, tone: .amber, suggestion: true,
            prereqs: [],
            sections: [
                // clashes with EXA427 T01 (Seg 13:30) — demonstrates a conflict
                ClassSection(
                    id: 8_000_030_401, label: "T01", tone: .amber,
                    coursePreferential: true, suggestion: false,
                    vacancies: 40, proposalsCount: 18, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .afternoon,
                            professors: ["Gustavo Lemos"], room: "PAT80 · UEFS",
                            slots: [
                                MeetingSlot(day: 1, start: "13:30", end: "15:30"),
                                MeetingSlot(day: 4, start: "13:30", end: "15:30"),
                            ]
                        )
                    ]
                ),
                ClassSection(
                    id: 8_000_030_402, label: "T02", tone: .amber,
                    coursePreferential: false, suggestion: false,
                    vacancies: 40, proposalsCount: 25, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .evening,
                            professors: ["Gustavo Lemos"], room: "PAT81 · UEFS",
                            slots: [
                                MeetingSlot(day: 1, start: "18:50", end: "20:50"),
                                MeetingSlot(day: 3, start: "18:50", end: "20:50"),
                            ]
                        )
                    ]
                ),
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_205, code: "TEC505", name: "Banco de Dados",
            workload: 60, mandatory: true, gradePeriod: 4, tone: .magenta, suggestion: false,
            prereqs: [],
            sections: [
                ClassSection(
                    id: 8_000_030_501, label: "T01", tone: .magenta,
                    coursePreferential: true, suggestion: false,
                    vacancies: 45, proposalsCount: 20, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .morning,
                            professors: ["Letícia Moura"], room: "LCC1 · UEFS",
                            slots: [
                                MeetingSlot(day: 3, start: "09:30", end: "11:30"),
                                MeetingSlot(day: 5, start: "09:30", end: "11:30"),
                            ]
                        )
                    ]
                ),
                // TBD schedule + professor → never conflicts, hidden from grid
                ClassSection(
                    id: 8_000_030_502, label: "T02", tone: .magenta,
                    coursePreferential: false, suggestion: false,
                    vacancies: 45, proposalsCount: 3, allowsOtherDefault: true,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .undefined,
                            professors: [], room: nil, slots: []
                        )
                    ]
                ),
            ]
        ),

        // ── Período 0 — optativas / eletivas ──────────────────────────────
        OfferedDiscipline(
            id: 8_000_000_301, code: "LET021", name: "Inglês Instrumental",
            workload: 30, mandatory: false, gradePeriod: 0, tone: .green, suggestion: false,
            prereqs: [],
            sections: [
                ClassSection(
                    id: 8_000_030_601, label: "T01", tone: .green,
                    coursePreferential: false, suggestion: false,
                    vacancies: 35, proposalsCount: 14, allowsOtherDefault: false,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .afternoon,
                            professors: ["Marina Coelho"], room: "MOD7 · UEFS",
                            slots: [MeetingSlot(day: 5, start: "13:30", end: "15:30")]
                        )
                    ]
                )
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_162, code: "CHF336", name: "Psicologia Social Comunitária",
            workload: 30, mandatory: false, gradePeriod: 0, tone: .coral, suggestion: false,
            // unmet prerequisite → warning badge
            prereqs: [Prerequisite(code: "CHF330", name: "Psicologia Social", met: false)],
            sections: [
                ClassSection(
                    id: 8_000_030_701, label: "T01", tone: .coral,
                    coursePreferential: false, suggestion: false,
                    vacancies: 50, proposalsCount: 8, allowsOtherDefault: false,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .evening,
                            professors: ["José Andrade"], room: "MOD3 · UEFS",
                            slots: [MeetingSlot(day: 2, start: "18:50", end: "20:50")]
                        )
                    ]
                )
            ]
        ),
        OfferedDiscipline(
            id: 8_000_000_302, code: "TEC540", name: "Introdução à Inteligência Artificial",
            workload: 60, mandatory: false, gradePeriod: 0, tone: .teal, suggestion: false,
            prereqs: [Prerequisite(code: "EXA427", name: "Estruturas de Dados", met: false)],
            sections: [
                ClassSection(
                    id: 8_000_030_801, label: "T01", tone: .teal,
                    coursePreferential: false, suggestion: false,
                    vacancies: 30, proposalsCount: 27, allowsOtherDefault: false,
                    meetings: [
                        SectionMeeting(
                            kind: "Teórica", shift: .afternoon,
                            professors: ["Daniel Prado"], room: "LCC2 · UEFS",
                            slots: [
                                MeetingSlot(day: 2, start: "15:30", end: "17:30"),
                                MeetingSlot(day: 4, start: "15:30", end: "17:30"),
                            ]
                        )
                    ]
                )
            ]
        ),
    ]
}
