package uk.co.atty29.jsgzpm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JSGZPMConfig {
    public static final long DEFAULT_ZPM_CAPACITY = 100_000_000_000L;
    public static final int DEFAULT_BANK_CONTROLLER_RADIUS = 32;
    public static final int DEFAULT_BANK_MAX_HOLDERS = 64;
    public static final int DEFAULT_GENERATOR_BASE_EFFICIENCY_PERCENT = 20;
    public static final int DEFAULT_GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT = 16;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.LongValue ZPM_CAPACITY;
    public static final ForgeConfigSpec.IntValue BANK_CONTROLLER_RADIUS;
    public static final ForgeConfigSpec.IntValue BANK_MAX_HOLDERS;
    public static final ForgeConfigSpec.IntValue GENERATOR_BASE_EFFICIENCY_PERCENT;
    public static final ForgeConfigSpec.IntValue GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("zpm");
        ZPM_CAPACITY = builder
                .comment("Maximum energy stored by a standard Zero Point Module, in FE.")
                .defineInRange("capacity", DEFAULT_ZPM_CAPACITY, 1L, Long.MAX_VALUE);
        builder.pop();

        builder.push("bank");
        BANK_CONTROLLER_RADIUS = builder
                .comment("Maximum block radius in which an Ancient Power Controller can claim ZPM holders. Only loaded chunks are scanned; the controller never chunk-loads holders.")
                .defineInRange("controllerRadius", DEFAULT_BANK_CONTROLLER_RADIUS, 4, 128);
        BANK_MAX_HOLDERS = builder
                .comment("Maximum number of ZPM holders managed by one Ancient Power Controller. Each holder contains three ZPM slots.")
                .defineInRange("maxHolders", DEFAULT_BANK_MAX_HOLDERS, 1, 256);
        builder.pop();

        builder.push("generator");
        GENERATOR_BASE_EFFICIENCY_PERCENT = builder
                .comment("Base charging efficiency of the Zero Point Energy Generator with no efficiency crystals installed.")
                .defineInRange("baseEfficiencyPercent", DEFAULT_GENERATOR_BASE_EFFICIENCY_PERCENT, 1, 100);
        GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT = builder
                .comment("Efficiency percentage points added by each JSG efficiency upgrade crystal. Final efficiency is capped at 100 percent.")
                .defineInRange("efficiencyPerUpgradePercent", DEFAULT_GENERATOR_EFFICIENCY_PER_UPGRADE_PERCENT, 0, 100);
        builder.pop();

        SPEC = builder.build();
    }

    private JSGZPMConfig() {
    }
}
