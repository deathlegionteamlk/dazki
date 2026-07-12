# dazki community health files

This folder holds GitHub-specific files: CI workflows, issue templates, PR template, and funding config. Everything here is read by GitHub to render the repo's community page.

## Files

| file                                   | what it does                                       |
|----------------------------------------|----------------------------------------------------|
| `workflows/build.yml`                  | Builds debug APKs on every push and PR.            |
| `workflows/lint.yml`                   | Runs Android lint on every push and PR.            |
| `ISSUE_TEMPLATE/bug_report.md`         | Template for bug reports.                          |
| `ISSUE_TEMPLATE/feature_request.md`    | Template for feature requests.                     |
| `ISSUE_TEMPLATE/security_report.md`    | Marker for security reports. Real reports go to email. |
| `PULL_REQUEST_TEMPLATE.md`             | Template for pull request descriptions.            |
| `FUNDING.yml`                          | Sponsorship buttons. Currently empty.              |

## Adding a new workflow

Drop a new `.yml` file in `workflows/`. Use existing files as a reference. Keep the workflow names short, because GitHub shows them in the Actions tab.

## Changing issue templates

Edit the files in `ISSUE_TEMPLATE/`. The YAML frontmatter at the top of each file controls the title prefix, labels, and assignees. GitHub documents the schema at https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests.
