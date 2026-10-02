# Client observability contract

The backend in `../unes-backrooms` now accepts an optional `record.context` on
`POST /api/logs`. Existing `service`, `records[].timestamp`, `severity`, `message`
and `attributes` remain compatible. This dictionary describes that wire contract;
availability on the server does not mean every client log already populates it.
Client sinks must explicitly adopt it and preserve these field meanings.

| Context field | Meaning and allowed value |
| --- | --- |
| `platform` | Required with context: `android`, `ios` or `backend`. |
| `environment` | Required with context: `production`, `staging`, `development` or `scenario`. Build configuration names such as `debug`/`release` are not environment values. |
| `feature` | Required with context: stable feature identifier, such as `auth` or `sync`. Avoid screen copy, routes with record IDs and localized names. |
| `operation` | Required with context: stable operation identifier, such as `refresh_token` or `initial_sync`. Preserve meaning across releases. |
| `appVersion` | Optional marketing version of the emitting app. |
| `appBuild` | Optional platform build identifier. |
| `commit` | Optional lowercase hexadecimal source revision, 7–40 characters. Omit when unknown. |
| `errorCode` | Optional stable machine-readable failure category; never an exception message or response body. |
| `retryable` | Optional boolean describing whether retrying this operation can recover under the current failure condition. |
| `requestId` | Optional UUID identifying the original API operation. |
| `flags` | Optional map of at most 32 relevant boolean flag assignments. Include only flags that explain the operation's behavior. |

Version/build, feature, operation, error-code and flag-key labels accept 1–100
characters from `A–Z`, `a–z`, `0–9`, `_`, `.`, `:` and `-`.

The server flattens context into OTel attributes, including `flag.<name>` for flag
assignments. A valid UUID in `X-Request-ID` is accepted, returned on the response
and bound to the server request logger; otherwise the server creates one. The
log upload's own correlation ID is stored separately as `ingestRequestId`.
Do not substitute it for the failed operation's `requestId`.

`X-Machine-Id` identifies an installation supplied by the client. It is not an
authenticated account ID and does not identify a Play review author. Request
IDs are correlation keys, not bearer credentials or short-lived support grants.
A user-initiated diagnostic reference with expiry and restricted lookup is still
a separate feature; this contract does not create one.

## Severity and operation outcomes

Use a stable operation outcome to explain user impact. Expected cancellation,
offline availability and a rejected credential are different from a client
invariant failure. The following is the intended policy for new instrumentation;
existing logging call sites still need auditing against it.

| Condition | Severity and outcome |
| --- | --- |
| User cancels or leaves a task | Debug/info; cancelled, not a crash/non-fatal incident. |
| Network unavailable with usable cached content | Info; degraded/offline. Record whether the requested data stayed usable. |
| Credentials rejected or refresh requires sign-in | Info/warn; recoverable authentication outcome. Do not log credential values. |
| Repeated backend failure prevents a task | Warn/error based on user impact; include stable error code and retryability. |
| Unexpected client invariant or unhandled failure | Error/fatal as appropriate; record sanitized diagnostic context. |

The existing KMP `LoggingConfig` defaults to warning-level Crashlytics non-fatal
reporting. Changing documentation does not change that runtime behavior. Audit
expected warning call sites before using raw non-fatal counts as an incident
rate. Native iOS already handles cancellation separately in its logging pipeline.

## Product outcome vocabulary

New measurements should preserve the following definitions and declare their
implementation status alongside dashboards; the table does not claim these
events are all currently emitted.

| Outcome | Success definition | Denominator |
| --- | --- | --- |
| Login usable | Authentication succeeds and the client reaches the next usable step. | Explicit login attempts, excluding cancellation. |
| Initial sync usable | The active semester needed for Home is persisted and renderable. | Initial-sync starts. |
| Schedule accessed | A user opens and can read a populated or valid empty schedule. | User schedule-open attempts. |
| Grades accessed | A user reaches readable grades or the legitimate no-grades state. | User grade-open attempts. |
| Recovery completed | A previously failed operation reaches its defined successful state after a user retry or supported automatic recovery. | Recovery attempts, keyed to the original operation category. |

Distinguish retry attempts from distinct user tasks and successful empty data from
transport errors. Test event emission around success, failure and cancellation so
renaming or moving a UI action does not silently redefine a metric. Do not report
rates until their attempt denominators are available and trustworthy.

## Evidence handling

No structured context may contain names, enrollment numbers, email addresses,
academic records, credentials, tokens or message bodies. The backend still
accepts legacy freeform `message` and `attributes`; schema validation alone does
not sanitize those fields. Keep restricted diagnostics outside the public
repository, and create synthetic fixtures from the behavior rather than copied
payloads. [Fixture checks](testing.md#synthetic-fixtures-and-credential-checks)
help catch mistakes but do not prove anonymization.

The backend's `docs/client-contracts-and-telemetry.md` owns the exact validation
schema and provider tests. Its `docs/reliability-worker.md` documents draft
deduplication, overlapping windows, persisted progress and freshness. Connect
real telemetry only with scoped credentials, explicit retention and a configured
private output location. Release decisions use [the recovery runbook](release-recovery.md).
