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
        /// Last time the worker applied upstream data for this semester —
        /// re-pulling a downloaded semester is warranted when it moves.
        var dirtyAt: String? = nil
        /// Enrolled disciplines, for the "download this semester" cards.
        var disciplineCount: Int? = nil
    }
}

extension SemesterListDTO.ItemDTO {
    var domain: Semester {
        Semester(id: id, code: code, description: description, startDate: startDate, endDate: endDate)
    }

    var record: SemesterRecord {
        SemesterRecord(
            id: id,
            code: code,
            description: description,
            startDate: startDate,
            endDate: endDate,
            disciplineCount: disciplineCount
        )
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
    /// Absent on pre-materials servers.
    var lectureMaterials: [LectureMaterialDTO]? = nil

    struct SemesterDTO: Decodable {
        let id: String
        let code: String
        let description: String
        let startDate: String
        let endDate: String
    }

    struct DisciplineDTO: Decodable {
        let id: String
        let code: String?
        let name: String
        var hours: Int? = nil
        /// Owning department, e.g. "Departamento de Ciências Exatas".
        var department: String? = nil
        /// Upstream "ementa" — the syllabus text.
        var program: String? = nil
    }

    struct OfferDTO: Decodable {
        let id: String
        let disciplineId: String
        /// Offer-level class-hours; unlike group hours these are never
        /// replicated per group, so they're the safe multi-group fallback.
        var hours: Int? = nil
    }

    struct ClassDTO: Decodable {
        let id: String
        let offerId: String
        let hours: Int
        var groupName: String? = nil
        var type: String? = nil
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
        /// Room number/code within the module (upstream "numero").
        let location: String
        /// Campus label (upstream "pavilhao"); "" when not set.
        var campus: String? = nil
        /// Building/module label (upstream "localizacao") — a short code
        /// ("MT") or descriptive prose; "" when not set.
        var modulo: String? = nil
    }

    struct AllocationDTO: Decodable {
        let id: String
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
        /// Decimal string, e.g. "7.7"; null while the semester is ongoing.
        var finalGrade: String? = nil
        var approved: Bool? = nil
        var wentToFinals: Bool? = nil
    }

    struct StudentGradeDTO: Decodable {
        let id: String
        let studentClassId: String
        let name: String?
        /// Compact evaluation label, e.g. "P2".
        let nameShort: String?
        let ordinal: Int
        /// Decimal string, e.g. "8.5"; null while ungraded.
        let value: String?
        let date: String?
        /// Upstream grade id — dedup key across replicated group rows.
        var platformId: String? = nil
        /// Decimal string.
        var weight: String? = nil
    }

    struct LectureDTO: Decodable {
        let id: String
        let classId: String
        /// Upstream position within the class; orders undated lectures.
        var ordinal: Int? = nil
        /// yyyy-MM-dd; null when not yet scheduled.
        let date: String?
        let subject: String?
    }

    struct LectureMaterialDTO: Decodable {
        let id: String
        let lectureId: String
        let description: String?
        let url: String
        var position: Int? = nil
    }
}

extension SemesterPayloadDTO {
    /// Every record carries the owning semester id so the mirror can replace
    /// one semester's scope atomically.
    var snapshot: SemesterSnapshot {
        let semesterId = semester.id
        return SemesterSnapshot(
            semester: SemesterRecord(
                id: semester.id,
                code: semester.code,
                description: semester.description,
                startDate: semester.startDate,
                endDate: semester.endDate
            ),
            disciplines: disciplines.map {
                DisciplineRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    code: $0.code,
                    name: $0.name,
                    hours: $0.hours,
                    department: $0.department,
                    program: $0.program
                )
            },
            disciplineOffers: disciplineOffers.map {
                DisciplineOfferRecord(id: $0.id, semesterId: semesterId, disciplineId: $0.disciplineId, hours: $0.hours)
            },
            classes: classes.map {
                ClassRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    offerId: $0.offerId,
                    hours: $0.hours,
                    groupName: $0.groupName,
                    type: $0.type
                )
            },
            teachers: teachers.map {
                TeacherRecord(id: $0.id, semesterId: semesterId, name: $0.name)
            },
            classTeachers: classTeachers.map {
                ClassTeacherRecord(semesterId: semesterId, classId: $0.classId, teacherId: $0.teacherId)
            },
            spaces: spaces.map {
                SpaceRecord(id: $0.id, semesterId: semesterId, location: $0.location, campus: $0.campus, modulo: $0.modulo)
            },
            allocations: allocations.map {
                AllocationRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    classId: $0.classId,
                    spaceId: $0.spaceId,
                    day: $0.day,
                    startTime: $0.startTime,
                    endTime: $0.endTime
                )
            },
            studentClasses: studentClasses.map {
                StudentClassRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    classId: $0.classId,
                    missedClasses: $0.missedClasses,
                    finalGrade: $0.finalGrade,
                    approved: $0.approved,
                    wentToFinals: $0.wentToFinals
                )
            },
            studentGrades: studentGrades.map {
                StudentGradeRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    studentClassId: $0.studentClassId,
                    name: $0.name,
                    nameShort: $0.nameShort,
                    ordinal: $0.ordinal,
                    value: $0.value,
                    date: $0.date,
                    platformId: $0.platformId,
                    weight: $0.weight
                )
            },
            lectures: lectures.map {
                LectureRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    classId: $0.classId,
                    ordinal: $0.ordinal,
                    date: $0.date,
                    subject: $0.subject
                )
            },
            lectureMaterials: (lectureMaterials ?? []).map {
                LectureMaterialRecord(
                    id: $0.id,
                    semesterId: semesterId,
                    lectureId: $0.lectureId,
                    caption: $0.description,
                    url: $0.url,
                    position: $0.position
                )
            }
        )
    }
}
