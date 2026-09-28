package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.VillageGarrison;
import de.niklas.villagegarrison.config.GarrisonConfig;
import de.niklas.villagegarrison.data.DefenderRole;
import de.niklas.villagegarrison.data.GarrisonData;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.target.EntityRelations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GarrisonService {
    private static final ResourceLocation GUARD_ID = ResourceLocation.fromNamespaceAndPath("guardvillagers", "guard");
    private static final ResourceLocation PRIEST_ID = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "priest");
    private static final long DATA_RETENTION_TICKS = 30L * 24000L;

    private long nextVillageScan;
    private long nextMageTargetScan;
    private final Set<Mob> pendingMageEquipment = new HashSet<>();
    private final Set<Mob> pendingGuardSetup = new HashSet<>();
    private final List<PendingArcherCast> pendingArcherCasts = new ArrayList<>();

    public GarrisonService() {
        EntityRelations.registerTeamMatcher("village_garrison", (attacker, target) -> {
            if (GarrisonEntityTags.isManaged(attacker)
                    && GarrisonEntityTags.isFriendlyToMage(target)) {
                return new EntityRelations.TeamRelation(true, false);
            }
            return null;
        });
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();

        if (!pendingMageEquipment.isEmpty()) {
            List<Mob> pending = List.copyOf(pendingMageEquipment);
            pendingMageEquipment.clear();
            pending.stream().filter(Entity::isAlive).forEach(this::applyMageEquipment);
        }

        if (!pendingGuardSetup.isEmpty()) {
            List<Mob> pending = List.copyOf(pendingGuardSetup);
            pendingGuardSetup.clear();
            pending.stream().filter(Entity::isAlive).forEach(guard -> assignGuardSkills(guard, guard.getRandom()));
        }

        if (!pendingArcherCasts.isEmpty()) {
            List<PendingArcherCast> pending = List.copyOf(pendingArcherCasts);
            pendingArcherCasts.clear();
            for (PendingArcherCast cast : pending) {
                if (cast.guard().isAlive() && cast.target().isAlive()
                        && !GarrisonEntityTags.isFriendlyToMage(cast.target())) {
                    faceTarget(cast.guard(), cast.target());
                    SpellRegistry.from(cast.guard().level()).getHolder(cast.spell()).ifPresent(entry ->
                            MobSpellCasting.cast(cast.guard(), cast.target(), entry));
                }
            }
        }

        if (tick >= nextMageTargetScan) {
            nextMageTargetScan = tick + GarrisonConfig.MAGE_TARGET_INTERVAL.get();
            event.getServer().getAllLevels().forEach(this::updateMageTargets);
        }

        if (tick < nextVillageScan) {
            return;
        }

        nextVillageScan = tick + GarrisonConfig.SCAN_INTERVAL.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Level game time is saved with the world; server tick count resets on restart.
            maintainLoadedVillages(level, level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!GarrisonConfig.MANAGED_DEFENDERS_DROP_LOOT.get()
                && GarrisonEntityTags.isManaged(event.getEntity())) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob
                && GarrisonEntityTags.isManagedMage(mob)) {
            // Priority zero and MOVE/LOOK flags suppress Guard Villagers' melee/ranged goals while fighting.
            mob.goalSelector.addGoal(0, new MageCombatGoal(mob));
            // Guard Villagers reapplies its vanilla hand loadout after insertion. Restore the mage
            // weapon on the first completed server tick, after every join/load lifecycle.
            pendingMageEquipment.add(mob);
        } else if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob
                && GarrisonEntityTags.isManagedGuard(mob)) {
            mob.goalSelector.addGoal(0, new GuardSupportGoal(mob));
            pendingGuardSetup.add(mob);
        }

        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractArrow arrow) {
            tryReplaceArrowWithSkill(event, arrow);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && GarrisonEntityTags.isManaged(attacker)
                && GarrisonEntityTags.isFriendlyToMage(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("villagegarrison")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("scan").executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos origin = BlockPos.containing(context.getSource().getPosition());
                    Optional<BlockPos> anchor = findNearestBell(level, origin, 256);
                    if (anchor.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("No village bell found within 256 blocks."));
                        return 0;
                    }
                    maintainGarrison(level, anchor.get(), GarrisonData.get(level), level.getGameTime());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Scanned village garrison at " + anchor.get().toShortString()), false);
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos origin = BlockPos.containing(context.getSource().getPosition());
                    Optional<BlockPos> anchor = findNearestBell(level, origin, 256);
                    if (anchor.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("No village bell found within 256 blocks."));
                        return 0;
                    }
                    int radius = GarrisonConfig.GARRISON_RADIUS.get();
                    EnumMap<DefenderRole, Integer> counts = countDefenders(level,
                            new AABB(anchor.get()).inflate(radius, radius / 2.0, radius));
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Garrison at " + anchor.get().toShortString()
                                    + ": guards=" + counts.get(DefenderRole.GUARD)
                                    + ", priests=" + counts.get(DefenderRole.PRIEST)
                                    + ", mages=" + counts.get(DefenderRole.MAGE)), false);
                    return 1;
                })));
    }

    private void maintainLoadedVillages(ServerLevel level, long tick) {
        if (level.players().isEmpty()) {
            return;
        }

        GarrisonData data = GarrisonData.get(level);
        for (BlockPos anchor : discoverVillageAnchors(level)) {
            maintainGarrison(level, anchor, data, tick);
        }
        data.prune(tick, DATA_RETENTION_TICKS);
    }

    private List<BlockPos> discoverVillageAnchors(ServerLevel level) {
        int radius = GarrisonConfig.PLAYER_DISCOVERY_RADIUS.get();
        Set<BlockPos> candidates = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            level.getPoiManager().findAll(
                    holder -> holder.is(PoiTypes.MEETING),
                    pos -> level.getBlockState(pos).getBlock() instanceof BellBlock,
                    player.blockPosition(),
                    radius,
                    PoiManager.Occupancy.ANY
            ).forEach(pos -> candidates.add(pos.immutable()));
        }

        List<BlockPos> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt((BlockPos pos) -> pos.getX())
                .thenComparingInt(pos -> pos.getZ())
                .thenComparingInt(pos -> pos.getY()));

        int separation = GarrisonConfig.MIN_BELL_SEPARATION.get();
        long separationSquared = (long) separation * separation;
        List<BlockPos> anchors = new ArrayList<>();
        for (BlockPos candidate : sorted) {
            if (anchors.stream().noneMatch(existing -> existing.distSqr(candidate) < separationSquared)) {
                anchors.add(candidate);
            }
        }
        return anchors;
    }

    private Optional<BlockPos> findNearestBell(ServerLevel level, BlockPos origin, int radius) {
        return level.getPoiManager().find(
                holder -> holder.is(PoiTypes.MEETING),
                pos -> level.getBlockState(pos).getBlock() instanceof BellBlock,
                origin,
                radius,
                PoiManager.Occupancy.ANY);
    }

    private void maintainGarrison(ServerLevel level, BlockPos anchor, GarrisonData data, long tick) {
        int radius = GarrisonConfig.GARRISON_RADIUS.get();
        AABB bounds = new AABB(anchor).inflate(radius, radius / 2.0, radius);
        long villagers = level.getEntitiesOfClass(Villager.class, bounds,
                villager -> villager.isAlive() && !villager.isBaby()).size();
        if (villagers < GarrisonConfig.MIN_VILLAGERS.get()) {
            return;
        }

        EnumMap<DefenderRole, Integer> counts = countDefenders(level, bounds);
        GarrisonData.Entry entry = data.getOrCreate(anchor, tick);
        replenishRole(level, anchor, data, entry, DefenderRole.GUARD,
                counts.get(DefenderRole.GUARD), GarrisonConfig.GUARD_COUNT.get(), tick);
        replenishRole(level, anchor, data, entry, DefenderRole.PRIEST,
                counts.get(DefenderRole.PRIEST), GarrisonConfig.PRIEST_COUNT.get(), tick);
        replenishRole(level, anchor, data, entry, DefenderRole.MAGE,
                counts.get(DefenderRole.MAGE), GarrisonConfig.MAGE_COUNT.get(), tick);
    }

    private EnumMap<DefenderRole, Integer> countDefenders(ServerLevel level, AABB bounds) {
        EnumMap<DefenderRole, Integer> counts = new EnumMap<>(DefenderRole.class);
        for (DefenderRole role : DefenderRole.values()) {
            counts.put(role, 0);
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds, Entity::isAlive)) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (GarrisonEntityTags.isManagedMage(mob)) {
                counts.compute(DefenderRole.MAGE, (role, count) -> count + 1);
            } else if (GUARD_ID.equals(id)) {
                counts.compute(DefenderRole.GUARD, (role, count) -> count + 1);
            } else if (PRIEST_ID.equals(id)) {
                counts.compute(DefenderRole.PRIEST, (role, count) -> count + 1);
            }
        }
        return counts;
    }

    private void replenishRole(ServerLevel level, BlockPos anchor, GarrisonData data,
                               GarrisonData.Entry entry, DefenderRole role,
                               int current, int desired, long tick) {
        if (current >= desired) {
            data.setNextSpawn(entry, role, tick + GarrisonConfig.RESPAWN_DELAY.get());
            return;
        }
        if (tick < entry.nextSpawn(role)) {
            return;
        }

        if (spawnDefender(level, anchor, role)) {
            data.setNextSpawn(entry, role, tick + GarrisonConfig.RESPAWN_DELAY.get());
        } else {
            data.setNextSpawn(entry, role, tick + Math.min(200, GarrisonConfig.RESPAWN_DELAY.get()));
        }
    }

    private boolean spawnDefender(ServerLevel level, BlockPos anchor, DefenderRole role) {
        ResourceLocation typeId = switch (role) {
            case GUARD -> GUARD_ID;
            case PRIEST -> PRIEST_ID;
            case MAGE -> GUARD_ID;
        };
        if (typeId == null) {
            return false;
        }

        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);
        if (type.isEmpty()) {
            VillageGarrison.LOGGER.error("Required defender entity type {} is not registered", typeId);
            return false;
        }

        Entity created = type.get().create(level);
        if (!(created instanceof Mob mob)) {
            VillageGarrison.LOGGER.error("Defender entity type {} did not create a Mob", typeId);
            return false;
        }

        Optional<BlockPos> spawnPosition = findSpawnPosition(level, anchor, mob);
        if (spawnPosition.isEmpty()) {
            return false;
        }

        BlockPos pos = spawnPosition.get();
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
        mob.finalizeSpawn(level, difficulty, MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean(GarrisonEntityTags.MANAGED, true);
        mob.getPersistentData().putString(GarrisonEntityTags.ROLE, role.serializedName());
        mob.getPersistentData().putLong(GarrisonEntityTags.ANCHOR, anchor.asLong());

        if (role == DefenderRole.GUARD) {
            equipGuard(mob, level.random);
        } else if (role == DefenderRole.MAGE && !equipMageGuard(mob, level.random)) {
            return false;
        }

        if (!level.addFreshEntity(mob)) {
            return false;
        }

        VillageGarrison.LOGGER.info("Spawned {} at village bell {} in {}",
                typeId, anchor, level.dimension().location());
        return true;
    }

    private Optional<BlockPos> findSpawnPosition(ServerLevel level, BlockPos anchor, Mob mob) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 18; attempt++) {
            int x = anchor.getX() + Mth.nextInt(random, -8, 8);
            int z = anchor.getZ() + Mth.nextInt(random, -8, 8);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            mob.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            if (level.getWorldBorder().isWithinBounds(pos)
                    && level.noCollision(mob)
                    && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private void equipGuard(Mob guard, RandomSource random) {
        var maxHealth = guard.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(GarrisonConfig.GUARD_HEALTH.get());
            guard.setHealth((float) maxHealth.getValue());
        }

        boolean crossbow = random.nextDouble() < GarrisonConfig.CROSSBOW_CHANCE.get();
        ItemStack weapon = crossbow ? new ItemStack(Items.CROSSBOW) : chooseMeleeWeapon(random);
        guard.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        if (!crossbow && isOneHandedWeapon(weapon)) {
            guard.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            guard.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        } else {
            guard.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        assignGuardSkills(guard, random);
    }

    private void assignGuardSkills(Mob guard, RandomSource random) {
        if (!GarrisonEntityTags.isManagedGuard(guard)) {
            return;
        }

        if (!guard.getPersistentData().getBoolean(GarrisonEntityTags.ARCHER_SKILL_ROLLED)
                && guard.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
            List<ResourceLocation> skills = availableSpells(guard, GarrisonConfig.ARCHER_SKILLS.get());
            if (!skills.isEmpty()) {
                ResourceLocation skill = skills.get(random.nextInt(skills.size()));
                guard.getPersistentData().putString(GarrisonEntityTags.ARCHER_SKILL, skill.toString());
                guard.getPersistentData().putBoolean(GarrisonEntityTags.ARCHER_SKILL_ROLLED, true);
            }
        }

        if (!guard.getPersistentData().getBoolean(GarrisonEntityTags.SUPPORT_SKILL_ROLLED)) {
            List<ResourceLocation> skills = availableSpells(guard, GarrisonConfig.SUPPORT_SKILLS.get());
            if (!skills.isEmpty()) {
                guard.getPersistentData().putBoolean(GarrisonEntityTags.SUPPORT_SKILL_ROLLED, true);
                if (random.nextDouble() < GarrisonConfig.SUPPORT_SKILL_GUARD_CHANCE.get()) {
                    ResourceLocation skill = skills.get(random.nextInt(skills.size()));
                    guard.getPersistentData().putString(GarrisonEntityTags.SUPPORT_SKILL, skill.toString());
                    guard.getPersistentData().putLong(GarrisonEntityTags.NEXT_SUPPORT_CAST,
                            guard.level().getGameTime() + 40 + random.nextInt(160));
                    setSpellPower(guard, "healing", GarrisonConfig.SUPPORT_SPELL_POWER.get());
                }
            }
        } else if (!guard.getPersistentData().getString(GarrisonEntityTags.SUPPORT_SKILL).isEmpty()) {
            setSpellPower(guard, "healing", GarrisonConfig.SUPPORT_SPELL_POWER.get());
        }
    }

    private List<ResourceLocation> availableSpells(Mob guard, List<? extends String> configured) {
        List<ResourceLocation> available = new ArrayList<>();
        for (String value : configured) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null && SpellRegistry.from(guard.level()).getHolder(id).isPresent()) {
                available.add(id);
            }
        }
        return available;
    }

    private ItemStack chooseMeleeWeapon(RandomSource random) {
        List<? extends String> configured = GarrisonConfig.MELEE_WEAPONS.get();
        List<Item> available = new ArrayList<>();
        for (String value : configured) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                continue;
            }
            BuiltInRegistries.ITEM.getOptional(id)
                    .filter(item -> item != Items.AIR)
                    .ifPresent(available::add);
        }
        if (available.isEmpty()) {
            return new ItemStack(Items.IRON_SWORD);
        }
        return new ItemStack(available.get(random.nextInt(available.size())));
    }

    private boolean isOneHandedWeapon(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        return path.contains("cutlass") || path.contains("dagger") || path.contains("rapier")
                || path.contains("sai") || path.endsWith("sword");
    }

    private boolean equipMageGuard(Mob mage, RandomSource random) {
        boolean advanced = random.nextDouble() < GarrisonConfig.MAGE_ADVANCED_CHANCE.get();
        List<MageLoadout> loadouts = configuredMageLoadouts().stream()
                .map(MageLoadout::parse)
                .filter(loadout -> loadout != null && loadout.advanced() == advanced)
                .filter(loadout -> isAvailableMageLoadout(mage, loadout))
                .toList();
        if (loadouts.isEmpty()) {
            loadouts = configuredMageLoadouts().stream()
                    .map(MageLoadout::parse)
                    .filter(loadout -> loadout != null)
                    .filter(loadout -> isAvailableMageLoadout(mage, loadout))
                    .toList();
        }
        if (loadouts.isEmpty()) {
            VillageGarrison.LOGGER.error("No configured mage loadout has a registered weapon item");
            return false;
        }

        MageLoadout loadout = loadouts.get(random.nextInt(loadouts.size()));
        mage.getPersistentData().putString(GarrisonEntityTags.MAGE_WEAPON, loadout.item().toString());
        mage.getPersistentData().putString(GarrisonEntityTags.MAGE_SPELL, loadout.spell().toString());
        mage.getPersistentData().putBoolean(GarrisonEntityTags.MAGE_ADVANCED, loadout.advanced());
        mage.getPersistentData().putLong(GarrisonEntityTags.NEXT_CAST,
                mage.level().getGameTime() + 20 + random.nextInt(60));

        var maxHealth = mage.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(GarrisonConfig.MAGE_HEALTH.get());
            mage.setHealth((float) maxHealth.getValue());
        }
        setMageSpellPower(mage);
        applyMageEquipment(mage);
        return true;
    }

    private List<String> configuredMageLoadouts() {
        List<String> values = new ArrayList<>();
        values.addAll(GarrisonConfig.MAGE_LOADOUTS.get());
        values.addAll(GarrisonConfig.ELEMENTAL_MAGE_LOADOUTS.get());
        return values;
    }

    private boolean isAvailableMageLoadout(Mob mage, MageLoadout loadout) {
        return BuiltInRegistries.ITEM.getOptional(loadout.item())
                .filter(item -> item != Items.AIR).isPresent()
                && SpellRegistry.from(mage.level()).getHolder(loadout.spell()).isPresent();
    }

    private void applyMageEquipment(Mob mage) {
        ResourceLocation weaponId = ResourceLocation.tryParse(
                mage.getPersistentData().getString(GarrisonEntityTags.MAGE_WEAPON));
        ResourceLocation spellId = ResourceLocation.tryParse(
                mage.getPersistentData().getString(GarrisonEntityTags.MAGE_SPELL));
        if (weaponId == null || spellId == null) return;
        BuiltInRegistries.ITEM.getOptional(weaponId).filter(item -> item != Items.AIR).ifPresent(item -> {
            if (!mage.getMainHandItem().is(item)) {
                mage.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
            }
            mage.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        });
        if (!mage.getOffhandItem().isEmpty()) {
            mage.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        MageLoadout loadout = new MageLoadout(weaponId, spellId,
                mage.getPersistentData().getBoolean(GarrisonEntityTags.MAGE_ADVANCED));
        equipMageArmor(mage, loadout);
    }

    private void setMageSpellPower(Mob mage) {
        for (String school : List.of("arcane", "fire", "frost", "air", "water", "earth", "generic")) {
            setSpellPower(mage, school, GarrisonConfig.MAGE_SPELL_POWER.get());
        }
    }

    private void setSpellPower(Mob mob, String school, double value) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("spell_power", school);
        BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(attribute -> {
            var instance = mob.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        });
    }

    private void equipMageArmor(Mob mage, MageLoadout loadout) {
        if (loadout.item().getNamespace().equals("elemental_wizards_rpg")) {
            String prefix = switch (loadout.school()) {
                case "wind" -> loadout.advanced() ? "netherite_wind" : "wind";
                case "aqua" -> loadout.advanced() ? "netherite_kelp" : "kelp";
                case "terra" -> loadout.advanced() ? "netherite_dripstone" : "dripstone";
                default -> "wind";
            };
            equipArmorSet(mage, "elemental_wizards_rpg", prefix);
            return;
        }

        String prefix = loadout.advanced() ? "netherite_" + loadout.school() + "_robe"
                : loadout.school() + "_robe";
        equipArmorSet(mage, "wizards", prefix);
    }

    private void equipArmorSet(Mob mage, String namespace, String prefix) {
        equipIfPresent(mage, EquipmentSlot.HEAD, namespace + ":" + prefix + "_head");
        equipIfPresent(mage, EquipmentSlot.CHEST, namespace + ":" + prefix + "_chest");
        equipIfPresent(mage, EquipmentSlot.LEGS, namespace + ":" + prefix + "_legs");
        equipIfPresent(mage, EquipmentSlot.FEET, namespace + ":" + prefix + "_feet");
    }

    private void equipIfPresent(Mob mob, EquipmentSlot slot, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return;
        BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR).ifPresent(item -> {
            mob.setItemSlot(slot, new ItemStack(item));
            mob.setDropChance(slot, 0.0F);
        });
    }

    private void tryReplaceArrowWithSkill(EntityJoinLevelEvent event, AbstractArrow arrow) {
        if (!(arrow.getOwner() instanceof Mob guard) || !GarrisonEntityTags.isManagedGuard(guard)) {
            return;
        }
        Entity targetEntity = guard.getTarget();
        if (!(targetEntity instanceof net.minecraft.world.entity.LivingEntity target)
                || !target.isAlive() || GarrisonEntityTags.isFriendlyToMage(target)) {
            return;
        }

        long now = guard.level().getGameTime();
        if (now < guard.getPersistentData().getLong(GarrisonEntityTags.NEXT_ARCHER_SKILL)) {
            return;
        }
        ResourceLocation spellId = ResourceLocation.tryParse(
                guard.getPersistentData().getString(GarrisonEntityTags.ARCHER_SKILL));
        if (spellId == null) {
            return;
        }
        var entry = SpellRegistry.from(guard.level()).getHolder(spellId).orElse(null);
        if (entry == null || guard.getRandom().nextDouble() >= archerSkillChance(guard)) {
            return;
        }

        event.setCanceled(true);
        int configured = GarrisonConfig.ARCHER_SKILL_COOLDOWN.get();
        int spellCooldown = Mth.ceil(entry.value().cost.cooldown.duration * 20.0F);
        guard.getPersistentData().putLong(GarrisonEntityTags.NEXT_ARCHER_SKILL,
                now + Math.max(configured, spellCooldown));
        pendingArcherCasts.add(new PendingArcherCast(guard, target, spellId));
    }

    private double archerSkillChance(Mob guard) {
        double rangedDamage = 0.0;
        ResourceLocation attributeId = ResourceLocation.fromNamespaceAndPath("ranged_weapon", "damage");
        var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
        if (attribute != null && guard.getAttribute(attribute) != null) {
            rangedDamage = Math.max(0.0, guard.getAttributeValue(attribute));
        }
        return Math.min(GarrisonConfig.ARCHER_TRIGGER_MAX_CHANCE.get(),
                GarrisonConfig.ARCHER_TRIGGER_BASE_CHANCE.get()
                        + rangedDamage * GarrisonConfig.ARCHER_TRIGGER_PER_RANGED_DAMAGE.get());
    }

    private void faceTarget(Mob guard, net.minecraft.world.entity.LivingEntity target) {
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

    private void updateMageTargets(ServerLevel level) {
        int discoveryRadius = GarrisonConfig.PLAYER_DISCOVERY_RADIUS.get();
        Set<Mob> managedMages = new HashSet<>();
        if (level.players().isEmpty()) {
            // Normally player-local boxes keep this cheap. Forced chunks still deserve protection
            // while a server is empty, so fall back to the currently loaded entity set only then.
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Mob mob && isManagedMage(mob)) managedMages.add(mob);
            });
        } else {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(discoveryRadius);
                managedMages.addAll(level.getEntitiesOfClass(Mob.class, box, this::isManagedMage));
            }
        }

        int targetRadius = GarrisonConfig.MAGE_TARGET_RADIUS.get();
        for (Mob mage : managedMages) {
            // Guard Villagers applies its default hand loadout during insertion/loading; restore the
            // immutable mage loadout after that lifecycle has completed.
            applyMageEquipment(mage);
            if (mage.getTarget() != null && mage.getTarget().isAlive()) {
                continue;
            }
            AABB targetBox = mage.getBoundingBox().inflate(targetRadius);
            level.getEntitiesOfClass(Mob.class, targetBox, this::isValidMageTarget).stream()
                    .min(Comparator.comparingDouble(mage::distanceToSqr))
                    .ifPresent(mage::setTarget);
        }
    }

    private boolean isManagedMage(Mob mob) {
        return mob.isAlive() && GarrisonEntityTags.isManagedMage(mob);
    }

    private boolean isValidMageTarget(Mob mob) {
        if (!mob.isAlive() || mob instanceof Creeper && !GarrisonConfig.MAGES_TARGET_CREEPERS.get()) {
            return false;
        }
        return mob instanceof Enemy && !GarrisonEntityTags.isManaged(mob);
    }

    private record PendingArcherCast(Mob guard, net.minecraft.world.entity.LivingEntity target,
                                     ResourceLocation spell) {
    }
}
