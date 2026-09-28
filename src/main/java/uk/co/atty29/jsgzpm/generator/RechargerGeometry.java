package uk.co.atty29.jsgzpm.generator;

/** Dimensions and bounded visual controls, independent of the world or charging math. */
public final class RechargerGeometry {
    private RechargerGeometry() {}
    public static final double BORE_RADIUS = .94D;
    public static final double SHIELD_DEPTH = 1.845D;
    public static final double MODULE_DEPTH = 1.07D;
    public static final float MODULE_SCALE = .40F;
    public static double slotU(int slot) { return (slot - 1) * .44D; }
    public static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    public static double shieldInnerRadius(float progress) { return BORE_RADIUS * (1 - clamp(progress)); }
    public static float gasAlpha(float progress) { return .24F * clamp(progress); }
    public static float sweep(double angle, double time) {
        return (float) Math.pow(.5D + .5D * Math.cos(angle - time * .09D), 12);
    }
}
