package uk.co.atty29.jsgzpm.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import uk.co.atty29.jsgzpm.JSGZPM;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JSGZPM.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.jsgzpm.main"))
                    .icon(() -> new ItemStack(ModRegistries.ZERO_POINT_MODULE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModRegistries.ZERO_POINT_MODULE.get());
                        output.accept(ModRegistries.CRYSTAL_BINDER.get());
                        output.accept(ModRegistries.ZERO_POINT_CONTAINMENT_MATRIX.get());
                        output.accept(ModRegistries.CENTRAL_POWER_REGULATOR.get());
                        output.accept(ModRegistries.ATLANTIS_ZPM_HUB_ITEM.get());
                        output.accept(ModRegistries.ANCIENT_ZPM_ARRAY_ITEM.get());
                        output.accept(ModRegistries.ANCIENT_ZPM_COLUMN_ITEM.get());
                        output.accept(ModRegistries.ANCIENT_POWER_CONTROLLER_ITEM.get());
                        output.accept(ModRegistries.ZERO_POINT_ENERGY_GENERATOR_CONTROLLER_ITEM.get());
                        output.accept(ModRegistries.ZERO_POINT_GENERATOR_CASING_ITEM.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
    }
}
