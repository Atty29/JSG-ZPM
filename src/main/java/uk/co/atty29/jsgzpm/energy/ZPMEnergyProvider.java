package uk.co.atty29.jsgzpm.energy;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item capability for a ZPM.
 *
 * The loose item deliberately refuses normal FE insert/extract calls. Charging
 * and discharging are performed by JSG-ZPM's generator/holder logic so players
 * cannot bypass the designed ZPM infrastructure with an arbitrary battery
 * charger. The capability still exposes stored/capacity information to other
 * mods and diagnostics.
 */
public final class ZPMEnergyProvider implements ICapabilitySerializable<CompoundTag> {
    private final LongEnergyStorage storage;
    private final LazyOptional<IEnergyStorage> energyCapability;

    public ZPMEnergyProvider(long capacity) {
        this.storage = new LongEnergyStorage(capacity, 0L, 0L);
        this.energyCapability = LazyOptional.of(() -> storage);
    }

    public LongEnergyStorage storage() {
        return storage;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return storage.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        storage.deserializeNBT(nbt);
    }
}
