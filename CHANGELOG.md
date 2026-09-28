# Changelog

## 1.2.2 — Portfolio export

- Replace neighboring source-build dependencies with pinned public Maven artifacts.
- Add a reproducible development dependency profile, Java 21 CI and regression tests.
- Persist last-seen village updates even when respawn times are unchanged.
- Reject malformed mage loadout tiers and accept surrounding whitespace.
- Add build, architecture, dependency, validation and provenance documentation.

The runtime profile uses publicly available Better Mob Combat Reimagined 1.0.41 instead of the previous local 1.0.40 JAR. Earlier workspace worlds, logs and dependency forks are not part of this source export.
