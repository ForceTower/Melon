import { mkdir } from "node:fs/promises";
import { parseArgs } from "node:util";
import { createMockHandler } from "./mock-melon";
import { pilot } from "./scenarios";

const { values } = parseArgs({
  args: process.argv.slice(2),
  options: { serial: { type: "string" }, profile: { type: "boolean" } },
});
const serial = values.serial ?? process.env.ANDROID_SERIAL;
if (!serial || serial.startsWith("emulator-")) {
  throw new Error(
    "Specify the dedicated physical device with --serial. Emulator timing is not accepted.",
  );
}
const variant = values.profile ? "Profile" : "Benchmark";
const suite = values.profile ? "HomeBaselineProfile" : "HomeBenchmark";
const packageId = "com.forcetower.uefs.benchmark";
const runId = new Date().toISOString().replace(/[:.]/g, "-");
const output = `artifacts/android-performance/${variant.toLowerCase()}/${runId}`;
const adb = (...args: string[]) => run(["adb", "-s", serial, ...args]);

async function run(command: string[], timeoutMs = 120_000) {
  const child = Bun.spawn(command, {
    env: { ...process.env, ANDROID_SERIAL: serial },
    stdout: "pipe",
    stderr: "pipe",
  });
  const timer = setTimeout(() => child.kill(), timeoutMs);
  try {
    const [stdout, stderr, exitCode] = await Promise.all([
      new Response(child.stdout).text(),
      new Response(child.stderr).text(),
      child.exited,
    ]);
    if (exitCode !== 0)
      throw new Error(`${command.join(" ")} failed (${exitCode})\n${stdout}\n${stderr}`);
    return stdout;
  } finally {
    clearTimeout(timer);
  }
}

if ((await adb("shell", "getprop", "ro.kernel.qemu")).trim() === "1") {
  throw new Error("A physical device is required; benchmark suppression flags are not used.");
}
if (Number((await adb("shell", "getprop", "ro.build.version.sdk")).trim()) < 33) {
  throw new Error("Use Android 13/API 33 or newer for unrooted Baseline Profile collection.");
}
await mkdir(output, { recursive: true });
const handler = createMockHandler();
const server = Bun.serve({ hostname: "127.0.0.1", port: 8787, fetch: handler });
let passed = false;
let failure = "";
try {
  await adb("reverse", "tcp:8787", "tcp:8787");
  console.log(`Building ${variant.toLowerCase()} target…`);
  await run(["./gradlew", `:apps:android:app:assemble${variant}`, "--console=plain"], 1_200_000);
  await adb(
    "install",
    "-r",
    "-t",
    `apps/android/app/build/outputs/apk/${variant.toLowerCase()}/app-${variant.toLowerCase()}.apk`,
  );
  await adb("shell", "pm", "clear", packageId);
  await adb("shell", "cmd", "locale", "set-app-locales", packageId, "--locales", "pt-BR");
  await handler(new Request("http://localhost/debug/reset", { method: "POST" }));
  console.log(`Running ${suite} on the physical device…`);
  const result = await run(
    [
      "./gradlew",
      `:apps:android:benchmark:connected${variant}AndroidTest`,
      `-Pandroid.testInstrumentationRunnerArguments.class=dev.forcetower.unes.benchmark.${suite}`,
      "--console=plain",
      "--stacktrace",
    ],
    1_800_000,
  );
  await Bun.write(`${output}/instrumentation.txt`, result);
  const state: unknown = await (await handler(new Request("http://localhost/debug/state"))).json();
  if (
    typeof state !== "object" ||
    state === null ||
    !("data" in state) ||
    typeof state.data !== "object" ||
    state.data === null ||
    !("unexpectedRequests" in state.data) ||
    !Array.isArray(state.data.unexpectedRequests) ||
    state.data.unexpectedRequests.length !== 0
  ) {
    throw new Error("Benchmark made unmocked requests; inspect server-state.json");
  }
  passed = true;
} catch (error) {
  failure = error instanceof Error ? error.message : String(error);
  await Bun.write(`${output}/failure.txt`, failure);
} finally {
  await Bun.write(
    `${output}/server-state.json`,
    await (await handler(new Request("http://localhost/debug/state"))).text(),
  );
  await Bun.write(
    `${output}/run.json`,
    JSON.stringify(
      {
        passed,
        failure,
        runId,
        variant,
        suite,
        serial,
        fixtureVersion: pilot.version,
        clock: pilot.clock,
        timezone: pilot.timezone,
        commit: (await run(["git", "rev-parse", "HEAD"]).catch(() => "unavailable")).trim(),
        worktreeDirty:
          (await run(["git", "status", "--porcelain"]).catch(() => "unknown")).trim() !== "",
        fingerprint: (
          await adb("shell", "getprop", "ro.build.fingerprint").catch(() => "unavailable")
        ).trim(),
        battery: await adb("shell", "dumpsys", "battery").catch(() => "unavailable"),
        reproduction: `bun run android:performance --serial ${serial}${values.profile ? " --profile" : ""}`,
      },
      null,
      2,
    ),
  );
  await adb("reverse", "--remove", "tcp:8787").catch(() => {});
  await server.stop(true);
}
if (!passed) {
  const summary = failure.match(
    /\* What went wrong:\n([\s\S]*?)(?=\n\* Try:|\n\* Exception is:)/,
  )?.[1];
  console.error(summary ?? failure.split("\n").slice(-55).join("\n"));
  console.error(`Full output: ${output}/failure.txt`);
  process.exitCode = 1;
}
console.log(`${passed ? "PASS" : "FAIL"} ${suite}: ${output}`);
