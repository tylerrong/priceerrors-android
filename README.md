# PriceErrors Android

Native Kotlin/Jetpack Compose counterpart to the PriceErrors iOS app. Open this
directory (not the repository root) in Android Studio.

## Toolchain and verification

- Android Studio with its bundled JDK 17+
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0
- Minimum Android version: API 26
- Target Android version: API 36

Run the local quality gate from this directory:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Compile the Compose instrumentation suite with:

```bash
./gradlew compileDebugAndroidTestKotlin
```

Run it on a connected emulator/device with `./gradlew connectedDebugAndroidTest`.

## Client integrations

The Android clients for Google Credential Manager, Play Billing, and Firebase
Cloud Messaging are implemented. They are configuration-safe: missing console
values produce an explicit unavailable state instead of granting a fake account,
subscription, or notification registration.

Provide non-secret project identifiers through user-level Gradle properties
(`~/.gradle/gradle.properties`) or same-named CI environment variables. Do **not**
put them in this repository's `gradle.properties` — that file is tracked, and the
repository is public:

```text
PRICEERRORS_GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com

# Backend. Without all four the app falls back to the built-in sample feed.
PRICEERRORS_SERVER_BASE_URL=https://api.example.com
PRICEERRORS_SERVER_API_KEY=...
PRICEERRORS_SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
PRICEERRORS_SUPABASE_ANON_KEY=...

# Defaults shown; override only if Play Console uses different product IDs.
PRICEERRORS_BILLING_WEEKLY_PRODUCT_ID=priceerrors_pro_weekly
PRICEERRORS_BILLING_MONTHLY_PRODUCT_ID=priceerrors_pro_monthly
PRICEERRORS_BILLING_YEARLY_PRODUCT_ID=priceerrors_pro_yearly
PRICEERRORS_BILLING_RESCUE_PRODUCT_ID=priceerrors_pro_monthly_rescue

PRICEERRORS_FIREBASE_APPLICATION_ID=1:...:android:...
PRICEERRORS_FIREBASE_API_KEY=...
PRICEERRORS_FIREBASE_PROJECT_ID=...
PRICEERRORS_FIREBASE_SENDER_ID=...

# Meta Ads measurement (App Events). Values from developers.facebook.com → your app.
PRICEERRORS_FACEBOOK_APP_ID=...
PRICEERRORS_FACEBOOK_CLIENT_TOKEN=...

# PostHog product analytics (phc_... client key from Project Settings).
PRICEERRORS_POSTHOG_API_KEY=...
PRICEERRORS_POSTHOG_HOST=https://us.i.posthog.com
```

Debug builds retain an explicitly labeled local email-auth and paywall bypass so
the UI can be developed without external consoles. Those bypasses are compiled
out of release behavior.

For the pre-production Play closed test, build the dedicated signed artifact:

```bash
./gradlew clean testDebugUnitTest bundleClosedTest
```

`app/build/outputs/bundle/closedTest/app-closedTest.aab` grants temporary full
access and skips every paywall. Never promote that artifact to production.
`bundleRelease` always compiles the normal paid entitlement and paywall behavior
back in.

**Meta Ads** and **PostHog** mirror the iOS client: both no-op until
`PRICEERRORS_FACEBOOK_*` and `PRICEERRORS_POSTHOG_API_KEY` are set. Meta App
Events cover install/session measurement, paywall views, checkout starts, and
subscriptions. `GrowthAnalytics` dual-writes the same product events to PostHog
and Supabase `analytics_events` when the user is signed in.

The Google ID token is exchanged for a Supabase session, which authorizes every
server call. The deal feed, votes, claims, and account deletion are served by the
PriceErrors API. Two integrations remain device-local:

## Google Sign-In on Play (beta / production)

Play App Signing re-signs every install with Google's **App signing** certificate.
Credential Manager will reject Sign in with Google until that SHA-1 is registered
in Google Cloud — registering only your upload-key SHA-1 is not enough for Play
beta testers.

### Required Google Cloud clients (same project)

1. **Web application** OAuth client — its Client ID is
   `PRICEERRORS_GOOGLE_WEB_CLIENT_ID` (baked into the AAB). This is what
   Credential Manager and Supabase use for the ID token `aud`.
2. **Android** OAuth client(s) for package `app.priceerrors`, one fingerprint each:
   - Debug keystore (local installs)
   - Upload keystore (local `bundleRelease` / sideload)
   - **Play App signing** certificate (Play beta + production) — copy SHA-1 from
     Play Console → Setup → App integrity / App signing

