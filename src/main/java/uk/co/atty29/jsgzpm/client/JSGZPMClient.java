package uk.co.atty29.jsgzpm.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import uk.co.atty29.jsgzpm.JSGZPM;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

@Mod.EventBusSubscriber(modid = JSGZPM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JSGZPMClient {
    private JSGZPMClient() {
    }

    @SubscribeEvent
    public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,tint) -> {
            if(tint!=1||level==null||pos==null)return 0xffffff;
            if(level.getBlockEntity(pos) instanceof uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity dhd)
                return uk.co.atty29.jsgzpm.compat.DHDUpgradeCompat.overlayTint(dhd.inventory.getStackInSlot(5));
            return 0xffffff;
        },ModRegistries.ATLANTIS_PEGASUS_DHD.get());
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ModRegistries.ATLANTIS_DHD_MENU.get(),AtlantisDHDScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistries.ZPM_HOLDER_BLOCK_ENTITY.get(), ZPMHolderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModRegistries.ZERO_POINT_ENERGY_GENERATOR_BLOCK_ENTITY.get(), ZeroPointEnergyGeneratorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModRegistries.ATLANTIS_PEGASUS_DHD_BLOCK_ENTITY.get(), AtlantisPegasusDHDBlockEntityRenderer::new);
    }
}
