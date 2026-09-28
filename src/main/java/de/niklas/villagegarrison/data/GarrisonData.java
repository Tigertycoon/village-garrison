package de.niklas.villagegarrison.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class GarrisonData extends SavedData {
    private static final String FILE_NAME = "village_garrison";
    private final Map<Long, Entry> entries = new HashMap<>();

    public static GarrisonData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(GarrisonData::new, GarrisonData::load), FILE_NAME);
    }

    public Entry getOrCreate(BlockPos anchor, long currentTick) {
        long key = anchor.asLong();
        Entry entry = entries.get(key);
        if (entry == null) {
            entry = new Entry(currentTick);
            entries.put(key, entry);
            setDirty();
        }
        if (entry.lastSeenTick != currentTick) {
            entry.lastSeenTick = currentTick;
            setDirty();
        }
        return entry;
    }

    public void setNextSpawn(Entry entry, DefenderRole role, long tick) {
        if (entry.nextSpawnTicks.getOrDefault(role, -1L) != tick) {
            entry.nextSpawnTicks.put(role, tick);
            setDirty();
        }
    }

    public void prune(long currentTick, long maximumAge) {
        if (entries.entrySet().removeIf(e -> currentTick - e.getValue().lastSeenTick > maximumAge)) {
            setDirty();
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((position, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.putLong("Anchor", position);
            saved.putLong("LastSeen", entry.lastSeenTick);
            for (DefenderRole role : DefenderRole.values()) {
                saved.putLong("Next_" + role.serializedName(), entry.nextSpawn(role));
            }
            list.add(saved);
        });
        tag.put("Garrisons", list);
        return tag;
    }

    static GarrisonData load(CompoundTag tag, HolderLookup.Provider registries) {
        GarrisonData data = new GarrisonData();
        ListTag list = tag.getList("Garrisons", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag saved = (CompoundTag) element;
            Entry entry = new Entry(saved.getLong("LastSeen"));
            for (DefenderRole role : DefenderRole.values()) {
                entry.nextSpawnTicks.put(role, saved.getLong("Next_" + role.serializedName()));
            }
            data.entries.put(saved.getLong("Anchor"), entry);
        }
        return data;
    }

    public static final class Entry {
        private final EnumMap<DefenderRole, Long> nextSpawnTicks = new EnumMap<>(DefenderRole.class);
        private long lastSeenTick;

        private Entry(long currentTick) {
            this.lastSeenTick = currentTick;
            for (DefenderRole role : DefenderRole.values()) {
                nextSpawnTicks.put(role, currentTick);
            }
        }

        public long nextSpawn(DefenderRole role) {
            return nextSpawnTicks.getOrDefault(role, 0L);
        }
    }
}
