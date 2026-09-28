package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.alarm.AtlantisAlarmState;
import uk.co.atty29.jsgzpm.block.AtlantisAlarmEmitterBlock;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

/**
 * A deliberately link-based alarm endpoint. It stores one DHD position instead
 * of repeatedly searching large areas, making dozens of emitters cheap to run.
 */
public final class AtlantisAlarmEmitterBlockEntity extends BlockEntity {
    private BlockPos linkedDhdPos;
    private ResourceLocation linkedDimension;
    private AtlantisAlarmState alarmState = AtlantisAlarmState.IDLE;
    private int soundClock;

    public AtlantisAlarmEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ATLANTIS_ALARM_EMITTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtlantisAlarmEmitterBlockEntity emitter) {
        AtlantisAlarmState desired = emitter.readLinkedAlarm(level);
        if (desired != emitter.alarmState) {
            emitter.alarmState = desired;
            emitter.soundClock = 0;
            if (state.getValue(AtlantisAlarmEmitterBlock.ALARM) != desired) {
                level.setBlock(pos, state.setValue(AtlantisAlarmEmitterBlock.ALARM, desired), Block.UPDATE_CLIENTS);
            }
            emitter.sync();
        }

        if (desired == AtlantisAlarmState.IDLE) {
            emitter.soundClock = 0;
            return;
        }

        emitter.soundClock++;
        if (desired == AtlantisAlarmState.GENERAL) {
            // Fast alternating electronic pulse. This is an original development
            // placeholder, not audio copied from Stargate Atlantis.
            if (emitter.soundClock % 12 == 1) {
                float pitch = ((emitter.soundClock / 12) & 1) == 0 ? 0.72F : 1.18F;
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.35F, pitch);
            }
        } else {
            // Off-world activation: slower two-tone chime placeholder.
            int phase = emitter.soundClock % 40;
            if (phase == 1) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.2F, 0.78F);
            } else if (phase == 9) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.2F, 1.18F);
            }
        }
    }

    private AtlantisAlarmState readLinkedAlarm(Level level) {
        if (linkedDhdPos == null || linkedDimension == null) return AtlantisAlarmState.IDLE;
        if (!level.dimension().location().equals(linkedDimension)) return AtlantisAlarmState.IDLE;
        if (!level.isLoaded(linkedDhdPos)) return AtlantisAlarmState.IDLE;
        if (!(level.getBlockEntity(linkedDhdPos) instanceof AtlantisPegasusDHDBlockEntity dhd)) {
            return AtlantisAlarmState.IDLE;
        }
        // General alarm intentionally wins if both are active: it represents a
        // manually escalated base emergency rather than a routine gate warning.
        if (dhd.isGeneralAlarmActive()) return AtlantisAlarmState.GENERAL;
        if (dhd.isOffworldAlarmActive()) return AtlantisAlarmState.OFFWORLD;
        return AtlantisAlarmState.IDLE;
    }

    public void linkTo(ResourceLocation dimension, BlockPos dhdPos) {
        this.linkedDimension = dimension;
        this.linkedDhdPos = dhdPos.immutable();
        sync();
    }

    public void clearLink() {
        linkedDimension = null;
        linkedDhdPos = null;
        alarmState = AtlantisAlarmState.IDLE;
        soundClock = 0;
        sync();
    }

    public boolean isLinked() {
        return linkedDhdPos != null && linkedDimension != null;
    }

    @Nullable
    public BlockPos getLinkedDhdPos() {
        return linkedDhdPos;
    }

    @Nullable
    public ResourceLocation getLinkedDimension() {
        return linkedDimension;
    }

    public AtlantisAlarmState getAlarmState() {
        return alarmState;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedDhdPos != null) tag.putLong("LinkedDhdPos", linkedDhdPos.asLong());
        if (linkedDimension != null) tag.putString("LinkedDimension", linkedDimension.toString());
        tag.putString("AlarmState", alarmState.getSerializedName());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedDhdPos = tag.contains("LinkedDhdPos") ? BlockPos.of(tag.getLong("LinkedDhdPos")) : null;
        linkedDimension = tag.contains("LinkedDimension") ? ResourceLocation.tryParse(tag.getString("LinkedDimension")) : null;
        if (tag.contains("AlarmState")) {
            String saved = tag.getString("AlarmState");
            for (AtlantisAlarmState value : AtlantisAlarmState.values()) {
                if (value.getSerializedName().equals(saved)) {
                    alarmState = value;
                    break;
                }
            }
        }
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
}
