# Village Garrison

[![Build and test](https://github.com/Tigertycoon/village-garrison/actions/workflows/build.yml/badge.svg)](https://github.com/Tigertycoon/village-garrison/actions/workflows/build.yml)

Persistent village defense for **Minecraft 1.21.1 / NeoForge**, with armed guards, priests and mage guards that cast Spell Engine spells.

The mod maintains configurable defender roles around loaded village bells. Mage AI keeps its distance, aims during a casting wind-up and delivers spells through a non-player adapter. Respawn schedules are stored with the world; friendly-fire filtering protects villagers, players and allied entities.

## What is implemented

- Configurable defender counts, health, weapons, spell loadouts and respawn delays.
- Ranged mage combat goals and optional support/archer skills.
- Existing nearby guards and priests count toward the cap.
- Persistent schedules using Minecraft saved data and world time.
- Operator commands `/villagegarrison status` and `/villagegarrison scan`.

[Gameplay and configuration details](docs/GAMEPLAY.md) · [Architecture](docs/ARCHITECTURE.md) · [Validation scope](docs/VALIDATION.md)

## Build

Install **JDK 21**, clone the repository and run:

```sh
./gradlew clean build
```

On Windows: `gradlew.bat clean build`.
The first build downloads pinned dependencies automatically. No local dependency checkouts, copied JARs or credentials are required.

Output: `build/libs/village_garrison-1.2.2.jar`.

## Runtime

Install the mod into a **Minecraft 1.21.1 NeoForge** instance with Guard Villagers, Iron's Spells 'n Spellbooks, Spell Engine, Spell Power, Wizards, Better Weaponry, Better Combat, Better Mob Combat Reimagined and their dependencies.

The exact baseline versions and links are in [the dependency profile](docs/DEPENDENCIES.md). Optional RPG Series additions are described in [gameplay](docs/GAMEPLAY.md); they are outside the baseline profile.

Run `./gradlew runClient` for that development profile. `./gradlew runServer` starts a dedicated development server after you review and accept the Minecraft EULA. Runtime files are isolated in ignored `run/` directories.

## Source tour

| Component | Responsibility |
|---|---|
| `GarrisonService` | Discovers village anchors, counts defenders and schedules replenishment. |
| `MageCombatGoal` / `GuardSupportGoal` | Movement, target selection and casting timing. |
| `MobSpellCasting` | Adapts Spell Engine delivery to non-player casters. |
| `GarrisonData` | Saves village anchors and per-role respawn times. |
| `MageLoadout` | Parses configured item/spell/tier combinations. |

## Author and license

Personal project by **Niklas / Tigertycoon**, developed with OpenAI Codex assistance. [Credits](CREDITS.md) distinguish this integration from the upstream mods. Project code: [MIT](LICENSE); Gradle wrapper: Apache-2.0.

Related: [Minecraft Integrations](https://github.com/Tigertycoon/minecraft-integrations), connecting magic and industrial systems to survival mechanics.
