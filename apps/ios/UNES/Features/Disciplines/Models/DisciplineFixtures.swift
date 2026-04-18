import SwiftUI

/// Mirrors `DISCIPLINES`, `UNDOWNLOADED_SEMESTERS` and `LAZY_DISCIPLINES` from
/// the prototype's `screens-disciplines-data.jsx`. Same data, same IDs so
/// future integration with a real backend can swap this out cleanly.
enum DisciplineFixtures {
    static let currentSemesterId = "2026.1"

    // Colors used across the mock data that aren't in UNESColor.
    private static let teal   = Color(red: 0x3B/255, green: 0x9E/255, blue: 0xAE/255)
    private static let green  = Color(red: 0x5C/255, green: 0x8C/255, blue: 0x3E/255)
    private static let rust   = Color(red: 0xD9/255, green: 0x77/255, blue: 0x57/255)
    private static let violet = Color(red: 0x6B/255, green: 0x4B/255, blue: 0x9C/255)

    static let semesters: [Semester] = [
        current,
        past2025_2,
        past2025_1,
    ]

    static let undownloaded: [Semester] = [
        Semester(id: "2024.2", disciplines: [], isDownloaded: false, estimatedCount: 6),
        Semester(id: "2024.1", disciplines: [], isDownloaded: false, estimatedCount: 5),
    ]

    /// Fully-populated disciplines for the lazy-loaded past semesters, keyed by
    /// semester id. Promoted into the list once the user taps "Baixar".
    static let lazyDisciplines: [String: [Discipline]] = [
        "2024.2": lazy2024_2,
        "2024.1": lazy2024_1,
    ]

    // MARK: - Current semester (2026.1)

