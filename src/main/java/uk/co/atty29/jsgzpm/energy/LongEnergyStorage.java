package uk.co.atty29.jsgzpm.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Energy storage that keeps its real state in longs while exposing the normal
 * Forge Energy interface for compatibility with ordinary FE/RF-style networks.
 *
 * Forge's IEnergyStorage API uses ints per call, so callers using that API see
 * clamped values and transfer in API-safe chunks. Native JSG-ZPM logic should
 * use the long methods when working with very large capacities.
 */
public class LongEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {
    private static final String NBT_ENERGY = "Energy";

    protected long capacity;
    protected final long maxReceive;
    protected final long maxExtract;
    protected long energy;

    public LongEnergyStorage(long capacity) {
        this(capacity, Long.MAX_VALUE, Long.MAX_VALUE, 0L);
    }

    public LongEnergyStorage(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, 0L);
    }

    public LongEnergyStorage(long capacity, long maxReceive, long maxExtract, long initialEnergy) {
        if (capacity < 0L || maxReceive < 0L || maxExtract < 0L) {
            throw new IllegalArgumentException("Energy capacity and transfer limits must be non-negative");
        }

        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = clamp(initialEnergy, 0L, capacity);
    }

    public void setCapacity(long capacity) {
        if(capacity<0)throw new IllegalArgumentException("Negative capacity");
        this.capacity=capacity;
        setEnergyStoredLong(energy);
    }

    public long receiveEnergyLong(long requested, boolean simulate) {
        if (!canReceive() || requested <= 0L) {
            return 0L;
        }

        long accepted = Math.min(requested, maxReceive);
        accepted = Math.min(accepted, capacity - energy);

        if (!simulate) {
            energy += accepted;
            onEnergyChanged();
        }

        return accepted;
    }

    public long extractEnergyLong(long requested, boolean simulate) {
        if (!canExtract() || requested <= 0L) {
            return 0L;
        }

        long extracted = Math.min(requested, maxExtract);
        extracted = Math.min(extracted, energy);

        if (!simulate) {
            energy -= extracted;
            onEnergyChanged();
        }

        return extracted;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return safeInt(receiveEnergyLong(Math.max(0, maxReceive), simulate));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return safeInt(extractEnergyLong(Math.max(0, maxExtract), simulate));
    }

    @Override
    public int getEnergyStored() {
        return safeInt(energy);
    }

    @Override
    public int getMaxEnergyStored() {
        return safeInt(capacity);
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0L;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0L;
    }

    public long getEnergyStoredLong() {
        return energy;
    }

    public long getMaxEnergyStoredLong() {
        return capacity;
    }

    public long getMaxReceiveLong() {
        return maxReceive;
    }

    public long getMaxExtractLong() {
        return maxExtract;
    }

    public void setEnergyStoredLong(long energy) {
        long clamped = clamp(energy, 0L, capacity);
        if (this.energy != clamped) {
            this.energy = clamped;
            onEnergyChanged();
        }
    }

    public double getChargeFraction() {
        if (capacity <= 0L) {
            return 0.0D;
        }
        return (double) energy / (double) capacity;
    }

    protected void onEnergyChanged() {
        // Hook for future ItemStack/BlockEntity implementations to mark data dirty.
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(NBT_ENERGY, energy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setEnergyStoredLong(nbt.getLong(NBT_ENERGY));
    }

    private static int safeInt(long value) {
        return (int) Math.min(Math.max(value, 0L), Integer.MAX_VALUE);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }
}
