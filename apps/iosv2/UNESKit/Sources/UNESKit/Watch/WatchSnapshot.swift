import Foundation

/// Everything the watch renders, pushed from the phone over
/// WatchConnectivity: the weekly schedule pattern (shared with the widgets),
/// the headline numbers, and the per-discipline grade/absence summaries for
/// drill-down. The phone is the only producer; the watch caches the latest
/// payload and renders it offline, so date-relative values (countdowns,
/// "em N dias") are always derived at render time, never baked in.
struct WatchSnapshot: Codable, Equatable, Sendable {
    var schedule = WidgetScheduleSnapshot()
    /// The cross-semester coefficient (CR); truncate for display.
    var coefficient: Double?
    /// Movement since the previous coefficient spark point.
    var coefficientDelta: Double?
    /// Presence guaranteed so far, semester-wide.
    var attendancePercent: Int?
    /// Class-hours of absence still allowed by the SAGRES 75% rule.
    var remainingAbsences: Int?
    var nextExam: Exam?
    /// Current-semester disciplines, name-sorted as everywhere else.
    var disciplines: [Discipline] = []
    /// The newest inbox messages, newest first, capped by the phone so the
    /// WatchConnectivity context stays within its budget.
    var messages: [Message] = []
    var syncedAt: Date

    struct Exam: Codable, Equatable, Sendable {
        /// Evaluation name as posted upstream, e.g. "P2".
        var label: String
        var disciplineName: String
        /// yyyy-MM-dd.
        var date: String
        /// "HH:mm" — the class slot on the exam's weekday, when there is one.
        var time: String?
    }

    struct Discipline: Codable, Equatable, Sendable, Identifiable {
        var id: String
        var code: String
        var name: String
        var teacherName: String?
        /// Catalog class-hours of the whole discipline.
        var hours: Int
        /// Class-hours of absence, discipline-wide.
        var missedHours: Int
        /// Deduplicated evaluations in upstream (ordinal) order.
        var grades: [Grade] = []
        /// Weighted mean of the released grades; nil until something lands.
        var partialAverage: Double?
        /// Index into `UNESColor.disciplinePalette`.
        var colorIndex: Int
    }

    struct Grade: Codable, Equatable, Sendable, Identifiable {
        var id: String
        /// Compact evaluation label, e.g. "P2".
        var label: String
        /// Full evaluation name, e.g. "Avaliação II".
        var name: String
        var value: Double?
        /// yyyy-MM-dd; nil when not scheduled.
        var date: String?
    }

    struct Message: Codable, Equatable, Sendable, Identifiable {
        var id: String
        var origin: MessageOrigin
        var disciplineCode: String?
        var disciplineName: String?
        var disciplineColorIndex: Int?
        var subject: String?
        /// Truncated by the phone (`MirrorStore.watchMessageBodyLimit`).
        var body: String
        var senderName: String
        var receivedAt: Date
        /// The phone-side read state; the watch overlays its own on top.
        var unread: Bool
        var attachments: [Attachment] = []

        struct Attachment: Codable, Equatable, Sendable, Identifiable {
            var id: String
            var kind: MessageAttachment.Kind
            var name: String?
            var url: String
        }
    }
}

extension WatchSnapshot.Discipline {
    var allowedMissedHours: Int { DisciplineRules.allowedMissedHours(ofTotal: hours) }

    var absenceRisk: AbsenceRisk {
        DisciplineRules.absenceRisk(missed: missedHours, allowed: allowedMissedHours)
    }

    var releasedCount: Int { grades.count { $0.value != nil } }
}

extension WatchSnapshot.Message {
    /// The inbox view of a pushed message, so the watch screens reuse the
    /// phone's message presentation (accent, badge, kind, preview).
    var item: MessageItem {
        MessageItem(
            id: id,
            origin: origin,
            disciplineCode: disciplineCode,
            disciplineName: disciplineName,
            disciplineColorIndex: disciplineColorIndex,
            subject: subject,
            body: body,
            senderName: senderName,
            receivedAt: receivedAt,
            unread: unread,
            starred: false,
            attachments: attachments.map {
                MessageAttachment(id: $0.id, kind: $0.kind, name: $0.name, url: $0.url)
            }
        )
    }
}

// MARK: - Preview data

