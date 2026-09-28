package de.niklas.villagegarrison.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GarrisonDataTest {
    @Test void spawnTimesSurviveSaveAndReloadForEveryRole() {
        GarrisonData data = new GarrisonData();
        BlockPos bell = new BlockPos(-200, 64, 900);
        var entry = data.getOrCreate(bell, 24_000);
        data.setNextSpawn(entry, DefenderRole.GUARD, 30_000);
        data.setNextSpawn(entry, DefenderRole.PRIEST, 31_000);
        data.setNextSpawn(entry, DefenderRole.MAGE, 32_000);
        GarrisonData restored = GarrisonData.load(data.save(new CompoundTag(), null), null);
        var reloaded = restored.getOrCreate(bell, 24_100);
        assertEquals(30_000, reloaded.nextSpawn(DefenderRole.GUARD));
        assertEquals(31_000, reloaded.nextSpawn(DefenderRole.PRIEST));
        assertEquals(32_000, reloaded.nextSpawn(DefenderRole.MAGE));
    }

    @Test void twoVillagesHaveIndependentRespawnSchedules() {
        GarrisonData data = new GarrisonData();
        var first = data.getOrCreate(new BlockPos(0, 64, 0), 100);
        var second = data.getOrCreate(new BlockPos(500, 64, 500), 100);
        data.setNextSpawn(first, DefenderRole.MAGE, 500);
        assertEquals(100, second.nextSpawn(DefenderRole.MAGE));
    }

    @Test void lastSeenUpdateMarksDataForSaving() {
        GarrisonData data = new GarrisonData();
        data.getOrCreate(BlockPos.ZERO, 100);
        data.setDirty(false);
        data.getOrCreate(BlockPos.ZERO, 200);
        assertTrue(data.isDirty());
        assertEquals(200, data.save(new CompoundTag(), null)
                .getList("Garrisons", 10).getCompound(0).getLong("LastSeen"));
    }

    @Test void retentionKeepsRecentlySeenVillageAfterReload() {
        GarrisonData data = new GarrisonData();
        data.getOrCreate(BlockPos.ZERO, 0);
        data.getOrCreate(new BlockPos(100, 64, 0), 95);
        GarrisonData restored = GarrisonData.load(data.save(new CompoundTag(), null), null);
        restored.prune(100, 10);
        assertEquals(1, restored.save(new CompoundTag(), null).getList("Garrisons", 10).size());
        assertTrue(restored.isDirty());
    }
}
