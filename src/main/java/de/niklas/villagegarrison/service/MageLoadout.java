package de.niklas.villagegarrison.service;

import net.minecraft.resources.ResourceLocation;

public record MageLoadout(ResourceLocation item, ResourceLocation spell, boolean advanced) {
    public static MageLoadout parse(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }
        ResourceLocation item = ResourceLocation.tryParse(parts[0].trim());
        ResourceLocation spell = ResourceLocation.tryParse(parts[1].trim());
        String tier = parts[2].trim();
        if (item == null || spell == null
                || (!tier.equalsIgnoreCase("BASIC") && !tier.equalsIgnoreCase("ADVANCED"))) {
            return null;
        }
        return new MageLoadout(item, spell, tier.equalsIgnoreCase("ADVANCED"));
    }

    public String school() {
        String value = item.getPath() + " " + spell.getPath();
        if (value.contains("fire")) return "fire";
        if (value.contains("frost") || value.contains("ice")) return "frost";
        if (value.contains("arcane")) return "arcane";
        if (value.contains("wind") || value.contains("air") || value.contains("aeternium")) return "wind";
        if (value.contains("aqua") || value.contains("water") || value.contains("crystal")) return "aqua";
        if (value.contains("terra") || value.contains("earth") || value.contains("ruby")) return "terra";
        return "generic";
    }
}
