import Foundation

// Preview / test fixture: Psicologia 20232, a 3rd-período student — the data
// set the design was drawn against.
extension CourseProgress {
    static func preview(
        curriculum: Bool = true,
        breakdown: Bool = true,
        stale: Bool = false,
        manualPick: Bool = false,
        syncedAt: Date = Date(timeIntervalSince1970: 1_786_777_920)
    ) -> CourseProgress {
        guard curriculum else {
            return CourseProgress(
                curriculum: nil,
                summary: CurriculumSummary(
                    completedHours: 1500, requiredHours: nil, percent: nil,
                    excludedHours: 0, unclassifiedHours: 0,
                    disciplinesCompleted: 26, disciplinesTotal: 0
                ),
                requirements: [],
                periods: [],
                currentPeriod: nil,
                prerequisitesKnown: false,
                syncedAt: syncedAt,
                availableVersions: previewVersions(stale: stale, bound: nil),
                approvedHours: 1500
            )
        }
        let versions = previewVersions(stale: stale, bound: manualPick ? .manual : .resolved)
        return CourseProgress(
            curriculum: versions[0],
            summary: CurriculumSummary(
                completedHours: 1500, requiredHours: 4040, percent: 37.13,
                excludedHours: 200, unclassifiedHours: 0,
                disciplinesCompleted: 13, disciplinesTotal: 63
            ),
            requirements: breakdown ? previewRequirements : [],
            periods: previewPeriods,
            currentPeriod: 3,
            prerequisitesKnown: true,
            syncedAt: syncedAt,
            availableVersions: versions,
            approvedHours: 1500
        )
    }

    /// Psicologia's succession: 20232 (current) ← 20122 ← 20051. `bound`
    /// is the source stamped on 20232, nil when nothing is bound.
    private static func previewVersions(stale: Bool, bound: CurriculumBindingSource?) -> [CurriculumVersion] {
        [
            CurriculumVersion(
                id: "cur-psi-20232", code: "20232",
                label: "BACHAREL E FORMAÇÃO DE PSICÓLOGO",
                asOf: "2024-03-04", minPeriods: 10, maxPeriods: 15, stale: stale,
                current: true, supersededBy: nil, source: bound,
                completedHours: 1500, requiredHours: 4040, percent: 37.13, fit: 100
            ),
            CurriculumVersion(
                id: "cur-psi-20122", code: "20122",
                label: "BACHAREL E FORMAÇÃO DE PSICÓLOGO",
                asOf: "2013-02-18", minPeriods: 10, maxPeriods: 15, stale: true,
                current: false,
                supersededBy: CurriculumSupersession(code: "20232", effectiveFrom: "20232"),
                source: nil,
                completedHours: 780, requiredHours: 3890, percent: 20.05, fit: 52
            ),
            CurriculumVersion(
                id: "cur-psi-20051", code: "20051",
                label: "PSICOLOGIA",
                asOf: "2005-03-01", minPeriods: 10, maxPeriods: 14, stale: true,
                current: false,
                supersededBy: CurriculumSupersession(code: "20122", effectiveFrom: nil),
                source: nil,
                completedHours: 240, requiredHours: 3420, percent: 7.02, fit: 16
            ),
        ]
    }

    private static let previewRequirements: [CurriculumRequirementProgress] = [
        req("nucleo-comum", .required, "Núcleo Comum + Ênfase", nil, 1, 2820, 1500, true, 53.19),
        req("estagio-basico", .internship, "Estágio Básico", nil, 4, 360, 0, true, 0),
        req("estagio-especifico-1", .internship, "Estágio Supervisionado Específico I", "Estágio Superv. Específico I", 9, 225, 0, true, 0),
        req("estagio-especifico-2", .internship, "Estágio Supervisionado Específico II", "Estágio Superv. Específico II", 10, 225, 0, true, 0),
        req("atividade-complementar", .complementary, "Atividade Complementar", nil, nil, 200, 0, false, 0),
        req("optativa", .elective, "Optativa", nil, 7, 150, 0, true, 0),
        req("formacao-profissional-1", .other, "Formação Profissional I", nil, 6, 30, 0, true, 0),
        req("formacao-profissional-2", .other, "Formação Profissional II", nil, 10, 30, 0, true, 0),
    ]

    private static func req(
        _ code: String, _ kind: CurriculumRequirementKind, _ label: String, _ short: String?,
        _ starts: Int?, _ required: Int, _ completed: Int, _ derivable: Bool, _ percent: Double
    ) -> CurriculumRequirementProgress {
        CurriculumRequirementProgress(
            code: code, kind: kind, label: label, shortLabel: short ?? label,
            startsAtPeriod: starts, hoursRequired: required, hoursCompleted: completed,
            derivable: derivable, percent: percent
        )
    }

