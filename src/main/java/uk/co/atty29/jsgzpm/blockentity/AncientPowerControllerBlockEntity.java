package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.config.JSGZPMConfig;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AncientPowerControllerBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL_TICKS = 40;

    private final List<BlockPos> linkedHolders = new ArrayList<>();
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(ControllerEnergyStorage::new);
    private int scanTicker;

    public AncientPowerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ANCIENT_POWER_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AncientPowerControllerBlockEntity controller) {
        controller.scanTicker++;
        if (controller.scanTicker >= SCAN_INTERVAL_TICKS) {
            controller.scanTicker = 0;
            controller.refreshNetwork();
        }
    }

    public void refreshNetwork() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int radius = getConfiguredRadius();
        int maxHolders = getConfiguredMaxHolders();
        long radiusSquared = (long) radius * (long) radius;
        boolean changed = false;

        Set<BlockPos> retained = new LinkedHashSet<>();
        for (BlockPos holderPos : linkedHolders) {
            if (distanceSquared(worldPosition, holderPos) > radiusSquared || retained.size() >= maxHolders) {
                ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
                if (holder != null) holder.releaseController(worldPosition);
                changed = true;
                continue;
            }

            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (holder == null) {
                if (!isChunkLoaded(serverLevel, holderPos)) {
                    retained.add(holderPos.immutable());
                } else {
                    changed = true;
                }
                continue;
            }

            if (holder.claimController(worldPosition)) {
                retained.add(holderPos.immutable());
            } else {
                changed = true;
            }
        }

        linkedHolders.clear();
        linkedHolders.addAll(retained);

        int minChunkX = (worldPosition.getX() - radius) >> 4;
        int maxChunkX = (worldPosition.getX() + radius) >> 4;
        int minChunkZ = (worldPosition.getZ() - radius) >> 4;
        int maxChunkZ = (worldPosition.getZ() + radius) >> 4;

        outer:
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ZPMHolderBlockEntity holder)) continue;
                    BlockPos holderPos = holder.getBlockPos();
                    if (distanceSquared(worldPosition, holderPos) > radiusSquared) continue;
                    if (linkedHolders.contains(holderPos)) continue;
                    if (linkedHolders.size() >= maxHolders) break outer;

                    if (holder.claimController(worldPosition)) {
                        linkedHolders.add(holderPos.immutable());
                        changed = true;
                    }
                }
            }
        }

        List<BlockPos> sorted = new ArrayList<>(linkedHolders);
        sorted.sort(Comparator
                .comparingLong((BlockPos pos) -> distanceSquared(worldPosition, pos))
                .thenComparingLong(BlockPos::asLong));
        if (!sorted.equals(linkedHolders)) {
            linkedHolders.clear();
            linkedHolders.addAll(sorted);
            changed = true;
        }

        if (changed) sync();
    }

    public void releaseAllClaims() {
        if (!(level instanceof ServerLevel serverLevel)) {
            linkedHolders.clear();
            setChanged();
            return;
        }

        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (holder != null) holder.releaseController(worldPosition);
        }
        linkedHolders.clear();
        sync();
    }

    public int getLinkedHolderCount() {
        return linkedHolders.size();
    }

    public int getOnlineHolderCount() {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        int count = 0;
        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (isManagedByThis(holder)) count++;
        }
        return count;
    }

    public int getInstalledZPMCount() {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        int count = 0;
        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (isManagedByThis(holder)) count += holder.getInstalledZPMCount();
        }
        return count;
    }

    public int getActiveZPMCount() {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        int count = 0;
        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (isManagedByThis(holder)) count += holder.getActiveZPMCount();
        }
        return count;
    }

    public long getTotalEnergyLong() {
        if (!(level instanceof ServerLevel serverLevel)) return 0L;
        long total = 0L;
        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (!isManagedByThis(holder)) continue;
            total = saturatingAdd(total, holder.getAvailableEnergyLong());
        }
        return total;
    }

    public long getTotalCapacityLong() {
        if (!(level instanceof ServerLevel serverLevel)) return 0L;
        long total = 0L;
        for (BlockPos holderPos : linkedHolders) {
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (!isManagedByThis(holder)) continue;
            total = saturatingAdd(total, holder.getAvailableCapacityLong());
        }
        return total;
    }

    public long extractEnergyLong(long requested, boolean simulate) {
        if (requested <= 0L || !(level instanceof ServerLevel serverLevel)) return 0L;

        long remaining = requested;
        long extracted = 0L;
        for (BlockPos holderPos : linkedHolders) {
            if (remaining <= 0L) break;
            ZPMHolderBlockEntity holder = getLoadedHolder(serverLevel, holderPos);
            if (!isManagedByThis(holder)) continue;

            long amount = holder.extractEnergyLong(remaining, simulate);
            extracted = saturatingAdd(extracted, amount);
            remaining -= amount;
        }

        if (!simulate && extracted > 0L) setChanged();
        return extracted;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
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
        energyCapability = LazyOptional.of(ControllerEnergyStorage::new);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLongArray("LinkedHolders", linkedHolders.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedHolders.clear();
        for (long packed : tag.getLongArray("LinkedHolders")) {
            linkedHolders.add(BlockPos.of(packed));
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

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private boolean isManagedByThis(@Nullable ZPMHolderBlockEntity holder) {
        return holder != null && worldPosition.equals(holder.getNetworkController());
    }

    @Nullable
    private static ZPMHolderBlockEntity getLoadedHolder(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        BlockEntity blockEntity = chunk.getBlockEntity(pos);
        return blockEntity instanceof ZPMHolderBlockEntity holder ? holder : null;
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0L && Long.MAX_VALUE - a < b) return Long.MAX_VALUE;
        return a + b;
    }

    private static int safeInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    private static int getConfiguredRadius() {
        try {
            return JSGZPMConfig.BANK_CONTROLLER_RADIUS.get();
        } catch (IllegalStateException ignored) {
            return JSGZPMConfig.DEFAULT_BANK_CONTROLLER_RADIUS;
        }
    }

    private static int getConfiguredMaxHolders() {
        try {
            return JSGZPMConfig.BANK_MAX_HOLDERS.get();
        } catch (IllegalStateException ignored) {
            return JSGZPMConfig.DEFAULT_BANK_MAX_HOLDERS;
        }
    }

    private final class ControllerEnergyStorage implements IEnergyStorage {
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
            return safeInt(getTotalEnergyLong());
        }

        @Override
        public int getMaxEnergyStored() {
            return safeInt(getTotalCapacityLong());
        }

        @Override
        public boolean canExtract() {
            return getTotalEnergyLong() > 0L;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
