# Android VTT v1 — Keyboard IME, voice-to-text only, offline-first

Goal: install Hex keyboard on Android, hold Hex key / tap Dictate, speak, get text
inserted in any app, fully offline after the one-time model download.
No History, no Meetings, no OpenCode, no cloud STT.

Source baseline: `android/` prototype on `dev` (app 0.1.1/code 2, `ly.anoma.hex`,
`minSdk 31`, `targetSdk 36`, `arm64-v8a` only).

## Phase 0 — Lock baseline
- Confirm toolchain: JDK 21, SDK platform 36, NDK 27.2.12479018, CMake 3.22.1,
  Build Tools 36.0.0.
- Windows toolchain (this PC, cmdline-tools only, no Studio):
  `D:\Android\jdk-21` (Temurin 21), `D:\Android\Sdk` (`ANDROID_HOME` +
  `ANDROID_SDK_ROOT`, user env), `cmdline-tools/latest` reorganized to
  `latest/bin/*.bat` (zip 11076708 ships them flat; sdkmanager needs `bin/`),
  packages: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`,
  `ndk;27.2.12479018`, `cmake;3.22.1`. Licenses accepted via sdkmanager.
  Note: repo README paths are macOS (`~/Library/Android/sdk`, Homebrew JDK).
- Windows checkout gotcha (fixed on `android-vtt-v1-phase0`): with
  `core.autocrlf=true`, `typing/en.valid` checked out as CRLF and failed the
  byte-exact `verifyTypingModels` hash. Root `.gitattributes` now marks
  `typing/*.hextm|*.valid` as `-text`.
- `assembleDebug`, `test` (5 unit tests debug+release), `adb install`, launch
  `ly.anoma.hex/.MainActivity`.
- Record baseline versions + hashes. Tiny works in airplane mode; small download works online.
- Exit: debug APK dictates in practice field + one external app on emulator.
- Phase 0 result (2026-11-21, Windows x64, `android-vtt-v1-phase0`):
  `./gradlew assembleDebug` BUILD SUCCESSFUL in 5m52s (`EXIT:0`),
  `app-debug.apk` 74,273,910 bytes; `./gradlew test` BUILD SUCCESSFUL —
  InsertionTest 5/5 + NeverSuggestTest 8/8, 0 failures, debug and release.
  Device run PASSED same day: Redmi Note 9 Pro Max (miatoll, SDK 37,
  arm64-v8a cortex-a76) over USB adb. Setup completed on-device (mic granted,
  Hex IME enabled + chosen, small model verified byte-complete at
  190,085,487 bytes in `no_backup`). User dictated via app Dictate bar and
  via keyboard IME — both inserted correctly. Logcat confirms 2x
  `HexSpeech: small transcribe complete` (small model, not tiny fallback).

## Phase 1 — Core VTT loop, English-first
Files: `speech/MicCapture.kt`, `speech/Whisper.kt`, `cpp/hexspeech.cpp`,
`speech/SpokenText.kt`.
- Freeze: 16 kHz mono `AudioRecord`, max 60 s RAM, mic released before inference,
  1–4 threads, pinned `whisper.cpp d1be6fde`, `audio_ctx=512` for <=8 s clips.
- Harden: `No speech heard`, `Microphone unavailable`, `Speech model unavailable`,
  cancel during Transcribing.
- Validate EN + `Style.Polished` default, `clean=false`, `translate=false`.
  Leave ES/DE/NB untouched.
- Exit: 20 fixed clips -> 100% insert or clean error, no crash, nothing persisted.
- Status: static half done on `android-vtt-v1-phase0`. Pins frozen and verified:
  tiny `5359861c`/SHA `81871056` (hash-checked in `prepareSpeechModel`),
  small 190,085,487 bytes/SHA `ae85e4a9` (size+hash-checked in `ModelStore`),
  whisper.cpp `d1be6fde`/URL_HASH (FetchContent-verified). New `SpokenTextTest`
  (10 tests) pins locale resolution, `translateFor` gating, formatter edges;
  `translateFor` extracted from `SpeechPreferences.options` (behavior unchanged)
  so the rule is JVM-testable. Full suite 46/46 green. MicCapture/hexspeech
  reviewed, no behavior change without a device to verify against. Clip-level
  exit criteria pending ARM64 runtime (Phase 4 device run).

## Phase 2 — IME insertion robustness
Files: `keyboard/HexKeyboard.kt`, `keyboard/Insertion.kt`, `keyboard/KeyPlane.kt`.
- Matrix: plain field, web field, chat, email, Docs via `InputConnection`.
  Cursor-aware spacing; editor change / keyboard hide drops pending result safely.
- Password/secure fields: typing-only, no suggestions, no mic (zero context reads).
- Contract: hold Hex = speak + release to insert; tap = hands-free (✓ finish,
  ✕ discard); dot waveform above keys; spinner replaces ✓ while transcribing.
- Runtime matrix PASSED 2026-11-21 on Redmi Note 9 Pro Max (same device):
  A. plain field (Hex practice): "Phase 2, Plain Field Test." inserted,
  Polished, engine 20:17:08. B. web field (Brave search, Chrome disabled):
  "Phase 2 Web Check." via tap-handsfree-✓, engine 20:18:48.
  C. ✕ discard: cancelled dictation left no trace, no engine run.
  D. password field (Wi-Fi add-network): empty strip, "Dictation off in
  password fields", Hex key inert, no engine run. dumpsys audio shows no
  held recording. Email/chat fields not on device — matrix gap noted.
- Status: static half done on `android-vtt-v1-phase0`. Reviewed
  `HexKeyboard`/`commitSpeech`/`leaveEditor`: editor-switch/hide cancels
  capture, `commitSpeech` guards null/secure/hidden view before its two
  single-char context reads, `beginSpeech` refuses secure/no-permission/busy.
  Extracted `classifyEditor` from `onStartInput` (behavior unchanged) + new
  `EditorKindTest` (6 tests) pins the matrix incl. all password variations.
  Full suite 58/58 green. Runtime matrix (plain/web/chat/email/Docs fields,
  hold-vs-tap gestures, password zero-read trace) pending device run.

## Phase 3 — Offline model + settings
Files: `speech/ModelStore.kt`, `HexSettings.kt`, `MainActivity.kt`.
- Single network op: `ggml-small-q5_1.bin` (190,085,487 bytes, SHA-256
  `ae85e4a9…0411bb`), atomic move to `noBackupFilesDir`, progress %, retry keeps
  tiny usable. Tiny stays bundled via `prepareSpeechModel`.
- Settings v1 lock: Home Ready/Finish setup, Dictate bar, Dictation/Keyboard/
  Privacy/Experiments(spatial only). No new settings.
- Privacy: on-device only, no keystroke/transcript store, offline policy + licences.
- Exit: fresh install -> verified download -> airplane-mode dictation; interrupted
  download -> safe Retry.

## Phase 4 — Quality gates
- Accuracy: re-run `benchmark/` EN clips tiny vs small (ref: small WER ~5.26%).
- Perf: `dumpsys meminfo` (ref emulator: 367–376 MiB PSS / 492–496 MiB RSS peak,
  63–68 MiB typing, 68–80 MiB Home after). Cold dictation time, insertion latency,
  heat/battery on 2–3 GB device per `play/README.md` checklist.
- Typing models pinned (`verifyTypingModels`), Inter TTF pinned (`verifyInter`).
- Exit: EN + ES smoke in airplane mode; no IME OOM-kill on low-end device.

## Phase 5 — Release candidate
- `:app:bundleRelease` with local `~/.config/hex-android/keystore.properties`.
  R8+shrink, `targetSdk 36`, manifest only `RECORD_AUDIO`+`INTERNET`, 16 KB ELF
  alignment, no release-DEX logs.
- Play artifacts from `play/`: listing texts, data-safety + IARC, icon + feature
  graphic, 4x 1080x1920 screenshots from signed build.
- Known blocker: publish Android privacy text at `anoma.ly/hex/privacy`.
- Real-device checklist via `bundletool install-apks` (handset mic + BT + music,
  low-memory phone, OEM picker + password-manager field, network handoff).
- Exit: signed AAB (~41 MB + 190 MB model) + `SHA256SUMS` + verification log.

## Non-goals for v1
Meetings/capture, History/stats, OpenCode rewriting, `hex service`/`local_api`/
`control`, terminal paste, global shortcuts, x86 emulator, other ABIs.
