package de.niklas.villagegarrison;

import de.niklas.villagegarrison.config.GarrisonConfig;
import de.niklas.villagegarrison.service.GarrisonService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VillageGarrison.MOD_ID)
public final class VillageGarrison {
    public static final String MOD_ID = "village_garrison";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VillageGarrison(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, GarrisonConfig.SPEC);
        NeoForge.EVENT_BUS.register(new GarrisonService());
        LOGGER.info("Village Garrison initialized");
    }
}
