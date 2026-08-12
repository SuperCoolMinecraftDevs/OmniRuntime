# Contributing

Thanks for taking an interest. This document covers how we work on OmniRuntime,
so read it before opening your first pull request.

## Before you start

Open an issue for anything larger than a bug fix. It is better to find out that
an idea conflicts with a decision record before you have written the code, and
it gives the rest of us a chance to point you at the parts of the codebase you
will need to touch.

## Branches

`main` is always releasable and is never committed to directly. Work happens on
a branch, and the branch is merged once CI passes and someone has reviewed it.

Name branches after what they do:

```
feat/module-lifecycle
fix/reload-leaks-host-functions
docs/guest-abi-reference
ci/cache-maven-repository
chore/bump-chicory
```

## Commits

We use Conventional Commits. The type is one of `feat`, `fix`, `docs`, `test`,
`ci`, `refactor`, `perf` or `chore`, followed by a colon and a short description
in the imperative mood.

```
feat: load modules from the plugins folder on startup
fix: release host function table when a module is unloaded
docs: explain capability grants in the guest ABI reference
```

Keep the subject under about seventy characters and do not end it with a full
stop. If a commit needs explaining, put the explanation in the body, separated
from the subject by a blank line, and explain why the change was made rather
than what it changed. The diff already says what changed.

Commit as you work. A branch made of several small commits that each do one
thing is easier to review, easier to revert and easier to read a year from now
than a branch made of one enormous commit.

## Pull requests

- One topic per pull request.
- Describe what the change does and why in the description, and link the issue.
- CI must be green before a merge. Do not merge around a failing check.
- Pull requests are squash merged, so `main` keeps one commit per change.

## Code style

- Four spaces, no tabs.
- Comments explain why, never what. If a comment restates the line below it,
  delete the comment. If the code needs a comment to be understood, consider
  whether a better name would have done the job instead.
- Keep dependencies to a minimum. Every dependency is a version to track, an
  advisory to read and a licence to check. If the thing you need is small,
  write it and test it rather than pulling in a library for it. New
  dependencies belong in a decision record.

## Tests

Anything that can be tested has a test. A pull request that changes behaviour
without touching the test suite will be asked about it during review. Bug fixes
come with a test that fails before the fix and passes after it.

## Versioning

Releases are tagged `vMAJOR.MINOR.PATCH`.

| Part | When it changes |
| --- | --- |
| Major | A release big enough to change how the project is used, such as a break in the guest ABI |
| Minor | New functionality that does not break what already exists |
| Patch | Bug fixes, CI repairs and other corrections that add nothing |

A major release resets the parts below it, so the release after `v1.4.2` is
either `v1.4.3`, `v1.5.0` or `v2.0.0`.

## Changelog

`CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com). Add your
entry to the `Unreleased` section in the same pull request as the change, under
`Added`, `Changed`, `Deprecated`, `Removed`, `Fixed` or `Security`. Write the
entry for someone running a server, not for someone reading the diff.

The `Unreleased` section becomes a version heading when we tag a release.

## Decision records

Choices that shape the project, such as which runtime we embed, how the guest
ABI is laid out or which server versions we support, are written down in
`docs/adr` in ADR format. If your pull request makes a decision of that size, it
should add a record. If it contradicts an existing record, it should add a
record that supersedes it rather than quietly editing the old one, because the
old reasoning is still worth reading.
