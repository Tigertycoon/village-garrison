package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.data.DefenderRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public final class GarrisonEntityTags {
    public static final String MANAGED = "VillageGarrisonManaged";
    public static final String ROLE = "VillageGarrisonRole";
    public static final String ANCHOR = "VillageGarrisonAnchor";
    public static final String MAGE_SPELL = "VillageGarrisonMageSpell";
    public static final String MAGE_WEAPON = "VillageGarrisonMageWeapon";
    public static final String MAGE_ADVANCED = "VillageGarrisonMageAdvanced";
    public static final String NEXT_CAST = "VillageGarrisonNextCast";
    public static final String ARCHER_SKILL = "VillageGarrisonArcherSkill";
    public static final String ARCHER_SKILL_ROLLED = "VillageGarrisonArcherSkillRolled";
    public static final String NEXT_ARCHER_SKILL = "VillageGarrisonNextArcherSkill";
    public static final String SUPPORT_SKILL = "VillageGarrisonSupportSkill";
    public static final String SUPPORT_SKILL_ROLLED = "VillageGarrisonSupportSkillRolled";
    public static final String NEXT_SUPPORT_CAST = "VillageGarrisonNextSupportCast";

    private static final ResourceLocation GUARD_ID =
            ResourceLocation.fromNamespaceAndPath("guardvillagers", "guard");
    private static final ResourceLocation PRIEST_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "priest");
    private static final ResourceLocation MINECOLONIES_CITIZEN_ID =
            ResourceLocation.fromNamespaceAndPath("minecolonies", "citizen");
    private static final ResourceLocation MINECOLONIES_VISITOR_ID =
            ResourceLocation.fromNamespaceAndPath("minecolonies", "visitor");

    public static boolean isManaged(Entity entity) {
        return entity.getPersistentData().getBoolean(MANAGED);
    }

    public static boolean isManagedMage(Entity entity) {
        return isManagedRole(entity, DefenderRole.MAGE);
    }

    public static boolean isManagedPriest(Entity entity) {
        return isManagedRole(entity, DefenderRole.PRIEST);
    }

    public static boolean isManagedGuard(Entity entity) {
        return isManagedRole(entity, DefenderRole.GUARD);
    }

    private static boolean isManagedRole(Entity entity, DefenderRole role) {
        return entity instanceof LivingEntity && isManaged(entity)
                && role.serializedName().equals(entity.getPersistentData().getString(ROLE));
    }

    public static boolean isFriendlyToMage(Entity entity) {
        if (isManaged(entity) || entity instanceof Player || entity instanceof AbstractVillager
                || entity instanceof IronGolem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (GUARD_ID.equals(id) || PRIEST_ID.equals(id)
                || MINECOLONIES_CITIZEN_ID.equals(id) || MINECOLONIES_VISITOR_ID.equals(id)) {
            return true;
        }
        return entity instanceof TamableAnimal tameable && tameable.getOwner() instanceof Player;
    }

    private GarrisonEntityTags() {
    }
}
