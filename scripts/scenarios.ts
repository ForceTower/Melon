import disciplines from "../contracts/v1/disciplines.json";
import messages from "../contracts/v1/messages.json";
import pilot from "../contracts/v1/pilot.json";

const missingMessageId = "00000000-0000-4000-8000-000000000404";

export const scenarioCatalog = [
  {
    id: "auth.login",
    route: "unes://home",
    stage: "login",
    expected: "Credential form is reachable from Welcome",
  },
  {
    id: "auth.invalid-credentials",
    route: "unes://home",
    stage: "login",
    expected: "Invalid credentials error; retry remains possible",
  },
  {
    id: "sync.unavailable",
    route: "unes://home",
    stage: "sync",
    expected: "Initial sync reports failure and offers recovery",
  },
  {
    id: "home.populated",
    route: "unes://home",
    stage: "home",
    expected: "Estudante and Algoritmos de Exemplo are visible",
  },
  {
    id: "home.offline-with-cache",
    route: "unes://home",
    stage: "after-login",
    expected: "Cached schedule survives a failed refresh",
  },
  {
    id: "auth.session-expired",
    route: "unes://home",
    stage: "after-login",
    expected: "Session expired banner with cached content",
  },
  {
    id: "messages.empty",
    route: "unes://messages",
    stage: "home",
    expected: "Successful empty inbox",
  },
  {
    id: "notification.message-missing",
    route: `unes://messages/${missingMessageId}`,
    stage: "home",
    expected:
      "A link to a message the server never returns falls back to the inbox after one refresh",
  },
  {
    id: "notification.message-after-refresh",
    route: `unes://messages/${messages.page.messages[0].id}`,
    stage: "home",
    expected: "A link that outruns the mirror refreshes once and opens the message",
  },
  {
    id: "enrollment.schedule-conflict",
    route: "unes://me",
    stage: "enrollment",
    expected: "Conflicting saved selections disable submission; no POST occurs",
  },
  {
    id: "enrollment.under-minimum",
    route: "unes://me",
    stage: "enrollment",
    expected: "Below-minimum workload disables submission; no POST occurs",
  },
  {
    id: "enrollment.over-maximum",
    route: "unes://me",
    stage: "enrollment",
    expected: "Above-maximum workload disables submission; no POST occurs",
  },
  {
    id: "enrollment.deadline-expired",
    route: "unes://me",
    stage: "enrollment",
    expected: "A stale OPEN response cannot permit submission after the deadline",
  },
  {
    id: "enrollment.submit-retry",
    route: "unes://me",
    stage: "enrollment",
    expected:
      "A full waitlisted section with a prerequisite warning can replace the saved proposal; failed submit retries unchanged",
  },
  {
    id: "disciplines.current",
    route: "unes://classes",
    stage: "disciplines",
    expected:
      "Current cards show status, partial mean and free absences; finals and low mean are flagged",
  },
  {
    id: "disciplines.history",
    route: "unes://classes",
    stage: "disciplines",
    expected:
      "Past semester shows CR, approval and an hours-weighted mean using the hours of the semester taken",
  },
  {
    id: "disciplines.download-retry",
    route: "unes://classes",
    stage: "disciplines",
    expected:
      "A failed past-semester download shows a snackbar and keeps the card; a retry downloads it",
  },
  {
    id: "discipline.detail.finals",
    route: "unes://classes",
    stage: "disciplines",
    expected: "A discipline in finals keeps its partial mean and shows the grade the final needs",
  },
  {
    id: "discipline.detail.attendance",
    route: "unes://classes",
    stage: "disciplines",
    expected: "Absences near the floored allowance warn and match the list card's free absences",
  },
] as const;

export type ScenarioId = (typeof scenarioCatalog)[number]["id"];

export function isScenarioId(value: string): value is ScenarioId {
  return scenarioCatalog.some((scenario) => scenario.id === value);
}

export function isDisciplinesScenario(id: ScenarioId) {
  return id.startsWith("disciplines.") || id.startsWith("discipline.");
}

export { disciplines, messages, pilot };
