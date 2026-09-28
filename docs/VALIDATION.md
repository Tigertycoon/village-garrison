# Validation

Baseline: Minecraft 1.21.1, NeoForge 21.1.236 and Java 21 with the [pinned runtime](DEPENDENCIES.md).

Run `./gradlew clean build` (Windows: `gradlew.bat clean build`). The test suite uses ModDevGradle's NeoForge JUnit integration and loads the real baseline mods.

| Coverage | Checks |
|---|---|
| Saved state | All three role timestamps survive NBT save/reload; villages have independent schedules; last-seen changes mark data dirty; retention survives reload. |
| Loadout parsing | Valid basic/advanced entries, whitespace and invalid IDs/tiers. |
| Actual content | An ephemeral NeoForge test server loads datapacks and verifies every default mage weapon and spell against the pinned runtime. |

The suite contains **15 passing cases** in the local preparation run. GitHub Actions runs the same build on Linux; the workflow badge links to the current result. The Gradle wrapper checks its distribution checksum, and mod dependencies resolve by fixed artifact versions.

## Limits

The automated checks cover mod loading, data and the state/selection behavior listed above. They do not establish in-game combat balance, animation quality, multiplayer behavior, long-running village replenishment or compatibility with every optional RPG add-on. No visual demo is part of this export.

For a gameplay check, create a separate test instance using the pinned runtime, place a village bell near enough adult villagers, inspect `/villagegarrison status`, and exercise combat, defender replacement and a world restart. Keep production worlds separate from testing.

## Test tooling

- [ModDevGradle JUnit integration](https://github.com/neoforged/ModDevGradle#unit-testing)
- [NeoForge test framework](https://github.com/neoforged/NeoForge/tree/1.21.x/testframework)

No upstream mod binaries, Minecraft worlds or test logs are committed in this source repository.

The pinned Iron's Spells release logs an invalid upstream `test/ring_gen_break_me` loot table during datapack loading. The ephemeral test-server helper may also log a temporary-directory cleanup warning on Windows after the tests complete. These diagnostics do not change the asserted loadout or persistence results; the tests are not advertised as a warning-free full gameplay run.
