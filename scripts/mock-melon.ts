// Local Melon API stand-in for client testing: enrollment endpoints return a
// rich fixture catalogue (mirrors the dc matricula-data.js set — full section,
// Monday clash, "a definir", unmet prereqs, waitlist) and campus-events serves
// a SIECOMP-shaped featured event with dates pinned around now; every other
// path proxies to prod so auth, sync, profile, messages, etc. keep working.
// Extend the same pattern to mock other endpoint groups when needed.
//
//   bun run scripts/mock-melon.ts    → listens on http://0.0.0.0:8787
//   curl localhost:8787/debug/reset  → re-open the window after a submit
//   curl localhost:8787/debug/campus-phase/upcoming|live|ended
//                                    → re-pin the campus event around now
//
// Pointing a client at it:
//   Android  ./gradlew :apps:android:app:assembleDebug -Pmelon.apiBaseUrl=http://127.0.0.1:8787
//            adb reverse tcp:8787 tcp:8787   (device or emulator)
//   iOS      set the `debug_api_base_url` UserDefaults override to
//            http://127.0.0.1:8787 (simulator reaches the host directly)

const UPSTREAM = "https://melon.forcetower.dev";
const PORT = 8787;

// ── window dates: opened 2 days ago, closes in 5 days at 23:59 local ──
function pad(n: number) {
  return String(n).padStart(2, "0");
}
function offsetDate(daysFromNow: number, hour: number, minute: number) {
  const d = new Date(Date.now() + daysFromNow * 86_400_000);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(hour)}:${pad(minute)}:00-03:00`;
}

// ── enrollment fixture state ──
let submitted: { sectionId: number; allowsOther: boolean; waitlist: boolean }[] | null = null;

const slot = (day: number, start: string, end: string) => ({ day, start, end });
const meeting = (
  kind: string,
  shift: string,
  professors: string[],
  room: string | null,
  slots: ReturnType<typeof slot>[],
) => ({ kind, shift, professors, room, slots });

// day: 1=Mon … 6=Sat. Deliberate test cases: EXA866 T01 is full+queued,
// TEC502 T01 clashes with EXA427 T01 on Monday, TEC505 T02 has no schedule,
// CHF336/TEC540 carry unmet prereqs, EXA427 T02 + TEC540 T01 are "quase cheia".
const disciplines = [
  {
    id: 8000000201, code: "EXA427", name: "Estruturas de Dados",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [{ code: "EXA418", name: "Algoritmos e Programação II", met: true }],
    sections: [
      { id: 8000030101, label: "T01", coursePreferential: true, suggestion: true,
        vacancies: 50, proposalsCount: 31, allowsOtherDefault: true, waitlistCount: 0, selected: true,
        meetings: [meeting("Teórica", "AFTERNOON", ["Matheus Andrade"], "PAT76 · UEFS",
          [slot(1, "13:30", "15:30"), slot(3, "13:30", "15:30")])] },
      { id: 8000030102, label: "T02", coursePreferential: false, suggestion: false,
        vacancies: 50, proposalsCount: 47, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "NIGHT", ["Cláudia Ribeiro"], "MA09 · UEFS",
          [slot(2, "18:50", "20:50"), slot(4, "18:50", "20:50")])] },
    ],
  },
  {
    id: 8000000202, code: "TEC499", name: "Sistemas Digitais",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true, prereqs: [],
    sections: [
      { id: 8000030201, label: "T01P01", coursePreferential: true, suggestion: true,
        vacancies: 40, proposalsCount: 22, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [
          meeting("Teórica", "AFTERNOON", ["Roberto Sales"], "PAT54 · UEFS", [slot(1, "13:30", "15:30")]),
          meeting("Prática", "AFTERNOON", ["Roberto Sales"], "Lab. Hardware · UEFS", [slot(5, "15:30", "17:30")]),
        ] },
      { id: 8000030202, label: "T02P02", coursePreferential: false, suggestion: false,
        vacancies: 40, proposalsCount: 12, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [
          meeting("Teórica", "MORNING", ["Roberto Sales"], "PAT55 · UEFS", [slot(2, "07:30", "09:30")]),
          meeting("Prática", "MORNING", ["Helena Past"], "Lab. Hardware · UEFS", [slot(4, "09:30", "11:30")]),
        ] },
    ],
  },
  {
    id: 8000000203, code: "EXA866", name: "Probabilidade e Estatística",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true,
    prereqs: [{ code: "EXA412", name: "Cálculo Diferencial e Integral I", met: true }],
    sections: [
      { id: 8000030301, label: "T01", coursePreferential: true, suggestion: true,
        vacancies: 45, proposalsCount: 45, allowsOtherDefault: true, waitlistCount: 6, selected: true,
        meetings: [meeting("Teórica", "MORNING", ["Sônia Vasconcelos"], "MA12 · UEFS",
          [slot(2, "07:30", "09:30"), slot(4, "07:30", "09:30")])] },
      { id: 8000030302, label: "T02", coursePreferential: false, suggestion: false,
        vacancies: 45, proposalsCount: 39, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "NIGHT", ["Ivan Coutinho"], "MA08 · UEFS",
          [slot(3, "20:50", "22:50"), slot(5, "20:50", "22:50")])] },
    ],
  },
  {
    id: 8000000204, code: "TEC502", name: "Concorrência e Conectividade",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: true, prereqs: [],
    sections: [
      { id: 8000030401, label: "T01", coursePreferential: true, suggestion: false,
        vacancies: 40, proposalsCount: 18, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "AFTERNOON", ["Gustavo Lemos"], "PAT80 · UEFS",
          [slot(1, "13:30", "15:30"), slot(4, "13:30", "15:30")])] },
      { id: 8000030402, label: "T02", coursePreferential: false, suggestion: false,
        vacancies: 40, proposalsCount: 25, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "NIGHT", ["Gustavo Lemos"], "PAT81 · UEFS",
          [slot(1, "18:50", "20:50"), slot(3, "18:50", "20:50")])] },
    ],
  },
  {
    id: 8000000205, code: "TEC505", name: "Banco de Dados",
    workload: 60, mandatory: true, gradePeriod: 4, suggestion: false, prereqs: [],
    sections: [
      { id: 8000030501, label: "T01", coursePreferential: true, suggestion: false,
        vacancies: 45, proposalsCount: 20, allowsOtherDefault: true, waitlistCount: 0, selected: true,
        meetings: [meeting("Teórica", "MORNING", ["Letícia Moura"], "LCC1 · UEFS",
          [slot(3, "09:30", "11:30"), slot(5, "09:30", "11:30")])] },
      { id: 8000030502, label: "T02", coursePreferential: false, suggestion: false,
        vacancies: 45, proposalsCount: 3, allowsOtherDefault: true, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "UNDEFINED", [], null, [])] },
    ],
  },
  {
    id: 8000000301, code: "LET021", name: "Inglês Instrumental",
    workload: 30, mandatory: false, gradePeriod: 0, suggestion: false, prereqs: [],
    sections: [
      { id: 8000030601, label: "T01", coursePreferential: false, suggestion: false,
        vacancies: 35, proposalsCount: 14, allowsOtherDefault: false, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "AFTERNOON", ["Marina Coelho"], "MOD7 · UEFS", [slot(5, "13:30", "15:30")])] },
    ],
  },
  {
    id: 8000000162, code: "CHF336", name: "Psicologia Social Comunitária",
    workload: 30, mandatory: false, gradePeriod: 0, suggestion: false,
    prereqs: [{ code: "CHF330", name: "Psicologia Social", met: false }],
    sections: [
      { id: 8000030701, label: "T01", coursePreferential: false, suggestion: false,
        vacancies: 50, proposalsCount: 8, allowsOtherDefault: false, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "NIGHT", ["José Andrade"], "MOD3 · UEFS", [slot(2, "18:50", "20:50")])] },
    ],
  },
  {
    id: 8000000302, code: "TEC540", name: "Introdução à Inteligência Artificial",
    workload: 60, mandatory: false, gradePeriod: 0, suggestion: false,
    prereqs: [{ code: "EXA427", name: "Estruturas de Dados", met: false }],
    sections: [
      { id: 8000030801, label: "T01", coursePreferential: false, suggestion: false,
        vacancies: 30, proposalsCount: 27, allowsOtherDefault: false, waitlistCount: 0, selected: false,
        meetings: [meeting("Teórica", "AFTERNOON", ["Daniel Prado"], "LCC2 · UEFS",
          [slot(2, "15:30", "17:30"), slot(4, "15:30", "17:30")])] },
    ],
  },
];

// ── campus-event fixture: SIECOMP-shaped, dates pinned around now ──
// `upcoming` starts in 2 days, `live` started 2 days ago (mid-week), `ended`
// wrapped 9 days ago. Switching phases bumps the revision so clients replace
// their (id, revision)-deduped snapshot instead of ignoring the new dates.
type CampusPhase = "upcoming" | "live" | "ended";
let campusPhase: CampusPhase = "upcoming";
let campusRevision = 1;

function campusEventPayload() {
  const dayShift = { upcoming: 2, live: -2, ended: -9 }[campusPhase];
  const at = (day: number, hour: number, minute = 0) => offsetDate(dayShift + day, hour, minute);

  const activities = [
    { id: "act-quest-da", title: "Quest: Dinâmica com o DA",
      details: "O DA Ecomp preparou uma dinâmica interativa para dar as boas-vindas aos calouros no curso! Para fazer o cadastro no bandejão, chegue no horário.",
      category: "quest", audience: "freshmen", venueId: "ven-praca", venueName: "Praça do Borogodó",
      speakerNames: ["DAECOMP"], startsAt: at(0, 8), endsAt: at(0, 12) },
    { id: "act-pbl-1", title: "Introdução à metodologia PBL — 1º dia",
      details: "Introduz os alunos à metodologia PBL através de uma apresentação e um mini-projeto prático em grupos.",
      category: "workshop", audience: "freshmen", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerIds: ["spk-claudia", "spk-claudio"], speakerNames: ["Claudia P. Pereira", "Cláudio Sérgio"],
      startsAt: at(0, 13, 30), endsAt: at(0, 16) },
    { id: "act-blackxp", title: "Black XP — Impulsionando Talentos Dissidentes na Tecnologia",
      details: "O Black XP é uma iniciativa voltada à formação tecnológica em games, com foco na inserção de jovens negros, indígenas e mulheres no mercado de tecnologia.",
      category: "presentation", audience: "everyone", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerIds: ["spk-tais"], speakerNames: ["Taís Rocha Ribeiro"], startsAt: at(0, 16), endsAt: at(0, 17) },
    { id: "act-comissao", title: "Comissão de Avaliação do Curso",
      details: "A Comissão Permanente de Avaliação compartilhará os resultados do semestre anterior.",
      category: "presentation", audience: "veterans", venueName: "Google Meet",
      speakerIds: ["spk-joao"], speakerNames: ["João B. Rocha"], startsAt: at(0, 19) },
    { id: "act-cd-1", title: "Introdução a Circuitos Digitais (Turma 1) — 1º dia",
      details: "Um pontapé inicial para os alunos que pegam o MI de Circuitos Digitais, reduzindo a desistência e o gargalo histórico da matéria.",
      category: "workshop", audience: "veterans", venueId: "ven-mp32", venueName: "MP 32 (LEI)",
      speakerNames: ["Paulo Queiroz de Carvalho"], startsAt: at(1, 8), endsAt: at(1, 11), requiresSignup: true },
    { id: "act-integracao", title: "Integração, colaboração e aprendizagem",
      details: "A dinâmica promove o conhecimento e a integração do grupo, refletindo sobre a importância da boa comunicação no trabalho em equipe.",
      category: "dynamic", audience: "freshmen", venueId: "ven-pat30", venueName: "PAT 30 — Módulo 3",
      speakerNames: ["Rosaria Trindade"], startsAt: at(1, 10), endsAt: at(1, 12) },
    { id: "act-jogos", title: "Introdução à programação para jogos",
      details: "Fundamentos de Game Design e prática na engine Godot: ao final, cada participante desenvolve seu primeiro mini-jogo 2D.",
      category: "workshop", audience: "veterans", venueId: "ven-less", venueName: "LESS (i11) — LABOTEC III",
      speakerNames: ["Liga de Jogos"], startsAt: at(1, 13, 30), endsAt: at(1, 15, 30), requiresSignup: true },
    { id: "act-entidades", title: "Conhecendo o DAECOMP e o DCE",
      details: "Apresentação das duas entidades estudantis mais importantes para o curso.",
      category: "presentation", audience: "freshmen", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["DAECOMP", "DCE"], startsAt: at(1, 15, 30), endsAt: at(1, 17, 30) },
    { id: "act-direito-ia", title: "Direito x IA — Aspectos cíveis e penais",
      details: "Os impactos da inteligência artificial no campo jurídico: desafios éticos e caminhos para uma regulação mais eficaz na era digital.",
      category: "lecture", audience: "everyone", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["Anderson Lopes"], startsAt: at(2, 8), endsAt: at(2, 9, 30) },
    { id: "act-intercambio", title: "Guia para o Intercâmbio — Do Sonho à Realidade",
      details: "Orientações para realizar um intercâmbio pela AERI: inscrição, processo seletivo e dicas para escolher a universidade ideal.",
      category: "lecture", audience: "veterans", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["Douglas Oliveira"], startsAt: at(2, 11), endsAt: at(2, 12) },
    { id: "act-atletica", title: "Dinâmica: Atlética Ecomp",
      details: "Momento de apresentação da atlética e dinâmica de integração esportiva.",
      category: "dynamic", audience: "freshmen", venueId: "ven-mp32", venueName: "MP 32 (LEI)",
      speakerNames: ["Atlética Ecomp"], startsAt: at(2, 15, 30), endsAt: at(2, 17, 30) },
    { id: "act-extracurriculares", title: "A importância de cursos extracurriculares",
      details: "O que vem depois de se tornar engenheiro(a)? Um papo sobre oportunidades, desafios e como cursos extracurriculares abrem portas.",
      category: "lecture", audience: "freshmen", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["Pâmela M. C. Cortez"], startsAt: at(3, 8), endsAt: at(3, 9, 30) },
    { id: "act-ecompjr", title: "EcompJr — seu primeiro contato com o mercado",
      details: "Apresenta a Empresa Júnior do curso: estrutura, serviços, cargos e processo de ingresso.",
      category: "lecture", audience: "everyone", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["Cláudio Daniel Peruna"], startsAt: at(3, 13, 30), endsAt: at(3, 14, 30) },
    { id: "act-ia-intro", title: "IA na Programação Introdutória: Positivo ou Negativo?",
      details: "Desde o ChatGPT, as ferramentas de IA Generativa são amplamente usadas — inclusive na educação. Que impactos isso traz para quem está aprendendo a programar?",
      category: "lecture", audience: "freshmen", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerIds: ["spk-diego", "spk-claudia"], speakerNames: ["Diego do Carmo", "Claudia P. Pereira"],
      startsAt: at(3, 16), endsAt: at(3, 17, 30) },
    { id: "act-boas-praticas", title: "Do Zero ao Código de Qualidade: Boas Práticas",
      details: "As principais boas práticas de programação para escrever códigos mais limpos, robustos e fáceis de manter.",
      category: "lecture", audience: "everyone", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerNames: ["Francisco Pereira"], startsAt: at(4, 8, 30), endsAt: at(4, 9, 30) },
    { id: "act-pbl-final", title: "Introdução à metodologia PBL — encontro final",
      details: "Encontro final da oficina de PBL, com a apresentação dos resultados dos mini-projetos.",
      category: "workshop", audience: "freshmen", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerIds: ["spk-claudia", "spk-claudio"], speakerNames: ["Claudia P. Pereira", "Cláudio Sérgio"],
      startsAt: at(4, 9, 30), endsAt: at(4, 12) },
    { id: "act-mundo-real", title: "Desenvolvimento de Software — Como funciona no mundo real?",
      details: "Experiência e conhecimento de mercado a partir das dúvidas que um estudante costuma ter — com espaço para perguntas ao vivo.",
      category: "lecture", audience: "everyone", venueId: "ven-aud", venueName: "Auditório da Biblioteca",
      speakerIds: ["spk-matheus"], speakerNames: ["Matheus Oliveira Borges"], startsAt: at(4, 15, 30), endsAt: at(4, 17, 30) },
  ];

  return {
    event: {
      id: "siecomp-33",
      revision: campusRevision,
      name: "SIECOMP",
      edition: "XXXIII",
      tagline: "Semana de Integração dos Estudantes do curso de Engenharia de Computação.",
      institution: "UEFS · Eng. de Computação",
      credit: "Uma realização do DAECOMP · Gestão Bittencourt",
      timezone: "America/Bahia",
      startsAt: at(0, 8),
      endsAt: at(4, 17, 30),
      activities,
      speakers: [
        { id: "spk-joao", name: "João B. Rocha", role: "Coordenador do curso", organization: "UEFS · PGCC",
          bio: "Doutor em Ciências da Computação pela NTNU e professor na UEFS desde 2004. Coordenador do curso, pesquisa big data, análise de dados e sistemas distribuídos.", tag: "Docente" },
        { id: "spk-claudia", name: "Claudia P. Pereira", role: "Professora e pesquisadora", organization: "UEFS · Mestrado em CC",
          bio: "Doutora em Difusão do Conhecimento (UFBA) e Mestre em Sistemas e Computação. Pesquisa informática na educação, tecnologias educacionais e educação inclusiva.", tag: "Docente" },
        { id: "spk-claudio", name: "Cláudio Sérgio", role: "Professor auxiliar", organization: "UEFS · Ciências Exatas",
          bio: "Mestrando em Educação Científica, Inclusão e Diversidade (UFRB) e pós-graduado em Informática (UNIFACS).", tag: "Docente" },
        { id: "spk-tais", name: "Taís Rocha Ribeiro", role: "Fundadora do Black XP", organization: "AimoTech",
          bio: "Gerente de projetos do gamestudio AimoTech e ativista na interseção entre tecnologia, arte e inclusão. Forma jovens negros em games e inovação digital.", tag: "Convidada" },
        { id: "spk-diego", name: "Diego do Carmo", role: "Engenheiro de Software", organization: "Tempest Secure Intelligence",
          bio: "Graduando em Eng. de Computação na UEFS e Engenheiro de Software na Tempest. Egresso do PET Engenharias.", tag: "Convidado" },
        { id: "spk-matheus", name: "Matheus Oliveira Borges", role: "Engenheiro de Software", organization: "+10 anos de experiência",
          bio: "Ex-aluno de Ecomp com mais de 10 anos de experiência como Engenheiro de Software.", tag: "Convidado" },
      ],
      workshops: [
        { id: "wsh-pbl", title: "Introdução à Metodologia PBL",
          details: "Introduz os calouros ao Problem Based Learning com apresentação e mini-projeto prático em grupos, ao longo de 4 dias.",
          audience: "freshmen", venueName: "Auditório / Salas do Dexa", instructors: "Claudia P. Pereira e Cláudio Sérgio" },
        { id: "wsh-eletronica", title: "Introdução à Eletrônica",
          details: "Relembra e solidifica conceitos elétricos fundamentais que acompanham o aluno durante todo o curso, com experimentos e implementações.",
          audience: "freshmen", venueName: "MP 32 (LEI)", instructors: "Brenda Barbosa de Oliveira" },
        { id: "wsh-cd", title: "Introdução ao MI de Circuitos Digitais",
          details: "Criada para lidar com a alta demanda do MI de Circuitos Digitais, ajudando os alunos a acompanhar a disciplina e evitar desistências.",
          audience: "veterans", venueName: "MP 32 (LEI)", instructors: "Paulo Queiroz de Carvalho", requiresSignup: true, slots: 14 },
        { id: "wsh-jogos", title: "Introdução à Programação para Jogos",
          details: "Fundamentos de Game Design e prática na engine Godot. Ao final, cada participante desenvolve seu primeiro mini-jogo 2D.",
          audience: "veterans", venueName: "LESS (i11) — LABOTEC III", instructors: "Liga de Jogos", requiresSignup: true, slots: 10 },
      ],
      venues: [
        { id: "ven-aud", name: "Auditório da Biblioteca", shortName: "Auditório", hint: "Palestras e apresentações", mapX: 30, mapY: 28 },
        { id: "ven-mp32", name: "MP 32 (LEI)", shortName: "MP 32", hint: "Oficinas de eletrônica e CD", mapX: 68, mapY: 22 },
        { id: "ven-less", name: "LESS (i11) — LABOTEC III", shortName: "LESS · LABOTEC III", hint: "Embarcados e jogos", mapX: 74, mapY: 62 },
        { id: "ven-pat30", name: "PAT 30 — Módulo 3", shortName: "PAT 30", hint: "Dinâmicas de integração", mapX: 46, mapY: 74 },
        { id: "ven-praca", name: "Praça do Borogodó", shortName: "Praça do Borogodó", hint: "Quest de boas-vindas", mapX: 50, mapY: 46 },
      ],
      organizations: [
        { id: "org-daecomp", name: "DAECOMP", fullName: "Diretório Acadêmico de Eng. de Computação", tag: "Organizador",
          details: "Diretório Acadêmico do curso — realizador da SIECOMP. Gestão Bittencourt." },
        { id: "org-dce", name: "DCE UEFS", fullName: "Diretório Central dos Estudantes", tag: "Entidade",
          details: "Principal entidade representativa dos estudantes da UEFS. Defende os direitos estudantis e a participação política." },
        { id: "org-ieee", name: "IEEE UEFS", fullName: "IEEE Student Branch", tag: "Ramo estudantil",
          details: "Ramo estudantil do IEEE — promove conhecimento e oportunidades de melhoria profissional em ciência e tecnologia." },
        { id: "org-liga", name: "Liga de Jogos", fullName: "Game League — UEFS", tag: "Liga",
          details: "Grupo de alunos que desenvolvem jogos usando Unity, Godot e Love2D, com foco em aprendizado e troca de conhecimento." },
        { id: "org-ecompjr", name: "EcompJr", fullName: "Empresa Júnior de Eng. de Computação", tag: "Empresa Jr.",
          details: "Empresa Júnior do curso — conecta os estudantes ao mercado através de projetos reais de tecnologia." },
        { id: "org-atletica", name: "Atlética Ecomp", fullName: "Imperador do Sertão", tag: "Atlética",
          details: "Representa os estudantes em atividades esportivas, organiza treinos e promove a integração entre os cursos." },
      ],
    },
  };
}

function windowPayload() {
  return {
    available: true,
    window: {
      semester: "2026.2",
      state: submitted ? "CLOSED" : "OPEN",
      startDate: offsetDate(-2, 8, 0),
      endDate: offsetDate(5, 23, 59),
      minHours: 240,
      maxHours: 420,
      useQueue: true,
      courseId: 1,
    },
  };
}

function offersPayload() {
  const submittedIds = new Set((submitted ?? []).map((s) => s.sectionId));
  return {
    disciplines: disciplines.map((d) => ({
      ...d,
      sections: d.sections.map((s) => ({
        ...s,
        selected: submitted ? submittedIds.has(s.id) : s.selected,
      })),
    })),
  };
}

function ok(data: unknown) {
  return Response.json({ ok: true, message: null, data });
}

async function passthrough(req: Request, url: URL): Promise<Response> {
  const headers = new Headers(req.headers);
  headers.delete("host");
  headers.delete("accept-encoding");
  const body = req.method === "GET" || req.method === "HEAD" ? undefined : await req.arrayBuffer();
  const upstream = await fetch(UPSTREAM + url.pathname + url.search, {
    method: req.method,
    headers,
    body,
    redirect: "manual",
  });
  const outHeaders = new Headers(upstream.headers);
  // fetch already decompressed the body — drop stale framing headers.
  outHeaders.delete("content-encoding");
  outHeaders.delete("content-length");
  outHeaders.delete("transfer-encoding");
  return new Response(upstream.body, { status: upstream.status, headers: outHeaders });
}

Bun.serve({
  port: PORT,
  hostname: "0.0.0.0",
  async fetch(req) {
    const url = new URL(req.url);
    const tag = `${req.method} ${url.pathname}`;

    if (url.pathname === "/api/enrollment/window") {
      console.log(`[mock] ${tag} → ${submitted ? "CLOSED" : "OPEN"}`);
      return ok(windowPayload());
    }
    if (url.pathname === "/api/enrollment/offers") {
      console.log(`[mock] ${tag}`);
      return ok(offersPayload());
    }
    if (url.pathname === "/api/enrollment/submit" && req.method === "POST") {
      const body = (await req.json().catch(() => null)) as
        | { selections?: { sectionId: number; allowsOther: boolean; waitlist: boolean }[] }
        | null;
      if (!body?.selections?.length) {
        console.log(`[mock] ${tag} → 400 (bad payload)`, body);
        return Response.json({ ok: false, message: "Proposta vazia", data: null }, { status: 400 });
      }
      submitted = body.selections;
      console.log(`[mock] ${tag} → accepted`, JSON.stringify(submitted));
      return ok({});
    }
    if (url.pathname === "/debug/reset") {
      submitted = null;
      console.log("[mock] window reset to OPEN");
      return ok({ reset: true });
    }

    if (url.pathname === "/api/campus-events/current") {
      console.log(`[mock] ${tag} → ${campusPhase} (rev ${campusRevision})`);
      return ok(campusEventPayload());
    }
    const phaseMatch = url.pathname.match(/^\/debug\/campus-phase\/(upcoming|live|ended)$/);
    if (phaseMatch) {
      campusPhase = phaseMatch[1] as CampusPhase;
      campusRevision += 1;
      console.log(`[mock] campus phase → ${campusPhase} (rev ${campusRevision})`);
      return ok({ phase: campusPhase, revision: campusRevision });
    }

    console.log(`[proxy] ${tag}`);
    try {
      return await passthrough(req, url);
    } catch (error) {
      console.error(`[proxy] ${tag} failed:`, error);
      return Response.json({ ok: false, message: "upstream unreachable", data: null }, { status: 502 });
    }
  },
});

console.log(
  `mock-melon on http://0.0.0.0:${PORT} — enrollment + campus-events mocked, rest proxied to ${UPSTREAM}`,
);