Local fingerprints from `./gradlew signingReport`:

```text
Debug SHA-1:   3A:36:46:DC:FC:67:8C:4B:D2:BC:42:C7:34:1B:AC:F0:5B:EB:3C:CD
Upload SHA-1:  26:60:28:84:35:B8:0B:20:7F:AD:0B:7D:0E:AA:7C:46:5D:3F:8C:3E
Play App signing SHA-1:  (from Play Console — required for closed/open testing)
```

### Supabase → Authentication → Providers → Google

- Enable Google.
- **Client IDs**: Web client ID first, then any Android client IDs, comma-separated.
- Client Secret: the Web client's secret.
- Prefer leaving nonce verification on; the app now sends a hashed nonce to Google
  and the raw nonce to Supabase. If an older provider setup still fails, you can
  temporarily enable Skip nonce check while debugging.

### How to tell which link is broken

| Symptom on device | Likely cause |
|---|---|
| Message mentions App signing / SHA-1 / developer console | Android OAuth client missing Play App signing SHA-1 |
| Google account picker works, then Supabase rejection | Web client ID mismatch in Supabase Client IDs |
| "Google sign-in needs PRICEERRORS_GOOGLE_WEB_CLIENT_ID" | Release AAB built without the Gradle property |

After changing Cloud Console fingerprints, wait a few minutes and retry on a
fresh Play install (not a sideloaded upload-key APK).

Two integrations remain device-local:

- **Play purchases** are queried, launched, restored, and acknowledged on-device,
  but nothing tells the server. The server derives Pro from a RevenueCat webhook,
  so an Android subscriber's `profiles.is_pro` is never set — and because
  `GET /deals` hard-paywalls non-Pro callers to an empty list, a paying Android
  user currently sees no deals. Server-side purchase verification has to land
  before Android billing is usable.
- **FCM tokens** are registered through `POST /devices/register` with
  `platform: "android"`, refreshed when Firebase rotates them, marked seen when
  the app resumes, and removed on opt-out, sign-out, or account deletion. The
  backend migration `016_android_push_platform.sql` and Railway
  `FIREBASE_SERVICE_ACCOUNT_JSON` secret must be present before release.

Notification permission is requested only after the user enables Push alerts.
Incoming FCM payloads may provide `dealId` or `deal_id`; the app routes verified
`priceerrors.app/deal/<id>` and `/deals/<id>` links into the matching deal.

Alert preferences (notify-all, categories, keyword watches, minimum discount) sync
via `POST /devices/preferences` after the FCM token is registered.

Before a store upload, run the release path as well:

```bash
./gradlew clean testDebugUnitTest lintRelease bundleRelease
```

Always start with `clean`: stale files under `app/build/intermediates` (for
example Finder-duplicated `... 2.ttf` fonts) fail resource-name validation even
when the source tree is correct.

`assembleRelease`/`bundleRelease` refuse to build when the result would not be
shippable — a missing `PRICEERRORS_GOOGLE_WEB_CLIENT_ID` leaves the app with no
sign-in path at all, because release disables the debug email fallback, and
missing backend values would ship the sample feed. Missing Firebase config and
unsigned output are reported as warnings rather than failures. To produce an
unsigned release purely for R8/lint validation, opt out explicitly:

```bash
./gradlew bundleRelease -PPRICEERRORS_ALLOW_INCOMPLETE_RELEASE=true
```

R8 code shrinking and resource shrinking are enabled for release builds. Keep
`app/build/outputs/mapping/release/mapping.txt` with each production release so
crash traces can be deobfuscated.

## Release identity and versioning

The permanent application ID is `app.priceerrors`. Confirm this ID in Play
Console before the first upload: Play does not allow an application ID change
after publishing.

The defaults are version code `1` and version name `0.1.0`. Override either from
a Gradle property or an environment variable in CI:

```text
PRICEERRORS_VERSION_CODE=2
PRICEERRORS_VERSION_NAME=0.2.0
```

Every Play upload needs a version code greater than all previous uploads.

## Release signing (no secrets in Git)

Enroll in Google Play App Signing and create a separate upload key. Store the
keystore outside this repository and back it up securely. One example:

```bash
keytool -genkeypair -v -keystore priceerrors-upload.jks -alias priceerrors-upload -keyalg RSA -keysize 4096 -validity 10000
```

