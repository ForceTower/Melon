export function secretFindings(source: string): string[] {
  const rules = [
    { name: "private-key", pattern: /-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----/ },
    {
      name: "github-token",
      pattern: /\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{60,})\b/,
    },
    { name: "aws-access-key", pattern: /\b(?:AKIA|ASIA)[A-Z0-9]{16}\b/ },
    { name: "google-service-key", pattern: /"type"\s*:\s*"service_account"/ },
    { name: "slack-token", pattern: /\bxox[baprs]-[A-Za-z0-9-]{20,}\b/ },
    { name: "jwt", pattern: /\beyJ[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\b/ },
  ];
  return rules.filter(({ pattern }) => pattern.test(source)).map(({ name }) => name);
}

export function fixtureFindings(source: string): string[] {
  const findings: string[] = [];
  const emails = source.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi) ?? [];
  if (
    emails.some(
      (email) =>
        !/@(?:[^@]+\.)?(?:example\.(?:com|org|net|invalid)|invalid|localhost)$/i.test(email),
    )
  ) {
    findings.push("fixture-email: use example.invalid or a reserved example domain");
  }
  if (/\b\d{3}\.\d{3}\.\d{3}-\d{2}\b/.test(source)) findings.push("fixture-cpf");
  if (
    /\b(?:cpf|enrollmentNumber|registrationNumber|matricula)["']?\s*[:=]\s*["']?\d{7,}/i.test(
      source,
    )
  ) {
    findings.push("fixture-identifier: use a clearly synthetic nonnumeric identifier");
  }
  return findings;
}
