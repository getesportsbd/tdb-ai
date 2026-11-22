# Fixes applied

- Set the Android launcher/app display name to **TDB Personal AI Assistant**.
- Corrected the official creator/founder/channel identity throughout the app:
  - Founder: Deep Barmon
  - Creator: Deep Barmon
  - Brand: TDB
  - Official vlog YouTube channel: Deep Barmon Bhai Vlog
- Updated the README to identify Bengali as the primary language.
- Updated the app-resource test to expect the full official app name.
- Updated GitHub Actions to use Java 21, matching the project's Java 21 source/target compatibility.
- Removed the checked-in local `.gradle` cache from the delivered ZIP.

## Validation status

- All XML resource/manifest files parsed successfully.
- All JSON files parsed successfully.
- A full Android/Gradle build could not be run in this environment because Gradle and the Android SDK are not installed. Run `gradle :app:assembleDebug` (and `gradle :app:testDebugUnitTest`) in Android Studio or GitHub Actions to confirm compilation and runtime behavior.

## Firebase note

The release App Check configuration looks for a `recaptcha_site_key` string resource, but the project does not include one. Add the actual reCAPTCHA Enterprise site key from your Firebase project before expecting Firebase App Check-protected AI calls to work in a release build. No key has been invented or added.
