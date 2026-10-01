package uk.co.atty29.jsgzpm.energy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LongEnergyStorageTest {
    @Test void capacityIncreasePreservesExistingCharge() {
        var storage=new LongEnergyStorage(100_000_000_000L);
        storage.setEnergyStoredLong(90_000_000_000L);
        storage.setCapacity(200_000_000_000L);
        assertEquals(90_000_000_000L,storage.getEnergyStoredLong());
        assertEquals(110_000_000_000L,storage.receiveEnergyLong(Long.MAX_VALUE,false));
    }
    @Test void loweringCapacityClampsAndPreventsOverfill() {
        var storage=new LongEnergyStorage(100_000_000_000L);
        storage.setEnergyStoredLong(90_000_000_000L);
        storage.setCapacity(5_000_000_000L);
        assertEquals(5_000_000_000L,storage.getEnergyStoredLong());
        assertEquals(0,storage.receiveEnergyLong(1,false));
        assertEquals(Integer.MAX_VALUE,storage.getMaxEnergyStored());
    }
    @Test void maximumLongDoesNotOverflow() {
        var storage=new LongEnergyStorage(Long.MAX_VALUE);
        storage.setEnergyStoredLong(Long.MAX_VALUE-10);
        assertEquals(10,storage.receiveEnergyLong(Long.MAX_VALUE,false));
        assertEquals(Long.MAX_VALUE,storage.extractEnergyLong(Long.MAX_VALUE,false));
        assertEquals(0,storage.getEnergyStoredLong());
    }
    @Test void loadedEnergyUsesNewCapacity() {
        var old=new LongEnergyStorage(100_000_000_000L);old.setEnergyStoredLong(50_000_000_000L);
        var next=new LongEnergyStorage(20_000_000_000L);next.deserializeNBT(old.serializeNBT());
        assertEquals(20_000_000_000L,next.getEnergyStoredLong());
    }
}
