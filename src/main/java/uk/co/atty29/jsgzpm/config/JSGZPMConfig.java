package uk.co.atty29.jsgzpm.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JSGZPMConfig {
    public static final long DEFAULT_ZPM_CAPACITY = 100_000_000_000L;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.LongValue ZPM_CAPACITY;

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

        SPEC = builder.build();
    }

    private JSGZPMConfig() {
    }
}
