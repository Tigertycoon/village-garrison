package de.niklas.villagegarrison.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class MageLoadoutTest {
    @Test void acceptsTrimmedCaseInsensitiveTier() {
        MageLoadout loadout = MageLoadout.parse(" wizards:fire_staff | wizards:meteor | advanced ");
        assertNotNull(loadout);
        assertTrue(loadout.advanced());
        assertEquals("fire", loadout.school());
    }

    @Test void basicLoadoutRemainsBasic() {
        MageLoadout loadout = MageLoadout.parse("wizards:frost_wand|wizards:frost_shard|BASIC");
        assertNotNull(loadout);
        assertFalse(loadout.advanced());
        assertEquals("frost", loadout.school());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "x:y|x:z", "x:y|x:z|BASIC|extra", "Bad ID|x:z|BASIC", "x:y|Bad ID|BASIC", "x:y|x:z|ADVANCE", "x:y|x:z|"})
    void invalidLoadoutsAreRejected(String input) { assertNull(MageLoadout.parse(input)); }
}
