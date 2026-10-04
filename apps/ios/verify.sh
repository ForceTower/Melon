#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd)
lane=${1:-kit}
destination=${IOS_DESTINATION:-}
if [[ -z "$destination" ]]; then
  simulator_id=$(xcrun simctl list devices available --json | bun -e '
    const { devices } = await Bun.stdin.json();
    const simulator = Object.entries(devices)
      .filter(([runtime]) => runtime.includes("SimRuntime.iOS-27"))
      .flatMap(([, entries]) => entries)
      .find(device => device.isAvailable && device.name.startsWith("iPhone"));
    if (!simulator) { console.error("Install an iOS 27 iPhone simulator or set IOS_DESTINATION."); process.exit(1); }
    console.log(simulator.udid);
  ')
  destination="platform=iOS Simulator,id=$simulator_id"
fi
artifacts=${IOS_ARTIFACTS:-$root/artifacts/ios}
mkdir -p "$artifacts"
common=(-destination "$destination" -skipMacroValidation -skipPackagePluginValidation -quiet)
tests=(-testLanguage pt-BR -testRegion BR -parallel-testing-enabled NO)

case "$lane" in
  app)
    cd "$root/apps/ios"
    # Same simulator and derived data as `ui`, so the journeys reuse this build
    # instead of recompiling (a generic destination also builds x86_64).
    xcodebuild build -project UNES.xcodeproj -scheme UNES -configuration Debug "${common[@]}" \
      -derivedDataPath .derivedData \
      CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO 2>&1 | tee "$artifacts/app.log"
    ;;
  kit)
    cd "$root/apps/ios/UNESKit"
    result="$artifacts/kit-$(date +%s).xcresult"
    xcodebuild test -scheme UNESKit "${common[@]}" "${tests[@]}" \
      -resultBundlePath "$result" 2>&1 | tee "$artifacts/kit.log"
    xcrun xcresulttool get test-results summary --path "$result" >"$artifacts/kit-summary.json"
    ;;
  ui)
    cd "$root"
    if lsof -nP -iTCP:8788 -sTCP:LISTEN >/dev/null; then
      printf 'Port 8788 is already in use; stop that server before running the iOS UI suite.\n' >&2
      exit 1
    fi
    MELON_MOCK_PORT=8788 bun scripts/mock-melon.ts >"$artifacts/mock.log" 2>&1 &
    mock_pid=$!
    trap 'kill "$mock_pid" 2>/dev/null || true' EXIT
    for attempt in {1..30}; do
      kill -0 "$mock_pid"
      if curl --fail --silent http://127.0.0.1:8788/debug/state >/dev/null; then break; fi
      sleep 1
    done
    curl --fail --silent http://127.0.0.1:8788/debug/state >/dev/null
    cd "$root/apps/ios"
    result="$artifacts/ui-$(date +%s).xcresult"
    xcodebuild test -project UNES.xcodeproj -scheme UNES "${common[@]}" "${tests[@]}" \
      -only-testing:UNESIntentTests -derivedDataPath .derivedData \
      -resultBundlePath "$result" \
      CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO 2>&1 | tee "$artifacts/ui.log"
    xcrun xcresulttool get test-results summary --path "$result" >"$artifacts/ui-summary.json"
    xcrun xcresulttool export attachments --path "$result" --output-path "$artifacts/ui-attachments"
    ;;
  *) printf 'Usage: bash apps/ios/verify.sh app|kit|ui\n' >&2; exit 2 ;;
esac
