import { expect, test } from "bun:test";
import { affectedPlatforms, verificationPassed } from "./affected-platforms";

test("documentation-only changes leave platform jobs unselected", () => {
  expect(affectedPlatforms(["docs/testing.md", ".github/pull_request_template.md"])).toEqual({
    android: false,
    ios: false,
    landing: false,
  });
});

test("KMP and Gradle configuration select Android without requiring macOS", () => {
  expect(
    affectedPlatforms([
      "packages/shared-kmp/core/network/build.gradle.kts",
      "gradle/libs.versions.toml",
    ]),
  ).toEqual({ android: true, ios: false, landing: false });
});

test("native iOS is independent and landing files select the site", () => {
  expect(
    affectedPlatforms(["apps/ios/UNESKit/Package.swift", "apps/landing/src/index.astro"]),
  ).toEqual({ android: false, ios: true, landing: true });
});

test("shared fixtures, unknown root config and workflows conservatively select all", () => {
  for (const path of [
    "contracts/v1/pilot.json",
    "scripts/scenarios.ts",
    ".mise.toml",
    "bun.lock",
    ".github/workflows/verify.yml",
  ]) {
    expect(affectedPlatforms([path])).toEqual({ android: true, ios: true, landing: true });
  }
});

test("the gate rejects failed, cancelled, missing and unexpectedly skipped selected lanes", () => {
  const selection = { android: true, ios: false, landing: false };
  const results = { repository: "success", android: "success", ios: "skipped", landing: "skipped" };
  expect(verificationPassed(selection, results)).toBe(true);
  for (const result of ["failure", "cancelled", "skipped", ""]) {
    expect(verificationPassed(selection, { ...results, android: result })).toBe(false);
  }
  expect(verificationPassed(selection, { ...results, repository: "failure" })).toBe(false);
});
