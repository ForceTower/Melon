import Foundation

// MARK: - Paradoxo — university-wide grade statistics

/// What a Paradoxo link points at: a discipline page or a teacher page.
enum ParadoxoEntityKind: String, Equatable, Sendable {
    case discipline, teacher
}

struct ParadoxoEntityRef: Equatable, Hashable, Sendable {
    var kind: ParadoxoEntityKind
    var id: String
}

/// The five grading-severity bands, keyed by overall mean. Display labels
/// and tones live with the feature.
enum ParadoxoTier: Equatable, Sendable {
    case relentless, demanding, balanced, fair, angel

    init(mean: Double) {
        self = switch mean {
        case 8.5...: .angel
        case 7.0..<8.5: .fair
        case 5.5..<7.0: .balanced
        case 3.5..<5.5: .demanding
        default: .relentless
        }
    }
}

/// A curated university-wide insight for the rotating hero. The title and
/// subtitle are server-authored copy; the label comes from `kind`.
struct ParadoxoPulseFact: Equatable, Sendable, Identifiable {
    enum Kind: String, Equatable, Sendable {
        case brutal, kind, trend, gap, rising, surprise, signature
    }

    var id: String
    var kind: Kind
    /// Headline number; `trend` and `rising` render it as a signed delta.
    var metric: Double
    var title: String
    var subtitle: String
    var ref: ParadoxoEntityRef
}

/// The four "Explorar" categories.
enum ParadoxoExploreKind: String, Equatable, Sendable, CaseIterable {
    case brutal, kind, rising, gap
}

struct ParadoxoRanking: Equatable, Sendable {
    var kind: ParadoxoExploreKind
    var entries: [ParadoxoRankedEntry]
}

struct ParadoxoRankedEntry: Equatable, Sendable, Identifiable {
    var ref: ParadoxoEntityRef
    var name: String
    /// Discipline code; nil for teachers.
    var code: String?
    var mean: Double
    var studentCount: Int
    /// The category value when it isn't the mean itself (rising slope,
    /// gap spread).
    var delta: Double?

    var id: String { "\(ref.kind.rawValue)-\(ref.id)" }
}

/// One of the student's current disciplines, backed by its historical stats.
struct ParadoxoDisciplineSummary: Equatable, Sendable, Identifiable {
    var id: String
    var code: String
    var name: String
    var mean: Double
    /// How many final grades feed the mean.
    var sampleCount: Int
    var spark: [Double]
    /// Share of historical students scoring below the student, 0–100.
    var myPercentile: Int?
}

/// The Paradoxo landing payload.
struct ParadoxoOverview: Equatable, Sendable {
    var pulse: [ParadoxoPulseFact]
    var myDisciplines: [ParadoxoDisciplineSummary]
    var rankings: [ParadoxoRanking]
    var studentCount: Int
    var meanCount: Int

    func ranking(_ kind: ParadoxoExploreKind) -> ParadoxoRanking? {
        rankings.first { $0.kind == kind }
    }
}

/// A searchable teacher or discipline. `searchKey` is pre-folded so typing
/// filters without re-normalizing the whole index per keystroke.
struct ParadoxoIndexEntry: Equatable, Sendable, Identifiable {
    var ref: ParadoxoEntityRef
    var name: String
    var code: String?
    var mean: Double
    var studentCount: Int
    var searchKey: String

    var id: String { "\(ref.kind.rawValue)-\(ref.id)" }

    static func fold(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "pt-BR"))
    }

    /// Every space-separated term must appear somewhere in the key.
    func matches(_ foldedQuery: String) -> Bool {
        foldedQuery.split(separator: " ").allSatisfy { searchKey.contains($0) }
    }
}

struct ParadoxoSemesterMean: Equatable, Sendable {
    var semester: String
    var mean: Double
}

struct ParadoxoDisciplineTeacher: Equatable, Sendable, Identifiable {
    var id: String
    var name: String
    var mean: Double
    var sampleCount: Int
    var lastSemester: String?
    var history: [ParadoxoSemesterMean]
}

struct ParadoxoDisciplineDetails: Equatable, Sendable {
    var id: String
    var code: String
    var name: String
    var department: String?
    var mean: Double
    var studentCount: Int
    var approved: Int
    var failed: Int
    var quit: Int
    var history: [ParadoxoSemesterMean]
    /// Share of final grades per integer bucket 0…10; empty when unavailable.
    var distribution: [Double]
    var myGrade: Double?
    var teachers: [ParadoxoDisciplineTeacher]
}

