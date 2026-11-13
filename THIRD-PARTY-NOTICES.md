# Third-party notices

The proprietary notice for Zelo covers its original code and assets. It does not replace the licenses of third-party components.

- **Inter font:** Copyright 2020 The Inter Project Authors. SIL Open Font License 1.1; the complete license is bundled at `app/src/main/assets/www/fonts/OFL.txt` and inside the APK.
- **Gradle wrapper:** Gradle's wrapper files and JAR are build tooling from the Gradle project, distributed under Apache License 2.0. See https://github.com/gradle/gradle/blob/master/LICENSE and the wrapper JAR's bundled license files.
- **Android platform and build tooling:** Android SDK and Android Gradle Plugin dependencies are obtained separately by the build, rather than committed as Zelo source. Their terms continue to apply.
- **Developer tooling:** Playwright, Prettier and prettier-plugin-java are development dependencies installed using the lockfile; their packages contain their license notices. org.json 20240303 is downloaded separately for native regression tests. These tools are not bundled as runtime JavaScript dependencies in the APK.

Before a commercial store release, review the actual release bundle and the notices for its resolved dependencies. This list is not a substitute for that release-specific review.
