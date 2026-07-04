# iOS 27 experiment (spec 0001 / Phase 3 §3.8)

Compile-gated spikes — **nothing here ships**. Both files are inert unless
the `UNES_IOS27_EXPERIMENT` compilation condition is set, which requires the
iOS 27 SDK (Xcode-beta). The main scheme keeps building with stable Xcode
with all of this compiled away.

Build the experiment from the CLI (no project changes needed):

```sh
DEVELOPER_DIR=/Applications/Xcode-beta.app/Contents/Developer \
  xcodebuild -project UNES.xcodeproj -scheme UNES \
  -destination 'platform=iOS,name=<your device>' \
  -xcconfig UNES/Experimental/Experiment.xcconfig \
  -allowProvisioningUpdates build
```

(or open the project in Xcode-beta and add `UNES_IOS27_EXPERIMENT` to the
UNES target's Debug `SWIFT_ACTIVE_COMPILATION_CONDITIONS` — don't commit.)

Findings live in `docs/research/0001-siri-integration/research.md` §9.

- **E1 — done (2026-07-04):** gated scheme builds with Xcode 27.0
  (27A5209h); main scheme unaffected under stable Xcode.
- **E2 (build half) — done:** the spec's `@AssistantEntity(schema:
  .calendar.event)` premise was superseded — iOS 27's mechanism is the new
  `AppSchema` system, and **`@AppEntity(schema: .calendar.event)` accepts
  entity-only conformance**: it compiles, and the metadata exports
  `{domain: calendar, name: EventEntity}` with zero write intents in the
  target. `ExperimentalEvaluationEvent` maps evaluations without invented
  data (title + all-day start date; empty attendees/organizers/alarms; a
  stub "UNES" calendar entity; everything else nil). The required-field
  table came from the metadata processor's own errors and is recorded in
  research §9.
- **E2 (device half) — pending:** does Siri AI answer "when is my test
  from …" better through the schema entity than the plain `IndexedEntity`?
  Currently data-blocked — no scheduled pending evaluation exists until
  2026.2 posts one.
- **E3 — no match on iOS 27.0 beta (2026-07-04, iPhone Air):** the
  Shortcuts action **"E3: index message bodies"** indexed the 5 newest
  messages; a distinctive body word produced no system-wide match — same
  as the iOS 17–26 regression. Re-run on a later beta/RC before Phase 4
  decides; messages stay on classic `CSSearchableItem`s until then.
