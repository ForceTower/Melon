import Foundation

// MARK: - Campus event — integration weeks and extraordinary course events

/// Where the event sits relative to now. Drives the Home card, the welcome
/// reveal and the hub hero.
enum CampusEventPhase: Equatable, Sendable {
    case upcoming, live, ended
}

/// Where one activity sits relative to now.
enum CampusEventActivityState: Equatable, Sendable {
    case upcoming, live, past
}

/// The activity taxonomy. Unknown server kinds land on `.other` so newer
/// categories degrade to a generic activity instead of dropping rows.
enum CampusEventCategory: String, Equatable, Sendable, Codable {
    case quest, workshop, lecture, presentation, groupDynamic = "dynamic", other
}

/// Who an activity is aimed at. Events that don't split their audience send
/// `everyone` for every activity, which hides the filter entirely.
enum CampusEventAudience: String, Equatable, Sendable, CaseIterable, Codable {
    case everyone, freshmen, veterans

    /// Whether an activity aimed at `audience` shows under this filter.
    func includes(_ audience: CampusEventAudience) -> Bool {
        self == .everyone || audience == .everyone || audience == self
    }
}

struct CampusEventActivity: Equatable, Sendable, Identifiable, Codable {
    var id: String
    var title: String
    var details: String?
    var category: CampusEventCategory
    var audience: CampusEventAudience
    /// Link into `venues` when the room is a real venue; the display name
    /// stands alone for ad-hoc rooms ("Google Meet").
    var venueId: String? = nil
    var venueName: String
    /// Links into `speakers`; `speakerNames` stays the display source so
    /// ad-hoc hosts ("DAECOMP") need no speaker entry.
    var speakerIds: [String] = []
    var speakerNames: [String]
    var startsAt: Date
    var endsAt: Date?
    var requiresSignup: Bool

    /// Open-ended activities count as live for a default stretch so they
    /// leave the "happening now" spot on their own.
    var effectiveEnd: Date {
        endsAt ?? startsAt.addingTimeInterval(90 * 60)
    }

    func state(now: Date) -> CampusEventActivityState {
        if now >= effectiveEnd { return .past }
        if now >= startsAt { return .live }
        return .upcoming
    }
}

struct CampusEventSpeaker: Equatable, Sendable, Identifiable, Codable {
    var id: String
    var name: String
    var role: String?
    var organization: String?
    var bio: String?
    /// Server-authored badge copy ("Docente", "Convidada", …).
    var tag: String?
}

struct CampusEventWorkshop: Equatable, Sendable, Identifiable, Codable {
    var id: String
    var title: String
    var details: String?
    var audience: CampusEventAudience
    var venueName: String?
    var instructors: String?
    var requiresSignup: Bool
    var slots: Int?
}

struct CampusEventVenue: Equatable, Sendable, Identifiable, Codable {
    var id: String
    var name: String
    var shortName: String?
    /// One-line hint of what happens there ("Palestras e apresentações").
    var hint: String?
    /// Schematic-map position, 0–100 on both axes. Venues without
    /// coordinates keep the map hidden and render list-only.
    var mapX: Double?
    var mapY: Double?

    var displayShortName: String { shortName ?? name }
}

struct CampusEventOrganization: Equatable, Sendable, Identifiable, Codable {
    var id: String
    var name: String
    var fullName: String?
    /// Server-authored badge copy ("Organizador", "Liga", …).
    var tag: String?
    var details: String?
}

/// One calendar day of the schedule, derived from activity dates.
struct CampusEventDay: Equatable, Sendable, Identifiable {
    var date: Date
    var activities: [CampusEventActivity]

    var id: Date { date }
}

