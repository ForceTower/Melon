import { expect, test } from "bun:test";
import { fixtureFindings, secretFindings } from "./content-safety";

test("detects credential classes without emitting the matched credential", () => {
  expect(secretFindings("ghp_" + "a".repeat(36))).toEqual(["github-token"]);
  expect(secretFindings("AKIA" + "A".repeat(16))).toEqual(["aws-access-key"]);
  expect(secretFindings("-----BEGIN " + "PRIVATE KEY-----")).toEqual(["private-key"]);
  expect(secretFindings(JSON.stringify({ type: "service_account" }))).toEqual([
    "google-service-key",
  ]);
  expect(
    secretFindings("eyJ" + "a".repeat(15) + "." + "b".repeat(15) + "." + "c".repeat(15)),
  ).toEqual(["jwt"]);
});

test("allows synthetic scenario tokens and public analytics client identifiers", () => {
  expect(secretFindings("synthetic-access-0 synthetic-refresh-0 phc_public pk_public")).toEqual([]);
});

test("rejects plausible personal identifiers in fixtures", () => {
  expect(fixtureFindings("student@" + "university.edu")).toHaveLength(1);
  expect(fixtureFindings("123.456." + "789-00")).toContain("fixture-cpf");
  expect(fixtureFindings('"enrollmentNumber": "' + "2026123456" + '"')).toHaveLength(1);
});

test("allows reserved synthetic addresses but rejects misleading domain suffixes", () => {
  expect(
    fixtureFindings("student@example.invalid student@example.com student@demo.invalid"),
  ).toEqual([]);
  expect(fixtureFindings("student@example.com.attacker.org")).toHaveLength(1);
});