struct ParadoxoTeacherDiscipline: Equatable, Sendable, Identifiable {
    var id: String
    var code: String
    var name: String
    var mean: Double
    var sampleCount: Int
}

struct ParadoxoTeacherDetails: Equatable, Sendable {
    var id: String
    var name: String
    var mean: Double
    var studentCount: Int
    var approved: Int
    var failed: Int
    var quit: Int
    var lastSemester: String?
    var history: [ParadoxoSemesterMean]
    var distribution: [Double]
    var disciplines: [ParadoxoTeacherDiscipline]
}

// MARK: - Derived statistics

/// How a grade distribution reads at a glance.
enum ParadoxoShapeKind: Equatable, Sendable {
    case bimodal, strict, lenient, balanced, regular
}

enum ParadoxoStats {
    /// Classifies an 11-bucket distribution: two separated humps read as
    /// bimodal, a low/high peak as strict/lenient, a mid-weighted mass as
    /// balanced.
    static func shapeKind(of distribution: [Double]) -> ParadoxoShapeKind {
        guard distribution.count >= 3, let top = distribution.max() else { return .regular }
        let peak = distribution.firstIndex(of: top) ?? 0
        let weightedMean = distribution.enumerated().reduce(0.0) { $0 + $1.element * Double($1.offset) }
        var peaks = 0
        for index in 1..<(distribution.count - 1)
        where distribution[index] > distribution[index - 1]
            && distribution[index] > distribution[index + 1]
            && distribution[index] > 0.08 {
            peaks += 1
        }
        if peaks >= 2 { return .bimodal }
        if peak <= 2 { return .strict }
        if peak >= 8 { return .lenient }
        if (6.0...7.5).contains(weightedMean) { return .balanced }
        return .regular
    }

    /// Share of the distribution strictly below the grade's bucket, 0–100.
    static func percentile(of grade: Double, in distribution: [Double]) -> Int? {
        guard !distribution.isEmpty else { return nil }
        let bucket = max(0, min(distribution.count - 1, Int(grade.rounded())))
        let below = distribution.prefix(bucket).reduce(0, +)
        return Int((below * 100).rounded())
    }

    /// How steady the semester means are: 100 at zero deviation, 0 once the
    /// standard deviation reaches 2.5 grade points.
    static func consistency(of means: [Double]) -> Int? {
        guard means.count >= 2 else { return nil }
        let average = means.reduce(0, +) / Double(means.count)
        let variance = means.reduce(0) { $0 + ($1 - average) * ($1 - average) } / Double(means.count)
        let score = (1 - variance.squareRoot() / 2.5) * 100
        return Int(max(0, min(100, score)).rounded())
    }

    static func approvalPercent(approved: Int, failed: Int, quit: Int) -> Double {
        let total = approved + failed + quit
        guard total > 0 else { return 0 }
        return Double(approved) / Double(total) * 100
    }
}

// MARK: - Previews

