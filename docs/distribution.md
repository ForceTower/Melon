# Internal distribution

Every push to `main` that passes **Required verification** sends the affected apps
to internal testers. The `Distribute` workflow (`.github/workflows/distribute.yml`)
runs the fastlane lanes in `fastlane/Fastfile` for the platforms that
`scripts/ci-affected.ts` selected for that push.

| Platform | Lane | Destination | Signing |
| --- | --- | --- | --- |
| iOS | `fastlane ios beta` | TestFlight, internal testers | fastlane match App Store certificate and profiles |
| Android | `fastlane android beta` | Play internal testing track | Upload key from the `stores` environment |

To upload a commit whose run was superseded or skipped, run **Distribute** from the
Actions tab on `main` and pick the platforms. Review submission and production
rollout are not automated; promote builds from App Store Connect and Play Console.

## Versions and build numbers

Marketing versions are still bumped by hand: `MARKETING_VERSION` in the Xcode project
and `marketingVersion` in `apps/android/app/build.gradle.kts`. Build numbers come from
`git rev-list --count HEAD`. iOS uses the count as the build number, and Android adds it
to 2130000 for the version code, so every `main` commit gets a unique, increasing build.

App Store Connect stops accepting builds of a version once it is approved. Bump
`MARKETING_VERSION` after a release goes out, or the next TestFlight upload fails.

## Signing

The Xcode project keeps automatic signing for local development and Xcode archives.
On CI, `ios beta` fetches the App Store certificate and profiles read-only from the
private match repository, then archives with a temporary xcconfig that switches every
target to manual signing with the match profile for its bundle ID. CI cannot create
or revoke certificates; only `ios certificates`, run locally, writes to match.

Android release builds are signed with the upload key only when
`ANDROID_UPLOAD_KEYSTORE_FILE` and its password variables are set. Without them,
release builds stay unsigned, as before, and local store builds are signed from
Android Studio.

## Secret exposure

Store credentials live only in the `stores` environment, which deploys from `main`
alone, so PR branches and manual runs elsewhere cannot read them. Pull requests
from forks never receive secrets, and no workflow uses `pull_request_target` or
`workflow_run`. `Verify` passes `secrets: inherit` to `Distribute` because called
workflows otherwise see environment secrets as empty
([actions/runner#4453](https://github.com/actions/runner/issues/4453)); that also
hands over repository-level secrets, so keep credentials out of the repository
scope and in `stores`.

## One-time setup

1. Install the repository Ruby and gems with `mise install` and `bundle install`.
   mise 2026.8 or newer installs a precompiled Ruby; older versions compile it, so
   update mise or run `mise settings ruby.compile=false` first.
2. Create the private repository `ForceTower/melon-certificates` (the `git_url` in
   `fastlane/Matchfile`). Add a read-only deploy key and keep its private half for CI.
3. Run `bundle exec fastlane ios certificates`. It signs in with your Apple ID, asks
   for the passphrase that encrypts the repository, and creates an Apple Distribution
   certificate plus App Store profiles for the app, widgets, watch app and watch
   widgets.
4. In App Store Connect, create an API key with the App Manager role, and enable
   automatic distribution on the internal TestFlight group that should get builds.
5. In Play Console, give a Google Cloud service account permission to release to
   testing tracks, and export its JSON key.
6. Create the GitHub environment `stores`, restrict its deployment branches to `main`,
   and add these secrets:

| Secret | Value |
| --- | --- |
| `ASC_KEY_ID` | App Store Connect API key ID |
| `ASC_ISSUER_ID` | App Store Connect API issuer ID |
| `ASC_KEY_P8` | Contents of the `.p8` API key file |
| `MATCH_PASSWORD` | Passphrase chosen in step 3 |
| `MATCH_GIT_PRIVATE_KEY` | Private half of the certificates repository deploy key |
| `PLAY_SERVICE_ACCOUNT_JSON` | Service account JSON key |
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | Output of `base64 -i upload.jks` |
| `ANDROID_UPLOAD_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_UPLOAD_KEY_ALIAS` | Upload key alias |
| `ANDROID_UPLOAD_KEY_PASSWORD` | Upload key password (the keystore password for PKCS12 keystores) |

## Maintenance

Adding a capability, App Group or entitlement in Xcode updates the App ID and
invalidates the match profiles, so the next TestFlight upload fails to sign.
Regenerate them locally:

```sh
MATCH_FORCE=true bundle exec fastlane ios certificates
```

A new shipped target also needs its bundle ID in `IOS_BUNDLE_IDS` in the Fastfile
before regenerating. The distribution certificate expires after a year; renew it and
its profiles with:

```sh
MATCH_RENEW_EXPIRED_CERTS=true MATCH_FORCE=true bundle exec fastlane ios certificates
```

The archive job selects the newest stable Xcode on the `xcode-27` runner, because
App Store Connect rejects builds made with a beta Xcode.
