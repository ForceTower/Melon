export function affectedPlatforms(paths: string[]) {
  const selected = { android: false, ios: false, landing: false };
  for (const path of paths) {
    if (
      /^(docs\/|\.github\/(?:ISSUE_TEMPLATE\/|pull_request_template\.md))/.test(path) ||
      /^(README|LICENSE)(?:\.|$)/.test(path)
    )
      continue;
    if (
      path.startsWith("apps/android/") ||
      path.startsWith("packages/shared-kmp/") ||
      /^(build-logic\/|gradle\/|gradlew(?:\.bat)?$|.*\.gradle\.kts$|gradle\.properties$)/.test(path)
    ) {
      selected.android = true;
    } else if (path.startsWith("apps/ios/")) selected.ios = true;
    else if (path.startsWith("apps/landing/")) selected.landing = true;
    else {
      selected.android = true;
      selected.ios = true;
      selected.landing = true;
    }
  }
  return selected;
}

export function verificationPassed(
  selection: ReturnType<typeof affectedPlatforms>,
  results: Record<string, string>,
) {
  return (
    results.repository === "success" &&
    Object.entries(selection).every(([platform, required]) =>
      required
        ? results[platform] === "success"
        : results[platform] === "skipped" || results[platform] === "success",
    )
  );
}