extension ParadoxoOverview {
    static func preview() -> ParadoxoOverview {
        ParadoxoOverview(
            pulse: [
                ParadoxoPulseFact(
                    id: "p1", kind: .brutal, metric: 3.5,
                    title: "Cálculo Diferencial e Integral I E",
                    subtitle: "60% reprovam · 6.868 alunos",
                    ref: ParadoxoEntityRef(kind: .discipline, id: "d1")
                ),
                ParadoxoPulseFact(
                    id: "p2", kind: .kind, metric: 9.1,
                    title: "Laura Carvalho",
                    subtitle: "86% aprovados · História do Brasil",
                    ref: ParadoxoEntityRef(kind: .teacher, id: "t6")
                ),
                ParadoxoPulseFact(
                    id: "p3", kind: .trend, metric: -2.3,
                    title: "Cálculo I · últimos 6 sem.",
                    subtitle: "de 4,9 (2020.1) para 2,9 (2025.2)",
                    ref: ParadoxoEntityRef(kind: .discipline, id: "d1")
                ),
                ParadoxoPulseFact(
                    id: "p5", kind: .rising, metric: 1.8,
                    title: "MI — Programação",
                    subtitle: "última década · +38% aprovação",
                    ref: ParadoxoEntityRef(kind: .discipline, id: "d4")
                ),
                ParadoxoPulseFact(
                    id: "p6", kind: .surprise, metric: 9.1,
                    title: "Adriana Matos no Cálculo I",
                    subtitle: "média 9,1 onde a disciplina é 3,5",
                    ref: ParadoxoEntityRef(kind: .teacher, id: "t2")
                ),
            ],
            myDisciplines: [
                ParadoxoDisciplineSummary(
                    id: "d1", code: "EXA704", name: "Cálculo Diferencial e Integral I E",
                    mean: 3.5, sampleCount: 6868,
                    spark: [3.5, 2.6, 3.2, 3.5, 2.9, 2.9], myPercentile: 88
                ),
                ParadoxoDisciplineSummary(
                    id: "d2", code: "EXA813", name: "Estruturas Discretas",
                    mean: 4.8, sampleCount: 1240,
                    spark: [4.8, 5.1, 4.7, 4.3, 4.8, 4.9], myPercentile: 71
                ),
                ParadoxoDisciplineSummary(
                    id: "d3", code: "EXA805", name: "Algoritmos e Programação II",
                    mean: 5.7, sampleCount: 1079,
                    spark: [6.1, 5.6, 5.4, 5.7, 6.0, 5.8], myPercentile: 84
                ),
                ParadoxoDisciplineSummary(
                    id: "d5", code: "HUM412", name: "Ética em Computação",
                    mean: 7.2, sampleCount: 190,
                    spark: [7.5, 7.1, 6.8, 7.0, 7.3, 7.5], myPercentile: 62
                ),
            ],
            rankings: [
                ParadoxoRanking(kind: .brutal, entries: [
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .discipline, id: "d6"),
                        name: "Álgebra Vetorial e Geometria Analítica",
                        code: "EXA702", mean: 1.5, studentCount: 54, delta: nil
                    ),
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .discipline, id: "d8"),
                        name: "Cálculo Diferencial",
                        code: "EXA198", mean: 3.1, studentCount: 376, delta: nil
                    ),
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .teacher, id: "t1"),
                        name: "Joilma Silva Carneiro",
                        code: nil, mean: 3.5, studentCount: 1335, delta: nil
                    ),
                ]),
                ParadoxoRanking(kind: .kind, entries: [
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .teacher, id: "t6"),
                        name: "Laura Carvalho",
                        code: nil, mean: 8.9, studentCount: 432, delta: nil
                    ),
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .teacher, id: "t12"),
                        name: "Aline Macedo Carvalho Freitas",
                        code: nil, mean: 8.4, studentCount: 188, delta: nil
                    ),
                ]),
                ParadoxoRanking(kind: .rising, entries: [
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .discipline, id: "d4"),
                        name: "MI — Programação",
                        code: "TEC498", mean: 6.3, studentCount: 958, delta: 1.8
                    ),
                ]),
                ParadoxoRanking(kind: .gap, entries: [
                    ParadoxoRankedEntry(
                        ref: ParadoxoEntityRef(kind: .discipline, id: "d1"),
                        name: "Cálculo Diferencial e Integral I E",
                        code: "EXA704", mean: 3.5, studentCount: 6220, delta: 6.3
                    ),
                ]),
            ],
            studentCount: 42318,
            meanCount: 3912
        )
    }
}

extension ParadoxoIndexEntry {
    static func preview() -> [ParadoxoIndexEntry] {
        let disciplines: [(String, String, String, Double, Int)] = [
            ("d1", "EXA704", "Cálculo Diferencial e Integral I E", 3.5, 6868),
            ("d2", "EXA813", "Estruturas Discretas", 4.8, 1240),
            ("d3", "EXA805", "Algoritmos e Programação II", 5.7, 1079),
            ("d4", "TEC498", "MI — Programação", 6.3, 958),
            ("d5", "HUM412", "Ética em Computação", 7.2, 190),
        ]
        let teachers: [(String, String, Double, Int)] = [
            ("t1", "Joilma Silva Carneiro", 3.5, 1335),
            ("t2", "Adriana Matos", 7.8, 892),
            ("t3", "Camila Ribeiro", 6.9, 1420),
            ("t6", "Laura Carvalho", 8.9, 432),
        ]
        return disciplines.map { id, code, name, mean, students in
            ParadoxoIndexEntry(
                ref: ParadoxoEntityRef(kind: .discipline, id: id),
                name: name, code: code, mean: mean, studentCount: students,
                searchKey: fold("\(code) \(name)")
            )
        } + teachers.map { id, name, mean, students in
            ParadoxoIndexEntry(
                ref: ParadoxoEntityRef(kind: .teacher, id: id),
                name: name, code: nil, mean: mean, studentCount: students,
                searchKey: fold(name)
            )
        }
    }
}

