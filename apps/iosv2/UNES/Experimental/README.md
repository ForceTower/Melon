# iOS 27 experiment (spec 0001 / Phase 3 §3.8)

Compile-gated spikes — **nothing here ships**. Both files are inert unless
the `UNES_IOS27_EXPERIMENT` compilation condition is set, which requires the
iOS 27 SDK (Xcode-beta). The main scheme keeps building with stable Xcode
with all of this compiled away.

To run the experiment locally:

1. Open the project with Xcode-beta (iOS 27 SDK).
2. Add `Experiment.xcconfig` as the Debug configuration file of a local
   scheme copy (or add `UNES_IOS27_EXPERIMENT` to
   `SWIFT_ACTIVE_COMPILATION_CONDITIONS` for the UNES target). Do not commit
   the scheme.
3. Build to answer E1/E2; run on the iOS 27 device for E3.

Deliverable is a verdict, not a merge — findings go into
`docs/research/0001-siri-integration/research.md` §9:

- **E2 — calendar entity-only conformance** (`ExperimentalEvaluationEvent`):
  does the build-time schema-group validator accept an entity with none of
  the calendar domain's write intents? Do the required fields map from what
  we have (title, start date — no invented data)? Does Siri AI answer
  evaluation questions better than with the plain `IndexedEntity`?
- **E3 — message body lexical search**
  (`ExperimentalIndexedMessageEntity`): with
  `@Property(indexingKey: \.textContent)` and `indexAppEntities`, does
  system-wide search match body words on device? If yes, Phase 4 adopts
  this as the permanent `#available(iOS 27)` path and Phase 2 criterion 3's
  blocked half finally closes.
