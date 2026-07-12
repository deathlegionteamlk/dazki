# Contribution guide

dazki is an internal project of death legion team. We accept
contributions from anyone who agrees to the rules below.

## How to contribute

1. Fork the repo.
2. Create a feature branch off `main`.
3. Make your changes. Follow the code style of the surrounding
   files.
4. Run `./gradlew assembleDebug lint` before pushing. The build
   must succeed and lint must produce no new warnings.
5. Open a pull request. Describe what you changed and why.
6. Sign your commits. We require `git commit -s`.

## Code style

- Kotlin: follow the official Kotlin coding conventions. Two space
  indent. No wildcard imports.
- Java: follow the AOSP style. Four space indent. Wildcard imports
  allowed for static imports only.
- Markdown: lines wrapped at 80 columns. Headings in sentence case,
  not title case.
- No emojis in code, comments, or commit messages.
- No em dashes or en dashes in prose. Use a comma, period, or
  parentheses instead.
- No AI vocabulary (delve, enhance, crucial, vibrant, tapestry,
  testament, underscore, etc.) in prose.

## Commit messages

First line: imperative mood, lowercase, no period. Example:
"add shutdown method to idazkiservice".

Body: explain what changed and why. Wrap at 72 columns. Reference
the issue number on its own line if applicable.

Footer: `Signed-off-by: Your Name <email>`.

## Pull request review

A maintainer will review your PR within a week. They may ask for
changes. Be prepared to iterate.

We will reject PRs that:

- Add a dependency without justification.
- Reduce test coverage below the current level (when we have tests).
- Change the wire protocol without bumping the version.
- Add features that require permissions the manager does not
  already hold.
- Use AI generated prose without human editing.

## Reporting bugs

Open an issue with:

- The dazki version (from the manager's About screen).
- The Android version and device model.
- The exact error message, if any.
- The audit log entries from the failed call.
- Steps to reproduce.

## Reporting security vulnerabilities

See [security.md](security.md).

## License

By contributing, you agree that your contributions are licensed
under the project's license (MIT for the SDK, proprietary for the
manager and server).
