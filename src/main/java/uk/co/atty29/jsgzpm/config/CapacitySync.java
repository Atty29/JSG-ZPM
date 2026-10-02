package uk.co.atty29.jsgzpm.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import uk.co.atty29.jsgzpm.JSGZPM;

/** COMMON configs live in config/, so capacity needs explicit server-to-client synchronization. */
public final class CapacitySync {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JSGZPM.MOD_ID, "capacity"), () -> "1", "1"::equals, "1"::equals);
    private static volatile Long remoteCapacity;
    private static long lastBroadcast = -1L;

    public static void register() {
        CHANNEL.messageBuilder(Capacity.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> buffer.writeLong(message.value()))
                .decoder(buffer -> new Capacity(buffer.readLong()))
                .consumerMainThread((message, context) -> {
                    if (message.value() > 0L) remoteCapacity = message.value();
                    context.get().setPacketHandled(true);
                }).add();
        MinecraftForge.EVENT_BUS.addListener(CapacitySync::onLogin);
        MinecraftForge.EVENT_BUS.addListener(CapacitySync::onServerTick);
    }

    public static Long clientCapacity() {
        return EffectiveSide.get().isClient() ? remoteCapacity : null;
    }

    public static void clearClientCapacity() {
        remoteCapacity = null;
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Capacity(JSGZPMConfig.ZPM_CAPACITY.get()));
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long capacity = JSGZPMConfig.ZPM_CAPACITY.get();
        if (capacity == lastBroadcast) return;
        lastBroadcast = capacity;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Capacity(capacity));
            }
        }
    }

    private record Capacity(long value) { }
    private CapacitySync() { }
}
