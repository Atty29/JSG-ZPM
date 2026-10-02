package uk.co.atty29.jsgzpm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.config.JSGZPMConfig;
import uk.co.atty29.jsgzpm.energy.LongEnergyStorage;
import uk.co.atty29.jsgzpm.energy.ZPMEnergyProvider;
import uk.co.atty29.jsgzpm.util.EnergyFormat;

import java.util.List;
import java.util.Locale;

public final class ZPMItem extends Item {
    public ZPMItem(Properties properties) {
        super(properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ZPMEnergyProvider(getConfiguredCapacity());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long capacity = getCapacity(stack);
        double percent = capacity <= 0L ? 0.0D : (double) stored * 100.0D / (double) capacity;

        tooltip.add(Component.translatable(
                "tooltip.jsgzpm.zpm.charge",
                String.format(Locale.ROOT, "%.1f%%", percent)
        ).withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable(
                "tooltip.jsgzpm.zpm.energy",
                EnergyFormat.format(stored),
                EnergyFormat.format(capacity)
        ).withStyle(ChatFormatting.GRAY));
    }

    public static long getStoredEnergy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage instanceof LongEnergyStorage longStorage
                        ? longStorage.getEnergyStoredLong()
                        : (long) storage.getEnergyStored())
                .orElse(0L);
    }

    public static long getCapacity(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage instanceof LongEnergyStorage longStorage
                        ? longStorage.getMaxEnergyStoredLong()
                        : (long) storage.getMaxEnergyStored())
                .orElseGet(ZPMItem::getConfiguredCapacity);
    }

    public static void setStoredEnergy(ItemStack stack, long amount) {
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage instanceof LongEnergyStorage longStorage) {
                longStorage.setEnergyStoredLong(amount);
            }
        });
    }

    /** Internal charging path for the future Zero Point Energy Generator. */
    public static long chargeInternal(ItemStack stack, long requested) {
        if (requested <= 0L) {
            return 0L;
        }

        final long[] accepted = {0L};
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage instanceof LongEnergyStorage longStorage) {
                long space = Math.max(0L, longStorage.getMaxEnergyStoredLong() - longStorage.getEnergyStoredLong());
                accepted[0] = Math.min(requested, space);
                longStorage.setEnergyStoredLong(longStorage.getEnergyStoredLong() + accepted[0]);
            }
        });
        return accepted[0];
    }

    /** Internal discharge path for ZPM hubs/arrays/columns. */
    public static long dischargeInternal(ItemStack stack, long requested) {
        if (requested <= 0L) {
            return 0L;
        }

        final long[] extracted = {0L};
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage instanceof LongEnergyStorage longStorage) {
                extracted[0] = Math.min(requested, longStorage.getEnergyStoredLong());
                longStorage.setEnergyStoredLong(longStorage.getEnergyStoredLong() - extracted[0]);
            }
        });
        return extracted[0];
    }

    public static long getConfiguredCapacity() {
        Long remote = uk.co.atty29.jsgzpm.config.CapacitySync.clientCapacity();
        if (remote != null) return remote;
        try {
            return JSGZPMConfig.ZPM_CAPACITY.get();
        } catch (IllegalStateException ignored) {
            return JSGZPMConfig.DEFAULT_ZPM_CAPACITY;
        }
    }
}