    // (code, name, hours, status, requirement, prerequisites)
    private typealias Row = (String, String, Int, CurriculumEntryStatus, String, [String])

    private static let previewRows: [(Int, [Row])] = [
        (1, [
            ("CHF289", "HIST. E EPISTEM. DA PSICOLOGIA", 60, .completed, "nucleo-comum", []),
            ("CHF288", "PROCESSOS PSICOLÓGI. BÁSICOS 1", 60, .completed, "nucleo-comum", []),
            ("SAU281", "ANATOMIA E FISIOLOGIA HUMANA", 60, .completed, "nucleo-comum", []),
            ("EXA810", "BIOESTATÍSTICA APLICADA", 60, .completed, "nucleo-comum", []),
            ("LET310", "LEITURA E PRODUÇÃO DE TEXTOS", 60, .completed, "nucleo-comum", []),
            ("CHF290", "ANTROPOLOGIA CULTURAL", 45, .completed, "nucleo-comum", []),
            ("CHF291", "PSIC. E RELAÇ. ÉTNICO-RACIAIS", 45, .completed, "nucleo-comum", []),
        ]),
        (2, [
            ("CHF299", "PROCESSOS PSICOLÓGI. BÁSICOS 2", 60, .completed, "nucleo-comum", ["CHF288"]),
            ("CHF295", "HISTÓRIA DA PSICOTERAPÊUTICA", 45, .completed, "nucleo-comum", ["CHF289"]),
            ("SAU283", "NEUROANATOMIA FUNCIONAL", 60, .completed, "nucleo-comum", ["SAU281"]),
            ("CHF292", "MÉTOD. E TÉCN. DE PESQ. EM PSI", 60, .failed, "nucleo-comum", []),
            ("CHF293", "PSICOLOGIA DO DESENVOLVIM. 1", 60, .completed, "nucleo-comum", []),
            ("CHF294", "FILOSOFIA E ÉTICA PROFISSIONAL", 45, .completed, "nucleo-comum", []),
            ("CHF296", "SOCIOLOGIA DAS ORGANIZAÇÕES", 45, .completed, "nucleo-comum", []),
        ]),
        (3, [
            ("CHF344", "AVALIAÇÃO PSICOLÓGICA 1", 60, .inProgress, "nucleo-comum", ["CHF299"]),
            ("CHF300", "PSICOLOGIA SOCIAL 1", 60, .inProgress, "nucleo-comum", ["CHF290"]),
            ("CHF301", "PSICOLOGIA DO DESENVOLVIM. 2", 60, .inProgress, "nucleo-comum", ["CHF293"]),
            ("CHF302", "TÉCN. DE ENTREVISTA EM PSIC.", 45, .inProgress, "nucleo-comum", ["CHF299"]),
            ("CHF343", "INTR. À PSICO. ORGAN. E TRAB.", 60, .inProgress, "nucleo-comum", ["CHF296"]),
            ("CHF303", "PROCESSOS GRUPAIS", 45, .withdrawn, "nucleo-comum", []),
            ("CHF304", "PSICOLOGIA E SAÚDE COLETIVA", 45, .available, "nucleo-comum", []),
        ]),
        (4, [
            ("CHF345", "AVALIAÇÃO PSICOLÓGICA 2", 60, .blocked, "nucleo-comum", ["CHF344"]),
            ("CHF353", "ESTÁGIO BÁSICO 1", 120, .blocked, "estagio-basico", ["CHF302"]),
            ("CHF312", "PESQ. QUALITAT. EM PSICOLOGIA", 60, .blocked, "nucleo-comum", ["CHF292"]),
            ("CHF310", "PSICOLOGIA SOCIAL 2", 45, .blocked, "nucleo-comum", ["CHF300"]),
            ("CHF311", "PSICOPATOLOGIA GERAL", 60, .available, "nucleo-comum", []),
            ("SAU284", "PSICOFARMACOLOGIA", 30, .available, "nucleo-comum", ["SAU283"]),
            ("CHF313", "PSIC. ESCOLAR E EDUCACIONAL", 60, .available, "nucleo-comum", []),
        ]),
        (5, [
            ("CHF320", "ABORD. PSICANALÍTICA 1", 60, .blocked, "nucleo-comum", ["CHF311"]),
            ("CHF321", "ABORD. COMPORTAMENTAL 1", 60, .blocked, "nucleo-comum", ["CHF311"]),
            ("CHF322", "PSICODIAGNÓSTICO", 60, .blocked, "nucleo-comum", ["CHF345"]),
            ("CHF354", "ESTÁGIO BÁSICO 2", 120, .blocked, "estagio-basico", ["CHF353"]),
            ("CHF365", "ATIVIDADE PEDAG. DE EXTENSÃO 6", 75, .available, "nucleo-comum", []),
            ("CHF323", "PSIC. HOSPITALAR E DA SAÚDE", 45, .blocked, "nucleo-comum", ["CHF304"]),
            ("CHF324", "MÉTODOS EM PSIC. EXPERIMENTAL", 45, .blocked, "nucleo-comum", ["CHF312"]),
        ]),
        (6, [
            ("CHF330", "ABORD. HUMANISTA-EXISTENCIAL", 60, .blocked, "nucleo-comum", ["CHF311"]),
            ("CHF331", "ABORD. PSICANALÍTICA 2", 60, .blocked, "nucleo-comum", ["CHF320"]),
            ("CHF332", "ABORD. COMPORTAMENTAL 2", 60, .blocked, "nucleo-comum", ["CHF321"]),
            ("CHF333", "PSIC. JURÍDICA E FORENSE", 45, .available, "nucleo-comum", ["CHF294"]),
            ("CHF334", "ORIENT. PROF. E DE CARREIRA", 45, .blocked, "nucleo-comum", ["CHF343"]),
            ("CHF391", "FORMAÇÃO PROFISSIONAL I", 30, .available, "formacao-profissional-1", []),
            ("CHF335", "TEORIAS E TÉCN. DE GRUPO", 45, .blocked, "nucleo-comum", ["CHF303"]),
        ]),
        (7, [
            ("CHF346", "CLÍN. PSICOL. DA CRIANÇA", 45, .blocked, "nucleo-comum", ["CHF322"]),
            ("CHF347", "NEUROPSICOLOGIA", 45, .blocked, "nucleo-comum", ["SAU284"]),
            ("CHF336", "PSICOTERAPIA BREVE", 60, .blocked, "nucleo-comum", ["CHF330"]),
            ("CHF337", "PSIC. COMUNITÁRIA E POL. PÚBL.", 60, .blocked, "nucleo-comum", ["CHF304"]),
            ("CHF348", "OPTATIVA I", 45, .available, "optativa", []),
            ("CHF360", "ESTÁGIO BÁSICO 3", 120, .blocked, "estagio-basico", ["CHF354"]),
        ]),
        (8, [
            ("CHF338", "TCC 1 · PROJETO DE PESQUISA", 60, .blocked, "nucleo-comum", ["CHF324"]),
            ("CHF339", "PSIC. DO TRABALHO E ERGONOMIA", 45, .blocked, "nucleo-comum", ["CHF334"]),
            ("CHF349", "AVAL. NEUROPSICOLÓGICA INFANTIL", 45, .blocked, "nucleo-comum", ["CHF347"]),
            ("CHF355", "OPTATIVA II", 45, .available, "optativa", []),
            ("CHF356", "ÉTICA E LEGISLAÇÃO EM PSIC.", 45, .available, "nucleo-comum", ["CHF294"]),
            ("CHF357", "PSICOLOGIA DA FAMÍLIA", 45, .blocked, "nucleo-comum", ["CHF335"]),
        ]),
        (9, [
            ("CHF340", "ESTÁGIO SUPERV. ESPECÍF. I", 225, .blocked, "estagio-especifico-1", ["CHF360"]),
            ("CHF341", "TCC 2 · MONOGRAFIA", 60, .blocked, "nucleo-comum", ["CHF338"]),
            ("CHF358", "PLANTÃO PSICOLÓGICO", 45, .blocked, "nucleo-comum", ["CHF336"]),
            ("CHF359", "OPTATIVA III", 60, .available, "optativa", []),
            ("CHF361", "PSIC. DA APRENDIZAGEM ESCOLAR", 45, .blocked, "nucleo-comum", ["CHF313"]),
        ]),
        (10, [
            ("CHF350", "ESTÁGIO SUPERV. ESPECÍF. II", 225, .blocked, "estagio-especifico-2", ["CHF340"]),
            ("CHF392", "FORMAÇÃO PROFISSIONAL II", 30, .blocked, "formacao-profissional-2", ["CHF391"]),
            ("CHF362", "SEMIN. DE INTEGR. PROFISSIONAL", 45, .blocked, "nucleo-comum", ["CHF341"]),
            ("CHF363", "CLÍNICA AMPLIADA", 45, .blocked, "nucleo-comum", ["CHF358"]),
        ]),
    ]

    private static let previewPeriods: [CurriculumPeriod] = previewRows.map { period, rows in
        CurriculumPeriod(
            period: period,
            entries: rows.map { code, name, hours, status, requirement, prerequisites in
                CurriculumEntry(
                    code: code, name: name, hours: hours, credits: nil, period: period,
                    coreqGroup: nil, requirementCode: requirement, status: status,
                    prerequisites: prerequisites, corequisites: []
                )
            }
        )
    }
}
