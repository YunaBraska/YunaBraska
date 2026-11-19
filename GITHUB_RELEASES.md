# GitHub releases

One stable version has one identity. The Git tag, GitHub Release title, artifact
filenames and package version use that exact version. A product name is never
prepended or appended to the GitHub Release title.

`wc_github_create_release.yml` owns the public GitHub Release. It accepts only
valid SemVer with an optional `v` prefix and prerelease suffix. It rejects
`*-snapshot`, regardless of letter case. A snapshot may publish to a snapshot package registry but never
creates a Git tag or GitHub Release.

Products prepare and verify their assets and optional release notes. They can
also pass a short prefix for generated notes. They do not invoke `gh release`.
The shared publisher then creates a draft with the exact version as its title,
uploads every prepared asset, and publishes only after upload succeeds. If a
new publication fails, it removes its draft and new tag.

This separation keeps package layout product-specific while version identity,
publication order, rollback, and snapshot policy have one owner.
