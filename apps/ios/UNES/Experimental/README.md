# iOS 27 experiments (spec 0001)

Only the **E3 body-index driver** lives here now. It compiles in every
`DEBUG` build (no compilation condition, no xcconfig — the
`UNES_IOS27_EXPERIMENT` flag was retired on 2026-09-09) and is gated on
`@available(iOS 27, *)`. Release builds never contain it.

The E2 calendar types and the E4 messages types were promoted into the app
target as `UNES/UNESSchemaEntities.swift`.

Findings live in `docs/research/0001-siri-integration/research.md` §9.

- **E1 — done (2026-07-04):** the gated scheme built with the first Xcode 27
  beta; the main scheme was unaffected.
- **E2 — done:** `@AppEntity(schema: .calendar.event)` accepts entity-only
  conformance (no write intents). Promoted.
- **E3 — closed, still broken (2026-09-09, iPhone Air, iOS 27.0 24A435):**
  neither the shipped classic `CSSearchableItem` + `textContent` path nor
  `indexAppEntities` + `@Property(indexingKey: \.textContent)` produce a
  system-wide Spotlight match for a body word. Messages stay classic items
  for the Spotlight list; the messages-schema entity feeds Siri AI. To
  re-check on a new OS build: run the Shortcuts action **"E3: index
  message bodies"** on a debug build, then search a body word from one of
  the five newest messages.
- **E4 — passed (2026-09-09):** entity-only `.messages.message` validates
  with zero messages intents. Promoted.
