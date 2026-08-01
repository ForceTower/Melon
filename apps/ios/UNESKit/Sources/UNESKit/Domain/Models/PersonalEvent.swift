import Foundation

/// An entry the student created themselves. Personal events never leave the
/// device — they live in the local mirror and share the Calendário timeline
/// with the institutional feed from SAGRES.
struct PersonalEvent: Equatable, Sendable, Identifiable {
    let id: String
    var title: String
    /// yyyy-MM-dd.
    var start: String
    /// yyyy-MM-dd of the last day; nil for single-day entries.
    var end: String?
    var category: Category
    /// Denormalized: the picker only offers currently enrolled disciplines,
    /// and the chip has to keep reading right once that semester rolls over.
    var discipline: DisciplineTag?
    var reminder: Reminder
    var notes: String
    var createdAt: Date

    enum Category: String, CaseIterable, Equatable, Sendable {
        case task, exam, study, life
    }

    /// Days before the start date; `none` schedules nothing.
    enum Reminder: Int, CaseIterable, Equatable, Sendable {
        case none = 0
        case dayBefore = 1
        case threeDays = 3
        case week = 7
    }

    struct DisciplineTag: Equatable, Sendable {
        var id: String
        var code: String
        var name: String
    }
}

// MARK: - Preview data

extension [PersonalEvent] {
    static func preview(now: Date = .now) -> [PersonalEvent] {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        return [
            PersonalEvent(
                id: "p1",
                title: "Entregar relatório do Lab 3",
                start: stamp(daysFromNow: 3),
                end: nil,
                category: .task,
                discipline: PersonalEvent.DisciplineTag(id: "d3", code: "EXA412", name: "Física II"),
                reminder: .dayBefore,
                notes: "Anexar os gráficos do experimento de pêndulo.",
                createdAt: now
            ),
            PersonalEvent(
                id: "p2",
                title: "Revisar listas 4 a 6 pra P2",
                start: stamp(daysFromNow: 5),
                end: stamp(daysFromNow: 7),
                category: .study,
                discipline: PersonalEvent.DisciplineTag(id: "d1", code: "EXA805", name: "Algoritmos e Programação II"),
                reminder: .dayBefore,
                notes: "",
                createdAt: now
            ),
            PersonalEvent(
                id: "p3",
                title: "Consulta no dentista",
                start: stamp(daysFromNow: 9),
                end: nil,
                category: .life,
                discipline: nil,
                reminder: .dayBefore,
                notes: "14h30 · Centro",
                createdAt: now
            ),
        ]
    }
}
