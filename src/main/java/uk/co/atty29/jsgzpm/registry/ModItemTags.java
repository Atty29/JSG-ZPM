package uk.co.atty29.jsgzpm.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import uk.co.atty29.jsgzpm.JSGZPM;

/**
 * Compatibility tags keep JSG-ZPM recipes independent from JSG's internal Java
 * package layout and allow the current jsg/jsg_core registry split to evolve.
 */
public final class ModItemTags {
    public static final TagKey<Item> ENERGY_CRYSTAL_BASIC = compat("energy_crystal_basic");
    public static final TagKey<Item> ENERGY_CRYSTAL_ADVANCED = compat("energy_crystal_advanced");
    public static final TagKey<Item> ENERGY_CRYSTAL_ULTIMATE = compat("energy_crystal_ultimate");
    public static final TagKey<Item> CIRCUIT_CONTROL_CRYSTAL = compat("circuit_control_crystal");
    public static final TagKey<Item> CIRCUIT_CONTROL_NAQUADAH = compat("circuit_control_naquadah");
    public static final TagKey<Item> RED_CRYSTAL = compat("red_crystal");
    public static final TagKey<Item> ENDER_CRYSTAL = compat("ender_crystal");

    private ModItemTags() {
    }

    private static TagKey<Item> compat(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(JSGZPM.MOD_ID, "compat/" + path));
    }
}
