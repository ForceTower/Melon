#!/usr/bin/env bash
set -euo pipefail

# Pinned by build, not version: the Xcode 27.1 beta (27A9269) and RC (27A9275)
# both report 27.1, and the beta isn't licensed as one, so `latest-stable` once
# archived a TestFlight build with it. App Store Connect needs a release Xcode.
release=27A9275
# The hosted xcode-27 image only ships the beta so far; drop this once it has the RC.
fallback=27A9269

build_of() {
  /usr/libexec/PlistBuddy -c "Print :ProductBuildVersion" "$1/Contents/version.plist" 2>/dev/null || true
}

select_build() {
  for app in /Applications/Xcode*.app; do
    if [[ $(build_of "$app") == "$1" ]]; then
      # Scoped to the job, unlike xcode-select, which switches the whole runner.
      echo "DEVELOPER_DIR=$app/Contents/Developer" >>"$GITHUB_ENV"
      printf 'Selected %s (%s)\n' "$app" "$1"
      return 0
    fi
  done
  return 1
}

select_build "$release" && exit 0
if select_build "$fallback"; then
  echo "::warning title=Prerelease Xcode::Xcode $release isn't on this runner, so this used $fallback. App Store review won't accept builds from it."
  exit 0
fi

printf 'Neither Xcode %s nor %s is installed on this runner. Installed:\n' "$release" "$fallback" >&2
for app in /Applications/Xcode*.app; do
  printf '  %s %s\n' "$app" "$(build_of "$app")" >&2
done
exit 1
