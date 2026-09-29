package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.compat.JSGGateCompat;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

/**
 * Atlantis Pegasus DHD state owned entirely by JSG-ZPM.
 *
 * Gate interaction is delegated to JSGGateCompat so released JSG 5.1.x builds
 * do not need to contain the private DHD implementation classes used by newer
 * JSG development builds.
 */
public final class AtlantisPegasusDHDBlockEntity extends BlockEntity {
    public static final int DISPLAY_BUTTON_COUNT = 37;
    private static final int AUTO_RELINK_INTERVAL = 100;
    private static final int INCOMING_CHECK_INTERVAL = 5;

    @Nullable
    private BlockPos linkedGatePos;
    private boolean generalAlarmActive;
    private boolean offworldAlarmActive;
    private boolean lastIncoming;
    private int lastPressedIndex = -1;
    private int pressFlashTicks;
    private int relinkTicker;
    private boolean gateEngaged;
    public boolean isGateEngaged(){return gateEngaged;}
    private int[] dialedSymbols=new int[0];
    public int[] getDialedSymbols(){return dialedSymbols.clone();}

    public AtlantisPegasusDHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ATLANTIS_PEGASUS_DHD_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtlantisPegasusDHDBlockEntity dhd) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        boolean changed = false;
        if (dhd.pressFlashTicks > 0) {
            dhd.pressFlashTicks--;
            if (dhd.pressFlashTicks == 0 && dhd.lastPressedIndex != -1) {
                dhd.lastPressedIndex = -1;
                changed = true;
            }
        }

        dhd.relinkTicker++;
        if (dhd.linkedGatePos == null && dhd.relinkTicker >= AUTO_RELINK_INTERVAL) {
            dhd.relinkTicker = 0;
            BlockPos found = JSGGateCompat.findNearestPegasusGate(serverLevel, pos);
            if (found != null) {
                dhd.linkedGatePos = found;
                changed = true;
            }
        }

        if (level.getGameTime() % INCOMING_CHECK_INTERVAL == 0L) {
            BlockEntity gate = JSGGateCompat.getLinkedGate(serverLevel, dhd.linkedGatePos);
            boolean engaged=uk.co.atty29.jsgzpm.compat.JSGDHDCompat.engaged(gate);
            if(engaged!=dhd.gateEngaged){dhd.gateEngaged=engaged;changed=true;}
            int[] dialed=uk.co.atty29.jsgzpm.compat.JSGDHDCompat.dialed(gate);
            if(!java.util.Arrays.equals(dialed,dhd.dialedSymbols)){dhd.dialedSymbols=dialed;changed=true;}
            if (gate == null && dhd.linkedGatePos != null) {
                dhd.linkedGatePos = null;
                dhd.lastIncoming = false;
                if (dhd.offworldAlarmActive) {
                    dhd.offworldAlarmActive = false;
                }
                changed = true;
            } else {
                boolean incoming = JSGGateCompat.isIncoming(gate);
                if (incoming != dhd.lastIncoming) {
                    dhd.lastIncoming = incoming;
                    dhd.offworldAlarmActive = incoming;
                    changed = true;
                }
            }
        }

        if (changed) dhd.syncCustomState();
    }

    public boolean pressSymbol(int index, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockEntity gate = JSGGateCompat.getLinkedGate(serverLevel, linkedGatePos);
        if (gate == null) {
            BlockPos found = JSGGateCompat.findNearestPegasusGate(serverLevel, worldPosition);
            if (found == null) return false;
            linkedGatePos = found;
            gate = JSGGateCompat.getLinkedGate(serverLevel, linkedGatePos);
        }
        if (!JSGGateCompat.pressPegasusSymbol(gate, index, player)) return false;

        lastPressedIndex = index;
        pressFlashTicks = 20;
        syncCustomState();
        return true;
    }

    public int getLastPressedIndex() {
        return lastPressedIndex;
    }

    public boolean hasLinkedGate() {
        return linkedGatePos != null;
    }

    public boolean hasProtection() {
        return JSGGateCompat.hasProtection(getServerGate());
    }

    public boolean linkedGateUsesShield() {
        return JSGGateCompat.usesShield(getServerGate());
    }

    public boolean isProtectionClosed() {
        return JSGGateCompat.isProtectionClosed(getServerGate());
    }

    public Component setProtectionClosed(boolean closed) {
        BlockEntity gate = getServerGate();
        if (gate == null) return Component.translatable("message.jsgzpm.dhd.not_linked");
        if (!JSGGateCompat.hasProtection(gate)) return Component.translatable("message.jsgzpm.dhd.no_protection");

        boolean shield = JSGGateCompat.usesShield(gate);
        if (!JSGGateCompat.setProtectionClosed(gate, closed)) {
            return Component.translatable("message.jsgzpm.dhd.protection_busy");
        }

        if (shield) {
            return Component.translatable(closed ? "message.jsgzpm.dhd.shield_on" : "message.jsgzpm.dhd.shield_off");
        }
        return Component.translatable(closed ? "message.jsgzpm.dhd.iris_closed" : "message.jsgzpm.dhd.iris_open");
    }

    public Component toggleGeneralAlarm() {
        generalAlarmActive = !generalAlarmActive;
        syncCustomState();
        return Component.translatable(generalAlarmActive
                ? "message.jsgzpm.dhd.general_alarm_on"
                : "message.jsgzpm.dhd.general_alarm_off");
    }

    public Component resetAlarms() {
        generalAlarmActive = false;
        offworldAlarmActive = lastIncoming;
        syncCustomState();
        return Component.translatable("message.jsgzpm.dhd.alarms_reset");
    }

    public boolean isGeneralAlarmActive() {
        return generalAlarmActive;
    }

    public boolean isOffworldAlarmActive() {
        return offworldAlarmActive;
    }

    public boolean isAnyAlarmActive() {
        return generalAlarmActive || offworldAlarmActive;
    }

    public Component relink() {
        if (!(level instanceof ServerLevel serverLevel)) return Component.empty();
        linkedGatePos = JSGGateCompat.findNearestPegasusGate(serverLevel, worldPosition);
        lastIncoming = false;
        offworldAlarmActive = false;
        syncCustomState();
        return Component.translatable(linkedGatePos != null
                ? "message.jsgzpm.dhd.linked"
                : "message.jsgzpm.dhd.not_linked");
    }

    @Nullable
    public BlockPos getLinkedGatePos() {
        return linkedGatePos;
    }

    @Nullable
    private BlockEntity getServerGate() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return JSGGateCompat.getLinkedGate(serverLevel, linkedGatePos);
    }

    private void syncCustomState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedGatePos != null) tag.putLong("LinkedGate", linkedGatePos.asLong());
        tag.putBoolean("GeneralAlarm", generalAlarmActive);
        tag.putBoolean("OffworldAlarm", offworldAlarmActive);
        tag.putBoolean("LastIncoming", lastIncoming);
        tag.putBoolean("GateEngaged",gateEngaged);
        tag.putIntArray("DialedSymbols",dialedSymbols);
        tag.putInt("LastPressedIndex", lastPressedIndex);
        tag.putInt("PressFlashTicks", pressFlashTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedGatePos = tag.contains("LinkedGate") ? BlockPos.of(tag.getLong("LinkedGate")) : null;
        gateEngaged=tag.getBoolean("GateEngaged");
        dialedSymbols=tag.getIntArray("DialedSymbols");
        generalAlarmActive = tag.getBoolean("GeneralAlarm");
        offworldAlarmActive = tag.getBoolean("OffworldAlarm");
        lastIncoming = tag.getBoolean("LastIncoming");
        lastPressedIndex = tag.contains("LastPressedIndex") ? tag.getInt("LastPressedIndex") : -1;
        pressFlashTicks = tag.getInt("PressFlashTicks");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-2, 0, -2), worldPosition.offset(3, 2, 3));
    }
}