extension WatchSnapshot {
    static func preview(now: Date = .now) -> WatchSnapshot {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        let weekday = Calendar.current.component(.weekday, from: now) - 1
        let nowMinute = Calendar.current.component(.hour, from: now) * 60
            + Calendar.current.component(.minute, from: now)
        return WatchSnapshot(
            schedule: WidgetScheduleSnapshot(
                semesterCode: "20261",
                sessions: [
                    .init(
                        classId: "c1", day: weekday, startMinute: 480, endMinute: 600,
                        code: "ALGI", title: "Algoritmos I", room: "LC-03",
                        teacherName: "Camila Ribeiro", colorIndex: 0, disciplineId: "d1"
                    ),
                    .init(
                        classId: "c2", day: weekday, startMinute: nowMinute + 39, endMinute: nowMinute + 139,
                        code: "CALC", title: "Cálculo II", room: "MT-14",
                        teacherName: "Adriana Matos", colorIndex: 1, disciplineId: "d2"
                    ),
                    .init(
                        classId: "c3", day: weekday, startMinute: 840, endMinute: 960,
                        code: "LPOO", title: "POO", room: "LC-01",
                        teacherName: "Rafael Almeida", colorIndex: 2, disciplineId: "d3"
                    ),
                    .init(
                        classId: "c4", day: weekday, startMinute: 980, endMinute: 1080,
                        code: "FIS2", title: "Física II", room: "PV-22",
                        teacherName: "João Nascimento", colorIndex: 3, disciplineId: "d4"
                    ),
                    .init(
                        classId: "c5", day: (weekday + 1) % 7, startMinute: 620, endMinute: 720,
                        code: "CALC", title: "Cálculo II", room: "MT-14",
                        teacherName: "Adriana Matos", colorIndex: 1, disciplineId: "d2"
                    ),
                    .init(
                        classId: "c6", day: (weekday + 1) % 7, startMinute: 940, endMinute: 1040,
                        code: "PROJ", title: "Projeto de Software", room: "LC-05",
                        teacherName: "Marcos Costa", colorIndex: 4, disciplineId: "d5"
                    ),
                ],
                topics: [
                    .init(classId: "c2", dayStamp: now.dayStamp, subject: "Integrais por partes"),
                ]
            ),
            coefficient: 8.5,
            coefficientDelta: 0.3,
            attendancePercent: 96,
            remainingAbsences: 2,
            nextExam: Exam(
                label: "P2", disciplineName: "Algoritmos I",
                date: stamp(daysFromNow: 5), time: "08:00"
            ),
            disciplines: [
                Discipline(
                    id: "d1", code: "ALGI", name: "Algoritmos I",
                    teacherName: "Camila Ribeiro", hours: 60, missedHours: 2,
                    grades: [
                        Grade(id: "g1", label: "P1", name: "Prova 1", value: 9.2, date: stamp(daysFromNow: -23)),
                        Grade(id: "g2", label: "T1", name: "Trabalho 1", value: 8.0, date: stamp(daysFromNow: -12)),
                        Grade(id: "g3", label: "P2", name: "Prova 2", value: nil, date: stamp(daysFromNow: 5)),
                    ],
                    partialAverage: 8.8, colorIndex: 0
                ),
                Discipline(
                    id: "d2", code: "CALC", name: "Cálculo II",
                    teacherName: "Adriana Matos", hours: 75, missedHours: 1,
                    grades: [
                        Grade(id: "g4", label: "P1", name: "Prova 1", value: 7.0, date: stamp(daysFromNow: -17)),
                        Grade(id: "g5", label: "L", name: "Listas", value: 8.0, date: nil),
                        Grade(id: "g6", label: "P2", name: "Prova 2", value: nil, date: stamp(daysFromNow: 12)),
                    ],
                    partialAverage: 7.5, colorIndex: 1
                ),
                Discipline(
                    id: "d3", code: "LPOO", name: "POO",
                    teacherName: "Rafael Almeida", hours: 60, missedHours: 0,
                    grades: [
                        Grade(id: "g7", label: "P1", name: "Prova 1", value: 9.5, date: stamp(daysFromNow: -15)),
                        Grade(id: "g8", label: "Pj", name: "Projeto", value: 9.3, date: stamp(daysFromNow: -3)),
                    ],
                    partialAverage: 9.4, colorIndex: 2
                ),
                Discipline(
                    id: "d4", code: "FIS2", name: "Física II",
                    teacherName: "João Nascimento", hours: 75, missedHours: 8,
                    grades: [
                        Grade(id: "g9", label: "P1", name: "Prova 1", value: nil, date: stamp(daysFromNow: 3)),
                    ],
                    partialAverage: nil, colorIndex: 3
                ),
                Discipline(
                    id: "d5", code: "PROJ", name: "Projeto de Software",
                    teacherName: "Marcos Costa", hours: 60, missedHours: 1,
                    grades: [
                        Grade(id: "g10", label: "E1", name: "Entrega 1", value: 8.0, date: stamp(daysFromNow: -30)),
                        Grade(id: "g11", label: "E2", name: "Entrega 2", value: 8.2, date: stamp(daysFromNow: -6)),
                        Grade(id: "g12", label: "F", name: "Prova Final", value: nil, date: stamp(daysFromNow: 20)),
                    ],
                    partialAverage: 8.1, colorIndex: 4
                ),
            ],
            messages: [
                Message(
                    id: "wm1", origin: .campus,
                    subject: "Retorno das aulas presenciais",
                    body: "Prezadxs estudantes,\n\nConfirmamos o retorno das atividades presenciais na próxima quarta-feira, 22 de abril, após a semana de recesso do feriado.\n\nA secretaria segue disponível para dúvidas sobre matrícula extemporânea até sexta.\n\nAtenciosamente,\nReitoria UEFS",
                    senderName: "Reitoria UEFS",
                    receivedAt: now.addingTimeInterval(-27 * 60), unread: true
                ),
                Message(
                    id: "wm2", origin: .discipline,
                    disciplineCode: "ALGI", disciplineName: "Algoritmos I", disciplineColorIndex: 0,
                    subject: "Gabarito da AV1 no moodle",
                    body: "Pessoal,\n\nSubi o gabarito comentado da primeira avaliação no moodle. Qualquer dúvida, usem o fórum ou me procurem no início da aula de segunda.\n\nA média da turma foi 6,2 — vamos revisar ponteiros duplos com calma na terça.\n\nAbs,\nAdriana",
                    senderName: "Adriana Lima",
                    receivedAt: now.addingTimeInterval(-99 * 60), unread: true,
                    attachments: [
                        .init(id: "wa1", kind: .pdf, name: "gabarito-av1.pdf", url: "https://example.org/gabarito-av1.pdf"),
                    ]
                ),
                Message(
                    id: "wm3", origin: .direct,
                    body: "Mariana,\n\nNotei que você não compareceu ao laboratório 03 ontem. Se foi alguma questão de saúde ou imprevisto, me avise em particular para eu ajustar a nota de participação.\n\nAtt,\nBeatriz",
                    senderName: "Beatriz Sampaio",
                    receivedAt: now.addingTimeInterval(-15 * 3600), unread: true
                ),
                Message(
                    id: "wm4", origin: .discipline,
                    disciplineCode: "LPOO", disciplineName: "POO", disciplineColorIndex: 2,
                    body: "Pessoal,\n\nAdicionei dois capítulos do Bloch (Effective Java) e o slide de mixins ao repositório da disciplina. Leiam antes de quinta.",
                    senderName: "Carlos Mendes",
                    receivedAt: now.addingTimeInterval(-2 * 86_400), unread: false,
                    attachments: [
                        .init(id: "wa2", kind: .pdf, name: "effective-java-cap3.pdf", url: "https://example.org/cap3.pdf"),
                        .init(id: "wa3", kind: .link, name: "Slides — mixins 2026", url: "https://drive.google.com/mixins-2026"),
                    ]
                ),
                Message(
                    id: "wm5", origin: .secretariat,
                    body: "Caros estudantes,\n\nConvidamos vocês para uma roda de conversa sobre TEA, organizada pelo núcleo de acessibilidade.\n\nData: 24 de abril, 14h.\nLocal: Auditório Central.\n\nInscrições até 22/04.",
                    senderName: "Secretaria Acadêmica",
                    receivedAt: now.addingTimeInterval(-3 * 86_400), unread: false
                ),
            ],
            syncedAt: now
        )
    }
}

