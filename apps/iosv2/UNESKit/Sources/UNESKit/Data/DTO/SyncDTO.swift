import Foundation

// MARK: - api/sync/onboarding-status

struct OnboardingStatusDTO: Decodable {
    let courseLinked: Bool
    let initial: InitialDTO?
    let semesters: PhaseDTO
    let messages: MessagesDTO
    let activeSemesterReady: Bool?

    struct InitialDTO: Decodable {
        let status: String
        let appliedSemesters: Int?
    }

    struct PhaseDTO: Decodable {
        let status: String
        let total: Int?
        let done: Int?
        let failed: Int?
    }

    struct MessagesDTO: Decodable {
        let status: String
    }
}

extension OnboardingStatusDTO {
    var domain: OnboardingStatus {
        let ready = activeSemesterReady ?? false
        return OnboardingStatus(
            courseLinked: courseLinked,
            // Missing `initial` means a pre-split server: synthesize from the ready flag.
            initial: initial.map {
                .init(state: .init(status: $0.status), appliedSemesters: $0.appliedSemesters ?? 0)
            } ?? .init(state: ready ? .done : .running, appliedSemesters: ready ? 1 : 0),
            semesters: .init(
                state: .init(status: semesters.status),
                total: semesters.total ?? 0,
                done: semesters.done ?? 0,
                failed: semesters.failed ?? 0
            ),
            messages: .init(state: .init(status: messages.status)),
            activeSemesterReady: ready
        )
    }
}

extension OnboardingStatus.PhaseState {
    init(status: String) {
        self = switch status {
        case "pending": .pending
        case "running": .running
        case "done": .done
        case "partial": .partial
        case "failed": .failed
        default: .unknown
        }
    }
}

// MARK: - api/sync/semesters

struct SemesterListDTO: Decodable {
    let semesters: [ItemDTO]

    struct ItemDTO: Decodable {
        let id: String
        let code: String
        let description: String
        let startDate: String
        let endDate: String
    }
}

extension SemesterListDTO.ItemDTO {
    var domain: Semester {
        Semester(id: id, code: code, description: description, startDate: startDate, endDate: endDate)
    }
}

// MARK: - api/sync/semesters/:id (subset — decoding ignores the rest)

struct SemesterPayloadDTO: Decodable {
    let semester: SemesterDTO
    let disciplines: [DisciplineDTO]
    let disciplineOffers: [OfferDTO]
    let classes: [ClassDTO]
    let teachers: [TeacherDTO]
    let classTeachers: [ClassTeacherDTO]
    let spaces: [SpaceDTO]
    let allocations: [AllocationDTO]
    let studentClasses: [StudentClassDTO]
    let studentGrades: [StudentGradeDTO]
    let lectures: [LectureDTO]

    struct SemesterDTO: Decodable {
        let code: String
    }

    struct DisciplineDTO: Decodable {
        let id: String
        let name: String
    }

    struct OfferDTO: Decodable {
        let id: String
        let disciplineId: String
    }

    struct ClassDTO: Decodable {
        let id: String
        let offerId: String
        let hours: Int
    }

    struct TeacherDTO: Decodable {
        let id: String
        let name: String
    }

    struct ClassTeacherDTO: Decodable {
        let classId: String
        let teacherId: String
    }

    struct SpaceDTO: Decodable {
        let id: String
        let location: String
    }

    struct AllocationDTO: Decodable {
        let classId: String
        let spaceId: String?
        /// Upstream day encoding: 0=Sunday … 6=Saturday.
        let day: Int?
        let startTime: String?
        let endTime: String?
    }

    struct StudentClassDTO: Decodable {
        let id: String
        let classId: String
        let missedClasses: Int?
    }

    struct StudentGradeDTO: Decodable {
        let studentClassId: String
        let ordinal: Int
        /// Decimal string, e.g. "8.5"; null while ungraded.
        let value: String?
        let date: String?
    }

    struct LectureDTO: Decodable {
        let classId: String
        /// yyyy-MM-dd; null when not yet scheduled.
        let date: String?
    }
}