/// A special event a course runs through the app — an integration week, an
/// academic week, any extraordinary multi-day happening. The whole payload
/// arrives in one fetch so every screen (and offline access) works from it.
struct CampusEvent: Equatable, Sendable, Identifiable, Codable {
    var id: String
    /// Bumped by the server on every publish; the mirror skips identical
    /// revisions so observers only wake for real changes.
    var revision: Int = 1
    /// Short display name ("SIECOMP").
    var name: String
    /// Edition marker rendered above the name ("XXXIII").
    var edition: String?
    /// Long descriptive line ("Semana de Integração dos Estudantes…").
    var tagline: String?
    /// Welcome-screen eyebrow ("UEFS · Eng. de Computação").
    var institution: String?
    /// Footer credit ("Uma realização do DAECOMP · Gestão Bittencourt").
    var credit: String?
    /// IANA identifier of the campus zone ("America/Bahia"). Days and times
    /// render in it so the schedule doesn't shift for a traveling student.
    var timeZoneIdentifier: String? = nil
    var startsAt: Date
    var endsAt: Date
    var activities: [CampusEventActivity]
    var speakers: [CampusEventSpeaker]
    var workshops: [CampusEventWorkshop]
    var venues: [CampusEventVenue]
    var organizations: [CampusEventOrganization]

    var timeZone: TimeZone {
        timeZoneIdentifier.flatMap(TimeZone.init(identifier:)) ?? .autoupdatingCurrent
    }

    /// The calendar every schedule derivation runs on — event zone, not
    /// device zone.
    var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        return calendar
    }

    func phase(now: Date) -> CampusEventPhase {
        if now >= endsAt { return .ended }
        if now >= startsAt { return .live }
        return .upcoming
    }
}

// MARK: - Derived schedule

extension CampusEvent {
    /// Activities grouped into calendar days (event zone), both levels
    /// sorted by time.
    func days() -> [CampusEventDay] {
        let calendar = calendar
        return Dictionary(grouping: activities) { calendar.startOfDay(for: $0.startsAt) }
            .map { CampusEventDay(date: $0.key, activities: $0.value.sorted { $0.startsAt < $1.startsAt }) }
            .sorted { $0.date < $1.date }
    }

    /// The day the schedule should open on: today while live, the first day
    /// before the event, the last one after.
    func currentDayDate(now: Date) -> Date? {
        let days = days()
        switch phase(now: now) {
        case .upcoming: return days.first?.date
        case .ended: return days.last?.date
        case .live:
            let today = calendar.startOfDay(for: now)
            return days.first { $0.date >= today }?.date ?? days.last?.date
        }
    }

    /// 1-based "Dia N de M" index while the event runs.
    func dayNumber(now: Date) -> Int {
        let today = calendar.startOfDay(for: now)
        guard let index = days().lastIndex(where: { $0.date <= today }) else { return 1 }
        return index + 1
    }

    var dayCount: Int { days().count }

    /// Whether `date` falls on `dayDate`'s calendar day in the event zone.
    func isDate(_ date: Date, onDay dayDate: Date) -> Bool {
        calendar.isDate(date, inSameDayAs: dayDate)
    }

    /// The activity opening the event — the hero's "Abertura" block.
    var opener: CampusEventActivity? {
        activities.min { $0.startsAt < $1.startsAt }
    }

    func activities(on dayDate: Date, matching filter: CampusEventAudience) -> [CampusEventActivity] {
        days()
            .first { $0.date == dayDate }?
            .activities
            .filter { filter.includes($0.audience) } ?? []
    }

    func activityCount(on dayDate: Date, matching filter: CampusEventAudience) -> Int {
        activities(on: dayDate, matching: filter).count
    }

    /// Whether the schedule splits by audience at all; a single-audience
    /// event renders without the filter.
    var hasAudienceSplit: Bool {
        activities.contains { $0.audience != .everyone }
    }

    func liveActivityCount(matching filter: CampusEventAudience, now: Date) -> Int {
        activities.count { filter.includes($0.audience) && $0.state(now: now) == .live }
    }

    /// The activity the live hero spotlights: the one running now, else the
    /// next to start, else the closing one.
    func liveOrNextActivity(matching filter: CampusEventAudience, now: Date) -> (activity: CampusEventActivity, isLive: Bool)? {
        let matching = activities
            .filter { filter.includes($0.audience) }
            .sorted { $0.startsAt < $1.startsAt }
        if let live = matching.first(where: { $0.state(now: now) == .live }) {
            return (live, true)
        }
        if let next = matching.first(where: { now < $0.startsAt }) {
            return (next, false)
        }
        return matching.last.map { ($0, false) }
    }

