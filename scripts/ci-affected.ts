import { appendFile } from "node:fs/promises";
import { affectedPlatforms } from "./verification/affected-platforms";

const base = process.env.CHANGE_BASE;
const head = process.env.CHANGE_HEAD;
let paths: string[] = [];
if (
  base &&
  head &&
  /^[a-f\d]{40}$/.test(base) &&
  /^[a-f\d]{40}$/.test(head) &&
  !/^0+$/.test(base)
) {
  const diff = Bun.spawnSync([
    "git",
    "diff",
    "--no-renames",
    "--name-only",
    "-z",
    `${base}...${head}`,
  ]);
  if (diff.exitCode !== 0) throw new Error("Could not determine changed files");
  paths = diff.stdout.toString().split("\0").filter(Boolean);
} else paths = ["<full-verification>"];
const selection = affectedPlatforms(paths);
console.log(JSON.stringify({ selection, changedFiles: paths.length }, null, 2));
if (process.env.GITHUB_OUTPUT) {
  await appendFile(
    process.env.GITHUB_OUTPUT,
    Object.entries(selection)
      .map(([key, value]) => `${key}=${value}\n`)
      .join(""),
  );
}
if (process.env.GITHUB_STEP_SUMMARY) {
  await appendFile(
    process.env.GITHUB_STEP_SUMMARY,
    `Platform selection: ${JSON.stringify(selection)} (${paths.length} changed paths). Repository checks always run.\n`,
  );
}
