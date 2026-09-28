package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.block.ZeroPointEnergyGeneratorControllerBlock;
import uk.co.atty29.jsgzpm.config.JSGZPMConfig;
import uk.co.atty29.jsgzpm.generator.GeneratorGeometry;
import uk.co.atty29.jsgzpm.generator.GeneratorState;
import uk.co.atty29.jsgzpm.item.ZPMItem;
import uk.co.atty29.jsgzpm.registry.ModItemTags;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ZeroPointEnergyGeneratorBlockEntity extends BlockEntity {
    public static final int ZPM_SLOT_COUNT = 3;
    public static final int UPGRADE_SLOT_COUNT = 5;
    private static final int UPGRADE_SLOT_START = ZPM_SLOT_COUNT;
    private static final int TOTAL_SLOTS = ZPM_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    private static final float VISUAL_STEP = 1.0F / 20.0F;
    private static final int STRUCTURE_CHECK_INTERVAL = 20;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= 0 && slot < ZPM_SLOT_COUNT) return stack.is(ModRegistries.ZERO_POINT_MODULE.get());
            if (slot >= UPGRADE_SLOT_START && slot < TOTAL_SLOTS) return stack.is(ModItemTags.EFFICIENCY_UPGRADE_CRYSTAL);
            return false;
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

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(GeneratorEnergyStorage::new);
    private GeneratorState generatorState = GeneratorState.IDLE;
    private boolean formed;
    private float shieldProgress;
    private float cosmicProgress;
    private int efficiencyRemainder;
    private int structureTicker;

    public ZeroPointEnergyGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ZERO_POINT_ENERGY_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ZeroPointEnergyGeneratorBlockEntity generator) {
        generator.structureTicker++;
        if (generator.structureTicker >= STRUCTURE_CHECK_INTERVAL) {
            generator.structureTicker = 0;
            generator.refreshStructure();
        }

        generator.advanceVisualState(true);
        if (generator.generatorState == GeneratorState.CHARGING && !generator.hasChargeableZPM()) {
            generator.beginVenting();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ZeroPointEnergyGeneratorBlockEntity generator) {
        generator.advanceVisualState(false);
        if (generator.generatorState == GeneratorState.CHARGING && generator.cosmicProgress > 0.05F) {
            generator.spawnChargingParticles();
        }
    }

    public GeneratorState getGeneratorState() {
        return generatorState;
    }

    public boolean isFormed() {
        return formed;
    }

    public float getShieldProgress() {
        return shieldProgress;
    }

    public float getCosmicProgress() {
        return cosmicProgress;
    }

    public Direction getMountNormal() {
        BlockState state = getBlockState();
        return state.hasProperty(ZeroPointEnergyGeneratorControllerBlock.FACING)
                ? state.getValue(ZeroPointEnergyGeneratorControllerBlock.FACING)
                : Direction.UP;
    }

    public ItemStack getZPM(int slot) {
        return slot >= 0 && slot < ZPM_SLOT_COUNT ? items.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public int insertZPM(ItemStack held) {
        if (generatorState.isLocked()) return -1;
        for (int slot = 0; slot < ZPM_SLOT_COUNT; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) continue;
            ItemStack inserted = held.copy();
            inserted.setCount(1);
            items.setStackInSlot(slot, inserted);
            sync();
            return slot;
        }
        return -1;
    }

    public boolean insertEfficiencyUpgrade(ItemStack held) {
        if (generatorState.isLocked() || !held.is(ModItemTags.EFFICIENCY_UPGRADE_CRYSTAL)) return false;
        for (int slot = UPGRADE_SLOT_START; slot < TOTAL_SLOTS; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) continue;
            ItemStack inserted = held.copy();
            inserted.setCount(1);
            items.setStackInSlot(slot, inserted);
            sync();
            return true;
        }
        return false;
    }

    public ItemStack removeLastZPM() {
        if (generatorState.isLocked()) return ItemStack.EMPTY;
        for (int slot = ZPM_SLOT_COUNT - 1; slot >= 0; slot--) {
            ItemStack removed = items.extractItem(slot, 1, false);
            if (!removed.isEmpty()) {
                sync();
                return removed;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeLastEfficiencyUpgrade() {
        if (generatorState.isLocked()) return ItemStack.EMPTY;
        for (int slot = TOTAL_SLOTS - 1; slot >= UPGRADE_SLOT_START; slot--) {
            ItemStack removed = items.extractItem(slot, 1, false);
            if (!removed.isEmpty()) {
                sync();
                return removed;
            }
        }
        return ItemStack.EMPTY;
    }

    public int getInstalledZPMCount() {
        int count = 0;
        for (int slot = 0; slot < ZPM_SLOT_COUNT; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) count++;
        }
        return count;
    }

    public int getEfficiencyUpgradeCount() {
        int count = 0;
        for (int slot = UPGRADE_SLOT_START; slot < TOTAL_SLOTS; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) count++;
        }
        return count;
    }

    public int getEfficiencyPercent() {
        int base = getConfiguredBaseEfficiency();
        int perUpgrade = getConfiguredEfficiencyPerUpgrade();
        return Math.min(100, base + getEfficiencyUpgradeCount() * perUpgrade);
    }

    public boolean hasChargeableZPM() {
        return getTotalDeficitLong() > 0L;
    }

    public long getTotalDeficitLong() {
        long total = 0L;
        for (int slot = 0; slot < ZPM_SLOT_COUNT; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            long deficit = Math.max(0L, ZPMItem.getCapacity(stack) - ZPMItem.getStoredEnergy(stack));
            total = saturatingAdd(total, deficit);
        }
        return total;
    }

    public void refreshStructure() {
        if (level == null) return;
        boolean nowFormed = true;
        Direction normal = getMountNormal();
        for (int u = -1; u <= 1 && nowFormed; u++) {
            for (int v = -1; v <= 1; v++) {
                if (u == 0 && v == 0) continue;
                BlockPos casingPos = GeneratorGeometry.planeOffset(worldPosition, normal, u, v);
                if (!level.getBlockState(casingPos).is(ModRegistries.ZERO_POINT_GENERATOR_CASING.get())) {
                    nowFormed = false;
                    break;
                }
            }
        }

        if (formed != nowFormed) {
            formed = nowFormed;
            if (!formed && generatorState.isLocked()) beginVenting();
            sync();
        }
    }

    public boolean startCharging() {
        if (generatorState != GeneratorState.IDLE) return false;
        refreshStructure();
        if (!formed || !hasChargeableZPM()) return false;
        generatorState = GeneratorState.SEALING;
        shieldProgress = 0.0F;
        cosmicProgress = 0.0F;
        efficiencyRemainder = 0;
        sync();
        return true;
    }

    public boolean stopCharging() {
        if (generatorState == GeneratorState.IDLE || generatorState == GeneratorState.VENTING) return false;
        beginVenting();
        return true;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack.copy());
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        setChanged();
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
        energyCapability = LazyOptional.of(GeneratorEnergyStorage::new);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("GeneratorState", generatorState.ordinal());
        tag.putBoolean("Formed", formed);
        tag.putFloat("ShieldProgress", shieldProgress);
        tag.putFloat("CosmicProgress", cosmicProgress);
        tag.putInt("EfficiencyRemainder", efficiencyRemainder);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        int stateOrdinal = tag.getInt("GeneratorState");
        generatorState = stateOrdinal >= 0 && stateOrdinal < GeneratorState.values().length
                ? GeneratorState.values()[stateOrdinal]
                : GeneratorState.IDLE;
        formed = tag.getBoolean("Formed");
        shieldProgress = clamp01(tag.getFloat("ShieldProgress"));
        cosmicProgress = clamp01(tag.getFloat("CosmicProgress"));
        efficiencyRemainder = Math.max(0, Math.min(99, tag.getInt("EfficiencyRemainder")));
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
        return new AABB(worldPosition).inflate(2.25D);
    }

    private void advanceVisualState(boolean server) {
        switch (generatorState) {
            case IDLE -> {
                shieldProgress = 0.0F;
                cosmicProgress = 0.0F;
            }
            case SEALING -> {
                shieldProgress = Math.min(1.0F, shieldProgress + VISUAL_STEP);
                cosmicProgress = 0.0F;
                if (server && shieldProgress >= 1.0F) {
                    generatorState = GeneratorState.CHARGING;
                    sync();
                }
            }
            case CHARGING -> {
                shieldProgress = 1.0F;
                cosmicProgress = Math.min(1.0F, cosmicProgress + VISUAL_STEP);
            }
            case VENTING -> {
                if (cosmicProgress > 0.0F) {
                    cosmicProgress = Math.max(0.0F, cosmicProgress - VISUAL_STEP);
                } else {
                    shieldProgress = Math.max(0.0F, shieldProgress - VISUAL_STEP);
                }
                if (server && cosmicProgress <= 0.0F && shieldProgress <= 0.0F) {
                    generatorState = GeneratorState.IDLE;
                    efficiencyRemainder = 0;
                    sync();
                }
            }
        }
    }

    private void beginVenting() {
        if (generatorState == GeneratorState.IDLE || generatorState == GeneratorState.VENTING) return;
        generatorState = GeneratorState.VENTING;
        sync();
    }

    private int acceptExternalEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0 || generatorState != GeneratorState.CHARGING || cosmicProgress < 1.0F || !formed) return 0;

        long deficit = getTotalDeficitLong();
        if (deficit <= 0L) {
            if (!simulate) beginVenting();
            return 0;
        }

        int efficiency = getEfficiencyPercent();
        long candidateScaled = (long) maxReceive * efficiency + efficiencyRemainder;
        long candidateOutput = candidateScaled / 100L;
        int acceptedInput = maxReceive;

        if (candidateOutput > deficit) {
            long scaledNeeded = deficit * 100L - efficiencyRemainder;
            if (scaledNeeded <= 0L) return 0;
            long requiredInput = (scaledNeeded + efficiency - 1L) / efficiency;
            acceptedInput = (int) Math.min((long) maxReceive, Math.max(0L, requiredInput));
        }

        if (simulate || acceptedInput <= 0) return acceptedInput;

        long scaled = (long) acceptedInput * efficiency + efficiencyRemainder;
        long output = Math.min(deficit, scaled / 100L);
        efficiencyRemainder = (int) (scaled % 100L);
        long stored = chargeBalanced(output);

        if (stored > 0L || acceptedInput > 0) setChanged();
        if (!hasChargeableZPM()) beginVenting();
        return acceptedInput;
    }

    private long chargeBalanced(long amount) {
        if (amount <= 0L) return 0L;
        long remaining = amount;
        long stored = 0L;

        while (remaining > 0L) {
            List<Integer> chargeable = new ArrayList<>();
            for (int slot = 0; slot < ZPM_SLOT_COUNT; slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty() && ZPMItem.getStoredEnergy(stack) < ZPMItem.getCapacity(stack)) chargeable.add(slot);
            }
            if (chargeable.isEmpty()) break;

            long share = Math.max(1L, remaining / chargeable.size());
            boolean moved = false;
            for (int slot : chargeable) {
                if (remaining <= 0L) break;
                ItemStack stack = items.getStackInSlot(slot);
                long request = Math.min(share, remaining);
                long accepted = ZPMItem.chargeInternal(stack, request);
                if (accepted > 0L) {
                    stored += accepted;
                    remaining -= accepted;
                    moved = true;
                }
            }
            if (!moved) break;
        }
        return stored;
    }

    private void spawnChargingParticles() {
        if (level == null || level.random.nextInt(2) != 0) return;
        Direction normal = getMountNormal();
        double u = (level.random.nextDouble() - 0.5D) * 1.4D;
        double v = (level.random.nextDouble() - 0.5D) * 1.4D;
        double outward = 0.18D + level.random.nextDouble() * 0.35D;
        Vec3 offset = GeneratorGeometry.localOffset(normal, u, v, outward);
        double x = worldPosition.getX() + 0.5D + offset.x;
        double y = worldPosition.getY() + 0.5D + offset.y;
        double z = worldPosition.getZ() + 0.5D + offset.z;
        level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0D, 0.01D, 0.0D);
        if (level.random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0L && Long.MAX_VALUE - a < b) return Long.MAX_VALUE;
        return a + b;
    }

    private static int safeInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    private static int getConfiguredBaseEfficiency() {
        try {
            return JSGZPMConfig.GENERATOR_BASE_EFFICIENCY_PERCENT.get();
        } catch (IllegalStateException ignored) {
            return JSGZPMConfig.DEFAULT_GENERATOR_BASE_EFFICIENCY_PERCENT;
        }
    }

    private static int getConfiguredEfficiencyPerUpgrade() {
        try {
            return JSGZPMConfig.GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT.get();
        } catch (IllegalStateException ignored) {
            return JSGZPMConfig.DEFAULT_GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT;
        }
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return acceptExternalEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return safeInt(getTotalDeficitLong());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return formed && generatorState == GeneratorState.CHARGING && cosmicProgress >= 1.0F && hasChargeableZPM();
        }
    }
}
