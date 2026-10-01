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
public final class AtlantisPegasusDHDBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public final DHDInventory inventory = new DHDInventory(this);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> items = net.minecraftforge.common.util.LazyOptional.of(() -> inventory);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluids = net.minecraftforge.common.util.LazyOptional.of(() -> inventory.tank);
    public void inventoryChanged() { syncCustomState(); }
    @Override public Component getDisplayName() { return Component.translatable("block.jsgzpm.atlantis_pegasus_dhd"); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory player,net.minecraft.world.entity.player.Player who) {
        return new uk.co.atty29.jsgzpm.menu.AtlantisDHDMenu(id,player,this);
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,@Nullable net.minecraft.core.Direction side) {
        if(cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)return items.cast();
        if(cap==net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)return fluids.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();items.invalidate();fluids.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();items=net.minecraftforge.common.util.LazyOptional.of(()->inventory);fluids=net.minecraftforge.common.util.LazyOptional.of(()->inventory.tank); }
    public void dropInventory() {
        if(level==null||level.isClientSide)return;
        for(int i=0;i<inventory.getSlots();i++) {
            net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.getStackInSlot(i));
            inventory.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
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
    private boolean cleanedLegacy;
    private boolean gateEngaged;
    public boolean isGateEngaged(){return gateEngaged;}
    private int[] dialedSymbols=new int[0];
    public int[] getDialedSymbols(){return dialedSymbols.clone();}

    public AtlantisPegasusDHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ATLANTIS_PEGASUS_DHD_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtlantisPegasusDHDBlockEntity dhd) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if(!dhd.cleanedLegacy) {
            uk.co.atty29.jsgzpm.block.AtlantisPegasusDHDBlock.removeLegacyWings(level,pos,state);
            dhd.cleanedLegacy=true;
        }
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
            int[] dialed=uk.co.atty29.jsgzpm.compat.JSGDHDCompat.entered(gate);
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

        dhd.inventory.tick();
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
        if(!inventory.control()) {
            player.displayClientMessage(Component.translatable("message.jsgzpm.dhd.control_required"),true);return false;
        }
        var symbols=JSGGateCompat.getPressableSymbols();
        if(index<0||index>=symbols.size())return false;
        Object symbol=symbols.get(index);
        boolean core=Boolean.TRUE.equals(uk.co.atty29.jsgzpm.compat.JSGDHDCompat.call(symbol,"brb"));
        boolean origin=Boolean.TRUE.equals(uk.co.atty29.jsgzpm.compat.JSGDHDCompat.call(symbol,"origin"));
        int entered=uk.co.atty29.jsgzpm.compat.JSGDHDCompat.entered(gate).length;
        if(!uk.co.atty29.jsgzpm.holder.DHDUpgradeRules.allows(inventory.control(),inventory.has("crystal_glyph_dhd"),core,origin,entered,uk.co.atty29.jsgzpm.compat.JSGDHDCompat.engaged(gate))) {
            player.displayClientMessage(Component.translatable("message.jsgzpm.dhd.glyph_required"),true);return false;
        }
        if (!JSGGateCompat.pressPegasusSymbol(gate, index, player)) return false;

        dialedSymbols=uk.co.atty29.jsgzpm.compat.JSGDHDCompat.entered(gate);
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
    public BlockEntity getServerGate() {
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
        tag.put("Upgrades",inventory.save());
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
        if(tag.contains("Upgrades"))inventory.load(tag.getCompound("Upgrades"));
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
