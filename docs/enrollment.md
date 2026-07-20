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

## Audit

Every submit attempt — success or failure — is recorded server-side with the
exact proposal set pushed to SAGRES and how far the sequence got. Users are
told in-app to confirm the result on the official portal; the portal is
always the source of truth.
