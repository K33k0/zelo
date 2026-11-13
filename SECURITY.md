# Security policy

## Supported versions

Security fixes target the latest published Zelo release and the current `main` branch. Older APKs are not maintained independently. Check [Releases](https://github.com/K33k0/zelo/releases) before reporting a version-specific problem.

## Report privately

Use [GitHub's private vulnerability reporting](https://github.com/K33k0/zelo/security/advisories/new). If that is unavailable, email [the maintainer](mailto:keewynne90@gmail.com) with the subject "Zelo security report".

Include the app version, Android/WebView version where relevant, steps to reproduce, and the expected impact. Use a minimal synthetic example. Do not send passwords, private signing keys, real family progress or full backups unless specifically necessary and agreed privately. Do not open a public issue with exploit details before the maintainer has had an opportunity to investigate. There is no paid bug bounty or guaranteed response time.

## Security boundaries and limits

The current app loads bundled content into a restricted WebView and keeps Android data in private app storage. External links are opened in the browser. Backup files are portable, unencrypted JSON and depend on your selected provider's access controls. The browser preview is intended for local evaluation, not a hosted multi-user service. A compromised or rooted device is outside the protection provided by app-private storage.

Public source is proprietary; see LICENSE. Reporting a vulnerability does not require permission to redistribute the app or its branding. Use your own device and synthetic data, and do not access anyone else's information.

## Maintainer controls

Changes to main require a pull request, resolved conversations and the GitHub Actions `verify` check. Release tags cannot be moved or deleted. Only repository administrators can create version tags. The release job uses the `release` environment with an explicit maintainer approval; signing secrets are scoped to that environment, not ordinary PR jobs. See [GitHub releases](docs/GITHUB-RELEASES.md) for the configured workflow and exceptions for a solo maintainer.
