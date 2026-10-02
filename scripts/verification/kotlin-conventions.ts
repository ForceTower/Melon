export interface Finding {
  rule: string;
  line: number;
  source: string;
}

interface Token {
  value: string;
  line: number;
  braces: number;
  parentheses: number;
  string: boolean;
}

// A lexer keeps comments and string contents out of the mechanical Kotlin checks.
function tokens(source: string): Token[] {
  const result: Token[] = [];
  let offset = 0;
  let line = 1;
  let braces = 0;
  let parentheses = 0;
  const advance = (end: number) => {
    line += source.slice(offset, end).split("\n").length - 1;
    offset = end;
  };
  while (offset < source.length) {
    const tail = source.slice(offset);
    if (/^\s/.test(tail)) {
      advance(offset + 1);
      continue;
    }
    if (tail.startsWith("//")) {
      const end = source.indexOf("\n", offset);
      advance(end < 0 ? source.length : end);
      continue;
    }
    if (tail.startsWith("/*")) {
      let depth = 1;
      let end = offset + 2;
      while (end < source.length && depth > 0) {
        if (source.startsWith("/*", end)) {
          depth++;
          end += 2;
        } else if (source.startsWith("*/", end)) {
          depth--;
          end += 2;
        } else end++;
      }
      advance(end);
      continue;
    }
    let end = offset + 1;
    const string = tail.startsWith('"');
    if (tail.startsWith('"""')) {
      const close = source.indexOf('"""', offset + 3);
      end = close < 0 ? source.length : close + 3;
    } else if (string || tail.startsWith("'")) {
      while (end < source.length) {
        if (source[end] === "\\") end += 2;
        else if (source[end++] === source[offset]) break;
      }
    } else {
      const word = tail.match(/^[\w]+/);
      if (word) end = offset + word[0].length;
    }
    const value = source.slice(offset, end);
    result.push({ value, line, braces, parentheses, string });
    if (value === "{") braces++;
    if (value === "}") braces--;
    if (value === "(") parentheses++;
    if (value === ")") parentheses--;
    advance(end);
  }
  return result;
}

export function backingTokenNames(source: string): Set<string> {
  const stream = tokens(source);
  return new Set(
    stream.flatMap((token, index) =>
      token.value === "val" && token.braces === 0 ? [stream[index + 1].value] : [],
    ),
  );
}

export function inspectKotlin(source: string, backingTokens = new Set<string>()): Finding[] {
  const stream = tokens(source);
  const lines = source.split("\n");
  const findings: Finding[] = [];
  const add = (rule: string, token: Token, detail = "") => {
    findings.push({ rule, line: token.line, source: lines[token.line - 1].trim() + detail });
  };
  for (const [index, token] of stream.entries()) {
    const next = stream[index + 1];
    const after = stream[index + 2];
    if (token.string) continue;
    if (token.value === "Color") {
      if (next?.value === "(" && /^(0x[\da-f]+|\d)/i.test(after?.value ?? "")) {
        add("theme-color", token);
      }
      // Transparent and Unspecified are allowed: they mean "no fill" and "use the default",
      // not colors a theme could own.
      if (
        next?.value === "." &&
        /^(Black|White|Red|Green|Blue|Yellow|Cyan|Magenta|Gray|LightGray|DarkGray)$/.test(
          after?.value ?? "",
        )
      ) {
        add("theme-color", token);
      }
    }
    if (token.value === "FontFamily" && (next?.value === "." || next?.value === "(")) {
      add("theme-typography", token);
    }
    if (backingTokens.has(token.value)) {
      add("theme-backing-token", token);
    }
    if (token.value === "spring" && next?.value === "(" && stream[index - 1]?.value !== ".") {
      add("theme-motion", token);
    }
    if (token.value === "Text" && next?.value === "(") {
      const first =
        after?.value === "text" && stream[index + 3]?.value === "=" ? stream[index + 4] : after;
      if (first?.string) add("string-resource", token, ` [${first.value}]`);
    }
    if (token.value === "contentDescription" && next?.value === "=" && after?.string) {
      add("string-resource", token);
    }
    if (
      token.braces === 0 &&
      token.parentheses === 0 &&
      /^(class|interface|object|fun|val|var|typealias)$/.test(token.value)
    ) {
      if (token.value === "fun" && (next?.value === "(" || next?.value === "interface")) continue;
      if (token.value === "object" && (next?.value === ":" || next?.value === "{")) continue;
      let cursor = index - 1;
      const modifiers = [];
      while (
        cursor >= 0 &&
        /^(public|private|internal|protected|data|sealed|enum|annotation|value|fun|inline|tailrec|suspend|operator|infix|const|lateinit|external|expect|actual|abstract|open|final)$/.test(
          stream[cursor].value,
        )
      ) {
        modifiers.push(stream[cursor--].value);
      }
      if (!modifiers.includes("internal") && !modifiers.includes("private")) {
        add("explicit-visibility", token);
      }
    }
  }
  return findings;
}

export function findingKey(path: string, finding: Finding): string {
  return `${path}:${finding.rule}:${finding.source}`;
}
