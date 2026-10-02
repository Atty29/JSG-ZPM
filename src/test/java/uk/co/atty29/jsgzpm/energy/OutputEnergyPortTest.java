package uk.co.atty29.jsgzpm.energy;

import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OutputEnergyPortTest {
    @Test void remainsConnectableWhileEmptyOrUnavailable() {
        OutputEnergyPort port = new OutputEnergyPort(() -> null);
        assertTrue(port.canExtract());
        assertFalse(port.canReceive());
        assertEquals(0, port.extractEnergy(100, false));
        assertEquals(0, port.getEnergyStored());
        assertEquals(0, port.receiveEnergy(100, false));
    }

    @Test void cachedPortFollowsControllerClaimAndRelease() {
        EnergyStorage local = new EnergyStorage(1000, 0, 1000, 500);
        EnergyStorage bank = new EnergyStorage(1000, 0, 1000, 700);
        AtomicReference<IEnergyStorage> target = new AtomicReference<>(local);
        OutputEnergyPort port = new OutputEnergyPort(target::get);
        assertEquals(100, port.extractEnergy(100, true));
        assertEquals(500, local.getEnergyStored());
        target.set(bank);
        assertEquals(700, port.getEnergyStored());
        assertEquals(100, port.extractEnergy(100, false));
        assertEquals(500, local.getEnergyStored());
        assertEquals(600, bank.getEnergyStored());
        target.set(null);
        assertEquals(0, port.extractEnergy(100, false));
        target.set(local);
        assertEquals(100, port.extractEnergy(100, false));
        assertEquals(400, local.getEnergyStored());
    }

    @Test void connectedPortCannotBypassReservePolicy() {
        EnergyStorage reserve = new EnergyStorage(1000, 0, 0, 800);
        OutputEnergyPort port = new OutputEnergyPort(() -> reserve);
        assertTrue(port.canExtract());
        assertEquals(800, port.getEnergyStored());
        assertEquals(0, port.extractEnergy(100, false));
        assertEquals(800, reserve.getEnergyStored());
    }
}
