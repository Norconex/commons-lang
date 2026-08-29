# Release Changelogs

Changelogs are created on **release branches** and preserved in git history at
each release tag. The release workflow requires
`changelogs/changelog-{VERSION}.md` to exist and have content before it will
publish, and uses that file as the GitHub Release notes.

## Quick Overview

1. Create `changelog-X.Y.Z.md` on the release branch before triggering a release.
2. The release workflow validates the file exists and is not empty.
3. The file becomes the GitHub Release body and stays in git history at the tag.

## File Naming

Files **must** be named `changelog-{VERSION}.md`, matching the version being
released:

- `changelog-3.0.0.md`
- `changelog-3.0.1.md`
- `CHANGELOG.md` — too generic, allows accidental reuse

This naming is enforced by the release workflow. Tying the filename to the
version is what prevents shipping a release with a stale changelog.

## Content

See [TEMPLATE.md](TEMPLATE.md). Write for users of the library, not for the
people who wrote the code: what changed for a caller, and what they have to do
about it.
