package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.block.ZPMHolderBlock;
import uk.co.atty29.jsgzpm.holder.ZPMHolderLayout;
import uk.co.atty29.jsgzpm.holder.ZPMSlotState;
import uk.co.atty29.jsgzpm.item.ZPMItem;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

public final class ZPMHolderBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 3;
    private static final float ANIMATION_STEP = 1.0F / 20.0F;
    private static final int NETWORK_VALIDATION_INTERVAL = 40;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return validSlot(slot) && stack.is(ModRegistries.ZERO_POINT_MODULE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ZPMSlotState[] slotStates = {
            ZPMSlotState.EMPTY, ZPMSlotState.EMPTY, ZPMSlotState.EMPTY
    };
    private final float[] animationProgress = {0.0F, 0.0F, 0.0F};
    private final int[] supplyingTicks = {0, 0, 0};

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(HolderEnergyStorage::new);
    @Nullable
    private BlockPos networkController;
    private int networkValidationTicker;

    public ZPMHolderBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ZPM_HOLDER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ZPMHolderBlockEntity holder) {
        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (holder.supplyingTicks[i] > 0) {
                holder.supplyingTicks[i]--;
                if (holder.supplyingTicks[i] == 0 && holder.slotStates[i] == ZPMSlotState.DOWN_SUPPLYING) {
                    holder.slotStates[i] = ZPMSlotState.DOWN_STANDBY;
                    changed = true;
                }
            }
        }
        changed |= holder.advanceAnimations();

        holder.networkValidationTicker++;
        if (holder.networkValidationTicker >= NETWORK_VALIDATION_INTERVAL) {
            holder.networkValidationTicker = 0;
            holder.validateNetworkController();
        }

        holder.updatePedestalLight();
        if (changed) holder.sync();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ZPMHolderBlockEntity holder) {
        holder.advanceAnimations();
    }

    private boolean advanceAnimations() {
        boolean terminalChange = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slotStates[i] == ZPMSlotState.MOVING_DOWN) {
                animationProgress[i] = Math.min(1.0F, animationProgress[i] + ANIMATION_STEP);
                if (animationProgress[i] >= 1.0F) {
                    slotStates[i] = items.getStackInSlot(i).isEmpty() ? ZPMSlotState.EMPTY : ZPMSlotState.DOWN_STANDBY;
                    terminalChange = true;
                }
            } else if (slotStates[i] == ZPMSlotState.MOVING_UP) {
                animationProgress[i] = Math.max(0.0F, animationProgress[i] - ANIMATION_STEP);
                if (animationProgress[i] <= 0.0F) {
                    slotStates[i] = items.getStackInSlot(i).isEmpty() ? ZPMSlotState.EMPTY : ZPMSlotState.UP;
                    terminalChange = true;
                }
            }
        }
        return terminalChange;
    }

    public boolean insertZPM(int slot, ItemStack held) {
        if (!validSlot(slot) || held.isEmpty() || !held.is(ModRegistries.ZERO_POINT_MODULE.get())) return false;
        if (!items.getStackInSlot(slot).isEmpty() || slotStates[slot].isMoving() || slotStates[slot].isDown()) return false;

        ItemStack inserted = held.copy();
        inserted.setCount(1);
        items.setStackInSlot(slot, inserted);
        slotStates[slot] = holderLayout() == ZPMHolderLayout.PEDESTAL ? ZPMSlotState.DOWN_STANDBY : ZPMSlotState.UP;
        animationProgress[slot] = holderLayout() == ZPMHolderLayout.PEDESTAL ? 1.0F : 0.0F;
        sync();
        return true;
    }

    public ItemStack removeZPM(int slot) {
        if (!validSlot(slot) || (holderLayout() != ZPMHolderLayout.PEDESTAL && slotStates[slot] != ZPMSlotState.UP)) return ItemStack.EMPTY;
        ItemStack removed = items.extractItem(slot, 1, false);
        if (!removed.isEmpty()) {
            slotStates[slot] = ZPMSlotState.EMPTY;
            animationProgress[slot] = 0.0F;
            sync();
        }
        return removed;
    }

    public boolean toggleSlot(int slot) {
        if (holderLayout() == ZPMHolderLayout.PEDESTAL) return false;
        if (!validSlot(slot) || items.getStackInSlot(slot).isEmpty()) return false;
        if (slotStates[slot] == ZPMSlotState.UP) {
            slotStates[slot] = ZPMSlotState.MOVING_DOWN;
            sync();
            return true;
        }
        if (slotStates[slot].isDown()) {
            slotStates[slot] = ZPMSlotState.MOVING_UP;
            supplyingTicks[slot] = 0;
            sync();
            return true;
        }
        return false;
    }

    public ItemStack getZPM(int slot) {
        return validSlot(slot) ? items.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public ZPMSlotState getSlotState(int slot) {
        return validSlot(slot) ? slotStates[slot] : ZPMSlotState.EMPTY;
    }

    public float getAnimationProgress(int slot) {
        return validSlot(slot) ? animationProgress[slot] : 0.0F;
    }

    public int getInstalledZPMCount() {
        int count = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }

    public int getActiveZPMCount() {
        int count = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty() && slotStates[i].isDown()) count++;
        }
        return count;
    }

    @Nullable
    public BlockPos getNetworkController() {
        return networkController;
    }

    public boolean isNetworked() {
        return networkController != null;
    }

    public boolean claimController(BlockPos controllerPos) {
        if (networkController != null && !networkController.equals(controllerPos)) return false;
        if (networkController == null) {
            networkController = controllerPos.immutable();
            refreshEnergyCapability();
            sync();
        }
        return true;
    }

    public void releaseController(BlockPos controllerPos) {
        if (networkController == null || !networkController.equals(controllerPos)) return;
        networkController = null;
        refreshEnergyCapability();
        sync();
    }

    public long getAvailableEnergyLong() {
        long total = 0L;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!slotStates[i].isDown()) continue;
            long value = ZPMItem.getStoredEnergy(items.getStackInSlot(i));
            total = saturatingAdd(total, value);
        }
        return total;
    }

    public long getAvailableCapacityLong() {
        long total = 0L;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!slotStates[i].isDown()) continue;
            long value = ZPMItem.getCapacity(items.getStackInSlot(i));
            total = saturatingAdd(total, value);
        }
        return total;
    }

    public long extractEnergyLong(long requested, boolean simulate) {
        if (requested <= 0L) return 0L;
        long remaining = requested;
        long extracted = 0L;
        boolean visualChange = false;

        for (int i = 0; i < SLOT_COUNT && remaining > 0L; i++) {
            if (!slotStates[i].isDown()) continue;
            ItemStack zpm = items.getStackInSlot(i);
            if (zpm.isEmpty()) continue;

            long available = ZPMItem.getStoredEnergy(zpm);
            long take = Math.min(remaining, available);
            if (take <= 0L) continue;

            long actual = simulate ? take : ZPMItem.dischargeInternal(zpm, take);
            extracted += actual;
            remaining -= actual;

            if (!simulate && actual > 0L) {
                supplyingTicks[i] = 4;
                if (slotStates[i] != ZPMSlotState.DOWN_SUPPLYING) {
                    slotStates[i] = ZPMSlotState.DOWN_SUPPLYING;
                    visualChange = true;
                }
            }
        }

        if (!simulate && extracted > 0L) {
            setChanged();
            updatePedestalLight();
            if (visualChange) sync();
        }
        return extracted;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack.copy());
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
            slotStates[i] = ZPMSlotState.EMPTY;
            animationProgress[i] = 0.0F;
            supplyingTicks[i] = 0;
        }
        setChanged();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && networkController == null) return energyCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(HolderEnergyStorage::new);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        int[] states = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            states[i] = slotStates[i].ordinal();
            tag.putFloat("Progress" + i, animationProgress[i]);
        }
        tag.putIntArray("SlotStates", states);
        if (networkController != null) tag.putLong("NetworkController", networkController.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        int[] states = tag.getIntArray("SlotStates");
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i < states.length && states[i] >= 0 && states[i] < ZPMSlotState.values().length) {
                slotStates[i] = ZPMSlotState.values()[states[i]];
            } else {
                slotStates[i] = items.getStackInSlot(i).isEmpty() ? ZPMSlotState.EMPTY : ZPMSlotState.UP;
            }
            animationProgress[i] = tag.contains("Progress" + i) ? tag.getFloat("Progress" + i) : (slotStates[i].isDown() ? 1.0F : 0.0F);
            if (items.getStackInSlot(i).isEmpty()) {
                slotStates[i] = ZPMSlotState.EMPTY;
                animationProgress[i] = 0.0F;
            }
        }
        if (holderLayout() == ZPMHolderLayout.PEDESTAL) {
            for (int i = 1; i < SLOT_COUNT; i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
                slotStates[i] = ZPMSlotState.EMPTY;
                animationProgress[i] = 0.0F;
            }
            if (!items.getStackInSlot(0).isEmpty()) {
                slotStates[0] = ZPMSlotState.DOWN_STANDBY;
                animationProgress[0] = 1.0F;
            }
        }
        networkController = tag.contains("NetworkController") ? BlockPos.of(tag.getLong("NetworkController")) : null;
        refreshEnergyCapability();
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
        ZPMHolderLayout layout = holderLayout();
        if (layout == ZPMHolderLayout.ARRAY) {
            Direction side = getBlockState().getValue(ZPMHolderBlock.FACING).getClockWise();
            BlockPos a = worldPosition.relative(side.getOpposite());
            BlockPos b = worldPosition.relative(side);
            return new AABB(a).minmax(new AABB(b.above(2))).inflate(0.5D);
        }
        if (layout == ZPMHolderLayout.COLUMN) {
            return new AABB(worldPosition.below()).minmax(new AABB(worldPosition.above(2))).inflate(0.5D);
        }
        return new AABB(worldPosition).inflate(0.75D, 1.25D, 0.75D);
    }

    private void validateNetworkController() {
        if (!(level instanceof ServerLevel serverLevel) || networkController == null) return;
        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(networkController.getX() >> 4, networkController.getZ() >> 4);
        if (chunk == null) return;
        BlockEntity blockEntity = chunk.getBlockEntity(networkController);
        if (!(blockEntity instanceof AncientPowerControllerBlockEntity)) {
            networkController = null;
            refreshEnergyCapability();
            sync();
        }
    }

    private void refreshEnergyCapability() {
        energyCapability.invalidate();
        energyCapability = LazyOptional.of(HolderEnergyStorage::new);
    }

    private void updatePedestalLight() {
        if (level == null || level.isClientSide || holderLayout() != ZPMHolderLayout.PEDESTAL) return;
        boolean charged = ZPMItem.getStoredEnergy(items.getStackInSlot(0)) > 0L;
        BlockState state = getBlockState();
        if (state.getValue(ZPMHolderBlock.LIT) != charged) {
            level.setBlock(worldPosition, state.setValue(ZPMHolderBlock.LIT, charged), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            updatePedestalLight();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private ZPMHolderLayout holderLayout() {
        if (getBlockState().getBlock() instanceof ZPMHolderBlock holderBlock) return holderBlock.layout();
        return ZPMHolderLayout.HUB;
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < (holderLayout() == ZPMHolderLayout.PEDESTAL ? 1 : SLOT_COUNT);
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0L && Long.MAX_VALUE - a < b) return Long.MAX_VALUE;
        return a + b;
    }

    private final class HolderEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return safeInt(extractEnergyLong(Math.max(0, maxExtract), simulate));
        }

        @Override
        public int getEnergyStored() {
            return safeInt(getAvailableEnergyLong());
        }

        @Override
        public int getMaxEnergyStored() {
            return safeInt(getAvailableCapacityLong());
        }

        @Override
        public boolean canExtract() {
            return getAvailableEnergyLong() > 0L;
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        private int safeInt(long value) {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
        }
    }
}
