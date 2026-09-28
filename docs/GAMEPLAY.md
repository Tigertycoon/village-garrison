# Village Garrison

NeoForge 1.21.1 integration mod for heavily modded village-defense packs.

For every loaded village bell with enough adult villagers, the mod maintains:

- 6 `guardvillagers:guard` entities with 40 health and random Better Weaponry melee weapons or crossbows;
- 2 `irons_spellbooks:priest` entities;
- 2 real `guardvillagers:guard` mage guards equipped with Wizards (RPG Series) wands/staves and robes.

Missing defenders are respawned one per role after the configurable delay. Existing nearby defenders count toward the cap, so the mod does not duplicate naturally spawned guards or priests. Mage guards use a high-priority ranged combat goal: they seek hostile mobs, keep their distance, aim during a visible wind-up and cast their configured Spell Engine spell. The default basic spell pool contains Fireball, Frost Shard and Arcane Bolt. Advanced mage guards use Meteor, Blizzard or Arcane Missiles.

When the corresponding optional RPG Series mods are installed, the same mage pool also includes Air Cutter, Water Whip and Stone Spear plus the advanced Aeroburst, Explosive Bubbles and Shattering Stone loadouts. Crossbow guards can receive Magic Arrow, Fan of Fire, Arctic Volley or Enchanted Crystal Arrow. Their base proc chance is 5% and scales with ranged damage to a configurable maximum of 15%. Some normal guards can also receive Circle of Healing, Barrier or Holy Prevention support magic.

Spell Engine treats villagers, MineColonies citizens and visitors, guards, priests, iron golems, players and player-owned pets as allies of every managed defender. A second damage-event guard prevents friendly fire even for effects that bypass Spell Engine's target filtering. Managed defenders persist and, by default, drop no loot to prevent equipment farms.

All counts, timings, health, spell power and weapon/spell loadouts are configurable in `config/village_garrison-common.toml`. A mage loadout is `item|spell|BASIC` or `item|spell|ADVANCED`, so other compatible Spell Engine weapon and spell IDs can be added without code.

Operators can use `/villagegarrison status` near a bell to inspect its current counts and
`/villagegarrison scan` to force an immediate maintenance pass.

## Runtime dependencies

- NeoForge for Minecraft 1.21.1
- Guard Villagers 2.4.10+
- Iron's Spells 'n Spellbooks 3.x for Minecraft 1.21.1
- Spell Engine 1.9.9+
- Spell Power 1.4.4+
- Wizards (RPG Series) 2.7.1+
- Better Weaponry 1.1.3+
- Better Combat 2.3.2 or newer
- Better Mob Combat Reimagined 1.0.40+

Those mods' own dependencies (such as GeckoLib, Curios, Player Animator, Forgified Fabric API, Accessories, AzureLib Armor, Runes, Structure Pool API and Cloth Config) are still required.

## Optional feature mods

None of these are hard dependencies. Missing items and spells are skipped automatically.

- Elemental Wizards RPG + LNE Wizards: wind, aqua and terra mage loadouts;
- Archers + Ranged Weapon API: Magic Arrow for ranged guards;
- Archers Expansion + LNE Archers + More RPG Library: Fan of Fire, Arctic Volley and Enchanted Crystal Arrow;
- Paladins: Circle of Healing and Barrier;
- LNE Paladins: Holy Prevention when that spell and its own content dependencies are available.

The current 1.21.1 add-ons place Aeroburst and Explosive Bubbles in LNE Wizards while Shattering Stone remains in Elemental Wizards RPG. All item and spell lists, assignment chances, trigger chances and cooldowns can be changed in `config/village_garrison-common.toml`.
