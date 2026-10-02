import { mkdir } from "node:fs/promises";
import { parseArgs } from "node:util";
import { createMockHandler } from "./mock-melon";
import { isScenarioId, pilot, scenarioCatalog } from "./scenarios";

const { values, positionals } = parseArgs({
  args: process.argv.slice(2),
  allowPositionals: true,
  options: {
    serial: { type: "string" },
    "no-build": { type: "boolean" },
    render: { type: "boolean" },
  },
});
const id = positionals[0] ?? "home.populated";
if (!isScenarioId(id))
  throw new Error(`Unknown scenario: ${id}. Choose ${scenarioCatalog.map((s) => s.id).join(", ")}`);
const serial = values.serial ?? process.env.ANDROID_SERIAL;
if (!serial || !/^[\w.:-]+$/.test(serial))
  throw new Error(
    "Specify a connected device with --serial DEVICE_ID. Only the isolated scenario package is cleared.",
  );

const packageId = "com.forcetower.uefs.scenario";
const scenarioName = values.render ? "home.render" : id;
const runId = Date.now().toString();
const directory = `artifacts/android/${scenarioName}`;
const output = `${directory}/runs/${runId}`;
const reproduction = `bun run android:scenario ${id}${values.render ? " --render" : ""} --serial ${serial}`;
await mkdir(output, { recursive: true });
const adb = (...args: string[]) => run(["adb", "-s", serial, ...args]);

async function run(command: string[], timeoutMs = 120_000) {
  const child = Bun.spawn(command, { stdout: "pipe", stderr: "pipe" });
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

if (!values["no-build"]) {
  console.log("Building isolated scenario app and instrumentation…");
  await run(
    [
      "./gradlew",
      ":apps:android:app:assembleScenario",
      ":apps:android:app:assembleScenarioAndroidTest",
      "-Pmelon.testBuildType=scenario",
      "--console=plain",
      "-q",
    ],
    1_200_000,
  );
}

const handler = createMockHandler();
const server = Bun.serve({ hostname: "127.0.0.1", port: 8787, fetch: handler });
let passed = false;
let failure = "";
try {
  await adb("wait-for-device");
  await adb("reverse", "tcp:8787", "tcp:8787");
  await adb("install", "-r", "-t", "apps/android/app/build/outputs/apk/scenario/app-scenario.apk");
  await adb(
    "install",
    "-r",
    "-t",
    "apps/android/app/build/outputs/apk/androidTest/scenario/app-scenario-androidTest.apk",
  );
  await adb("shell", "pm", "clear", packageId);
  await adb("shell", "cmd", "locale", "set-app-locales", packageId, "--locales", "pt-BR");
  const result = await adb(
    "shell",
    "am",
    "instrument",
    "-w",
    "-r",
    "-e",
    "class",
    values.render
      ? "dev.forcetower.unes.OverviewContentTest"
      : "dev.forcetower.unes.ScenarioJourneyTest",
    "-e",
    "scenario",
    id,
    `${packageId}.test/androidx.test.runner.AndroidJUnitRunner`,
  );
  await Bun.write(`${output}/instrumentation.txt`, result);
  passed =
    /OK \([12] tests?\)/.test(result) &&
    !/FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=/.test(result);
  if (!passed) failure = result;
} catch (error) {
  failure = error instanceof Error ? error.message : String(error);
} finally {
  const captureErrors: string[] = [];
  try {
    await adb("pull", `/sdcard/Android/data/${packageId}/files/scenario-evidence/.`, output);
  } catch (error) {
    captureErrors.push(String(error));
  }
  try {
    const pid = await adb("shell", "pidof", packageId)
      .then((value) => value.trim())
      .catch(() => "");
    if (pid)
      await Bun.write(
        `${output}/logcat.txt`,
        await adb("logcat", "-d", "--pid", pid, "-v", "threadtime"),
      );
  } catch (error) {
    captureErrors.push(String(error));
  }
  const state = await (await handler(new Request("http://localhost/debug/state"))).text();
  await Bun.write(`${output}/server-state.json`, state);
  const screenshots = Array.from(new Bun.Glob("*.png").scanSync({ cwd: output })).sort();
  if (passed && screenshots.length === 0) {
    passed = false;
    failure = "Assertions passed but screenshot evidence is missing";
  }
  await Bun.write(
    `${output}/result.json`,
    JSON.stringify(
      {
        scenario: scenarioName,
        passed,
        failure,
        captureErrors,
        fixtureVersion: pilot.version,
        fixtureFiles: id.startsWith("enrollment.")
          ? ["contracts/v1/pilot.json", "contracts/v1/enrollment.json"]
          : ["contracts/v1/pilot.json"],
        featureFlags: { enrollment: id.startsWith("enrollment.") },
        clock: pilot.clock,
        timezone: pilot.timezone,
        serial,
        commit: (await run(["git", "rev-parse", "HEAD"])).trim(),
        worktreeDirty: (await run(["git", "status", "--porcelain"])).trim().length > 0,
        device: (await adb("shell", "getprop", "ro.build.fingerprint")).trim(),
        reproduction,
      },
      null,
      2,
    ),
  );
  const evidence = Array.from(new Bun.Glob("*.{json,txt,xml}").scanSync({ cwd: output })).sort();
  await Bun.write(
    `${output}/index.html`,
    `<!doctype html><html lang="en"><meta charset="utf-8"><title>${scenarioName}</title>
<style>body{font:16px system-ui;max-width:1000px;margin:2rem auto}img{max-width:420px;width:100%}a{margin-right:1rem}</style>
<h1>${scenarioName}</h1><p>${passed ? "Passed" : "Failed"} · Fixture v${pilot.version} · ${pilot.clock} · ${pilot.timezone}</p>
<p><code>${reproduction}</code></p>
<p>${evidence.map((name) => `<a href="${name}">${name}</a>`).join(" ")}</p>
${screenshots.map((name) => `<figure><img src="${name}" alt="${name}"><figcaption>${name}</figcaption></figure>`).join("\n")}</html>`,
  );
  await Bun.write(
    `${directory}/index.html`,
    `<!doctype html><html lang="en"><meta charset="utf-8"><title>${scenarioName}</title><h1>${scenarioName}</h1><a href="runs/${runId}/index.html">Latest run: ${passed ? "passed" : "failed"}</a><p>Earlier attempts remain under runs/.</p></html>`,
  );
  await adb("reverse", "--remove", "tcp:8787").catch(() => {});
  await server.stop(true);
}
if (!passed) {
  console.error(failure);
  process.exitCode = 1;
}
console.log(`${passed ? "PASS" : "FAIL"} ${scenarioName}: ${output}/index.html`);
