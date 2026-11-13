# GitHub builds and releases

Repository: https://github.com/K33k0/zelo (private).

## Every push and pull request

The CI workflow runs on `main` pushes, pull requests, and manual dispatch. It installs locked dependencies, checks formatting, runs unit/browser/native regressions, builds the Android debug app, runs lint, and compiles the device test APK. It uploads a preview APK and reports for 14 days. Device tests still require a phone/emulator; CI compiles them but does not execute them.

Preview APKs use a disposable runner debug key. They are intended for a clean test device/emulator, and cannot update an existing installation signed with the personal key. Use tagged release APKs to update your phone. Never uninstall your existing app just to try a preview without exporting your progress first.

Actions are pinned to exact commits; Dependabot checks Actions and npm monthly. Workflows run with read-only repository access except the final release job, which needs write access to publish its APK and checksum. Pull requests never receive signing secrets.

## Signing

The four Actions repository secrets are `ZELO_KEYSTORE_BASE64`, `ZELO_KEYSTORE_PASSWORD`, `ZELO_KEY_ALIAS`, and `ZELO_KEY_PASSWORD`. The key is the original personal debug signing identity, reused intentionally so installed Stride/Zelo builds can update without losing private data. This is personal APK distribution, not a Play Store production-signing setup. Do not rotate or regenerate it casually.

Original certificate SHA-256: `C0:30:4F:E6:B9:34:0C:D7:5F:C8:C3:D7:E0:F6:CC:FB:40:1F:49:6F:A0:CF:73:A0:44:71:F5:D7:70:D4:95:2E`.

The local copy lives at ignored `.tools/signing/zelo.keystore`; it is never committed or uploaded as a build artifact. Local debug and release builds use that file when present. Alternatively set `ZELO_KEYSTORE_PATH`, `ZELO_KEYSTORE_PASSWORD`, `ZELO_KEY_ALIAS`, and `ZELO_KEY_PASSWORD`. On a new checkout, restore the original key from your secure backup before installing local updates. Without a configured key, debug builds use a fresh debug key and release builds are unsigned. Tagged release jobs fail if any signing secret is missing, and verify the signed APK before publishing.

Keep a secure backup of the original keystore outside this checkout. GitHub secrets cannot be downloaded as a substitute for that backup. The runner restores the secret only for the release job and removes its temporary file afterwards.

## Check signing without publishing

Run the **Release APK** workflow manually on `main` in Actions (or use `gh workflow run release.yml --repo K33k0/zelo --ref main`). This runs CI, builds a signed APK with the repository secrets, verifies its certificate against the original identity, and uploads a signed artifact with checksums. A manual main run does not publish a Release. This is useful after restoring signing secrets or changing the build pipeline.

## Publish a version

1. Update `versionName` and increase `versionCode` in `app/build.gradle`; match the version in `package.json`, `package-lock.json`, the app header, the Settings footer, and README.
2. Add `RELEASE-X.Y.Z.md` and run the checks in [Development](DEVELOPMENT.md).
3. Commit and push `main`, and let CI pass.
4. Tag that commit and push the tag:

```powershell
git tag -a v2.3.2 -m "Release Zelo 2.3.2"
git push origin v2.3.2
```

The example matches the prepared 2.3.2 version. Creating the tag publishes a GitHub Release after a fresh CI pass, with `zelo-2.3.2.apk` and `SHA256SUMS.txt`. The tag must exactly match package and Android versions, and versioned release notes must exist. No tag is created by a normal push to main.

Download the APK from Releases, export your progress as a precaution, then install it over the existing app. Application ID and signing certificate are unchanged. Releases use a non-debuggable build with the same personal signing key.

## Failed runs

Open Actions for logs and downloadable reports. Correct failing tests or SDK setup on main before tagging. Do not move a published version tag to different source. If publication fails after creating a release, inspect the existing release and its assets before retrying; the workflow deliberately refuses to overwrite an existing release. A new change should get a new version and higher Android version code.
