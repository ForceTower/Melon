import { fixtureFindings, secretFindings } from "./verification/content-safety";

const files = Bun.spawnSync([
  "git",
  "ls-files",
  "-z",
  "--cached",
  "--others",
  "--exclude-standard",
]);
if (files.exitCode !== 0) throw new Error("Could not enumerate repository files");
const errors: string[] = [];
const paths = new Set(files.stdout.toString().split("\0").filter(Boolean));
for (const path of paths) {
  if (
    !/\.(?:ts|tsx|js|jsx|mjs|cjs|json|jsonc|java|astro|sh|kt|kts|swift|xml|plist|yml|yaml|toml|md|txt|properties|env|pem|key|lock)$/.test(
      path,
    ) &&
    !/(^|\/)\.env(?:\.|$)/.test(path) &&
    path !== "gradlew"
  )
    continue;
  const file = Bun.file(path);
  if (!(await file.exists())) continue;
  const source = await file.text();
  for (const rule of secretFindings(source)) errors.push(`${path}: ${rule}`);
  if (
    /^contracts\/|fixtures?[/.-]|Fixtures?\.(?:kt|swift)$|^scripts\/mock-melon\.ts$/i.test(path)
  ) {
    for (const rule of fixtureFindings(source)) errors.push(`${path}: ${rule}`);
  }
  if (/^contracts\/v\d+\/.*\.json$/.test(path)) {
    const contract: unknown = JSON.parse(source);
    if (
      typeof contract !== "object" ||
      contract === null ||
      !("origin" in contract) ||
      typeof contract.origin !== "string" ||
      !/synthetic/i.test(contract.origin)
    ) {
      errors.push(`${path}: contract origin must describe its synthetic provenance`);
    }
  }
}
if (errors.length > 0) {
  console.error(errors.join("\n"));
  process.exit(1);
}
console.log("Fixture identifiers, synthetic contract provenance, and credential patterns passed.");
