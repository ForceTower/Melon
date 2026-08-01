import Foundation

/// Mocked catalogue mirroring the real Pergamum record shapes — dirty title
/// strings, text-typed years, missing fields, and the degraded "novas
/// aquisições" listing. Serves the feature until the backend lands.
enum LibraryFixtures {
    /// Due dates are laid out relative to `now` so the mock always shows a
    /// mix of upcoming returns and stale never-closed loan records.
    static func works(now: Date) -> [LibraryWork] {
        let due = { (days: Int) in Calendar.current.date(byAdding: .day, value: days, to: now) ?? now }
        let staleDue = { (year: Int) in
            Calendar.current.date(from: DateComponents(year: year, month: 3, day: 14)) ?? now
        }

        return [
            LibraryWork(
                id: "4278",
                rawTitle: "Cálculo : um novo horizonte - 6. ed / 0000",
                callNumber: "515 A638c",
                type: .book,
                rawYear: "2000",
                authors: ["Anton, Howard", "Bivens, Irl", "Davis, Stephen"],
                subjects: ["Cálculo", "Matemática", "Geometria analítica"],
                branches: [.central],
                rawISBN: "85-7307-655-3: (Broch.)",
                language: "Português",
                volumes: "2 v.",
                reference: "ANTON, Howard; BIVENS, Irl; DAVIS, Stephen. **Cálculo:** um novo horizonte. 6. ed. Porto Alegre: Bookman, 2000. 2 v.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "515 A638c"),
                    LibraryRecordField(label: "Autor Principal", value: "Anton, Howard"),
                    LibraryRecordField(label: "Título Principal", value: "Cálculo : um novo horizonte"),
                    LibraryRecordField(label: "Edição", value: "6. ed"),
                    LibraryRecordField(label: "Publicação", value: "Porto Alegre, RS | Bookman | 2000"),
                    LibraryRecordField(label: "Descrição Física", value: "2 v. : il."),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 8573076553"),
                    LibraryRecordField(label: "Assuntos", value: "Cálculo | Matemática | Geometria analítica"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "515 A638c 6. ed v. 1",
                               available: 51, loans: [due(13)], missing: 14)
                    + copies(.central, area: "Coleção Geral", call: "515 A638c 6. ed v. 2",
                             available: 47, loans: [staleDue(2011)], missing: 8)
            ),
            LibraryWork(
                id: "10233",
                rawTitle: "Um curso de cálculo, v. 1 - 5. ed / 2001",
                callNumber: "515 G981c",
                type: .book,
                rawYear: "2001",
                authors: ["Guidorizzi, Hamilton Luiz"],
                subjects: ["Cálculo", "Cálculo diferencial", "Análise matemática"],
                branches: [.central, .health],
                rawISBN: "85-216-1258-9",
                language: "Português",
                reference: "GUIDORIZZI, Hamilton Luiz. **Um curso de cálculo.** 5. ed. Rio de Janeiro: LTC, 2001. v. 1.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "515 G981c"),
                    LibraryRecordField(label: "Autor Principal", value: "Guidorizzi, Hamilton Luiz"),
                    LibraryRecordField(label: "Título Principal", value: "Um curso de cálculo"),
                    LibraryRecordField(label: "Edição", value: "5. ed"),
                    LibraryRecordField(label: "Publicação", value: "Rio de Janeiro, RJ | LTC | 2001"),
                    LibraryRecordField(label: "Descrição Física", value: "4 v."),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 8521612589"),
                    LibraryRecordField(label: "Assuntos", value: "Cálculo | Cálculo diferencial | Análise matemática"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC | BSS"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "515 G981c 5. ed v. 1",
                               available: 2,
                               loans: [due(2), due(6), due(21), due(8), staleDue(2002), due(27)],
                               missing: 2)
                    + copies(.health, area: "Coleção Geral", call: "515 G981c 5. ed v. 1", available: 1)
            ),
            LibraryWork(
                id: "8891",
                rawTitle: "O cálculo com geometria analítica / 0000",
                callNumber: "515 L557c",
                type: .book,
                rawYear: "1994",
                authors: ["Leithold, Louis"],
                subjects: ["Cálculo", "Geometria analítica"],
                branches: [.central],
                language: "Português",
                reference: "LEITHOLD, Louis. **O cálculo com geometria analítica.** 3. ed. São Paulo: Harbra, 1994.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "515 L557c"),
                    LibraryRecordField(label: "Autor Principal", value: "Leithold, Louis"),
                    LibraryRecordField(label: "Título Principal", value: "O cálculo com geometria analítica"),
                    LibraryRecordField(label: "Publicação", value: "São Paulo, SP | Harbra | 1994"),
                    LibraryRecordField(label: "Descrição Física", value: "2 v."),
                    LibraryRecordField(label: "Assuntos", value: "Cálculo | Geometria analítica"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "515 L557c v. 1",
                               loans: [due(5), due(10), due(15), due(33), staleDue(2020), due(16)],
                               missing: 2)
            ),
            LibraryWork(
                id: "15720",
                rawTitle: "Cálculo numérico : aspectos teóricos e computacionais - 2. ed / 1996",
                callNumber: "519.4 R921c",
                type: .book,
                rawYear: "1996",
                authors: ["Ruggiero, Márcia A. Gomes", "Lopes, Vera Lúcia da Rocha"],
                subjects: ["Cálculo numérico", "Análise numérica", "Computação"],
                branches: [.central],
                rawISBN: "85-346-0204-2: (Broch.)",
                language: "Português",
                collection: "Coleção Ciência da Computação",
                reference: "RUGGIERO, Márcia A. Gomes; LOPES, Vera Lúcia da Rocha. **Cálculo numérico:** aspectos teóricos e computacionais. 2. ed. São Paulo: Makron Books, 1996.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "519.4 R921c"),
                    LibraryRecordField(label: "Autor Principal", value: "Ruggiero, Márcia A. Gomes"),
                    LibraryRecordField(label: "Título Principal", value: "Cálculo numérico : aspectos teóricos e computacionais"),
                    LibraryRecordField(label: "Edição", value: "2. ed"),
                    LibraryRecordField(label: "Publicação", value: "São Paulo, SP | Makron Books | 1996"),
                    LibraryRecordField(label: "Descrição Física", value: "406 p. : il."),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 8534602042"),
                    LibraryRecordField(label: "Coleção", value: "Coleção Ciência da Computação"),
                    LibraryRecordField(label: "Assuntos", value: "Cálculo numérico | Análise numérica | Computação"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "519.4 R921c 2. ed",
                               available: 4, loans: [due(9), due(23)], missing: 1)
            ),
            LibraryWork(
                id: "33455",
                rawTitle: "Cálculo A : funções, limite, derivação, integração - 6. ed. rev. e ampl. / 2006",
                callNumber: "515 F598c",
                type: .book,
                rawYear: "2006",
                authors: ["Flemming, Diva Marília", "Gonçalves, Mírian Buss"],
                subjects: ["Cálculo", "Funções (Matemática)", "Integrais"],
                branches: [.central, .lencois],
                rawISBN: "978-85-7605-115-1",
                language: "Português",
                reference: "FLEMMING, Diva Marília; GONÇALVES, Mírian Buss. **Cálculo A:** funções, limite, derivação, integração. 6. ed. rev. e ampl. São Paulo: Pearson, 2006.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "515 F598c"),
                    LibraryRecordField(label: "Autor Principal", value: "Flemming, Diva Marília"),
                    LibraryRecordField(label: "Título Principal", value: "Cálculo A : funções, limite, derivação, integração"),
                    LibraryRecordField(label: "Edição", value: "6. ed. rev. e ampl."),
                    LibraryRecordField(label: "Publicação", value: "São Paulo, SP | Pearson Prentice Hall | 2006"),
                    LibraryRecordField(label: "Descrição Física", value: "448 p. : il."),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 9788576051151"),
                    LibraryRecordField(label: "Assuntos", value: "Cálculo | Funções (Matemática) | Integrais"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC | BAL"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "515 F598c 6. ed",
                               available: 11, loans: [due(4), due(20)], missing: 3)
                    + copies(.lencois, area: "Coleção Geral", call: "515 F598c 6. ed", available: 1)
            ),
            LibraryWork(
                id: "22104",
                rawTitle: "Cálculo diferencial e integral de funções de várias variáveis / 0000",
                callNumber: "515.84 G588c",
                type: .book,
                rawYear: "m",
                authors: ["Gonçalves, Mírian Buss", "Flemming, Diva Marília"],
                subjects: ["Cálculo diferencial", "Cálculo integral", "Funções de várias variáveis"],
                branches: [.central],
                rawISBN: "(Broch.)",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "515.84 G588c"),
                    LibraryRecordField(label: "Autor Principal", value: "Gonçalves, Mírian Buss"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "Cálculo diferencial e integral de funções de várias variáveis"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Florianópolis, SC | UFSC"),
                    LibraryRecordField(
                        label: "Assuntos",
                        value: "Cálculo diferencial | Cálculo integral | Funções de várias variáveis"
                    ),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "515.84 G588c",
                               available: 1, loans: [due(12), due(29)], missing: 1)
            ),
            LibraryWork(
                id: "41266",
                rawTitle: "O ensino de cálculo diferencial e integral mediado por tecnologias digitais : um estudo com licenciandos / 2019",
                callNumber: "T 515.07 S719e",
                type: .dissertation,
                rawYear: "2019",
                authors: ["Souza, Ana Paula Ribeiro de"],
                subjects: ["Cálculo — Estudo e ensino", "Educação matemática", "Tecnologia educacional"],
                branches: [.central],
                language: "Português",
                series: "Programa de Pós-Graduação em Educação — UEFS",
                reference: "SOUZA, Ana Paula Ribeiro de. **O ensino de cálculo diferencial e integral mediado por tecnologias digitais:** um estudo com licenciandos. 2019. Dissertação (Mestrado em Educação) — Universidade Estadual de Feira de Santana, Feira de Santana, 2019.",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "T 515.07 S719e"),
                    LibraryRecordField(label: "Autor Principal", value: "Souza, Ana Paula Ribeiro de"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "O ensino de cálculo diferencial e integral mediado por tecnologias digitais"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Feira de Santana, BA | UEFS | 2019"),
                    LibraryRecordField(label: "Descrição Física", value: "142 f. : il."),
                    LibraryRecordField(label: "Nota de Tese", value: "Dissertação (Mestrado em Educação) — UEFS, 2019"),
                    LibraryRecordField(
                        label: "Assuntos",
                        value: "Cálculo — Estudo e ensino | Educação matemática | Tecnologia educacional"
                    ),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Teses e Dissertações", call: "T 515.07 S719e",
                               localUse: 1, localUseNote: "Consulta local — não empresta")
            ),
            LibraryWork(
                id: "30871",
                rawTitle: "Cálculo e detalhamento de estruturas usuais de concreto armado / 0000",
                callNumber: "624.183 C337c",
                type: .pamphlet,
                rawYear: "1998",
                authors: ["Carvalho, Roberto Chust"],
                subjects: ["Concreto armado", "Estruturas — Cálculo"],
                branches: [.lencois],
                language: "Português",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "624.183 C337c"),
                    LibraryRecordField(label: "Autor Principal", value: "Carvalho, Roberto Chust"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "Cálculo e detalhamento de estruturas usuais de concreto armado"
                    ),
                    LibraryRecordField(label: "Publicação", value: "São Carlos, SP | EdUFSCar | 1998"),
                    LibraryRecordField(label: "Assuntos", value: "Concreto armado | Estruturas — Cálculo"),
                    LibraryRecordField(label: "Biblioteca", value: "BAL"),
                ],
                copies: copies(.lencois, area: "Coleção Geral", call: "624.183 C337c", available: 2)
            ),
            LibraryWork(
                id: "5502",
                rawTitle: "O cálculo do vaqueiro : peleja em versos de cordel / 0000",
                callNumber: "C 869.1 M539c",
                type: .cordel,
                rawYear: "1987",
                authors: ["Melo, José Costa de"],
                subjects: ["Literatura de cordel", "Poesia popular — Bahia"],
                branches: [.central],
                language: "Português",
                collection: "Coleção Cordel — Acervo Regional",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "C 869.1 M539c"),
                    LibraryRecordField(label: "Autor Principal", value: "Melo, José Costa de"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "O cálculo do vaqueiro : peleja em versos de cordel"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Feira de Santana, BA | [s.n.] | 1987"),
                    LibraryRecordField(label: "Descrição Física", value: "8 p."),
                    LibraryRecordField(label: "Coleção", value: "Coleção Cordel — Acervo Regional"),
                    LibraryRecordField(label: "Assuntos", value: "Literatura de cordel | Poesia popular — Bahia"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Cordel", call: "C 869.1 M539c",
                               localUse: 2, localUseNote: "Consulta local — acervo especial")
            ),
            LibraryWork(
                id: "19003",
                rawTitle: "Cálculo do índice de área foliar em caatinga preservada / 2014",
                callNumber: "P 581.7 A538c",
                type: .article,
                rawYear: "2014",
                authors: ["Almeida, Cláudia Rejane de", "Nascimento, Luciano Barbosa"],
                subjects: ["Caatinga", "Sensoriamento remoto"],
                branches: [.central],
                rawISBN: "1809-4457",
                language: "Português",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "P 581.7 A538c"),
                    LibraryRecordField(label: "Autor Principal", value: "Almeida, Cláudia Rejane de"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "Cálculo do índice de área foliar em caatinga preservada"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Feira de Santana, BA | Sitientibus | 2014"),
                    LibraryRecordField(label: "Número Normalizado", value: "ISSN 1809-4457"),
                    LibraryRecordField(label: "Assuntos", value: "Caatinga | Sensoriamento remoto"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Periódicos", call: "P 581.7 A538c",
                               localUse: 1, localUseNote: "Consulta local — hemeroteca")
            ),
        ]
    }

    /// The "novas no acervo" listing — 23 fields instead of the full record:
    /// a single author string, no citation, no copy counts up front.
    static func newAcquisitions(now: Date) -> [LibraryWork] {
        let due = { (days: Int) in Calendar.current.date(byAdding: .day, value: days, to: now) ?? now }
        return [
            LibraryWork(
                id: "52310",
                rawTitle: "Como produzir textos acadêmicos e científicos / 2026",
                callNumber: "001.8 B823c",
                type: .book,
                rawYear: "2026",
                authors: ["Brasileiro, Ada Magaly Matias"],
                subjects: ["Pesquisa — Metodologia", "Redação acadêmica"],
                branches: [.central],
                rawISBN: "9786555410051",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "001.8 B823c"),
                    LibraryRecordField(label: "Autor Principal", value: "Brasileiro, Ada Magaly Matias"),
                    LibraryRecordField(label: "Título Principal", value: "Como produzir textos acadêmicos e científicos"),
                    LibraryRecordField(label: "Publicação", value: "São Paulo, SP | Contexto | 2026"),
                    LibraryRecordField(label: "Descrição Física", value: "271 p."),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 9786555410051"),
                    LibraryRecordField(label: "Assuntos", value: "Pesquisa — Metodologia | Redação acadêmica"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Geral", call: "001.8 B823c", available: 3),
                isNewAcquisition: true
            ),
            LibraryWork(
                id: "52288",
                rawTitle: "A seca de 1932 em versos : cordel do sertão baiano / 0000",
                callNumber: "C 869.1 S237s",
                type: .cordel,
                rawYear: "2025",
                authors: ["Santos, Zé do Sertão"],
                subjects: ["Literatura de cordel", "Seca — Bahia"],
                branches: [.central],
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "C 869.1 S237s"),
                    LibraryRecordField(label: "Autor Principal", value: "Santos, Zé do Sertão"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "A seca de 1932 em versos : cordel do sertão baiano"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Feira de Santana, BA | [s.n.] | 2025"),
                    LibraryRecordField(label: "Assuntos", value: "Literatura de cordel | Seca — Bahia"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Cordel", call: "C 869.1 S237s",
                               localUse: 1, localUseNote: "Consulta local"),
                isNewAcquisition: true
            ),
            LibraryWork(
                id: "52401",
                rawTitle: "Sequência didática para o ensino de genética no ensino médio / 2025",
                callNumber: "PE 576.5 O48s",
                type: .educationalProduct,
                rawYear: "2025",
                authors: ["Oliveira, Tiago Ramos de"],
                subjects: ["Genética — Estudo e ensino", "Ensino médio"],
                branches: [.central],
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "PE 576.5 O48s"),
                    LibraryRecordField(label: "Autor Principal", value: "Oliveira, Tiago Ramos de"),
                    LibraryRecordField(
                        label: "Título Principal",
                        value: "Sequência didática para o ensino de genética no ensino médio"
                    ),
                    LibraryRecordField(label: "Publicação", value: "Feira de Santana, BA | UEFS | 2025"),
                    LibraryRecordField(label: "Assuntos", value: "Genética — Estudo e ensino | Ensino médio"),
                    LibraryRecordField(label: "Biblioteca", value: "BCJC"),
                ],
                copies: copies(.central, area: "Coleção Teses e Dissertações", call: "PE 576.5 O48s", available: 1),
                isNewAcquisition: true
            ),
            LibraryWork(
                id: "52377",
                rawTitle: "Semiologia médica - 8. ed / 2025",
                callNumber: "616.075 P816s",
                type: .book,
                rawYear: "2025",
                authors: ["Porto, Celmo Celeno"],
                subjects: ["Semiologia", "Diagnóstico clínico"],
                branches: [.health],
                rawISBN: "9788527738224",
                record: [
                    LibraryRecordField(label: "Número de Chamada", value: "616.075 P816s"),
                    LibraryRecordField(label: "Autor Principal", value: "Porto, Celmo Celeno"),
                    LibraryRecordField(label: "Título Principal", value: "Semiologia médica"),
                    LibraryRecordField(label: "Edição", value: "8. ed"),
                    LibraryRecordField(label: "Publicação", value: "Rio de Janeiro, RJ | Guanabara Koogan | 2025"),
                    LibraryRecordField(label: "Número Normalizado", value: "ISBN 9788527738224"),
                    LibraryRecordField(label: "Assuntos", value: "Semiologia | Diagnóstico clínico"),
                    LibraryRecordField(label: "Biblioteca", value: "BSS"),
                ],
                copies: copies(.health, area: "Coleção Geral", call: "616.075 P816s 8. ed",
                               available: 2, loans: [due(7)]),
                isNewAcquisition: true
            ),
        ]
    }

    static func all(now: Date) -> [LibraryWork] {
        works(now: now) + newAcquisitions(now: now)
    }

    private static func copies(
        _ branch: LibraryBranch,
        area: String,
        call: String,
        available: Int = 0,
        loans: [Date] = [],
        missing: Int = 0,
        localUse: Int = 0,
        localUseNote: String = "Consulta local"
    ) -> [LibraryCopy] {
        var out: [LibraryCopy] = []
        let push = { (status: LibraryCopyStatus) in
            out.append(LibraryCopy(branch: branch, area: area, callNumber: call, status: status))
        }
        for _ in 0..<available { push(.available) }
        for dueDate in loans { push(.onLoan(due: dueDate)) }
        for _ in 0..<missing { push(.missing) }
        for _ in 0..<localUse { push(.localUse(note: localUseNote)) }
        return out
    }
}
