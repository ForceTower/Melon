import { appendFile } from "node:fs/promises";
import { verificationPassed } from "./verification/affected-platforms";

function selected(value: string | undefined) {
  if (value !== "true" && value !== "false") throw new Error("Missing platform selection");
  return value === "true";
}
const selection = {
  android: selected(process.env.ANDROID_SELECTED),
  ios: selected(process.env.IOS_SELECTED),
  landing: selected(process.env.LANDING_SELECTED),
};
const results = {
  repository: process.env.REPOSITORY_RESULT ?? "missing",
  android: process.env.ANDROID_RESULT ?? "missing",
  ios: process.env.IOS_RESULT ?? "missing",
  landing: process.env.LANDING_RESULT ?? "missing",
};
console.log(JSON.stringify({ selection, results }, null, 2));
if (process.env.GITHUB_STEP_SUMMARY) {
  await appendFile(
    process.env.GITHUB_STEP_SUMMARY,
    "| Lane | Result |\n| --- | --- |\n" +
      Object.entries(results)
        .map(([name, result]) => `| ${name} | ${result} |\n`)
        .join(""),
  );
}
if (process.env.CHANGES_RESULT !== "success" || !verificationPassed(selection, results)) {
  throw new Error("Repository checks and every selected platform must succeed; see lane evidence.");
}
