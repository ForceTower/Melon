import enrollment from "../contracts/v1/enrollment.json";

export type EnrollmentSelection = (typeof enrollment.submission.selections)[number];

export function enrollmentFixture(
  scenario: string,
  now: number,
  submitted: EnrollmentSelection[] | null,
) {
  const window = structuredClone(enrollment.window);
  const offers = structuredClone(enrollment.offers);
  if (scenario === "enrollment.under-minimum") window.window.minHours = 120;
  if (scenario === "enrollment.over-maximum") {
    window.window.minHours = 0;
    window.window.maxHours = 30;
  }
  if (scenario === "enrollment.deadline-expired") {
    window.window.endDate = new Date(now - 1_000).toISOString();
  }
  if (submitted !== null) window.window.state = "CLOSED";
  for (const discipline of offers.disciplines) {
    if (scenario === "enrollment.submit-retry") {
      discipline.prereqs = discipline.prereqs.map((prerequisite) => ({
        ...prerequisite,
        met: false,
      }));
    }
    for (const section of discipline.sections) {
      if (submitted !== null)
        section.selected = submitted.some((selection) => selection.sectionId === section.id);
      else if (scenario === "enrollment.schedule-conflict" && discipline.id === 202)
        section.selected = true;
    }
  }
  return { window, offers };
}

function isSelection(value: unknown): value is EnrollmentSelection {
  return (
    typeof value === "object" &&
    value !== null &&
    "sectionId" in value &&
    typeof value.sectionId === "number" &&
    Number.isSafeInteger(value.sectionId) &&
    "allowsOther" in value &&
    typeof value.allowsOther === "boolean" &&
    "waitlist" in value &&
    typeof value.waitlist === "boolean"
  );
}

export function parseEnrollmentSelections(value: unknown): EnrollmentSelection[] | null {
  if (
    typeof value !== "object" ||
    value === null ||
    !("selections" in value) ||
    !Array.isArray(value.selections) ||
    value.selections.length === 0 ||
    !value.selections.every(isSelection)
  )
    return null;
  return value.selections.map(({ sectionId, allowsOther, waitlist }) => ({
    sectionId,
    allowsOther,
    waitlist,
  }));
}
