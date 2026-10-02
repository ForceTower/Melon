# Enrollment (Matrícula)

Beta feature, both platforms. Entry is a card on the Eu tab that only appears
while a SAGRES enrollment window is open for the student.

Unlike grades/schedule/messages (served from the synced mirror), everything
here is read live from SAGRES on every screen — offers, vacancies, and the
saved proposal change by the second during a selection window, so nothing is
cached.

## API surface

| Endpoint | Purpose |
| --- | --- |
| `GET /api/enrollment/window` | Cheap window status for the hub gate + entry screen |
| `GET /api/enrollment/offers` | Full disciplines tree for the current step |
| `POST /api/enrollment/submit` | The whole matrícula transaction in one call |

`submit` takes the **complete desired set** of sections (not a delta) — the
portal's `finalizar-proposta` replaces the saved proposal wholesale, so
dropping a class means omitting it. Server-side the call is really
open → publish → close: SAGRES requires a finalized step to be reopened
before it accepts edits and finalized again to confirm.

## Client-side guards

The portal itself accepts proposals it shouldn't. The apps refuse to submit
when the proposal:

- is outside the step's official min/max hour bounds;
- contains a schedule conflict;
- is after the step's deadline.

The portal opening submissions **early** is not blocked — the official system
defines the window, and the apps match it.

Both clients refresh their injected clock immediately before submission, so a window
that expired while Review was open cannot pass the guard. The exact deadline
instant remains valid; strictly later is blocked. Prerequisites are warnings,
not submission blockers, because the provider filters the offered catalogue.

## Verification

`EnrollmentRulesTest`, `EnrollmentViewModelTest` and the KMP enrollment suite
cover conflict boundaries, hour limits, deadline crossing, waitlist/toggle
mapping, cancellation, and retry of the complete desired proposal. The shared
example is `contracts/v1/enrollment.json`; provider serializer/service tests in
the private backend verify it and the replace/open/publish/close boundary.
The five `enrollment.*` scenarios in [the catalog](scenarios.md) exercise the
installed app with synthetic responses and capture screenshots and request
evidence. No scenario submits to a real enrollment service.

Native `EnrollmentDeadlineTests` exercise expired OPEN/reopened proposals,
crossing the cutoff while Review stays open, exact-deadline acceptance and early
OPEN submission. The installed-app enrollment journeys currently run on Android;
native UI parity remains tracked in [the screen inventory](scenario-coverage.md).

## Audit

Every submit attempt — success or failure — is recorded server-side with the
exact proposal set pushed to SAGRES and how far the sequence got. Users are
told in-app to confirm the result on the official portal; the portal is
always the source of truth.
