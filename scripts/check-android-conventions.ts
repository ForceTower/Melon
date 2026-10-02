import { backingTokenNames, findingKey, inspectKotlin } from "./verification/kotlin-conventions";
import baseline from "./verification/android-conventions-baseline.json";

const glob = new Bun.Glob("apps/android/app/src/{main,debug,scenario}/**/*.kt");
const themePath =
  "apps/android/design-system/src/main/kotlin/dev/forcetower/unes/designsystem/theme";
const backing = backingTokenNames(
  (await Bun.file(`${themePath}/Color.kt`).text()) +
    "\n" +
    (await Bun.file(`${themePath}/Fonts.kt`).text()),
);
const findings = [];
for await (const path of glob.scan(".")) {
  for (const finding of inspectKotlin(await Bun.file(path).text(), backing)) {
    findings.push({ path, ...finding });
  }
}
findings.sort((a, b) => a.path.localeCompare(b.path) || a.line - b.line);
const counts: Record<string, number> = {};
for (const finding of findings) {
  const key = findingKey(finding.path, finding);
  counts[key] = (counts[key] ?? 0) + 1;
}
if (process.argv.includes("--inventory")) {
  console.log(JSON.stringify(counts, null, 2));
  process.exit(0);
}
const remaining = new Map(Object.entries(baseline));
const errors = [];
for (const finding of findings) {
  const key = findingKey(finding.path, finding);
  const allowance = remaining.get(key) ?? 0;
  if (allowance > 0) remaining.set(key, allowance - 1);
  else errors.push(`${finding.path}:${finding.line}: ${finding.rule}`);
}
if (errors.length > 0) {
  console.error(errors.join("\n"));
  console.error(
    "Use theme tokens, string resources and internal/private declarations. See docs/testing.md.",
  );
  process.exit(1);
}
console.log(
  `Android conventions passed (${findings.length} known findings; baseline additions require review).`,
);