    private static let current = Semester(id: "2026.1", disciplines: [
        Discipline(
            code: "ALGI", fullCode: "EXA805",
            title: "Algoritmos e Programação II",
            dept: "Ciências Exatas", prof: "Camila Ribeiro",
            color: UNESColor.coral,
            hours: 60,
            absences: 2, allowedAbsences: 15,
            sections: [
                GradeSection(name: "Teórica", grades: [
                    GradeEntry(label: "AV1", title: "Prova 1",       date: "31/03/2026", score: 8.3),
                    GradeEntry(label: "AV2", title: "Prova 2",       date: "12/05/2026", score: nil),
                    GradeEntry(label: "AV3", title: "Projeto Final", date: "07/07/2026", score: nil),
                ])
            ],
            classes: [
                ClassEntry(date: "10/03/2026", title: "Apresentação da disciplina",        attachments: 1, past: true),
                ClassEntry(date: "12/03/2026", title: "Revisão de ponteiros e structs",    attachments: 2, past: true),
                ClassEntry(date: "17/03/2026", title: "Listas ligadas: inserção e remoção", attachments: 3, past: true),
                ClassEntry(date: "19/03/2026", title: "Listas duplamente ligadas",          attachments: 1, past: true),
                ClassEntry(date: "24/03/2026", title: "Pilhas — aplicações",                attachments: 0, past: true),
                ClassEntry(date: "14/04/2026", title: "Filas e deques",                     attachments: 2, past: false, isNext: true),
                ClassEntry(date: "16/04/2026", title: "Árvores binárias — introdução",      attachments: nil, past: false),
            ],
            attachments: [
                Attachment(name: "Lista de exercícios 03 — pilhas.pdf", kind: .pdf,    added: "24/03", group: nil),
                Attachment(name: "Slides — listas duplamente ligadas",  kind: .slides, added: "19/03", group: nil),
                Attachment(name: "Repo exemplos da aula",               kind: .link,   added: "17/03", group: nil),
                Attachment(name: "Material complementar — Sedgewick",   kind: .pdf,    added: "10/03", group: nil),
            ],
            ementa: "Estruturas de dados lineares e não-lineares: listas, pilhas, filas, árvores e grafos. Análise de complexidade. Algoritmos de ordenação e busca. Implementação em C com ênfase em gerenciamento manual de memória e modularização."
        ),
        Discipline(
            code: "CALC", fullCode: "EXA704",
            title: "Cálculo Diferencial II",
            dept: "Ciências Exatas", prof: "Adriana Matos",
            color: teal,
            hours: 75,
            absences: 4, allowedAbsences: 18,
            sections: [
                GradeSection(name: "Teórica", grades: [
                    GradeEntry(label: "AV1", title: "I Avaliação",   date: "03/04/2026", score: 7.2),
                    GradeEntry(label: "AV2", title: "II Avaliação",  date: "15/05/2026", score: nil),
                    GradeEntry(label: "AV3", title: "III Avaliação", date: "10/07/2026", score: nil),
                ])
            ],
            classes: [
                ClassEntry(date: "11/03/2026", title: "Revisão: integrais definidas", attachments: 1, past: true),
                ClassEntry(date: "18/03/2026", title: "Integrais por substituição",   attachments: 2, past: true),
                ClassEntry(date: "25/03/2026", title: "Integrais por partes",         attachments: 3, past: true),
                ClassEntry(date: "17/04/2026", title: "Aplicações: área e volume",    attachments: 1, past: false, isNext: true),
            ],
            attachments: [
                Attachment(name: "Lista 02 — integrais por partes", kind: .pdf,   added: "25/03", group: nil),
                Attachment(name: "Notas de aula — cap. 4",          kind: .notes, added: "18/03", group: nil),
            ],
            ementa: "Técnicas de integração. Integrais impróprias. Aplicações geométricas e físicas. Sequências e séries numéricas. Séries de potências. Séries de Taylor e Maclaurin. Equações diferenciais ordinárias de primeira ordem."
        ),
        Discipline(
            code: "LPOO", fullCode: "EXA807",
            title: "Programação Orientada a Objetos",
            dept: "Ciências Exatas", prof: "Rafael Almeida",
            color: UNESColor.magenta,
            hours: 60,
            absences: 8, allowedAbsences: 15,
            sections: [
                GradeSection(name: "Teórica", grades: [
                    GradeEntry(label: "AV1", title: "Avaliação I",   date: nil, score: nil),
                    GradeEntry(label: "AV2", title: "Avaliação II",  date: nil, score: nil),
                    GradeEntry(label: "AV3", title: "Avaliação III", date: nil, score: nil),
                ])
            ],
            classes: [
                ClassEntry(date: "09/03/2026", title: "Encapsulamento",          attachments: 1, past: true),
                ClassEntry(date: "16/03/2026", title: "Herança vs composição",   attachments: 2, past: true),
            ],
            ementa: "Paradigma de orientação a objetos. Classes, objetos, encapsulamento, herança, polimorfismo. Interfaces e classes abstratas. Padrões de projeto introdutórios. Tratamento de exceções. Testes unitários."
        ),
        Discipline(
            code: "FIS2", fullCode: "EXA412",
            title: "Física II",
            dept: "Ciências Exatas", prof: "João Nascimento",
            color: UNESColor.plum,
            hours: 75,
            absences: 4, allowedAbsences: 29,
            sections: [
                GradeSection(name: "Teórica", group: "T01", grades: [
                    GradeEntry(label: "AV1", title: "I Avaliação",   date: "07/04/2026", score: 6.8),
                    GradeEntry(label: "AV2", title: "II Avaliação",  date: "19/05/2026", score: nil),
                    GradeEntry(label: "AV3", title: "III Avaliação", date: "14/07/2026", score: nil),
                ]),
                GradeSection(name: "Prática", group: "T01P01", grades: [
                    GradeEntry(label: "LAB", title: "Relatórios", date: nil, score: 9.0),
                ]),
            ],
            attachments: [
                Attachment(name: "Lista 01 — termodinâmica", kind: .pdf,    added: "20/03", group: "T01"),
                Attachment(name: "Slides — oscilações",      kind: .slides, added: "02/04", group: "T01"),
                Attachment(name: "Roteiro lab 01",           kind: .pdf,    added: "15/03", group: "T01P01"),
                Attachment(name: "Template de relatório",   kind: .slides, added: "15/03", group: "T01P01"),
            ],
            ementa: "Termodinâmica, oscilações, ondas mecânicas e acústica. Leis da termodinâmica, máquinas térmicas, entropia. Movimento harmônico simples e amortecido. Ondas em cordas e tubos.",
            groups: [
                DisciplineGroup(code: "T01",    kind: "Teórica", prof: "João Nascimento"),
                DisciplineGroup(code: "T01P01", kind: "Prática", prof: "Beatriz Sampaio"),
            ]
        ),
        Discipline(
            code: "EST", fullCode: "EXA902",
            title: "Estatística",
            dept: "Ciências Exatas", prof: "Laís Pinheiro",
            color: UNESColor.amber,
            hours: 60,
            absences: 1, allowedAbsences: 15,
            sections: [
                GradeSection(name: "Teórica", grades: [
                    GradeEntry(label: "AV1", title: "Prova 1", date: "01/04/2026", score: 9.1),
                    GradeEntry(label: "AV2", title: "Prova 2", date: "13/05/2026", score: nil),
                    GradeEntry(label: "AV3", title: "Projeto", date: "05/07/2026", score: nil),
                ])
            ],
            ementa: "Estatística descritiva. Probabilidade. Variáveis aleatórias. Distribuições discretas e contínuas. Inferência estatística básica."
        ),
    ])

