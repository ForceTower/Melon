import { expect, test } from "bun:test";
import { backingTokenNames, inspectKotlin } from "./kotlin-conventions";

test("rejects colors, typography, inline text, descriptions, motion, and public declarations", () => {
  const findings = inspectKotlin(
    `
class Example {
  val ink = Color(0xFF010203)
  val white = Color.White
  val font = FontFamily.Default
  val color = BrandPlum
  fun render() {
    Text(text = "Olá")
    Icon(contentDescription = "Voltar")
    spring(stiffness = 900f)
  }
}
public fun visible() = Unit
val implicit = 1
`,
    new Set(["BrandPlum"]),
  );
  expect(findings.map((finding) => finding.rule)).toEqual([
    "explicit-visibility",
    "theme-color",
    "theme-color",
    "theme-typography",
    "theme-backing-token",
    "string-resource",
    "string-resource",
    "theme-motion",
    "explicit-visibility",
    "explicit-visibility",
  ]);
});

test("derives backing token names without treating unrelated Brand-prefixed functions as colors", () => {
  const names = backingTokenNames(
    "internal val BrandPlum = Color(0xFF123456)\nprivate val InkLight = Color.White",
  );
  expect(inspectKotlin("internal fun BrandRow() = Unit", names)).toEqual([]);
  expect(
    inspectKotlin("internal val color = InkLight", names).map((finding) => finding.rule),
  ).toEqual(["theme-backing-token"]);
});

test("accepts theme/resource usage, scoped declarations and nested members", () => {
  expect(
    inspectKotlin(`
internal data class Example(val name: String) {
  fun render() {
    Text(stringResource(R.string.hello))
    Icon(contentDescription = null)
    val color = MaterialTheme.colorScheme.primary
    val font = MaterialTheme.typography.bodyLarge
    val motion = MelonMotion.spring()
  }
}
private suspend fun load() = Unit
internal val result = 1
`),
  ).toEqual([]);
});

test("ignores comments, escaped quotes and braces inside literal strings", () => {
  const escapedQuote = String.fromCharCode(92, 34);
  expect(
    inspectKotlin(`
/* /* nested */ Color.Red and class Bad */
// Text("literal")
private val sample = "Color.White { class Bad ${escapedQuote} }"
private val multi = """Text("unsafe") { }"""
internal fun okay() = Unit
`),
  ).toEqual([]);
});

test("detects multiline string call and visibility modifiers on their own line", () => {
  const findings = inspectKotlin(`
internal
data class State(val name: String)
internal fun render() {
  Text(
    text = """Olá"""
  )
}
`);
  expect(findings.map((finding) => finding.rule)).toEqual(["string-resource"]);
});

test("accepts scoped functional interfaces and anonymous expressions", () => {
  expect(
    inspectKotlin(`
internal fun interface Listener { fun changed() }
private val action = fun() { }
private val listener = object : Listener { override fun changed() = Unit }
`),
  ).toEqual([]);
});