    /// Resolves one of an activity's hosts: by id when linked, by display
    /// name otherwise (ad-hoc hosts have neither and return nil).
    func speaker(for activity: CampusEventActivity, at index: Int) -> CampusEventSpeaker? {
        if index < activity.speakerIds.count,
           let linked = speakers.first(where: { $0.id == activity.speakerIds[index] }) {
            return linked
        }
        guard index < activity.speakerNames.count else { return nil }
        return speakers.first { $0.name == activity.speakerNames[index] }
    }

    /// Resolves an activity's venue: by id when linked, by display name
    /// otherwise (ad-hoc rooms like "Google Meet" return nil).
    func venue(for activity: CampusEventActivity) -> CampusEventVenue? {
        if let id = activity.venueId, let linked = venues.first(where: { $0.id == id }) {
            return linked
        }
        return venues.first { $0.name == activity.venueName }
    }

    func activityCount(at venue: CampusEventVenue) -> Int {
        activities.count { $0.venueName == venue.name }
    }
}

// MARK: - Previews

extension CampusEvent {
    /// A SIECOMP-shaped fixture with dates pinned around `now` so previews
    /// can render each phase live.
    static func preview(now: Date = .now, phase: CampusEventPhase = .upcoming) -> CampusEvent {
        let calendar = Calendar.autoupdatingCurrent
        let dayOffset = switch phase {
        case .upcoming: 2
        case .live: -2
        case .ended: -9
        }
        let firstDay = calendar.date(byAdding: .day, value: dayOffset, to: calendar.startOfDay(for: now))!
        func at(_ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
            calendar.date(byAdding: DateComponents(day: day, hour: hour, minute: minute), to: firstDay)!
        }

        let activities = [
            CampusEventActivity(
                id: "s1", title: "Quest: Dinâmica com o DA",
                details: "O DA preparou uma dinâmica interativa para dar as boas-vindas aos calouros no curso! Para fazer o cadastro no bandejão, chegue no horário.",
                category: .quest, audience: .freshmen, venueName: "Praça do Borogodó",
                speakerNames: ["DAECOMP"], startsAt: at(0, 8), endsAt: at(0, 12), requiresSignup: false
            ),
            CampusEventActivity(
                id: "s2", title: "Introdução à metodologia PBL — 1º dia",
                details: "Introduz os alunos à metodologia PBL através de uma apresentação e um mini-projeto prático em grupos.",
                category: .workshop, audience: .freshmen, venueName: "Auditório da Biblioteca",
                speakerNames: ["Claudia P. Pereira", "Cláudio Sérgio"], startsAt: at(0, 13, 30), endsAt: at(0, 16), requiresSignup: false
            ),
            CampusEventActivity(
                id: "s3", title: "Black XP — Impulsionando Talentos Dissidentes na Tecnologia",
                details: "O Black XP é uma iniciativa voltada à formação tecnológica em games, com foco na inserção de jovens negros, indígenas e mulheres no mercado de tecnologia.",
                category: .presentation, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["Taís Rocha Ribeiro"], startsAt: at(0, 16), endsAt: at(0, 17), requiresSignup: false
            ),
            CampusEventActivity(
                id: "s4", title: "Comissão de Avaliação do Curso",
                details: "A Comissão Permanente de Avaliação compartilhará os resultados do semestre anterior.",
                category: .presentation, audience: .veterans, venueName: "Google Meet",
                speakerNames: ["João B. Rocha"], startsAt: at(0, 19), endsAt: nil, requiresSignup: false
            ),
            CampusEventActivity(
                id: "t1", title: "Introdução a Circuitos Digitais (Turma 1) — 1º dia",
                details: "Um pontapé inicial para os alunos que pegam o MI de Circuitos Digitais, reduzindo a desistência e o gargalo histórico da matéria.",
                category: .workshop, audience: .veterans, venueName: "MP 32 (LEI)",
                speakerNames: ["Paulo Queiroz de Carvalho"], startsAt: at(1, 8), endsAt: at(1, 11), requiresSignup: true
            ),
            CampusEventActivity(
                id: "t4", title: "Integração, colaboração e aprendizagem",
                details: "A dinâmica promove o conhecimento e a integração do grupo, refletindo sobre a importância da boa comunicação no trabalho em equipe.",
                category: .groupDynamic, audience: .freshmen, venueName: "PAT 30 — Módulo 3",
                speakerNames: ["Rosaria Trindade"], startsAt: at(1, 10), endsAt: at(1, 12), requiresSignup: false
            ),
            CampusEventActivity(
                id: "t5", title: "Introdução à programação para jogos",
                details: "Fundamentos de Game Design e prática na engine Godot: ao final, cada participante desenvolve seu primeiro mini-jogo 2D.",
                category: .workshop, audience: .veterans, venueName: "LESS (i11) — LABOTEC III",
                speakerNames: ["Liga de Jogos"], startsAt: at(1, 13, 30), endsAt: at(1, 15, 30), requiresSignup: true
            ),
            CampusEventActivity(
                id: "t7", title: "Conhecendo o DAECOMP e o DCE",
                details: "Apresentação das duas entidades estudantis mais importantes para o curso.",
                category: .presentation, audience: .freshmen, venueName: "Auditório da Biblioteca",
                speakerNames: ["DAECOMP", "DCE"], startsAt: at(1, 15, 30), endsAt: at(1, 17, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "q1", title: "Direito x IA — Aspectos cíveis e penais",
                details: "Os impactos da inteligência artificial no campo jurídico: desafios éticos e caminhos para uma regulação mais eficaz na era digital.",
                category: .lecture, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["Anderson Lopes"], startsAt: at(2, 8), endsAt: at(2, 9, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "q4", title: "Guia para o Intercâmbio — Do Sonho à Realidade",
                details: "Orientações para realizar um intercâmbio pela AERI: inscrição, processo seletivo e dicas para escolher a universidade ideal.",
                category: .lecture, audience: .veterans, venueName: "Auditório da Biblioteca",
                speakerNames: ["Douglas Oliveira"], startsAt: at(2, 11), endsAt: at(2, 12), requiresSignup: false
            ),
            CampusEventActivity(
                id: "q5", title: "AERI — Mobilidade Estudantil",
                details: "Participação especial da Assessoria Especial de Relações Institucionais, que fomenta a cooperação entre a UEFS e demais instituições.",
                category: .lecture, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["AERI"], startsAt: at(2, 13, 30), endsAt: at(2, 15, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "q8", title: "Dinâmica: Atlética Ecomp",
                details: "Momento de apresentação da atlética e dinâmica de integração esportiva.",
                category: .groupDynamic, audience: .freshmen, venueName: "MP 32 (LEI)",
                speakerNames: ["Atlética Ecomp"], startsAt: at(2, 15, 30), endsAt: at(2, 17, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "u1", title: "A importância de cursos extracurriculares",
                details: "O que vem depois de se tornar engenheiro(a)? Um papo sobre oportunidades, desafios e como cursos extracurriculares abrem portas.",
                category: .lecture, audience: .freshmen, venueName: "Auditório da Biblioteca",
                speakerNames: ["Pâmela M. C. Cortez"], startsAt: at(3, 8), endsAt: at(3, 9, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "u6", title: "EcompJr — seu primeiro contato com o mercado",
                details: "Apresenta a Empresa Júnior do curso: estrutura, serviços, cargos e processo de ingresso.",
                category: .lecture, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["Cláudio Daniel Peruna"], startsAt: at(3, 13, 30), endsAt: at(3, 14, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "u8", title: "IA na Programação Introdutória: Positivo ou Negativo?",
                details: "Desde o ChatGPT, as ferramentas de IA Generativa são amplamente usadas — inclusive na educação. Que impactos isso traz para quem está aprendendo a programar?",
                category: .lecture, audience: .freshmen, venueName: "Auditório da Biblioteca",
                speakerNames: ["Diego do Carmo", "Claudia P. Pereira"], startsAt: at(3, 16), endsAt: at(3, 17, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "x2", title: "Do Zero ao Código de Qualidade: Boas Práticas",
                details: "As principais boas práticas de programação para escrever códigos mais limpos, robustos e fáceis de manter.",
                category: .lecture, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["Francisco Pereira"], startsAt: at(4, 8, 30), endsAt: at(4, 9, 30), requiresSignup: false
            ),
            CampusEventActivity(
                id: "x3", title: "Introdução à metodologia PBL — encontro final",
                details: "Encontro final da oficina de PBL, com a apresentação dos resultados dos mini-projetos.",
                category: .workshop, audience: .freshmen, venueName: "Auditório da Biblioteca",
                speakerNames: ["Claudia P. Pereira", "Cláudio Sérgio"], startsAt: at(4, 9, 30), endsAt: at(4, 12), requiresSignup: false
            ),
            CampusEventActivity(
                id: "x6", title: "Desenvolvimento de Software — Como funciona no mundo real?",
                details: "Experiência e conhecimento de mercado a partir das dúvidas que um estudante costuma ter — com espaço para perguntas ao vivo.",
                category: .lecture, audience: .everyone, venueName: "Auditório da Biblioteca",
                speakerNames: ["Matheus Oliveira Borges"], startsAt: at(4, 15, 30), endsAt: at(4, 17, 30), requiresSignup: false
            ),
        ]

        return CampusEvent(
            id: "siecomp-33",
            name: "SIECOMP",
            edition: "XXXIII",
            tagline: "Semana de Integração dos Estudantes do curso de Engenharia de Computação.",
            institution: "UEFS · Eng. de Computação",
            credit: "Uma realização do DAECOMP · Gestão Bittencourt",
            timeZoneIdentifier: TimeZone.autoupdatingCurrent.identifier,
            startsAt: activities.first!.startsAt,
            endsAt: activities.last!.effectiveEnd,
            activities: activities,
            speakers: [
                CampusEventSpeaker(
                    id: "sp1", name: "João B. Rocha", role: "Coordenador do curso", organization: "UEFS · PGCC",
                    bio: "Doutor em Ciências da Computação pela NTNU e professor na UEFS desde 2004. Coordenador do curso, pesquisa big data, análise de dados e sistemas distribuídos.",
                    tag: "Docente"
                ),
                CampusEventSpeaker(
                    id: "sp2", name: "Claudia P. Pereira", role: "Professora e pesquisadora", organization: "UEFS · Mestrado em CC",
                    bio: "Doutora em Difusão do Conhecimento (UFBA) e Mestre em Sistemas e Computação. Pesquisa informática na educação, tecnologias educacionais e educação inclusiva.",
                    tag: "Docente"
                ),
                CampusEventSpeaker(
                    id: "sp3", name: "Cláudio Sérgio", role: "Professor auxiliar", organization: "UEFS · Ciências Exatas",
                    bio: "Mestrando em Educação Científica, Inclusão e Diversidade (UFRB) e pós-graduado em Informática (UNIFACS).",
                    tag: "Docente"
                ),
                CampusEventSpeaker(
                    id: "sp4", name: "Taís Rocha Ribeiro", role: "Fundadora do Black XP", organization: "AimoTech",
                    bio: "Gerente de projetos do gamestudio AimoTech e ativista na interseção entre tecnologia, arte e inclusão. Forma jovens negros em games e inovação digital.",
                    tag: "Convidada"
                ),
                CampusEventSpeaker(
                    id: "sp5", name: "Diego do Carmo", role: "Engenheiro de Software", organization: "Tempest Secure Intelligence",
                    bio: "Graduando em Eng. de Computação na UEFS e Engenheiro de Software na Tempest. Egresso do PET Engenharias.",
                    tag: "Convidado"
                ),
                CampusEventSpeaker(
                    id: "sp6", name: "Matheus Oliveira Borges", role: "Engenheiro de Software", organization: "+10 anos de experiência",
                    bio: "Ex-aluno de Ecomp com mais de 10 anos de experiência como Engenheiro de Software.",
                    tag: "Convidado"
                ),
            ],
            workshops: [
                CampusEventWorkshop(
                    id: "of1", title: "Introdução à Metodologia PBL",
                    details: "Introduz os calouros ao Problem Based Learning com apresentação e mini-projeto prático em grupos, ao longo de 4 dias.",
                    audience: .freshmen, venueName: "Auditório / Salas do Dexa",
                    instructors: "Claudia P. Pereira e Cláudio Sérgio", requiresSignup: false, slots: nil
                ),
                CampusEventWorkshop(
                    id: "of2", title: "Introdução à Eletrônica",
                    details: "Relembra e solidifica conceitos elétricos fundamentais que acompanham o aluno durante todo o curso, com experimentos e implementações.",
                    audience: .freshmen, venueName: "MP 32 (LEI)",
                    instructors: "Brenda Barbosa de Oliveira", requiresSignup: false, slots: nil
                ),
                CampusEventWorkshop(
                    id: "of3", title: "Introdução ao MI de Circuitos Digitais",
                    details: "Criada para lidar com a alta demanda do MI de Circuitos Digitais, ajudando os alunos a acompanhar a disciplina e evitar desistências.",
                    audience: .veterans, venueName: "MP 32 (LEI)",
                    instructors: "Paulo Queiroz de Carvalho", requiresSignup: true, slots: 14
                ),
                CampusEventWorkshop(
                    id: "of4", title: "Introdução à Programação para Jogos",
                    details: "Fundamentos de Game Design e prática na engine Godot. Ao final, cada participante desenvolve seu primeiro mini-jogo 2D.",
                    audience: .veterans, venueName: "LESS (i11) — LABOTEC III",
                    instructors: "Liga de Jogos", requiresSignup: true, slots: 10
                ),
            ],
            venues: [
                CampusEventVenue(id: "v1", name: "Auditório da Biblioteca", shortName: "Auditório", hint: "Palestras e apresentações", mapX: 30, mapY: 28),
                CampusEventVenue(id: "v2", name: "MP 32 (LEI)", shortName: "MP 32", hint: "Oficinas de eletrônica e CD", mapX: 68, mapY: 22),
                CampusEventVenue(id: "v3", name: "LESS (i11) — LABOTEC III", shortName: "LESS · LABOTEC III", hint: "Embarcados e jogos", mapX: 74, mapY: 62),
                CampusEventVenue(id: "v4", name: "PAT 30 — Módulo 3", shortName: "PAT 30", hint: "Dinâmicas de integração", mapX: 46, mapY: 74),
                CampusEventVenue(id: "v5", name: "Praça do Borogodó", shortName: "Praça do Borogodó", hint: "Quest de boas-vindas", mapX: 50, mapY: 46),
            ],
            organizations: [
                CampusEventOrganization(
                    id: "og1", name: "DAECOMP", fullName: "Diretório Acadêmico de Eng. de Computação", tag: "Organizador",
                    details: "Diretório Acadêmico do curso — realizador da SIECOMP. Gestão Bittencourt."
                ),
                CampusEventOrganization(
                    id: "og2", name: "DCE UEFS", fullName: "Diretório Central dos Estudantes", tag: "Entidade",
                    details: "Principal entidade representativa dos estudantes da UEFS. Defende os direitos estudantis e a participação política."
                ),
                CampusEventOrganization(
                    id: "og3", name: "IEEE UEFS", fullName: "IEEE Student Branch", tag: "Ramo estudantil",
                    details: "Ramo estudantil do IEEE — promove conhecimento e oportunidades de melhoria profissional em ciência e tecnologia."
                ),
                CampusEventOrganization(
                    id: "og4", name: "Liga de Jogos", fullName: "Game League — UEFS", tag: "Liga",
                    details: "Grupo de alunos que desenvolvem jogos usando Unity, Godot e Love2D, com foco em aprendizado e troca de conhecimento."
                ),
                CampusEventOrganization(
                    id: "og5", name: "EcompJr", fullName: "Empresa Júnior de Eng. de Computação", tag: "Empresa Jr.",
                    details: "Empresa Júnior do curso — conecta os estudantes ao mercado através de projetos reais de tecnologia."
                ),
                CampusEventOrganization(
                    id: "og6", name: "Atlética Ecomp", fullName: "Imperador do Sertão", tag: "Atlética",
                    details: "Representa os estudantes em atividades esportivas, organiza treinos e promove a integração entre os cursos."
                ),
            ]
        )
    }
}
