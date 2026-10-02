package uk.co.atty29.jsgzpm.energy;

import net.minecraftforge.energy.IEnergyStorage;
import java.util.function.Supplier;

/** A stable cable endpoint whose backing storage can change without disconnecting cables. */
public final class OutputEnergyPort implements IEnergyStorage {
    private final Supplier<IEnergyStorage> target;

    public OutputEnergyPort(Supplier<IEnergyStorage> target) {
        this.target = target;
    }

    @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
    @Override public boolean canReceive() { return false; }
    @Override public boolean canExtract() { return true; }

    @Override public int extractEnergy(int amount, boolean simulate) {
        IEnergyStorage storage = target.get();
        return storage == null ? 0 : storage.extractEnergy(Math.max(0, amount), simulate);
    }

    @Override public int getEnergyStored() {
        IEnergyStorage storage = target.get();
        return storage == null ? 0 : storage.getEnergyStored();
    }

    @Override public int getMaxEnergyStored() {
        IEnergyStorage storage = target.get();
        return storage == null ? 0 : storage.getMaxEnergyStored();
    }
}
