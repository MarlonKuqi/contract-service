<!--
PR title = squash commit message (Conventional Commits): feat | fix | refactor | chore | test | docs, e.g. feat(contract): add import jobs
Base branch (ADR-020): feat/* -> develop ; bugfix/* -> the branch it fixes (develop, release/x.y.z or master)
-->

## Why

<!-- The need or the bug, with the related issue: Closes #123 -->

## What

<!-- The change and the decisions taken. Remove the lines that do not apply below. -->

## Impacts

- [ ] Domain: ADR created or updated in `docs/ADR.md`
- [ ] Database: new Flyway migration, no existing migration modified
- [ ] API contract: controllers documentation and `api-collections` updated
- [ ] Breaking change: `!` in the title and `BREAKING CHANGE:` footer (a relaxed rule goes in the release notes instead)

## Checklist

- [ ] Tests written first (TDD): unit tests for domain and handlers, acceptance tests for what an API user sees
- [ ] Bugfix: the regression test fails without the fix
- [ ] `./mvnw clean verify` passes locally

<!--
Merge method (ADR-020):
- feat/* or bugfix/* into a major branch: Squash and merge
- between develop, release/* and master: Create a merge commit, never squash or rebase
-->
