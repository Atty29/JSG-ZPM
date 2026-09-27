package uk.co.atty29.jsgzpm.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import uk.co.atty29.jsgzpm.compat.JSGEnergyReader;
import uk.co.atty29.jsgzpm.item.ZPMItem;
import uk.co.atty29.jsgzpm.registry.ModItemTags;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

public final class ZPMAssemblyRecipe extends CustomRecipe {
    private static final int[] ENERGY_SLOTS = {0, 2, 3, 5, 6, 8};

    public ZPMAssemblyRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level level) {
        if (inventory.getWidth() != 3 || inventory.getHeight() != 3) {
            return false;
        }

        return inventory.getItem(0).is(ModItemTags.ENERGY_CRYSTAL_BASIC)
                && inventory.getItem(1).is(ModRegistries.CENTRAL_POWER_REGULATOR.get())
                && inventory.getItem(2).is(ModItemTags.ENERGY_CRYSTAL_BASIC)
                && inventory.getItem(3).is(ModItemTags.ENERGY_CRYSTAL_ADVANCED)
                && inventory.getItem(4).is(ModRegistries.CRYSTAL_BINDER.get())
                && inventory.getItem(5).is(ModItemTags.ENERGY_CRYSTAL_ADVANCED)
                && inventory.getItem(6).is(ModItemTags.ENERGY_CRYSTAL_ULTIMATE)
                && inventory.getItem(7).is(ModRegistries.ZERO_POINT_CONTAINMENT_MATRIX.get())
                && inventory.getItem(8).is(ModItemTags.ENERGY_CRYSTAL_ULTIMATE);
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        ItemStack output = new ItemStack(ModRegistries.ZERO_POINT_MODULE.get());
        long inheritedEnergy = 0L;

        for (int slot : ENERGY_SLOTS) {
            long crystalEnergy = JSGEnergyReader.getStoredEnergy(inventory.getItem(slot));
            if (Long.MAX_VALUE - inheritedEnergy < crystalEnergy) {
                inheritedEnergy = Long.MAX_VALUE;
                break;
            }
            inheritedEnergy += crystalEnergy;
        }

        ZPMItem.setStoredEnergy(output, Math.min(inheritedEnergy, ZPMItem.getCapacity(output)));
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ModRegistries.ZERO_POINT_MODULE.get());
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRegistries.ZPM_ASSEMBLY_SERIALIZER.get();
    }
}