extension SemesterPayloadDTO {
    func readyOverview(now: Date, calendar: Calendar = .current) -> ReadyOverview {
        let enrolledIds = Set(studentClasses.map(\.classId))
        let enrolledClasses = classes.filter { enrolledIds.contains($0.id) }
        let spark = gradeSpark

        return ReadyOverview(
            semesterCode: semester.code,
            classCount: enrolledClasses.count,
            // 1 credit = 15 class-hours (a 60h course is 4 credits).
            totalCredits: enrolledClasses.reduce(0) { $0 + $1.hours } / 15,
            nextClass: nextClass(enrolledIds: enrolledIds, now: now, calendar: calendar),
            coefficient: spark.isEmpty ? nil : spark.reduce(0, +) / Double(spark.count),
            gradeSpark: spark,
            attendancePercent: attendancePercent(enrolledIds: enrolledIds, now: now)
        )
    }

    /// Posted grade values in (date, ordinal) order. Grade values are decimal
    /// strings with a dot separator.
    private var gradeSpark: [Double] {
        studentGrades
            .filter { $0.value != nil }
            .sorted { ($0.date ?? "", $0.ordinal) < ($1.date ?? "", $1.ordinal) }
            .compactMap { $0.value.flatMap(Double.init) }
    }

    /// Presence over lectures already held (dated up to today).
    private func attendancePercent(enrolledIds: Set<String>, now: Date) -> Int? {
        let today = now.dayStamp
        let held = lectures.count { lecture in
            guard let date = lecture.date else { return false }
            return enrolledIds.contains(lecture.classId) && date <= today
        }
        guard held > 0 else { return nil }
        let missed = studentClasses.reduce(0) { $0 + ($1.missedClasses ?? 0) }
        return max(0, min(100, 100 - (missed * 100 + held / 2) / held))
    }

    /// The allocation with the smallest forward distance in the week from
    /// `now`, using the same week-slot arithmetic as the KMP dashboard.
    private func nextClass(enrolledIds: Set<String>, now: Date, calendar: Calendar) -> NextClassInfo? {
        let minutesInDay = 24 * 60
        let minutesInWeek = 7 * minutesInDay
        let parts = calendar.dateComponents([.weekday, .hour, .minute], from: now)
        // Calendar weekday is 1=Sunday..7; upstream is 0=Sunday..6.
        let nowSlot = (parts.weekday! - 1) * minutesInDay + parts.hour! * 60 + parts.minute!

        var winner: (delta: Int, allocation: AllocationDTO)?
        for allocation in allocations where enrolledIds.contains(allocation.classId) {
            guard let day = allocation.day, let start = parseHhMm(allocation.startTime) else { continue }
            let slot = day * minutesInDay + start
            let delta = ((slot - nowSlot) % minutesInWeek + minutesInWeek) % minutesInWeek
            if winner.map({ delta < $0.delta }) ?? true {
                winner = (delta, allocation)
            }
        }
        guard let winner else { return nil }

        let classesById = Dictionary(classes.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let offersById = Dictionary(disciplineOffers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let disciplinesById = Dictionary(disciplines.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let spacesById = Dictionary(spaces.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teachersById = Dictionary(teachers.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let teacherIdByClass = Dictionary(classTeachers.map { ($0.classId, $0.teacherId) }, uniquingKeysWith: { first, _ in first })

        let allocation = winner.allocation
        let discipline = classesById[allocation.classId]
            .flatMap { offersById[$0.offerId] }
            .flatMap { disciplinesById[$0.disciplineId] }

        return NextClassInfo(
            disciplineName: discipline?.name ?? "",
            startTime: hhMm(allocation.startTime) ?? "",
            endTime: hhMm(allocation.endTime),
            location: allocation.spaceId.flatMap { spacesById[$0]?.location },
            teacherName: teacherIdByClass[allocation.classId].flatMap { teachersById[$0]?.name },
            startsInMinutes: winner.delta
        )
    }

    /// Upstream times come as "HH:mm:ss"; the UI speaks "HH:mm".
    private func hhMm(_ time: String?) -> String? {
        guard let time, time.count >= 5 else { return time }
        return String(time.prefix(5))
    }

    private func parseHhMm(_ value: String?) -> Int? {
        guard let value, !value.isEmpty else { return nil }
        let parts = value.split(separator: ":")
        guard parts.count >= 2, let h = Int(parts[0]), let m = Int(parts[1].prefix(2)) else { return nil }
        return h * 60 + m
    }
}

// MARK: - api/sync/messages (fetched for warmth; content unused here)

struct MessagesPageDTO: Decodable {}
