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
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistries.ZPM_HOLDER_BLOCK_ENTITY.get(), ZPMHolderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModRegistries.ZERO_POINT_ENERGY_GENERATOR_BLOCK_ENTITY.get(), ZeroPointEnergyGeneratorBlockEntityRenderer::new);
    }
}
