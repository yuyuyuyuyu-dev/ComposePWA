# Test web app

A minimal Compose Multiplatform web app that CI builds as a PWA to test the ComposePWA
plugin, against every `index.html` location the plugin supports.

On purpose, no source set contains an `index.html`: each CI job copies one of the
templates below into the resources directory under test and then builds. Building this
project as committed — without copying one first — is the test for the plugin's
missing-index.html error message.

- [index.html](index.html) — no PWA tags, so the plugin has to add them.
- [index-with-pwa-tags.html](index-with-pwa-tags.html) — the tags already present, in the
  shape Prettier reformats them into, so the plugin has to leave the file alone.

Dependency versions are pinned on purpose; this app is a test fixture, not a sample to
copy (see [example](../../example) for that).
