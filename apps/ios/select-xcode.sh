#!/usr/bin/env bash
set -euo pipefail

# Pinned by build, not version: the Xcode 27.1 beta (27A9269) and RC (27A9275)
# both report 27.1, and the beta isn't licensed as one, so `latest-stable` once
# archived a TestFlight build with it. App Store Connect needs a release Xcode.
build=27A9275

build_of() {
  /usr/libexec/PlistBuddy -c "Print :ProductBuildVersion" "$1/Contents/version.plist" 2>/dev/null || true
}

for app in /Applications/Xcode*.app; do
  if [[ $(build_of "$app") == "$build" ]]; then
    # Scoped to the job, unlike xcode-select, which switches the whole runner.
    echo "DEVELOPER_DIR=$app/Contents/Developer" >>"$GITHUB_ENV"
    printf 'Selected %s (%s)\n' "$app" "$build"
    exit 0
  fi
done

printf 'Xcode %s is not installed on this runner. Installed:\n' "$build" >&2
for app in /Applications/Xcode*.app; do
  printf '  %s %s\n' "$app" "$(build_of "$app")" >&2
done
exit 1
