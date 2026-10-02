package uk.co.atty29.jsgzpm.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import uk.co.atty29.jsgzpm.compat.JSGEnergyReader;
import uk.co.atty29.jsgzpm.item.ZPMItem;
import uk.co.atty29.jsgzpm.registry.ModItemTags;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

/** A visible shaped recipe whose result inherits the input crystals' energy. */
public final class ZPMAssemblyRecipe extends ShapedRecipe {
    public ZPMAssemblyRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
        ItemStack output = new ItemStack(ModRegistries.ZERO_POINT_MODULE.get());
        long inheritedEnergy = 0L;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack ingredient = inventory.getItem(slot);
            if (!ingredient.is(ModItemTags.ENERGY_CRYSTAL_BASIC)
                    && !ingredient.is(ModItemTags.ENERGY_CRYSTAL_ADVANCED)
                    && !ingredient.is(ModItemTags.ENERGY_CRYSTAL_ULTIMATE)) continue;
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
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRegistries.ZPM_ASSEMBLY_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<ZPMAssemblyRecipe> {
        private final ShapedRecipe.Serializer delegate = new ShapedRecipe.Serializer();

        @Override
        public ZPMAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ZPMAssemblyRecipe(delegate.fromJson(id, json));
        }

        @Override
        public ZPMAssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ZPMAssemblyRecipe(delegate.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ZPMAssemblyRecipe recipe) {
            delegate.toNetwork(buffer, recipe);
        }
    }
}
