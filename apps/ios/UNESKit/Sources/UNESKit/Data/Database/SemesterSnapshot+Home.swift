import Foundation

// MARK: - Semester snapshot → HomeOverview

extension SemesterSnapshot {
    func homeOverview(now: Date, calendar: Calendar = .current) -> HomeOverview {
        let index = SnapshotIndex(snapshot: self)
        let today = now.dayStamp
        let weekday = calendar.component(.weekday, from: now) - 1

        return HomeOverview(
            semesterId: semester.id,
            semesterCode: semester.code,
            hero: hero(index: index, now: now, calendar: calendar),
            coefficient: coefficientSummary,
            attendance: attendanceSummary(index: index, today: today),
            nextExam: nextExam(index: index, now: now, calendar: calendar),
            messages: nil,
            today: mergedSessions(on: weekday, index: index).map { session in
                TodayClass(
                    id: session.allocationId,
                    classId: session.classId,
                    disciplineId: index.discipline(forClass: session.classId)?.id ?? session.classId,
                    offerId: index.offerId(forClass: session.classId),
                    startMinute: session.startMinute,
                    endMinute: session.endMinute,
                    startTime: minutesLabel(session.startMinute),
                    code: index.displayCode(forClass: session.classId),
                    title: index.discipline(forClass: session.classId)?.name ?? "",
                    room: session.spaceId.flatMap { index.spacesById[$0]?.location },
                    topic: index.topic(forClass: session.classId, on: today),
                    colorIndex: index.colorIndex(forClass: session.classId)
                )
            },
            disciplines: index.sortedDisciplines.enumerated().map { position, discipline in
                let offers = disciplineOffers.filter { $0.disciplineId == discipline.id }
                return DisciplineCard(
                    id: discipline.id,
                    offerId: offers.count == 1 ? offers.first?.id : nil,
                    code: index.displayCode(for: discipline),
                    name: discipline.name,
                    partial: index.partialGrade(forDiscipline: discipline.id),
                    colorIndex: position
                )
            }
        )
    }

    // MARK: Hero — the running class, or the next occurrence in the week

    private func hero(index: SnapshotIndex, now: Date, calendar: Calendar) -> HomeHeroClass? {
        let weekday = calendar.component(.weekday, from: now) - 1
        let nowMinute = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
        let today = mergedSessions(on: weekday, index: index)

        // A running class holds the hero until halfway through — and the
        // day's last class until it ends — before the next one takes over.
        if let current = today.first(where: { $0.startMinute <= nowMinute && nowMinute < endEstimate($0) }) {
            let later = today.first { $0.startMinute > nowMinute }
            let halfway = (current.startMinute + endEstimate(current)) / 2
            if nowMinute < halfway || later == nil {
                return heroClass(for: current, daysAhead: 0, inProgress: true, index: index, now: now, calendar: calendar)
            }
            return heroClass(for: later!, daysAhead: 0, inProgress: false, index: index, now: now, calendar: calendar)
        }

        // Next session, scanning the rest of today then the coming week
        // (offset 7 wraps back to today's weekday for once-a-week schedules).
        for daysAhead in 0...7 {
            let sessions = daysAhead == 0 ? today : mergedSessions(on: (weekday + daysAhead) % 7, index: index)
            let candidate = daysAhead == 0 ? sessions.first { $0.startMinute > nowMinute } : sessions.first
            if let candidate {
                return heroClass(for: candidate, daysAhead: daysAhead, inProgress: false, index: index, now: now, calendar: calendar)
            }
        }
        return nil
    }

    /// Upstream occasionally omits the end slot — assume one class hour.
    private func endEstimate(_ session: DaySession) -> Int {
        session.endMinute.map { max($0, session.startMinute + 1) } ?? session.startMinute + 50
    }