The build accepts all four values as Gradle properties or same-named environment
variables:

```text
PRICEERRORS_RELEASE_STORE_FILE=/absolute/path/to/priceerrors-upload.jks
PRICEERRORS_RELEASE_STORE_PASSWORD=...
PRICEERRORS_RELEASE_KEY_ALIAS=priceerrors-upload
PRICEERRORS_RELEASE_KEY_PASSWORD=...
```

For local work, put them in the user-level Gradle properties file, not this
repository. In CI, use encrypted secrets. Partial signing configuration fails
fast; with no signing values, Gradle can create an unsigned local release for
R8/lint validation.

The signed bundle is generated at:

```text
app/build/outputs/bundle/release/app-release.aab
```

Verify the signing configuration before uploading:

```bash
./gradlew signingReport bundleRelease
```

## App links

The manifest registers verified HTTPS links for both current URL shapes on the
canonical `www` host:

- `https://www.priceerrors.app/deal/<slug>` (public website)
- `https://www.priceerrors.app/deals/<id>` (existing app fixtures/contracts)

Before release, publish this structure at
`https://www.priceerrors.app/.well-known/assetlinks.json` using the SHA-256
fingerprint from the **Play App Signing** certificate (and any extra certificate
needed for non-Play testing):

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "app.priceerrors",
      "sha256_cert_fingerprints": ["PLAY_APP_SIGNING_SHA256"]
    }
  }
]
```

The apex host redirects at Vercel's domain layer and is intentionally excluded
from the verified intent filter. Android does not follow redirects while
verifying App Links, so only the direct `www` host is declared.

Serve it as JSON over HTTPS without redirects, then verify on a device:

```bash
adb shell pm verify-app-links --re-verify app.priceerrors
adb shell pm get-app-links app.priceerrors
```

The Activity still needs to map an incoming slug/ID to the corresponding deal
after the real repository contract is connected.

## Legal and privacy prerequisites

Canonical URL resources are defined in `app/src/main/res/values/strings.xml`:

- `https://priceerrors.app/privacy`
- `https://priceerrors.app/terms`
- `https://priceerrors.app/support`
- `https://priceerrors.app/delete-account`

These pages must be public, mobile-friendly, and return successful responses
before release. The account-deletion page must explain how a user can request
deletion outside the app and which data, if any, must be retained. The in-app
delete action must also delete server data once the backend is connected.

The Space Grotesk SIL OFL license and runtime third-party notices are packaged in
`app/src/main/res/raw`. Keep `THIRD_PARTY_NOTICES.md` current whenever a runtime
SDK, font, or redistributable asset is added.

Complete Play Console's Data safety form from the behavior of the final build,
including authentication identifiers, community posts, purchase/subscription
data, diagnostics, notification tokens, retention, deletion, encryption, and
third-party sharing. Do not copy the iOS disclosure without reviewing Android
SDK behavior.

## Play Console release checklist

1. Reserve `app.priceerrors`, enroll in Play App Signing, and upload the first
   signed AAB to Internal testing.
2. Set the app name, short/full descriptions, category, contact details, and
   support website/email.
3. Upload a 512×512 Play icon, 1024×500 feature graphic, and representative
   phone screenshots captured from the release candidate.
4. Complete App access, Ads, Content rating, Target audience, News/financial
   feature declarations where applicable, Data safety, and privacy-policy forms.
5. Enter the public account-deletion URL because the app supports accounts.
6. Create subscription products in Play Console and test them only with licensed
   test accounts and an Internal testing build.
7. Verify notification permission behavior, deep links, purchases/restores,
   account deletion, TalkBack, large fonts, dark mode, offline/error states, and
   process restoration on physical devices.
8. Confirm the AAB version, signing certificate, R8 mapping file, legal text,
   store prices, and screenshots all match the exact candidate being submitted.
9. Complete any closed-testing or production-access requirement shown for the
   specific Play developer account before requesting production rollout.

## Architecture seam

`AppContainer` owns the repository dependency. It selects `NetworkDealRepository`
when the four backend values above are present and falls back to
`FakeDealRepository` when they are not, so UI work stays possible without
credentials. `PriceErrorsApi` depends on an `AccessTokenProvider` rather than the
whole sign-in stack, which is what lets the API and refresh-retry behavior be
tested against a mock server. Secrets and production service credentials must
stay in local files or CI secret storage and must never be committed.