extension ParadoxoDisciplineDetails {
    static func preview() -> ParadoxoDisciplineDetails {
        ParadoxoDisciplineDetails(
            id: "d1", code: "EXA704", name: "Cálculo Diferencial e Integral I E",
            department: "Ciências Exatas",
            mean: 3.5, studentCount: 6220, approved: 2477, failed: 3743, quit: 648,
            history: [
                ("2010.1", 4.0), ("2010.2", 3.5), ("2011.1", 3.1), ("2011.2", 2.9),
                ("2012.1", 2.5), ("2012.2", 3.1), ("2013.1", 3.5), ("2013.2", 3.3),
                ("2014.1", 3.0), ("2014.2", 3.5), ("2015.1", 3.9), ("2015.2", 3.8),
                ("2016.1", 3.3), ("2016.2", 3.5), ("2017.1", 4.7), ("2017.2", 4.1),
                ("2018.1", 3.5), ("2018.2", 2.9), ("2019.1", 3.3), ("2019.2", 3.1),
                ("2020.1", 4.9), ("2020.2", 5.2), ("2021.1", 4.8), ("2021.2", 4.6),
                ("2022.1", 4.6), ("2022.2", 4.1), ("2023.1", 3.5), ("2023.2", 2.6),
                ("2024.1", 3.2), ("2024.2", 3.5), ("2025.1", 2.9), ("2025.2", 2.9),
            ].map { ParadoxoSemesterMean(semester: $0.0, mean: $0.1) },
            distribution: [0.20, 0.18, 0.16, 0.13, 0.10, 0.08, 0.06, 0.04, 0.03, 0.01, 0.01],
            myGrade: 7.2,
            teachers: [
                ParadoxoDisciplineTeacher(
                    id: "t1", name: "Joilma Silva Carneiro", mean: 2.8, sampleCount: 324,
                    lastSemester: "2025.2",
                    history: [
                        ("2011.2", 1.7), ("2014.1", 3.1), ("2014.2", 2.8),
                        ("2024.2", 2.5), ("2025.1", 3.1), ("2025.2", 3.0),
                    ].map { ParadoxoSemesterMean(semester: $0.0, mean: $0.1) }
                ),
                ParadoxoDisciplineTeacher(
                    id: "t2", name: "Adriana Matos", mean: 5.9, sampleCount: 428,
                    lastSemester: "2024.1",
                    history: [
                        ("2015.1", 5.2), ("2017.2", 6.0), ("2019.1", 6.3),
                        ("2021.2", 6.1), ("2023.1", 5.8), ("2024.1", 5.9),
                    ].map { ParadoxoSemesterMean(semester: $0.0, mean: $0.1) }
                ),
                ParadoxoDisciplineTeacher(
                    id: "t10", name: "Alexandre de Macêdo Wahrhaftig", mean: 2.9, sampleCount: 512,
                    lastSemester: "2023.2",
                    history: [
                        ("2013.1", 3.2), ("2016.2", 3.0), ("2019.2", 2.8),
                        ("2021.1", 2.5), ("2023.2", 2.9),
                    ].map { ParadoxoSemesterMean(semester: $0.0, mean: $0.1) }
                ),
            ]
        )
    }
}

extension ParadoxoTeacherDetails {
    static func preview() -> ParadoxoTeacherDetails {
        ParadoxoTeacherDetails(
            id: "t1", name: "Joilma Silva Carneiro",
            mean: 3.5, studentCount: 1335, approved: 542, failed: 793, quit: 112,
            lastSemester: "2025.2",
            history: [
                ("2023.1", 4.2), ("2023.2", 3.8), ("2024.1", 3.6),
                ("2024.2", 3.5), ("2025.1", 3.3), ("2025.2", 3.5),
            ].map { ParadoxoSemesterMean(semester: $0.0, mean: $0.1) },
            distribution: [0.28, 0.22, 0.18, 0.12, 0.08, 0.05, 0.03, 0.02, 0.01, 0.005, 0.005],
            disciplines: [
                ParadoxoTeacherDiscipline(
                    id: "d1", code: "EXA704", name: "Cálculo Diferencial e Integral I E",
                    mean: 2.8, sampleCount: 324
                ),
                ParadoxoTeacherDiscipline(
                    id: "d8", code: "EXA198", name: "Cálculo Diferencial",
                    mean: 3.1, sampleCount: 376
                ),
                ParadoxoTeacherDiscipline(
                    id: "d7", code: "EXA703", name: "Álgebra Linear I E",
                    mean: 3.9, sampleCount: 284
                ),
                ParadoxoTeacherDiscipline(
                    id: "d9", code: "EXA441", name: "Orientação à Pesquisa III",
                    mean: 7.3, sampleCount: 8
                ),
            ]
        )
    }
}
