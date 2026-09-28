package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.VillageGarrison;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.arrow.ArrowHelper;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Server-side Spell Engine delivery for non-player casters.
 *
 * <p>Spell Engine 1.9.9 only exposes its full cast entry point for players, although its
 * projectile, meteor, cloud and impact delivery methods already support every LivingEntity.
 * Keeping this small adapter in Village Garrison makes guards work with the official 1.9.9
 * release without requiring a custom Spell Engine fork.</p>
 */
public final class MobSpellCasting {
    private MobSpellCasting() {
    }

    public static boolean cast(LivingEntity caster, LivingEntity preferredTarget,
                               Holder<Spell> spellEntry) {
        Level level = caster.level();
        Spell spell = spellEntry.value();
        if (level.isClientSide() || spell.active == null || spell.target == null
                || spell.deliver == null || spell.impacts == null) {
            return false;
        }

        SpellHelper.ImpactContext context = new SpellHelper.ImpactContext()
                .power(SpellPower.getSpellPower(spell.school, caster))
                .target(SpellHelper.focusMode(spell));

        List<Entity> targets = resolveTargets(caster, preferredTarget, spell);
        if (spell.target.cap > 0 && targets.size() > spell.target.cap) {
            targets = targets.stream()
                    .sorted(Comparator.comparingDouble(caster::distanceToSqr))
                    .limit(spell.target.cap)
                    .toList();
        }

        Vec3 targetLocation = preferredTarget != null ? preferredTarget.position() : null;
        boolean delivered = deliver(level, caster, spellEntry, spell, targets, targetLocation, context);
        if (delivered && spell.release != null) {
            ParticleHelper.sendBatches(caster, spell.release.particles);
            SoundHelper.playSound(level, caster, spell.release.sound);
        }
        return delivered;
    }

    private static List<Entity> resolveTargets(LivingEntity caster, LivingEntity preferredTarget,
                                                Spell spell) {
        float range = spell.range * caster.getScale();
        Predicate<Entity> allowed = entity -> targetAllowed(caster, entity, spell);

        return switch (spell.target.type) {
            case NONE -> List.of();
            case CASTER -> List.of(caster);
            case AIM, FROM_TRIGGER -> {
                if (preferredTarget != null && preferredTarget.isAlive() && allowed.test(preferredTarget)) {
                    yield List.of(preferredTarget);
                }
                if (spell.target.aim != null && spell.target.aim.use_caster_as_fallback) {
                    yield List.of(caster);
                }
                yield List.of();
            }
            case BEAM -> TargetHelper.targetsFromRaycast(caster, range, allowed);
            case AREA -> {
                List<Entity> areaTargets = new ArrayList<>(
                        TargetHelper.targetsFromArea(caster, range, spell.target.area, allowed));
                if (spell.target.area != null && spell.target.area.include_caster
                        && !areaTargets.contains(caster)) {
                    areaTargets.add(caster);
                }
                yield areaTargets;
            }
        };
    }

    private static boolean targetAllowed(LivingEntity caster, Entity target, Spell spell) {
        var focusMode = SpellHelper.focusMode(spell);
        boolean allowed = SpellHelper.deliveryIntent(spell)
                .map(intent -> EntityRelations.actionAllowed(focusMode, intent, caster, target))
                .orElse(false);
        for (Spell.Impact impact : spell.impacts) {
            boolean impactAllowed = impact.action.apply_to_caster
                    ? target == caster
                    : EntityRelations.actionAllowed(
                            focusMode, SpellHelper.impactIntent(impact.action), caster, target);
            allowed |= impactAllowed;
        }
        return allowed;
    }

    private static boolean deliver(Level level, LivingEntity caster, Holder<Spell> spellEntry,
                                   Spell spell, List<Entity> targets, Vec3 targetLocation,
                                   SpellHelper.ImpactContext context) {
        return switch (spell.deliver.type) {
            case DIRECT -> deliverDirect(level, caster, spellEntry, spell, targets, targetLocation, context);
            case PROJECTILE -> {
                if (targets.isEmpty()) {
                    SpellHelper.shootProjectile(level, caster, null, spellEntry, context);
                } else {
                    targets.forEach(target -> SpellHelper.shootProjectile(
                            level, caster, target, spellEntry, context));
                }
                yield true;
            }
            case METEOR -> {
                boolean launched = false;
                if (targets.isEmpty() && targetLocation != null) {
                    launched = SpellHelper.fallProjectile(
                            level, caster, null, targetLocation, spellEntry, context);
                } else {
                    for (Entity target : targets) {
                        launched |= SpellHelper.fallProjectile(
                                level, caster, target, null, spellEntry, context);
                    }
                }
                yield launched;
            }
            case CLOUD -> {
                if (targets.isEmpty() && targetLocation != null) {
                    SpellHelper.placeCloud(level, caster, null, targetLocation,
                            spellEntry, context.position(targetLocation));
                    yield true;
                }
                targets.forEach(target -> SpellHelper.placeCloud(
                        level, caster, target, null, spellEntry, context));
                yield !targets.isEmpty();
            }
            case SHOOT_ARROW -> {
                ArrowHelper.shootArrow(level, caster, spellEntry, context);
                yield true;
            }
            case AFFECT_ARROW, MELEE, STASH_EFFECT, CUSTOM -> {
                VillageGarrison.LOGGER.warn(
                        "Guard caster does not support Spell Engine delivery type {} for {}",
                        spell.deliver.type, spellEntry.unwrapKey().map(Object::toString).orElse("unknown"));
                yield false;
            }
        };
    }

    private static boolean deliverDirect(Level level, LivingEntity caster, Holder<Spell> spellEntry,
                                         Spell spell, List<Entity> targets, Vec3 targetLocation,
                                         SpellHelper.ImpactContext context) {
        Vec3 casterCenter = caster.position().add(0.0, caster.getBbHeight() / 2.0, 0.0);
        if (targets.isEmpty() && targetLocation != null && spell.area_impact != null) {
            Vec3 position = targetLocation.lerp(casterCenter, 0.001);
            SpellHelper.performImpacts(level, caster, caster, null, spellEntry,
                    spell.impacts, context.position(position));
            return true;
        }

        boolean success = false;
        for (Entity target : targets) {
            Vec3 position = target == caster
                    ? casterCenter
                    : target.position().add(0.0, target.getBbHeight() / 2.0, 0.0)
                            .lerp(casterCenter, 0.01);
            success |= SpellHelper.performImpacts(level, caster, target, target, spellEntry,
                    spell.impacts, context.position(position));
        }
        return success;
    }
}
