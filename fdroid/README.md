# F-Droid playbook — `in.shripadkhandare.hex`

The live recipe is `metadata/in.shripadkhandare.hex.yml` in fdroiddata
(MR [!59425](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/59425)).
`fdroid/in.shripadkhandare.hex.yml` in this repo is a synced draft copy.
Fastlane listing lives in `fastlane/metadata/android/en-US/`.

## Releasing a new version

1. Bump `versionCode` (+1) and `versionName` in `android/app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (one short paragraph, max ~500 chars).
3. Commit, push `mine/android-vtt`.
4. Tag `android-vX.Y.Z` on the release commit, push the tag.
   Tag names MUST match `^android-v[0-9]+\.[0-9]+\.[0-9]+$` — the
   `UpdateCheckMode` pattern depends on it, and the rolling
   `android-vtt-latest` tag must never look like a release.
5. F-Droid's autoupdate bot detects the new tag and opens the version-bump
   MR by itself (via `UpdateCheckMode: Tags` + `UpdateCheckData`).
   If it doesn't within a few days, update the fdroiddata recipe by hand:
   new `Builds` block (replace the old one — only the latest version is kept),
   new pinned `commit` hash, `CurrentVersion`/`CurrentVersionCode`.
6. Never submit `CurrentVersion` ahead of a tag the bot can't see, and never
   pin a branch — `commit` must be the full hash.

## Recipe rules (learned the hard way — CI enforces all of these)

- **LF line endings + trailing newline.** No CRLF, no exceptions.
  Never paste file content from a Windows working copy into GitLab;
  copy from the GitHub raw URL or push via git.
- **No comments anywhere in the metadata file.** `rewritemeta` strips them
  and then fails the job for the diff.
- **Field order is canonical** (both `rewritemeta` and `checkupdates`
  re-derive it and fail on any deviation):
  top level `... Donate, <blank>, AutoName, <blank>, RepoType, Repo, Builds,
  MaintainerNotes, AutoUpdateMode, UpdateCheckMode, UpdateCheckData,
  CurrentVersion, CurrentVersionCode`; inside a build,
  `versionName, versionCode, commit, subdir, gradle, output, scandelete, ...`.
- **`UpdateCheckData` long value must be folded**: `UpdateCheckData: `
  (note the ONE trailing space) on its own line, value on the next line
  indented two spaces. Copy the exact shape, don't "fix" the trailing space.
- **`scandelete` is a YAML list** (`- path` lines), placed after `output:`.
  Every listed path MUST exist in the pinned commit, otherwise the build
  fails with "Non-exist scandelete path". Remove the entry once the file
  is gone upstream instead of leaving it.
- **Required fields newcomers forget**: `AutoName`, `CurrentVersion`,
  `CurrentVersionCode`.
- **`AutoUpdateMode: Version`** — plain, no `%v` suffix. (The `%v` pattern
  form is for `UpdateCheckMode: HTTP`; with `Tags` the schema rejects it.)
- **`UpdateCheckData` path is repo-root-relative**: ours is
  `android/app/build.gradle.kts` (NOT `app/...` — the `subdir: android`
  does not apply here). Regexes must match the Kotlin DSL exactly, e.g.
  `versionCode\s*=\s*(\d+)` (note the `=` — Groovy-style `versionCode (\d+)`
  does NOT match `versionCode = 2`).
- **`UpdateCheckMode: Tags ^android-v[0-9]+\.[0-9]+\.[0-9]+$`** — the pattern
  keeps Tags-mode away from the rolling `android-vtt-latest` tag (moving
  tags break checkouts) and from desktop `v*` / `ios-beta-*` tags.

## Repo hygiene (the scanner is merciless)

- No binaries anywhere in git: no `DS_Store`, no `Thumbs.db`, no stray blobs.
  `.gitignore` covers `DS_Store` and `.DS_Store`.
- **`.gitignore` must stay plain ASCII.** Never append with PowerShell
  `echo ... >> file` — it writes UTF-16 (NUL bytes) and the scanner flags
  the file itself as binary. Edit with a real editor and verify with:
  `python3 -c "d=open('.gitignore','rb').read();print(d.count(b'\x00'))"`
  (must print `0`).
- `gradle-wrapper.jar` is auto-removed by the scanner — expected, not an error.
- Release Gradle tasks must build without the private Play key:
  only `packageReleaseBundle`/`signReleaseBundle`/`bundleRelease` may
  `dependsOn(requireUploadKey)`. `packageRelease`/`assembleRelease` stay
  ungated so F-Droid (which signs with its own key) gets an unsigned APK
  at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## When CI fails, read it as

- `schema validation` → a required field is missing or a value has a
  illegal shape (it names the field). Fix the file, not the schema.
- `fdroid rewritemeta` → your file differs from canonical form. Its diff
  IS the fix: apply it byte-for-byte (mind trailing spaces).
- `checkupdates` → it shows the diff it wants (usually `AutoName` on a new
  app, or a version bump). If it fails on checkout, a tag is missing,
  moved, or filtered wrongly — check `git ls-remote --tags`.
- `fdroid build` → tail of the log names it: scan errors (binaries /
  scandelete paths) die in seconds; real compile errors need code fixes.
- `check apk` stays skipped until a build succeeds — normal.

## Open items for human review

- Reproducible builds not set up (accepted F-Droid signing for now).
- No srclibs: whisper.cpp is a pinned tarball + SHA-256 in CMake,
  tiny model pinned URL + SHA-256 in Gradle. Vendor if reviewers ask.
- Daily silent GitHub update check kept; disclosed in MaintainerNotes —
  flag as Tracking AntiFeature if reviewers say so.
- `arm64-v8a`-only build, so no ABI split.
