# Path to a commercial Google Play release

## What exists today

The GitHub APK is a personal beta: `com.stride.challenges`, signed with the original personal debug certificate so existing installations can update. Keep this key and package stable for that channel. No Play Console app, merchant account, production signing key or store listing has been created by this preparation.

Making the repository public does not make the source open source. LICENSE reserves rights to Zelo's original source and branding; third-party licenses still apply. A public repository can be viewed and forked under GitHub's terms, and a notice cannot technically stop copying. Commercial protection also depends on branding, distribution, support, and enforcement. Before selling, have the final commercial terms and brand rights reviewed.

## Recommended sequence

1. Create and verify your Google Play developer account under your intended publishing identity. Decide whether ecletra is a personal trading name or a business publisher, and choose the public support contact and website.
2. Decide how you will charge before the first public Play listing: paid download, or a free app with paid features. Google permits paid-to-free changes but does not allow changing the same published free app to paid. Do not publish a free listing as a placeholder if you intend to charge for the download.
3. Choose the permanent production application ID after confirming the publishing identity. A separate production package keeps the current personal APK and future store app distinct. Users would transfer progress using Zelo's export/restore rather than installing a differently signed store build over the personal app. Do not blindly change the current package or key.
4. Enroll the new app in **Play App Signing**, letting Google manage the production app signing key. Create a separate upload key for submitting Android App Bundles. The upload key is not the key that signs APKs delivered by Google. Keep an offline backup and use environment-scoped CI secrets only when the production build path is implemented.
5. Add a distinct production build configuration with the chosen ID, a non-debuggable release, current required target SDK, version codes and an `.aab` output signed by the upload key. The existing GitHub APK workflow remains the personal beta channel. Do not reuse the personal debug key for the production store identity.
6. Prepare a publicly accessible privacy-policy page, accurate Data safety answers, content rating, screenshots, accessibility checks, store description and support details. The repository privacy policy describes today's app; revisit it if billing, crash reporting, analytics or other services are added. Complete any testing or developer-verification requirements shown for your account in Play Console.
7. Test the actual production bundle on devices, including clean install, export/restore, reminders, widgets, backups and purchase/restore behavior if paid features are introduced. Use internal/closed testing before public rollout.

## Decisions needed before implementation

The developer account/publisher identity, permanent package name and paid-download versus paid-feature model are product/account decisions. This document prepares the route without registering a name, creating billing commitments, generating an unmanaged production key or submitting an app on your behalf.

## Official references

- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [App pricing and free/paid changes](https://support.google.com/googleplay/android-developer/answer/6334373)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play policy center](https://play.google.com/about/developer-content-policy/)
- [GitHub licensing a repository](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository)