    // MARK: - 2025.2

    private static let past2025_2 = Semester(id: "2025.2", disciplines: [
        Discipline(
            code: "ALG1", fullCode: "EXA801", title: "Algoritmos e Programação I",
            dept: "Ciências Exatas", prof: "Camila Ribeiro", color: UNESColor.coral,
            hours: 60, absences: 4, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "I Avaliação",   date: "01/04/2025", score: 6.3),
                GradeEntry(label: "AV2", title: "II Avaliação",  date: "13/05/2025", score: 7.9),
                GradeEntry(label: "AV3", title: "III Avaliação", date: "12/06/2025", score: 9.6),
            ])],
            finalGrade: 7.9
        ),
        Discipline(
            code: "TEC1", fullCode: "TEC401", title: "Circuitos Digitais",
            dept: "Tecnologia", prof: "Paulo Serra", color: violet,
            hours: 60, absences: 8, allowedAbsences: 15,
            sections: [
                GradeSection(name: "Teórica", grades: [
                    GradeEntry(label: "AV1", title: "Unidade 1", date: "09/05/2025", score: 1.0),
                    GradeEntry(label: "AV2", title: "Unidade 2", date: "30/05/2025", score: 5.5),
                    GradeEntry(label: "AV3", title: "Unidade 3", date: "04/07/2025", score: 9.5),
                ]),
                GradeSection(name: "Notas Complementares", grades: [
                    GradeEntry(label: "ADI", title: "Prova Final", date: nil, score: 7.0),
                ]),
            ],
            finalGrade: 5.9
        ),
        Discipline(
            code: "CAL1", fullCode: "EXA701", title: "Introdução ao Cálculo",
            dept: "Ciências Exatas", prof: "Adriana Matos", color: teal,
            hours: 75, absences: 2, allowedAbsences: 18,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "10/04/2025", score: 8.5),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "20/05/2025", score: 8.0),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "25/06/2025", score: 7.5),
            ])],
            finalGrade: 8.0
        ),
        Discipline(
            code: "LIB", fullCode: "LET502", title: "Libras: Noções Básicas",
            dept: "Letras e Artes", prof: "Lidineia Alves Cerqueira Barreiros", color: green,
            hours: 45, absences: 3, allowedAbsences: 11,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "08/04/2025", score: 9.0),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "20/05/2025", score: 9.5),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "24/06/2025", score: 9.8),
            ])],
            finalGrade: 9.4
        ),
    ])

    // MARK: - 2025.1

    private static let past2025_1 = Semester(id: "2025.1", disciplines: [
        Discipline(
            code: "MI", fullCode: "TEC498", title: "Projeto de Circuitos Digitais",
            dept: "Tecnologia", prof: "Paulo Serra", color: violet,
            hours: 60, absences: 2, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica-Prática", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "24/04/2025", score: 9.5),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "13/06/2025", score: 10.0),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "10/07/2025", score: 10.0),
            ])],
            finalGrade: 9.8
        ),
        Discipline(
            code: "TFC", fullCode: "TEC221", title: "Tópicos de Formação Complementar",
            dept: "Tecnologia", prof: "Marcela Duarte", color: rust,
            hours: 30, absences: 1, allowedAbsences: 8,
            sections: [GradeSection(name: "Teórica-Prática", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "25/03/2025", score: 9.0),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "08/05/2025", score: 6.0),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "05/06/2025", score: 8.3),
            ])],
            finalGrade: 7.7
        ),
    ])

    // MARK: - 2024.2 (lazy)

    private static let lazy2024_2: [Discipline] = [
        Discipline(
            code: "BIO", fullCode: "EXA301", title: "Introdução à Biologia",
            dept: "Ciências da Vida", prof: "Fernanda Lira", color: green,
            hours: 60, absences: 5, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "02/09/2024", score: 7.0),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "21/10/2024", score: 8.4),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "11/12/2024", score: 7.8),
            ])],
            finalGrade: 7.7
        ),
        Discipline(
            code: "QUI", fullCode: "EXA415", title: "Química Geral",
            dept: "Ciências Exatas", prof: "Roberto Andrade", color: UNESColor.magenta,
            hours: 60, absences: 2, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Prova 1", date: "10/09/2024", score: 5.5),
                GradeEntry(label: "AV2", title: "Prova 2", date: "15/10/2024", score: 7.0),
                GradeEntry(label: "AV3", title: "Prova 3", date: "03/12/2024", score: 8.8),
            ])],
            finalGrade: 7.1
        ),
        Discipline(
            code: "MAT", fullCode: "EXA102", title: "Matemática Básica",
            dept: "Ciências Exatas", prof: "Adriana Matos", color: teal,
            hours: 75, absences: 1, allowedAbsences: 18,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "05/09/2024", score: 9.0),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "18/10/2024", score: 9.5),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "06/12/2024", score: 9.8),
            ])],
            finalGrade: 9.4
        ),
        Discipline(
            code: "RED", fullCode: "LET201", title: "Oficina de Redação",
            dept: "Letras e Artes", prof: "Marina Costa", color: rust,
            hours: 30, absences: 0, allowedAbsences: 8,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Ensaio 1", date: "12/09/2024", score: 8.5),
                GradeEntry(label: "AV2", title: "Ensaio 2", date: "30/10/2024", score: 7.5),
            ])],
            finalGrade: 8.0
        ),
        Discipline(
            code: "FIL", fullCode: "HUM108", title: "Filosofia e Ética",
            dept: "Humanidades", prof: "Hugo Ferraz", color: violet,
            hours: 45, absences: 4, allowedAbsences: 11,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Prova 1", date: "08/09/2024", score: 6.0),
                GradeEntry(label: "AV2", title: "Prova 2", date: "20/10/2024", score: 4.5),
                GradeEntry(label: "AV3", title: "Prova 3", date: "09/12/2024", score: 5.5),
            ])],
            finalGrade: 5.3
        ),
        Discipline(
            code: "ING", fullCode: "LET404", title: "Inglês Instrumental",
            dept: "Letras e Artes", prof: "Sarah Moura", color: UNESColor.amber,
            hours: 30, absences: 2, allowedAbsences: 8,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Exam 1", date: "15/09/2024", score: 8.0),
                GradeEntry(label: "AV2", title: "Exam 2", date: "05/11/2024", score: 9.2),
            ])],
            finalGrade: 8.6
        ),
    ]

    // MARK: - 2024.1 (lazy)

    private static let lazy2024_1: [Discipline] = [
        Discipline(
            code: "POR", fullCode: "LET101", title: "Português Instrumental",
            dept: "Letras e Artes", prof: "Marina Costa", color: rust,
            hours: 60, absences: 3, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "18/03/2024", score: 7.5),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "22/04/2024", score: 8.0),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "10/06/2024", score: 8.5),
            ])],
            finalGrade: 8.0
        ),
        Discipline(
            code: "SOC", fullCode: "HUM201", title: "Sociologia",
            dept: "Humanidades", prof: "Ricardo Braga", color: violet,
            hours: 45, absences: 1, allowedAbsences: 11,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Prova 1", date: "25/03/2024", score: 9.0),
                GradeEntry(label: "AV2", title: "Prova 2", date: "29/04/2024", score: 8.5),
                GradeEntry(label: "AV3", title: "Prova 3", date: "17/06/2024", score: 9.2),
            ])],
            finalGrade: 8.9
        ),
        Discipline(
            code: "LOG", fullCode: "EXA099", title: "Lógica Básica",
            dept: "Ciências Exatas", prof: "Camila Ribeiro", color: UNESColor.coral,
            hours: 60, absences: 6, allowedAbsences: 15,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Avaliação 1", date: "20/03/2024", score: 6.5),
                GradeEntry(label: "AV2", title: "Avaliação 2", date: "24/04/2024", score: 7.0),
                GradeEntry(label: "AV3", title: "Avaliação 3", date: "12/06/2024", score: 7.3),
            ])],
            finalGrade: 6.9
        ),
        Discipline(
            code: "EDF", fullCode: "SAU101", title: "Educação Física",
            dept: "Saúde", prof: "Rogério Lima", color: teal,
            hours: 30, absences: 2, allowedAbsences: 8,
            sections: [GradeSection(name: "Teórica-Prática", grades: [
                GradeEntry(label: "FR1", title: "Frequência", date: nil, score: 9.5),
            ])],
            finalGrade: 9.5
        ),
        Discipline(
            code: "HIS", fullCode: "HUM311", title: "História do Brasil",
            dept: "Humanidades", prof: "Laura Carvalho", color: UNESColor.amber,
            hours: 45, absences: 0, allowedAbsences: 11,
            sections: [GradeSection(name: "Teórica", grades: [
                GradeEntry(label: "AV1", title: "Prova 1", date: "27/03/2024", score: 7.8),
                GradeEntry(label: "AV2", title: "Prova 2", date: "01/05/2024", score: 8.5),
                GradeEntry(label: "AV3", title: "Prova 3", date: "14/06/2024", score: 8.0),
            ])],
            finalGrade: 8.1
        ),
    ]
}
