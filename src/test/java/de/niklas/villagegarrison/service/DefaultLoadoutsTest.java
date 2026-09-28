package de.niklas.villagegarrison.service;

import de.niklas.villagegarrison.config.GarrisonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import net.spell_engine.api.spell.registry.SpellRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class DefaultLoadoutsTest {
    @Test void baselineMageEquipmentAndSpellsExistInPinnedRuntime(MinecraftServer server) {
        var spells = server.registryAccess().registryOrThrow(SpellRegistry.KEY);
        assertFalse(GarrisonConfig.MAGE_LOADOUTS.get().isEmpty());
        for (String configured : GarrisonConfig.MAGE_LOADOUTS.get()) {
            MageLoadout loadout = MageLoadout.parse(configured);
            assertNotNull(loadout, configured);
            assertTrue(BuiltInRegistries.ITEM.containsKey(loadout.item()), "Missing weapon: " + configured);
            assertTrue(spells.containsKey(loadout.spell()), "Missing spell: " + configured);
        }
    }
}
