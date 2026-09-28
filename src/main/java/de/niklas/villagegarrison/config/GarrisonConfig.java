package de.niklas.villagegarrison.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class GarrisonConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SCAN_INTERVAL;
    public static final ModConfigSpec.IntValue PLAYER_DISCOVERY_RADIUS;
    public static final ModConfigSpec.IntValue GARRISON_RADIUS;
    public static final ModConfigSpec.IntValue MIN_BELL_SEPARATION;
    public static final ModConfigSpec.IntValue MIN_VILLAGERS;
    public static final ModConfigSpec.IntValue GUARD_COUNT;
    public static final ModConfigSpec.IntValue PRIEST_COUNT;
    public static final ModConfigSpec.IntValue MAGE_COUNT;
    public static final ModConfigSpec.IntValue RESPAWN_DELAY;

    public static final ModConfigSpec.DoubleValue GUARD_HEALTH;
    public static final ModConfigSpec.DoubleValue CROSSBOW_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MELEE_WEAPONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ARCHER_SKILLS;
    public static final ModConfigSpec.DoubleValue ARCHER_TRIGGER_BASE_CHANCE;
    public static final ModConfigSpec.DoubleValue ARCHER_TRIGGER_PER_RANGED_DAMAGE;
    public static final ModConfigSpec.DoubleValue ARCHER_TRIGGER_MAX_CHANCE;
    public static final ModConfigSpec.IntValue ARCHER_SKILL_COOLDOWN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUPPORT_SKILLS;
    public static final ModConfigSpec.DoubleValue SUPPORT_SKILL_GUARD_CHANCE;
    public static final ModConfigSpec.DoubleValue SUPPORT_HEALTH_THRESHOLD;
    public static final ModConfigSpec.IntValue SUPPORT_CAST_WINDUP;
    public static final ModConfigSpec.IntValue SUPPORT_CAST_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SUPPORT_SPELL_POWER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MAGE_LOADOUTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ELEMENTAL_MAGE_LOADOUTS;

    public static final ModConfigSpec.IntValue MAGE_TARGET_INTERVAL;
    public static final ModConfigSpec.IntValue MAGE_TARGET_RADIUS;
    public static final ModConfigSpec.DoubleValue MAGE_HEALTH;
    public static final ModConfigSpec.DoubleValue MAGE_SPELL_POWER;
    public static final ModConfigSpec.DoubleValue MAGE_ADVANCED_CHANCE;
    public static final ModConfigSpec.IntValue MAGE_CAST_WINDUP;
    public static final ModConfigSpec.IntValue MAGE_BASIC_COOLDOWN;
    public static final ModConfigSpec.IntValue MAGE_ADVANCED_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MAGE_MIN_RANGE;
    public static final ModConfigSpec.DoubleValue MAGE_PREFERRED_RANGE;
    public static final ModConfigSpec.BooleanValue MAGES_TARGET_CREEPERS;
    public static final ModConfigSpec.BooleanValue MANAGED_DEFENDERS_DROP_LOOT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("village");
        SCAN_INTERVAL = builder
                .comment("Ticks between village scans. 20 ticks = one second.")
                .defineInRange("scanIntervalTicks", 200, 20, 1200);
        PLAYER_DISCOVERY_RADIUS = builder
                .comment("Only bells this close to an online player are maintained.")
                .defineInRange("playerDiscoveryRadius", 128, 32, 256);
        GARRISON_RADIUS = builder
                .comment("Radius around a bell used to count villagers and defenders.")
                .defineInRange("garrisonRadius", 48, 16, 128);
        MIN_BELL_SEPARATION = builder
                .comment("Nearby bells are treated as one village garrison.")
                .defineInRange("minimumBellSeparation", 64, 16, 128);
        MIN_VILLAGERS = builder
                .comment("A bell needs at least this many living adult villagers nearby.")
                .defineInRange("minimumVillagers", 3, 1, 100);
        GUARD_COUNT = builder
                .comment("Desired Guard Villagers guards per active village.")
                .defineInRange("guards", 6, 0, 64);
        PRIEST_COUNT = builder
                .comment("Desired Iron's Spells priests per active village.")
                .defineInRange("priests", 2, 0, 32);
        MAGE_COUNT = builder
                .comment("Desired spell-casting Guard Villagers mages per active village.")
                .defineInRange("mages", 2, 0, 32);
        RESPAWN_DELAY = builder
                .comment("Delay before each missing defender is replenished.")
                .defineInRange("respawnDelayTicks", 1200, 20, 240000);
        builder.pop();

        builder.push("guards");
        GUARD_HEALTH = builder
                .comment("Maximum health assigned to guards spawned by this mod.")
                .defineInRange("health", 40.0, 1.0, 2048.0);
        CROSSBOW_CHANCE = builder
                .comment("Chance that a guard receives a vanilla crossbow instead of a melee weapon.")
                .defineInRange("crossbowChance", 0.30, 0.0, 1.0);
        MELEE_WEAPONS = builder
                .comment("Random melee weapon pool. Missing IDs are skipped; minecraft:iron_sword is the fallback.")
                .defineList("meleeWeapons", List.of(
                        "better_weaponry:iron_battleaxe",
                        "better_weaponry:iron_broadsword",
                        "better_weaponry:iron_claymore",
                        "better_weaponry:iron_cutlass",
                        "better_weaponry:iron_glaive",
                        "better_weaponry:iron_hammer",
                        "better_weaponry:iron_katana",
                        "better_weaponry:iron_rapier",
                        "better_weaponry:iron_sai",
                        "better_weaponry:iron_scythe",
                        "better_weaponry:iron_sickle",
                        "better_weaponry:iron_spear",
                        "better_weaponry:diamond_cutlass",
                        "better_weaponry:diamond_rapier",
                        "minecraft:iron_sword"
                ), GarrisonConfig::isResourceLocation);

        ARCHER_SKILLS = builder
                .comment("Spell Engine skills randomly assigned to newly spawned ranged guards.",
                        "Only registered spells are considered, so Archers and Archers Expansion remain optional.")
                .defineList("archerSkills", List.of(
                        "archers:magic_arrow",
                        "archers_expansion:fan_of_fire",
                        "archers_expansion:arctic_volley",
                        "archers_expansion:enchanted_crystal_arrow"
                ), GarrisonConfig::isResourceLocation);
        ARCHER_TRIGGER_BASE_CHANCE = builder
                .comment("Base chance for a ranged guard's assigned skill to replace an arrow shot.")
                .defineInRange("archerSkillBaseChance", 0.05, 0.0, 1.0);
        ARCHER_TRIGGER_PER_RANGED_DAMAGE = builder
                .comment("Additional trigger chance per point of ranged_weapon:damage.")
                .defineInRange("archerSkillChancePerRangedDamage", 0.01, 0.0, 1.0);
        ARCHER_TRIGGER_MAX_CHANCE = builder
                .comment("Maximum trigger chance for ranged guard skills.")
                .defineInRange("archerSkillMaxChance", 0.15, 0.0, 1.0);
        ARCHER_SKILL_COOLDOWN = builder
                .comment("Fallback cooldown after a ranged guard skill triggers, in ticks.")
                .defineInRange("archerSkillCooldownTicks", 160, 1, 12000);

        SUPPORT_SKILLS = builder
                .comment("Optional Paladins/LnE support spells assigned to some newly spawned normal guards.",
                        "Only registered spells are considered; the content mods are not hard dependencies.")
                .defineList("supportSkills", List.of(
                        "paladins:circle_of_healing",
                        "paladins:barrier",
                        "lne_paladins:holy_prevention"
                ), GarrisonConfig::isResourceLocation);
        SUPPORT_SKILL_GUARD_CHANCE = builder
                .comment("Chance that a newly spawned normal guard receives one available holy support skill.")
                .defineInRange("supportSkillGuardChance", 0.35, 0.0, 1.0);
        SUPPORT_HEALTH_THRESHOLD = builder
                .comment("Healing and prevention skills activate when a nearby ally falls below this health fraction.")
                .defineInRange("supportHealthThreshold", 0.70, 0.05, 1.0);
        SUPPORT_CAST_WINDUP = builder
                .comment("Ticks a guard pauses and aims before casting a holy support skill.")
                .defineInRange("supportCastWindupTicks", 20, 1, 200);
        SUPPORT_CAST_COOLDOWN = builder
                .comment("Fallback cooldown for holy support skills, in ticks.")
                .defineInRange("supportCastCooldownTicks", 400, 1, 24000);
        SUPPORT_SPELL_POWER = builder
                .comment("Base healing spell power assigned to guards with a holy support skill.")
                .defineInRange("supportSpellPower", 6.0, 0.1, 2048.0);
        builder.pop();

        builder.push("mages");
        MAGE_LOADOUTS = builder
                .comment("Mage loadouts in item|spell|tier form. Tier is BASIC or ADVANCED. Missing mod items/spells are skipped.",
                        "This also lets modpack authors add compatible Spell Engine weapons and spells from other mods.")
                .defineList("loadouts", List.of(
                        "wizards:wand_fire|wizards:fireball|BASIC",
                        "wizards:wand_frost|wizards:frost_shard|BASIC",
                        "wizards:wand_arcane|wizards:arcane_bolt|BASIC",
                        "wizards:staff_fire|wizards:fireball|BASIC",
                        "wizards:staff_frost|wizards:frost_shard|BASIC",
                        "wizards:staff_arcane|wizards:arcane_bolt|BASIC",
                        "wizards:wand_netherite_fire|wizards:fire_meteor|ADVANCED",
                        "wizards:wand_netherite_frost|wizards:frost_blizzard|ADVANCED",
                        "wizards:wand_netherite_arcane|wizards:arcane_missile|ADVANCED",
                        "wizards:staff_netherite_fire|wizards:fire_meteor|ADVANCED",
                        "wizards:staff_netherite_frost|wizards:frost_blizzard|ADVANCED",
                        "wizards:staff_netherite_arcane|wizards:arcane_missile|ADVANCED"
                ), GarrisonConfig::isMageLoadout);
        ELEMENTAL_MAGE_LOADOUTS = builder
                .comment("Optional Elemental Wizards and LNE Wizards loadouts in item|spell|tier form.",
                        "This is a separate key so existing configs receive the new defaults on upgrade.")
                .defineList("elementalLoadouts", List.of(
                        "elemental_wizards_rpg:wand_wind|elemental_wizards_rpg:wind_air_cutter|BASIC",
                        "elemental_wizards_rpg:staff_wind|elemental_wizards_rpg:wind_air_cutter|BASIC",
                        "elemental_wizards_rpg:wand_aqua|elemental_wizards_rpg:aqua_water_whip|BASIC",
                        "elemental_wizards_rpg:staff_aqua|elemental_wizards_rpg:aqua_water_whip|BASIC",
                        "elemental_wizards_rpg:wand_terra|elemental_wizards_rpg:terra_stone_spear|BASIC",
                        "elemental_wizards_rpg:staff_terra|elemental_wizards_rpg:terra_stone_spear|BASIC",
                        "elemental_wizards_rpg:wand_netherite_wind|lne_wizards:wind_aeroburst|ADVANCED",
                        "elemental_wizards_rpg:staff_netherite_wind|lne_wizards:wind_aeroburst|ADVANCED",
                        "elemental_wizards_rpg:wand_netherite_aqua|lne_wizards:aqua_explosive_bubbles|ADVANCED",
                        "elemental_wizards_rpg:staff_netherite_aqua|lne_wizards:aqua_explosive_bubbles|ADVANCED",
                        "elemental_wizards_rpg:wand_netherite_terra|elemental_wizards_rpg:terra_shattering_stone|ADVANCED",
                        "elemental_wizards_rpg:staff_netherite_terra|elemental_wizards_rpg:terra_shattering_stone|ADVANCED",
                        "elemental_wizards_rpg:staff_aeternium_wind|lne_wizards:wind_aeroburst|ADVANCED",
                        "elemental_wizards_rpg:staff_crystal_aqua|lne_wizards:aqua_explosive_bubbles|ADVANCED",
                        "elemental_wizards_rpg:staff_ruby_terra|elemental_wizards_rpg:terra_shattering_stone|ADVANCED"
                ), GarrisonConfig::isMageLoadout);
        MAGE_TARGET_INTERVAL = builder
                .comment("Ticks between proactive target searches for managed mages.")
                .defineInRange("targetIntervalTicks", 10, 1, 200);
        MAGE_TARGET_RADIUS = builder
                .comment("How far managed mages look for hostile mobs.")
                .defineInRange("targetRadius", 40, 8, 128);
        MAGE_HEALTH = builder
                .comment("Maximum health assigned to spell-casting guards.")
                .defineInRange("health", 40.0, 1.0, 2048.0);
        MAGE_SPELL_POWER = builder
                .comment("Base fire, frost and arcane spell power assigned to mage guards.")
                .defineInRange("spellPower", 8.0, 0.1, 2048.0);
        MAGE_ADVANCED_CHANCE = builder
                .comment("Chance that a newly spawned mage receives an advanced weapon and spell.")
                .defineInRange("advancedChance", 0.25, 0.0, 1.0);
        MAGE_CAST_WINDUP = builder
                .comment("Ticks a mage visibly aims before releasing a spell.")
                .defineInRange("castWindupTicks", 16, 1, 200);
        MAGE_BASIC_COOLDOWN = builder
                .comment("Fallback cooldown for basic mage spells, in ticks.")
                .defineInRange("basicCooldownTicks", 80, 1, 12000);
        MAGE_ADVANCED_COOLDOWN = builder
                .comment("Fallback cooldown for advanced mage spells, in ticks.")
                .defineInRange("advancedCooldownTicks", 200, 1, 12000);
        MAGE_MIN_RANGE = builder
                .comment("Mage guards retreat when a target gets closer than this many blocks.")
                .defineInRange("minimumRange", 6.0, 0.0, 64.0);
        MAGE_PREFERRED_RANGE = builder
                .comment("Mage guards approach targets until they are within this many blocks.")
                .defineInRange("preferredRange", 18.0, 2.0, 128.0);
        MAGES_TARGET_CREEPERS = builder
                .comment("Allow mages to attack creepers. Disabled by default to reduce village damage.")
                .define("targetCreepers", false);
        builder.pop();

        builder.push("loot");
        MANAGED_DEFENDERS_DROP_LOOT = builder
                .comment("If false, respawning defenders cannot be farmed for equipment or mob loot.")
                .define("managedDefendersDropLoot", false);
        builder.pop();

        SPEC = builder.build();
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String string && ResourceLocation.tryParse(string) != null;
    }

    private static boolean isMageLoadout(Object value) {
        if (!(value instanceof String string)) {
            return false;
        }
        String[] parts = string.split("\\|", -1);
        return parts.length == 3
                && ResourceLocation.tryParse(parts[0]) != null
                && ResourceLocation.tryParse(parts[1]) != null
                && (parts[2].equalsIgnoreCase("BASIC") || parts[2].equalsIgnoreCase("ADVANCED"));
    }

    private GarrisonConfig() {
    }
}
