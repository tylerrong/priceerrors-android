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

PRICEERRORS_FIREBASE_APPLICATION_ID=1:...:android:...
PRICEERRORS_FIREBASE_API_KEY=...
PRICEERRORS_FIREBASE_PROJECT_ID=...
PRICEERRORS_FIREBASE_SENDER_ID=...
```

Debug builds retain an explicitly labeled local email-auth and paywall bypass so
the UI can be developed without external consoles. Those bypasses are compiled
out of release behavior.

The Google ID token is exchanged for a Supabase session, which authorizes every
server call. The deal feed, votes, claims, and account deletion are served by the
PriceErrors API. Two integrations remain device-local:

- **Play purchases** are queried, launched, restored, and acknowledged on-device,
  but nothing tells the server. The server derives Pro from a RevenueCat webhook,
  so an Android subscriber's `profiles.is_pro` is never set — and because
  `GET /deals` hard-paywalls non-Pro callers to an empty list, a paying Android
  user currently sees no deals. Server-side purchase verification has to land
  before Android billing is usable.
- **FCM tokens** are retained as pending and never logged. `POST /devices/register`
  is deliberately not called: `device_tokens` has no platform column and
  `getAllDeviceTokens()` feeds every row to APNs, so registering FCM tokens there
  would inject unroutable tokens into the iOS push path.

Notification permission is requested only after the user enables Push alerts.
Incoming FCM payloads may provide `dealId` or `deal_id`; the app routes verified
`priceerrors.app/deal/<id>` and `/deals/<id>` links into the matching deal.

Community reporting, blocking, and posting-guideline acceptance are persisted
on-device. Pending reports clearly state that they have not reached PriceErrors
staff; uploading them is part of the backend milestone.

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

The manifest registers verified HTTPS links for both current URL shapes:

- `https://priceerrors.app/deal/<slug>` (public website)
- `https://priceerrors.app/deals/<id>` (existing app fixtures/contracts)

Before release, publish this structure at
`https://priceerrors.app/.well-known/assetlinks.json` using the SHA-256
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

The manifest declares **both** `priceerrors.app` and `www.priceerrors.app`,
because the site 307s the apex to www, so indexed and shared links are
canonically www. Each declared host must serve its own copy of the file.

This is the easy one to get wrong: the apex currently redirects *everything*,
including `/.well-known/assetlinks.json`. Android will not follow a redirect when
verifying, so the apex must serve that path directly (a Vercel rewrite/header
exception) or verification silently fails for it.

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
