# Release verification and recovery

This runbook records the available recovery mechanisms and the evidence needed
for a release decision. It does not publish builds, alter production flags, halt
store rollouts or set automatic health thresholds. Those actions stay with the
release owner until an explicit operational policy is configured.

## Before a rollout

Record the commit, marketing version/build, target platform and rollout cohort.
Link the successful **Required verification** run and its artifacts. For a fix,
include the original incident key, synthetic reproduction and regression result.
Identify the release owner and next review time in the release record.

Verify a release-like installation and cold launch on the supported device,
then exercise sign-in, initial sync, cached Home while offline, session recovery
and notification navigation. Verify an upgrade from the currently shipped build
with existing cached data; clean-install tests cannot establish migration safety.
Enrollment verification uses synthetic accounts and never submits real student
proposals. See [testing](testing.md) and [Android performance](android-performance.md)
for implemented automation and its current boundaries.

For each newly exposed feature, record the relevant flag, default/cache behavior,
expected disabled state and recovery action. Verify both enabled and disabled
behavior in a release-like configuration. Ordinary Android debug builds force
many feature gates on and cannot establish that a production flag disables them.
The isolated scenario/performance variants do not use that debug override.

## Read health evidence

Compare the new release with the previous healthy release across comparable
time windows, platforms, OS/device cohorts and flag assignments. Record:

- Crash/ANR affected users or sessions, numerator, denominator and window.
- Sign-in, initial-sync and session-recovery outcomes per attempted operation.
- Store vitals and review symptoms, with candidate issue links and uncertainty.
- Telemetry freshness, last successful collection and any unavailable source.

If no trustworthy denominator exists, report counts and that limitation. Empty
or stale telemetry is not evidence of a healthy release. The private backend
repository's reliability worker prepares deduplicated drafts and freshness
evidence; it does not change rollout state. Metrics and structured-field semantics
are documented in [observability](observability.md).

## Recovery actions

| Observation | Available action | Verification |
| --- | --- | --- |
| A remotely gated feature is broken | The owner may disable its existing Lever flag for the affected platform/cohort. | Confirm the published value reaches the affected release and that its disabled state remains usable. |
| A release causes a broad startup/login regression | The owner may halt further staged distribution and prepare a corrected build. | Record the store rollout state, affected versions and the corrected build's reproduction results. |
| A backend outage spans several client releases | Restore or mitigate the backend operation and keep client cached/offline behavior usable. | Compare the same operation across releases and confirm provider recovery with fresh evidence. |
| An update is available but installed clients still fail | Use the existing Android Play in-app update path where eligible, then verify the fixed version's health. | Confirm installation eligibility and observed adoption; an update prompt is not a remote disable switch. |

Halting a rollout does not remove versions already installed. Disabling a UI gate
does not revoke an API operation or necessarily close an already-open screen.
Lever serves cached values when offline, so a flag publish is not instantaneous
for all clients. Record the intended scope and confirm actual behavior before
calling the mitigation successful.

The current shared feature keys are `enable_enrollment`,
`enable_enrollment_certificate`, `enable_academic_history`, `enable_paradoxo`,
`enable_materials`, `enable_library`, `enable_campus_event`,
`enable_evaluation_reminders` and `enable_course_progress`. Native iOS also has
`enable_retrospective`; Android has `enable_in_app_review` and the string allowlist
`in_app_review_triggers`. These keys already exist; this runbook adds none.
Document captcha keys configure the portal interaction and are not general
feature kill switches.

Android's existing update policy selects an immediate update for Play priority
4+ or 30-day staleness when allowed, and flexible updates for priority 2+ or
7-day staleness when allowed and not suppressed. See
[in-app updates](in-app-update.md). A remote minimum-supported-version gate and
an equivalent native iOS recovery path have not been introduced here.

## Verify and close

After mitigation or a corrected rollout, inspect fresh evidence for the specific
affected release and operation. Keep the incident open if telemetry is stale,
the corrected release has insufficient exposure, or affected users remain on the
broken version. Record whether the action was kept, adjusted or reverted, with
the next check time. Add the incident's synthetic fixture and regression to the
required suite before closing the repair work.

Numeric rollout halt thresholds, minimum sample volume, observation duration,
retention/access limits and response ownership need an explicit baseline and
maintainer decision. They are intentionally not guessed from the source tree.
