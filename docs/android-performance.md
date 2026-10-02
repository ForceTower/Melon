# Android performance pilot

The `apps/android/benchmark` test APK drives an isolated app package,
`com.forcetower.uefs.benchmark`, against the hermetic mock. It signs in with the
synthetic account and exercises real startup, navigation and cached Home
rendering. It never installs or clears the production package.

`HomeBenchmark` records ten cold starts and five Home scrolling iterations using
Macrobenchmark's startup/frame timing metrics. Sign-in and initial sync occur
outside the measured blocks. The measured app uses release minification and
resource shrinking, is profileable and is not debuggable. The test APK itself is
debuggable. Telemetry, push and remote settings are isolated as in scenario runs.
Results use `CompilationMode.None` as an explicit pre-profile baseline.

`HomeBaselineProfile` uses the same startup/Home journey against the nonminified,
nondebuggable `profile` variant so generated rules use source class names. It
collects a Baseline Profile and startup profile; it does not fabricate or
automatically accept a profile. This pilot's small synthetic semester is not a
representative benchmark of the heaviest lists. Add realistic synthetic workload
sizes and their journeys before using it to assess list-performance regressions.

## Run on a dedicated device

Install the repository toolchains and Android SDK, connect an unlocked physical
Android 13/API 33 or newer device, and ensure no other service uses port 8787.
Choose one device, fixed OS/build, thermal state, battery policy and display
settings for comparable measurements. A rooted device is not required at API 33+.
Keep system animations enabled for performance measurements.

```sh
mise exec -- bun run android:performance --serial YOUR_DEVICE_SERIAL
mise exec -- bun run android:performance --serial YOUR_DEVICE_SERIAL --profile
```

The runner rejects emulators, sets the isolated app's locale to pt-BR, clears only
its synthetic state, forwards localhost to the mock, and fails on unknown API
requests. Benchmark library device/thermal warnings are not suppressed. A clear
or unsupported environment should fail instead of producing comparable-looking
numbers. Do not run the scenario runner and performance runner concurrently.

Raw Perfetto traces, benchmark JSON and generated profile text live below
`apps/android/benchmark/build/outputs/connected_android_test_additional_output`.
JUnit results and HTML reports live in that module's usual build output folders.
`artifacts/android-performance` keeps a separate timestamped directory for each
attempt, so a passing rerun retains the first failure. It records commit and
dirty-worktree status, fixture version, fixed clock,
device fingerprint, battery status, reproduction command and mock requests.

Inspect generated `*-baseline-prof.txt` and `*-startup-prof.txt` artifacts before
copying approved rules into the app's `src/main/baselineProfiles/` source set.
Profile generation uses isolated dependencies, so review the rules against the
production journey and measure their benefit before shipping them. Generated
profiles remain run artifacts until that review and comparison are complete.
Once an approved profile exists, add a comparison using `CompilationMode.Partial` with
`BaselineProfileMode.Require`, which fails if the profile cannot be installed.

## CI enablement and limitations

The manual **Android performance** workflow targets a dedicated self-hosted Linux
runner with label `android-performance`. Set `ANDROID_PERFORMANCE_SERIAL` as a
repository variable and install the Android SDK/connected device on that runner.
The workflow serializes runs and uploads raw results for 30 days. It is not a PR
timing gate and does not automatically run untrusted pull-request code on that
device.

No consistent device, measured baseline, noise tolerance, sustained-regression
threshold or nightly schedule is selected by these files. Provision the runner,
measure repeatability and review several comparable runs before adding those
policies. Keep first-attempt failures and the raw traces. A trend service or repair
ticket workflow must ingest the JSON with device, app commit and fixture identity;
this module does not claim to detect production regressions on its own.

The local pilot on October 2, 2026 passed both timing tests and both profile
collection tests on a Pixel 9 Pro XL running API 37. Startup and Home profile
collection converged without suppressing benchmark errors. This verifies the
commands and produces reviewable artifacts; it is not a release performance
baseline or evidence that generated profiles improve a production build.

The implementation follows Android's [Macrobenchmark guide](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
and [Baseline Profile generation guide](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile).
