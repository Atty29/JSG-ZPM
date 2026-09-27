package uk.co.atty29.jsgzpm;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import uk.co.atty29.jsgzpm.config.JSGZPMConfig;
import uk.co.atty29.jsgzpm.registry.ModCreativeTabs;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

@Mod(JSGZPM.MOD_ID)
public final class JSGZPM {
    public static final String MOD_ID = "jsgzpm";

    public JSGZPM() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistries.register(modBus);
        ModCreativeTabs.register(modBus);

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.SERVER,
                JSGZPMConfig.SPEC,
                "jsgzpm-server.toml"
        );
    }
}
