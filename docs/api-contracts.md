# Client/server contract boundary

The private `unes-backrooms` repository owns HTTP behavior. Melon contains native
Kotlin and Swift consumers; native iOS does not link the KMP graph. There is no
`apps/api` service in this repository.

`contracts/v1/pilot.json` is a versioned synthetic example shared by the mock
server, KMP JVM decoding tests and native Swift decoding/database tests. Its
`login`, `profile`, `onboarding`, `semester`, `messages` and `events` properties
are response **data**; HTTP adds `{ ok, message, data }` envelopes. The provider
keeps a matching example and verifies production serializers against it. Changes
to public wire shapes should update both copies and both pipelines in one task.
Provider checks are maintained in the private repository and do not require
credentials or a real university account.

`contracts/v1/messages.json` is a populated one-message inbox page (response
**data** for `GET /api/sync/messages`) used by the notification journeys and
decoded by KMP. The provider does not yet verify a populated message against it.

`contracts/v1/enrollment.json` adds the live window, offers and complete desired
submission set. Provider tests exercise actual serializers and the service's
open/publish/close sequence, omission of removed sections, waitlist/toggle mapping,
error handling and audit records using synthetic dependencies. KMP decodes the
same example and tests the real repository's HTTP body and error mapping. The
mock's named variants alter constraints or metadata to reproduce client failures;
they do not claim those invalid proposals are safe to submit to the real portal.

`contracts/v1/disciplines.json` holds two semester payloads (response **data**
for `GET /api/sync/semesters/{id}`): the pilot's current semester with grades,
a pending Prova Final and absences added, and a closed past semester. One past
discipline keeps 60 offer hours while its catalog row says 75, as happens when
the catalog changes after the semester: the provider writes both from the same
upstream workload, but the catalog row is overwritten by every later sync and
the offer is not. Clients count the offer's hours first. Another past
discipline failed for absences with no mean (`REPROVADO POR FALTA`), which the
CR counts as 0 over its hours. KMP and Swift decode this example; the provider
does not yet verify against it.

`contracts/v1/transit.json` covers "Meu trajeto" (response **data** for
`GET /api/transit/network`, `GET /api/transit/stops/{id}/arrivals`,
`GET /api/transit/follow` and `GET`/`PUT`/`DELETE /api/transit/plan`, all
signed-in only): a route pair with travel-ordered stops, one arrival in each
state (`live`, `waiting`, `historical`), a follow payload and a saved plan.
Instants are ISO strings; clients format bands and never show seconds. The
server owns the saved plan: `PUT` replaces it whole (both directions always
sent, `null` removes one) and answers with the stop names it resolved, which
clients mirror locally. Swift decodes it in `TransitContractTests`; Android
consumes it in its follow-up.

Keep nullable fields distinct from missing required fields. Opaque IDs remain
strings, platform numeric IDs retain 64-bit capacity, and schedule weekdays stay
Sunday=0 through Saturday=6. Date-only semester boundaries differ from timestamped
events. Login refresh tokens rotate and must not be replayed.

Examples are human-authored, with reserved `example.invalid` email addresses,
obviously synthetic account names and nonfunctional tokens. Never copy production
student data, credentials, enrollment numbers, messages or raw incident payloads
into this public repository. Reduce an incident to the fields needed to reproduce
it and document the synthetic origin. Run `bun run check:fixtures`; its heuristic
scan supplements review and cannot prove that arbitrary prose is synthetic.

Decoding examples protect consumer compatibility, and provider serializer checks
protect the produced shape. Neither exercises a live deployment, authentication
infrastructure, upstream data collection, or a real enrollment submission. Staging
checks need a separate isolated account and cleanup policy.
