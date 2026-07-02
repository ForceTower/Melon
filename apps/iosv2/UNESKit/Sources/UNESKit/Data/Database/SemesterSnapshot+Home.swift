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
                DisciplineCard(
                    id: discipline.id,
                    code: index.displayCode(for: discipline),
                    name: discipline.name,
                    partial: index.partialGrade(forDiscipline: discipline.id),
                    colorIndex: position
                )
            }
        )
    }

    // MARK: Hero — next class occurrence in the week

    private func hero(index: SnapshotIndex, now: Date, calendar: Calendar) -> HomeHeroClass? {
        let minutesInDay = 24 * 60
        let minutesInWeek = 7 * minutesInDay
        let nowSlot = (calendar.component(.weekday, from: now) - 1) * minutesInDay
            + calendar.component(.hour, from: now) * 60
            + calendar.component(.minute, from: now)

        var winner: (delta: Int, day: Int, allocation: AllocationRecord)?
        for allocation in allocations where index.enrolledClassIds.contains(allocation.classId) {
            guard let day = allocation.day, let start = parseHhMm(allocation.startTime) else { continue }
            let slot = day * minutesInDay + start
            let delta = ((slot - nowSlot) % minutesInWeek + minutesInWeek) % minutesInWeek
            if winner.map({ delta < $0.delta }) ?? true {
                winner = (delta, day, allocation)
            }
        }
        guard let winner, let startMinute = parseHhMm(winner.allocation.startTime) else { return nil }

        // The winner's merged session provides the real end of the block.
        let session = mergedSessions(on: winner.day, index: index)
            .first { $0.classId == winner.allocation.classId && $0.startMinute <= startMinute
                && startMinute <= ($0.endMinute ?? $0.startMinute) }

        // Whole midnights between now and the occurrence, so "tomorrow 08:00"
        // counts as one day even when it is less than 24h away.
        let minutesOfDay = nowSlot % minutesInDay
        let dayOffset = (minutesOfDay + winner.delta) / minutesInDay
        // Anchoring on the occurrence day's midnight keeps the wall-clock time
        // exact even if a clock shift lands in between.
        guard let dayStart = calendar.date(byAdding: .day, value: dayOffset, to: calendar.startOfDay(for: now)),
              let startsAt = calendar.date(byAdding: .minute, value: startMinute, to: dayStart)
        else { return nil }
        let endMinute = session?.endMinute ?? parseHhMm(winner.allocation.endTime)

        let discipline = index.discipline(forClass: winner.allocation.classId)
        return HomeHeroClass(
            disciplineId: discipline?.id,
            disciplineName: discipline?.name ?? "",
            startsAt: startsAt,
            endsAt: endMinute.flatMap { calendar.date(byAdding: .minute, value: $0, to: dayStart) },
            startTime: minutesLabel(startMinute),
            endTime: endMinute.map(minutesLabel),
            topic: index.topic(forClass: winner.allocation.classId, on: startsAt.dayStamp),
            room: winner.allocation.spaceId.flatMap { index.spacesById[$0]?.location },
            teacherName: index.teacherName(forClass: winner.allocation.classId)
        )
    }

    // MARK: Coefficient (shared with the Me overview)

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
            label: label ?? "Avaliação",
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

