package uk.co.atty29.jsgzpm.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.lang.reflect.Method;

/** Reads the full long-backed energy value from JSG energy crystals when available. */
public final class JSGEnergyReader {
    private JSGEnergyReader() {
    }

    public static long getStoredEnergy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(JSGEnergyReader::readStorage)
                .orElse(0L);
    }

    private static long readStorage(IEnergyStorage storage) {
        Long reflected = invokeLongGetter(storage, "getTrueEnergyStored");
        if (reflected != null) {
            return Math.max(0L, reflected);
        }

        reflected = invokeLongGetter(storage, "getEnergyStoredLong");
        if (reflected != null) {
            return Math.max(0L, reflected);
        }

        return Math.max(0, storage.getEnergyStored());
    }

    private static Long invokeLongGetter(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // Fall through to the normal Forge Energy API.
        }
        return null;
    }
}
