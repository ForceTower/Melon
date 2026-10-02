import { mkdir } from "node:fs/promises";

await mkdir("artifacts", { recursive: true });
const commands = [
  [
    "test",
    "scripts/verification",
    "scripts/mock-melon.test.ts",
    "--reporter=junit",
    "--reporter-outfile=artifacts/repository-tests.xml",
  ],
  ["run", "check:conventions"],
  ["run", "check:fixtures"],
];
for (const command of commands) {
  const child = Bun.spawn([process.execPath, ...command], { stdout: "inherit", stderr: "inherit" });
  const exitCode = await child.exited;
  if (exitCode !== 0) process.exit(exitCode);
}
