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
