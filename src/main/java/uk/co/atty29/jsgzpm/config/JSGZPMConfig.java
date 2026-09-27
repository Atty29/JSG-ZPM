package uk.co.atty29.jsgzpm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JSGZPMConfig {
    public static final long DEFAULT_ZPM_CAPACITY = 100_000_000_000L;
    public static final int DEFAULT_BANK_CONTROLLER_RADIUS = 32;
    public static final int DEFAULT_BANK_MAX_HOLDERS = 64;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.LongValue ZPM_CAPACITY;
    public static final ForgeConfigSpec.IntValue BANK_CONTROLLER_RADIUS;
    public static final ForgeConfigSpec.IntValue BANK_MAX_HOLDERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("zpm");
        ZPM_CAPACITY = builder
                .comment("Maximum energy stored by a standard Zero Point Module, in FE.")
                .defineInRange(
                        "capacity",
                        DEFAULT_ZPM_CAPACITY,
                        1L,
                        Long.MAX_VALUE
                );
        builder.pop();

        builder.push("bank");
        BANK_CONTROLLER_RADIUS = builder
                .comment("Maximum block radius in which an Ancient Power Controller can claim ZPM holders. Only loaded chunks are scanned; the controller never chunk-loads holders.")
                .defineInRange(
                        "controllerRadius",
                        DEFAULT_BANK_CONTROLLER_RADIUS,
                        4,
                        128
                );
        BANK_MAX_HOLDERS = builder
                .comment("Maximum number of ZPM holders managed by one Ancient Power Controller. Each holder contains three ZPM slots.")
                .defineInRange(
                        "maxHolders",
                        DEFAULT_BANK_MAX_HOLDERS,
                        1,
                        256
                );
        builder.pop();

        SPEC = builder.build();
    }

    private JSGZPMConfig() {
    }
}
