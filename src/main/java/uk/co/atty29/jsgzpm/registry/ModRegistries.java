package uk.co.atty29.jsgzpm.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.atty29.jsgzpm.JSGZPM;
import uk.co.atty29.jsgzpm.block.AncientPowerControllerBlock;
import uk.co.atty29.jsgzpm.block.ZPMHolderBlock;
import uk.co.atty29.jsgzpm.blockentity.AncientPowerControllerBlockEntity;
import uk.co.atty29.jsgzpm.blockentity.ZPMHolderBlockEntity;
import uk.co.atty29.jsgzpm.holder.ZPMHolderLayout;
import uk.co.atty29.jsgzpm.item.ZPMItem;
import uk.co.atty29.jsgzpm.recipe.ZPMAssemblyRecipe;

public final class ModRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, JSGZPM.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JSGZPM.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, JSGZPM.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, JSGZPM.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, JSGZPM.MOD_ID);

    public static final RegistryObject<Block> ATLANTIS_ZPM_HUB = BLOCKS.register("atlantis_zpm_hub", () -> new ZPMHolderBlock(ZPMHolderLayout.HUB));
    public static final RegistryObject<Block> ANCIENT_ZPM_ARRAY = BLOCKS.register("ancient_zpm_array", () -> new ZPMHolderBlock(ZPMHolderLayout.ARRAY));
    public static final RegistryObject<Block> ANCIENT_ZPM_COLUMN = BLOCKS.register("ancient_zpm_column", () -> new ZPMHolderBlock(ZPMHolderLayout.COLUMN));
    public static final RegistryObject<Block> ANCIENT_POWER_CONTROLLER = BLOCKS.register("ancient_power_controller", AncientPowerControllerBlock::new);

    public static final RegistryObject<Item> ZERO_POINT_MODULE = ITEMS.register("zero_point_module", () -> new ZPMItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CRYSTAL_BINDER = ITEMS.register("crystal_binder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ZERO_POINT_CONTAINMENT_MATRIX = ITEMS.register("zero_point_containment_matrix", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CENTRAL_POWER_REGULATOR = ITEMS.register("central_power_regulator", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ATLANTIS_ZPM_HUB_ITEM = ITEMS.register("atlantis_zpm_hub", () -> new BlockItem(ATLANTIS_ZPM_HUB.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_ZPM_ARRAY_ITEM = ITEMS.register("ancient_zpm_array", () -> new BlockItem(ANCIENT_ZPM_ARRAY.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_ZPM_COLUMN_ITEM = ITEMS.register("ancient_zpm_column", () -> new BlockItem(ANCIENT_ZPM_COLUMN.get(), new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_POWER_CONTROLLER_ITEM = ITEMS.register("ancient_power_controller", () -> new BlockItem(ANCIENT_POWER_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<ZPMHolderBlockEntity>> ZPM_HOLDER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "zpm_holder",
            () -> BlockEntityType.Builder.of(
                    ZPMHolderBlockEntity::new,
                    ATLANTIS_ZPM_HUB.get(),
                    ANCIENT_ZPM_ARRAY.get(),
                    ANCIENT_ZPM_COLUMN.get()
            ).build(null)
    );

    public static final RegistryObject<BlockEntityType<AncientPowerControllerBlockEntity>> ANCIENT_POWER_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "ancient_power_controller",
            () -> BlockEntityType.Builder.of(
                    AncientPowerControllerBlockEntity::new,
                    ANCIENT_POWER_CONTROLLER.get()
            ).build(null)
    );

    public static final RegistryObject<RecipeSerializer<ZPMAssemblyRecipe>> ZPM_ASSEMBLY_SERIALIZER = RECIPE_SERIALIZERS.register(
            "zpm_assembly",
            () -> new SimpleCraftingRecipeSerializer<>(ZPMAssemblyRecipe::new)
    );

    private ModRegistries() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
