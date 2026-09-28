package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.config.GarrisonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.spell_engine.api.spell.registry.SpellRegistry;

import java.util.Comparator;
import java.util.EnumSet;

/** Periodic Paladins/LnE support casting for managed normal guards. */
public final class GuardSupportGoal extends Goal {
    private final PathfinderMob guard;
    private ResourceLocation spellId;
    private LivingEntity focus;
    private long releaseAt = -1;
    private boolean running;

    public GuardSupportGoal(PathfinderMob guard) {
        this.guard = guard;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GarrisonEntityTags.isManagedGuard(guard)
                || guard.level().getGameTime() < guard.getPersistentData().getLong(GarrisonEntityTags.NEXT_SUPPORT_CAST)) {
            return false;
        }

        spellId = ResourceLocation.tryParse(
                guard.getPersistentData().getString(GarrisonEntityTags.SUPPORT_SKILL));
        if (spellId == null || SpellRegistry.from(guard.level()).getHolder(spellId).isEmpty()) {
            return false;
        }

        String path = spellId.getPath();
        if (path.contains("barrier")) {
            focus = guard.getTarget();
            if (focus != null && focus.isAlive() && !GarrisonEntityTags.isFriendlyToMage(focus)) {
                return true;
            }
            AABB dangerZone = guard.getBoundingBox().inflate(12.0);
            focus = guard.level().getEntitiesOfClass(LivingEntity.class, dangerZone,
                            entity -> entity instanceof Enemy && entity.isAlive())
                    .stream().min(Comparator.comparingDouble(guard::distanceToSqr)).orElse(null);
            return focus != null;
        }

        double radius = path.contains("circle_of_healing") ? 7.5 : 20.0;
        double threshold = GarrisonConfig.SUPPORT_HEALTH_THRESHOLD.get();
        focus = guard.level().getEntitiesOfClass(LivingEntity.class,
                        guard.getBoundingBox().inflate(radius),
                        entity -> entity.isAlive()
                                && GarrisonEntityTags.isFriendlyToMage(entity)
                                && entity.getHealth() < entity.getMaxHealth() * threshold
                                && (entity == guard || guard.getSensing().hasLineOfSight(entity)))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.getHealth() / entity.getMaxHealth()))
                .orElse(null);
        return focus != null;
    }

    @Override
    public boolean canContinueToUse() {
        return running && GarrisonEntityTags.isManagedGuard(guard);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        running = true;
        releaseAt = guard.level().getGameTime() + GarrisonConfig.SUPPORT_CAST_WINDUP.get();
        guard.getNavigation().stop();
    }

    @Override
    public void tick() {
        guard.getNavigation().stop();
        if (focus != null && focus.isAlive()) {
            face(focus);
        }
        if (guard.level().getGameTime() < releaseAt) {
            return;
        }

        var entry = SpellRegistry.from(guard.level()).getHolder(spellId).orElse(null);
        if (entry != null) {
            MobSpellCasting.cast(guard, focus, entry);
            int configured = GarrisonConfig.SUPPORT_CAST_COOLDOWN.get();
            int spellCooldown = Mth.ceil(entry.value().cost.cooldown.duration * 20.0F);
            guard.getPersistentData().putLong(GarrisonEntityTags.NEXT_SUPPORT_CAST,
                    guard.level().getGameTime() + Math.max(configured, spellCooldown));
        }
        running = false;
    }

    @Override
    public void stop() {
        running = false;
        releaseAt = -1;
        focus = null;
        spellId = null;
    }

    private void face(LivingEntity target) {
        double dx = target.getX() - guard.getX();
        double dz = target.getZ() - guard.getZ();
        double dy = target.getEyeY() - guard.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
        guard.setYRot(yaw);
        guard.setYHeadRot(yaw);
        guard.setYBodyRot(yaw);
        guard.setXRot(pitch);
        guard.getLookControl().setLookAt(target, 90.0F, 90.0F);
    }
}