    private func heroClass(
        for session: DaySession,
        daysAhead: Int,
        inProgress: Bool,
        index: SnapshotIndex,
        now: Date,
        calendar: Calendar
    ) -> HomeHeroClass? {
        // Anchoring on the occurrence day's midnight keeps the wall-clock time
        // exact even if a clock shift lands in between.
        guard let dayStart = calendar.date(byAdding: .day, value: daysAhead, to: calendar.startOfDay(for: now)),
              let startsAt = calendar.date(byAdding: .minute, value: session.startMinute, to: dayStart)
        else { return nil }
        // The in-progress treatment needs an end to count toward; fall back
        // to the estimate when upstream omitted the slot's end.
        let endMinute = session.endMinute ?? (inProgress ? endEstimate(session) : nil)

        let discipline = index.discipline(forClass: session.classId)
        return HomeHeroClass(
            disciplineId: discipline?.id,
            offerId: index.offerId(forClass: session.classId),
            disciplineName: discipline?.name ?? "",
            startsAt: startsAt,
            endsAt: endMinute.flatMap { calendar.date(byAdding: .minute, value: $0, to: dayStart) },
            startTime: minutesLabel(session.startMinute),
            endTime: session.endMinute.map(minutesLabel),
            topic: index.topic(forClass: session.classId, on: startsAt.dayStamp),
            room: session.spaceId.flatMap { index.spacesById[$0]?.location },
            teacherName: index.teacherName(forClass: session.classId),
            isInProgress: inProgress
        )
    }

    // MARK: Coefficient stand-in (shared with the Me overview)

    /// Plain mean of the semester's posted grades. MirrorStore replaces this
    /// with the cross-semester CR (`CoefficientHistory`) once any discipline
    /// has a closed result, so it only shows before the first one does.
    var coefficientSummary: CoefficientSummary? {
        let spark = studentGrades
            .filter { $0.value != nil }
            .sorted { ($0.date ?? "", $0.ordinal) < ($1.date ?? "", $1.ordinal) }
            .compactMap { $0.value.flatMap(Double.init) }
        guard !spark.isEmpty else { return nil }
        return CoefficientSummary(
            value: spark.reduce(0, +) / Double(spark.count),
            spark: spark,
            delta: spark.count >= 2 ? spark[spark.count - 1] - spark[spark.count - 2] : nil
        )
    }

    // MARK: Attendance — SAGRES counts absences in class-hours (shared with Me)

    func attendanceSummary(index: SnapshotIndex, today: String) -> AttendanceSummary? {
        let totalHours = classes
            .filter { index.enrolledClassIds.contains($0.id) }
            .reduce(0) { $0 + $1.hours }
        let held = lectures.contains { lecture in
            guard let date = lecture.date else { return false }
            return index.enrolledClassIds.contains(lecture.classId) && date <= today
        }
        guard totalHours > 0, held else { return nil }

        let totalMissed = totalMissedClassHours
        let percent = ((1 - Double(totalMissed) / Double(totalHours)) * 100).rounded()
        return AttendanceSummary(
            percent: max(0, min(100, Int(percent))),
            remainingAbsences: max(0, Int(Double(totalHours) * 0.25) - totalMissed)
        )
    }

    // MARK: Next exam — earliest future-dated evaluation still ungraded

    private func nextExam(index: SnapshotIndex, now: Date, calendar: Calendar) -> ExamSummary? {
        let today = now.dayStamp
        let candidate = studentGrades
            .filter { $0.value == nil && ($0.date ?? "") >= today }
            .min { ($0.date ?? "", $0.ordinal) < ($1.date ?? "", $1.ordinal) }
        guard let candidate,
              let date = candidate.date,
              let examDay = parseDayStamp(date, calendar: calendar),
              let classId = index.studentClassesById[candidate.studentClassId]?.classId
        else { return nil }

        let days = calendar.dateComponents([.day], from: calendar.startOfDay(for: now), to: examDay).day ?? 0
        let weekday = calendar.component(.weekday, from: examDay) - 1
        // Exams run in the class's own slot; surface that slot's start when
        // the class meets on the exam's weekday.
        let time = allocations
            .filter { $0.classId == classId && $0.day == weekday }
            .compactMap { parseHhMm($0.startTime) }
            .min()

        let label = [candidate.nameShort, candidate.name]
            .compactMap { $0?.trimmingCharacters(in: .whitespaces) }
            .first { !$0.isEmpty }
        return ExamSummary(
            label: label ?? String.localized(.commonAssessment),
            disciplineName: index.discipline(forClass: classId)?.name ?? "",
            date: date,
            time: time.map(minutesLabel),
            daysUntil: max(0, days)
        )
    }

    private func minutesLabel(_ minutes: Int) -> String {
        String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }
}

