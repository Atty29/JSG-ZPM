package uk.co.atty29.jsgzpm.blockentity;

import dev.tauri.jsg.api.entity.StargateAddressData;
import dev.tauri.jsg.api.item.IDHDFluidTank;
import dev.tauri.jsg.api.item.IDHDPartItem;
import dev.tauri.jsg.api.registry.JSGSymbolTypes;
import dev.tauri.jsg.api.stargate.network.address.symbol.types.SymbolPegasusEnum;
import dev.tauri.jsg.common.blockentity.dialhomedevice.DHDAbstractBE;
import dev.tauri.jsg.common.blockentity.stargate.StargateClassicBaseBE;
import dev.tauri.jsg.common.dialhomedevice.manager.state.DHDAbstractStateManager;
import dev.tauri.jsg.common.dialhomedevice.manager.state.DHDPegasusStateManager;
import dev.tauri.jsg.common.registry.JSGItems;
import dev.tauri.jsg.common.registry.JSGSoundEvents;
import dev.tauri.jsg.common.registry.tags.JSGBlockTags;
import dev.tauri.jsg.core.common.sound.ISoundEvent;
import dev.tauri.jsg.core.common.symbol.SymbolType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Functional Pegasus-only DHD core for the Atlantis floor console.
 *
 * Dialling/linking is provided by JSG's DHD implementation; JSG-ZPM only adds
 * the new physical console, side-panel controls and alarm state.
 */
public final class AtlantisPegasusDHDBlockEntity extends DHDAbstractBE {
    private static final List<SymbolPegasusEnum> PRESSABLE_SYMBOLS = Arrays.stream(SymbolPegasusEnum.values())
            .filter(SymbolPegasusEnum::canBePressed)
            .toList();

    private boolean generalAlarmActive;
    private boolean offworldAlarmActive;
    private boolean lastIncoming;

    public AtlantisPegasusDHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ATLANTIS_PEGASUS_DHD_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected DHDAbstractStateManager<?, ?> createStateManager() {
        return new DHDPegasusStateManager(this);
    }

    @Override
    public SymbolType<SymbolPegasusEnum> getSymbolType() {
        return JSGSymbolTypes.PEGASUS.get();
    }

    @Override
    public TagKey<Block> getLinkableBlocks() {
        return JSGBlockTags.DHD_PEGASUS_LINKABLE_BLOCKS;
    }

    @Override
    public IDHDFluidTank getFluidTankItemPart() {
        return JSGItems.DHD_NAQUADAH_TANK.get();
    }

    @Override
    public ISoundEvent getButtonPressSound() {
        return JSGSoundEvents.DHD_PEGASUS_PRESS;
    }

    @Override
    public ISoundEvent getBRBPressSound() {
        return JSGSoundEvents.DHD_PEGASUS_PRESS_BRB;
    }

    @Override
    public Item getControlCrystal() {
        return JSGItems.PEGASUS_DHD_MAIN_CRYSTAL.get();
    }

    /** The custom console is manufactured as a complete control station. */
    @Override
    public boolean hasControlCrystal() {
        return true;
    }

    @Override
    public boolean isAssembled(IDHDPartItem part) {
        return true;
    }

    @Override
    public boolean isAssembled() {
        return true;
    }

    @Override
    public LinkedList<IDHDPartItem> getAllParts() {
        return new LinkedList<>();
    }

    @Override
    public void tick(Level level) {
        super.tick(level);
        if (level.isClientSide || level.getGameTime() % 5L != 0L) return;

        boolean incoming = getLinkedDeviceOptional()
                .map(gate -> gate.getDialingManager().getStargateState().incoming())
                .orElse(false);
        if (incoming != lastIncoming) {
            lastIncoming = incoming;
            offworldAlarmActive = incoming;
            syncCustomState();
        }
    }

    public static List<SymbolPegasusEnum> getPressableSymbols() {
        return PRESSABLE_SYMBOLS;
    }

    public boolean pressSymbol(int index, ServerPlayer player) {
        if (index < 0 || index >= PRESSABLE_SYMBOLS.size()) return false;
        pushSymbolButton(PRESSABLE_SYMBOLS.get(index), player, false);
        return true;
    }

    public boolean hasLinkedGate() {
        return getLinkedDevice() != null;
    }

    public boolean hasProtection() {
        return getClassicGate() != null && getClassicGate().getIrisManager().hasIris();
    }

    public boolean linkedGateUsesShield() {
        StargateClassicBaseBE<?> gate = getClassicGate();
        return gate != null && gate.getIrisManager().hasShield();
    }

    public boolean isProtectionClosed() {
        StargateClassicBaseBE<?> gate = getClassicGate();
        return gate != null && gate.getIrisManager().isIrisClosed();
    }

    public Component setProtectionClosed(boolean closed) {
        StargateClassicBaseBE<?> gate = getClassicGate();
        if (gate == null) return Component.translatable("message.jsgzpm.dhd.not_linked");
        if (!gate.getIrisManager().hasIris()) return Component.translatable("message.jsgzpm.dhd.no_protection");

        boolean shield = gate.getIrisManager().hasShield();
        boolean already = closed ? gate.getIrisManager().isIrisClosed() : gate.getIrisManager().isIrisOpened();
        if (!already) {
            boolean toggled = gate.getIrisManager().toggleIris();
            gate.setChanged();
            if (!toggled) return Component.translatable("message.jsgzpm.dhd.protection_busy");
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
        // The off-world alarm follows the actual incoming-gate state and cannot
        // be permanently silenced while an incoming activation is still active.
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
        if (level == null || level.isClientSide) return Component.empty();
        updateLinkStatus(level, worldPosition);
        return Component.translatable(isLinked()
                ? "message.jsgzpm.dhd.linked"
                : "message.jsgzpm.dhd.not_linked");
    }

    @Nullable
    private StargateClassicBaseBE<?> getClassicGate() {
        return getLinkedDevice() instanceof StargateClassicBaseBE<?> gate ? gate : null;
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
        tag.putBoolean("GeneralAlarm", generalAlarmActive);
        tag.putBoolean("OffworldAlarm", offworldAlarmActive);
        tag.putBoolean("LastIncoming", lastIncoming);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        generalAlarmActive = tag.getBoolean("GeneralAlarm");
        offworldAlarmActive = tag.getBoolean("OffworldAlarm");
        lastIncoming = tag.getBoolean("LastIncoming");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("GeneralAlarm", generalAlarmActive);
        tag.putBoolean("OffworldAlarm", offworldAlarmActive);
        tag.putBoolean("LastIncoming", lastIncoming);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        generalAlarmActive = tag.getBoolean("GeneralAlarm");
        offworldAlarmActive = tag.getBoolean("OffworldAlarm");
        lastIncoming = tag.getBoolean("LastIncoming");
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
