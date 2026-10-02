Run one scenario from `docs/scenarios.md` using `bun run android:scenario $ARGUMENTS`.
Read `scripts/scenarios.ts` first, require an explicit connected-device serial,
inspect the generated screenshot and assertions, and report the artifact path.
Use the isolated scenario package and hermetic server. Never supply real credentials.
