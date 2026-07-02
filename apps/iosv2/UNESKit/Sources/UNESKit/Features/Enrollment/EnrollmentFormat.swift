import Foundation

/// pt-BR display strings for the matrícula flow.
enum EnrollmentFormat {
    /// 0 = Domingo … 6 = Sábado, the upstream weekday scale.
    static let daysShort = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"]
    static let daysFull = ["domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado"]

    static func dayShort(_ day: Int) -> String {
        daysShort.indices.contains(day) ? daysShort[day] : "?"
    }

    static func dayFull(_ day: Int) -> String {
        daysFull.indices.contains(day) ? daysFull[day] : "?"
    }

    /// "15 jun" — pt-BR month abbreviation without the trailing period.
    static func dayLabel(_ date: Date?) -> String {
        guard let date else { return "—" }
        return string(from: date, format: "d MMM").replacingOccurrences(of: ".", with: "")
    }

    /// "22 jun · 23h59" for the window's end bound.
    static func endLabel(_ date: Date?) -> String {
        guard let date else { return "—" }
        return "\(dayLabel(date)) · \(string(from: date, format: "H'h'mm"))"
    }

    private static func string(from date: Date, format: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "pt_BR")
        // The deadline is SAGRES wall-clock time (UEFS, no DST); rendering in
        // the device zone would shift it for anyone traveling.
        formatter.timeZone = TimeZone(identifier: "America/Bahia")
        formatter.dateFormat = format
        return formatter.string(from: date)
    }

    static func sectionCountLabel(_ count: Int) -> String {
        count == 1 ? "1 turma" : "\(count) turmas"
    }

    static func conflictCountLabel(_ count: Int) -> String {
        count == 1 ? "1 conflito" : "\(count) conflitos"
    }

    static func shiftLabel(_ shift: EnrollmentShift) -> String {
        switch shift {
        case .morning: "Matutino"
        case .afternoon: "Vespertino"
        case .night: "Noturno"
        case .undefined: "A definir"
        }
    }

    static func blockerLabel(_ blocker: EnrollmentBlocker) -> String {
        switch blocker {
        case let .conflicts(count): conflictCountLabel(count)
        case let .underMinimum(missing): "\(missing)h abaixo do mínimo"
        case let .overMaximum(excess): "\(excess)h acima do máximo"
        case .empty: "nenhuma disciplina"
        }
    }

    static func message(for error: any Error) -> String {
        guard let failure = error as? EnrollmentFailure else {
            return "O SAGRES não conseguiu responder agora. Tente de novo em instantes."
        }
        // Exhaustive so a new failure case forces its copy here.
        switch failure {
        case .sessionExpired:
            return "Sua sessão expirou. Entre novamente para continuar."
        case .network:
            return "Sem conexão. Verifique sua internet e tente de novo."
        case .server(let message?):
            return message
        case .server(nil):
            return "O SAGRES não conseguiu responder agora. Tente de novo em instantes."
        }
    }

    /// One line per distinct time band: ("Seg, Qua", "13:30–15:30").
    static func scheduleLines(for section: EnrollmentSection) -> [(days: String, time: String)] {
        var order: [String] = []
        var daysByTime: [String: [Int]] = [:]
        for slot in section.slots {
            let time = "\(ScheduleFormat.timeLabel(slot.startMinute))–\(ScheduleFormat.timeLabel(slot.endMinute))"
            if daysByTime[time] == nil { order.append(time) }
            daysByTime[time, default: []].append(slot.day)
        }
        return order.map { time in
            let days = daysByTime[time, default: []].sorted().map(dayShort).joined(separator: ", ")
            return (days: days, time: time)
        }
    }
}

extension EnrollmentWindow {
    /// Whole days until the window closes, floored at zero.
    func daysLeft(now: Date) -> Int {
        guard let endDate else { return 0 }
        return max(0, Int(ceil(endDate.timeIntervalSince(now) / 86_400)))
    }

    /// Share of the window still ahead, 0…1 — the countdown ring's fill.
    func remainingFraction(now: Date) -> Double {
        guard let startDate, let endDate, endDate > startDate else { return 0 }
        let fraction = endDate.timeIntervalSince(now) / endDate.timeIntervalSince(startDate)
        return min(1, max(0, fraction))
    }
}
