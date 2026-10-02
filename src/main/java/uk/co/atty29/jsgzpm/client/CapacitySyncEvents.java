package uk.co.atty29.jsgzpm.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import uk.co.atty29.jsgzpm.JSGZPM;
import uk.co.atty29.jsgzpm.config.CapacitySync;

@Mod.EventBusSubscriber(modid = JSGZPM.MOD_ID, value = Dist.CLIENT)
public final class CapacitySyncEvents {
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CapacitySync.clearClientCapacity();
    }
}
