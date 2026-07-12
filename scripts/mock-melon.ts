// Local Melon API stand-in for client testing: enrollment endpoints return a
// rich fixture catalogue (mirrors the dc matricula-data.js set — full section,
// Monday clash, "a definir", unmet prereqs, waitlist); every other path
// proxies to prod so auth, sync, profile, messages, etc. keep working.
// Extend the same pattern to mock other endpoint groups when needed.
//
//   bun run scripts/mock-melon.ts    → listens on http://0.0.0.0:8787
//   curl localhost:8787/debug/reset  → re-open the window after a submit
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

    console.log(`[proxy] ${tag}`);
    try {
      return await passthrough(req, url);
    } catch (error) {
      console.error(`[proxy] ${tag} failed:`, error);
      return Response.json({ ok: false, message: "upstream unreachable", data: null }, { status: 502 });
    }
  },
});

console.log(`mock-melon on http://0.0.0.0:${PORT} — enrollment mocked, rest proxied to ${UPSTREAM}`);
