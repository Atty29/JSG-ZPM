package uk.co.atty29.jsgzpm.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Registry-based support for the public 1.20.1 DHD crystals; no private class linkage. */
public final class DHDUpgradeCompat {
    private DHDUpgradeCompat() {}
    public static boolean item(ItemStack stack, String path) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return !stack.isEmpty() && id != null && (id.getNamespace().equals("jsg") || id.getNamespace().equals("jsg_core")) && id.getPath().equals(path);
    }
    public static boolean upgrade(ItemStack stack) {
        return item(stack,"crystal_glyph_dhd") || item(stack,"crystal_upgrade_capacity") || item(stack,"crystal_upgrade_efficiency");
    }
    public static double setting(String name, double fallback) {
        for(String type : new String[]{"dev.tauri.jsg.config.JSGConfig$DialHomeDevice", "dev.tauri.jsg.api.config.JSGConfig$DialHomeDevice"}) {
            try {
                Object value = JSGDHDCompat.call(Class.forName(type).getField(name).get(null), "get");
                if(value instanceof Number n) return n.doubleValue();
            } catch(ReflectiveOperationException | LinkageError ignored) {}
        }
        return fallback;
    }
    public static int overlayTint(ItemStack stack) {
        Object overlay=overlay(stack);
        if(overlay==null)return 0xffffff;
        try {
            Class<?> registry=Class.forName("dev.tauri.jsg.stargate.BiomeOverlayRegistry");
            for(String name:new String[]{"FROST","MOSSY","AGED","SOOTY"}) {
                if(overlay.equals(registry.getField(name).get(null)))return switch(name){case "FROST"->0xbeddeb;case "MOSSY"->0x829e65;case "AGED"->0xbba67b;default->0x666b72;};
            }
        }catch(ReflectiveOperationException | LinkageError ignored){}
        return 0xffffff;
    }
    public static Object overlay(ItemStack stack) {
        return JSGDHDCompat.staticCall("dev.tauri.jsg.stargate.BiomeOverlayRegistry", "getBiomeOverlayByItem", stack);
    }
}
