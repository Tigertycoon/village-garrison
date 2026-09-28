package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.VillageGarrison;
import de.niklas.villagegarrison.config.GarrisonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.registry.SpellRegistry;

import java.util.EnumSet;

/** High-priority ranged combat for managed mage guards. */
public final class MageCombatGoal extends Goal {
    private final PathfinderMob mage;
    private long releaseAt = -1;
    private boolean warnedMissingSpell;

    public MageCombatGoal(PathfinderMob mage) {
        this.mage = mage;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return GarrisonEntityTags.isManagedMage(mage) && validTarget(mage.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        releaseAt = -1;
        mage.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = mage.getTarget();
        if (!validTarget(target)) {
            return;
        }

        faceTarget(target);
        long now = mage.level().getGameTime();
        double distanceSquared = mage.distanceToSqr(target);
        double min = GarrisonConfig.MAGE_MIN_RANGE.get();
        double preferred = GarrisonConfig.MAGE_PREFERRED_RANGE.get();

        if (releaseAt >= 0) {
            mage.getNavigation().stop();
            if (now >= releaseAt) {
                releaseSpell(target, now);
            }
            return;
        }

        if (distanceSquared < min * min) {
            if (mage.tickCount % 10 == 0) {
                Vec3 away = DefaultRandomPos.getPosAway(mage, 12, 7, target.position());
                if (away != null) {
                    mage.getNavigation().moveTo(away.x, away.y, away.z, 1.05D);
                }
            }
        } else if (distanceSquared > preferred * preferred || !mage.getSensing().hasLineOfSight(target)) {
            mage.getNavigation().moveTo(target, 0.9D);
        } else {
            mage.getNavigation().stop();
        }

        long nextCast = mage.getPersistentData().getLong(GarrisonEntityTags.NEXT_CAST);
        if (now >= nextCast && distanceSquared <= preferred * preferred
                && mage.getSensing().hasLineOfSight(target)) {
            releaseAt = now + GarrisonConfig.MAGE_CAST_WINDUP.get();
            mage.swing(InteractionHand.MAIN_HAND);
            mage.getNavigation().stop();
        }
    }

    private void releaseSpell(LivingEntity target, long now) {
        releaseAt = -1;
        if (!validTarget(target)) {
            return;
        }
        faceTarget(target);
        String value = mage.getPersistentData().getString(GarrisonEntityTags.MAGE_SPELL);
        ResourceLocation spellId = ResourceLocation.tryParse(value);
        if (spellId == null) {
            return;
        }

        var entry = SpellRegistry.from(mage.level()).getHolder(spellId).orElse(null);
        if (entry == null) {
            if (!warnedMissingSpell) {
                VillageGarrison.LOGGER.warn("Mage guard {} could not resolve configured Spell Engine spell {}",
                        mage.getUUID(), spellId);
                warnedMissingSpell = true;
            }
            mage.getPersistentData().putLong(GarrisonEntityTags.NEXT_CAST, now + 200);
            return;
        }

        MobSpellCasting.cast(mage, target, entry);
        boolean advanced = mage.getPersistentData().getBoolean(GarrisonEntityTags.MAGE_ADVANCED);
        int cooldown = advanced ? GarrisonConfig.MAGE_ADVANCED_COOLDOWN.get()
                : GarrisonConfig.MAGE_BASIC_COOLDOWN.get();
        mage.getPersistentData().putLong(GarrisonEntityTags.NEXT_CAST,
                now + cooldown + mage.getRandom().nextInt(Math.max(2, cooldown / 5)));
    }

    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - mage.getX();
        double dz = target.getZ() - mage.getZ();
        double dy = target.getEyeY() - mage.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
        mage.setYRot(yaw);
        mage.setYHeadRot(yaw);
        mage.setYBodyRot(yaw);
        mage.setXRot(pitch);
        mage.getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    private boolean validTarget(LivingEntity target) {
        return target != null && target.isAlive() && !GarrisonEntityTags.isFriendlyToMage(target);
    }
}
