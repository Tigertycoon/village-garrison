# Architecture

The server event service periodically discovers loaded village bells, requires enough adult villagers, counts defenders by role and replenishes missing roles after a configured delay. Existing guards and priests count too. Saved data is keyed by bell position within each dimension, with independent timestamps for guard, priest and mage respawns.

Persistent schedules use level game time rather than the process tick count so restarts do not reset the time base. Updating last-seen time marks the data dirty, allowing retention information to survive a save/reload.

Mage combat is an explicit goal: keep distance, require line of sight before beginning a cast, stop moving during wind-up, deliver the configured spell, then apply cooldown. `MobSpellCasting` calls Spell Engine delivery and impact APIs for living non-player entities. Unsupported delivery types are logged rather than presented as implemented.

Ally checks feed Spell Engine relations and a damage-event fallback. Optional RPG equipment/spells are selected by registry ID, so absent optional content is skipped. The required baseline runtime is pinned separately from the optional feature list.

The project compiles against the official Spell Engine 1.9.9 release. This does not establish compatibility with every newer release; use the documented runtime profile when reproducing results.
