package uk.co.atty29.jsgzpm.holder;

/** Visual mounting axes for the wall array and sloped column. */
public final class WallHolderGeometry {
    private WallHolderGeometry() {}

    public static final float MODULE_SCALE = 0.483333F;
    public static final double TRAVEL = 0.54D;
    public static final double SEATED_OFFSET = -0.512D * MODULE_SCALE;

    public static float tiltDegrees(ZPMHolderLayout layout) {
        return layout == ZPMHolderLayout.ARRAY ? 20.0F : 45.0F;
    }

    public static double centreY(ZPMHolderLayout layout, int slot, float progress) {
        double base = layout == ZPMHolderLayout.ARRAY ? 0.69D : 0.50D + slot - 1;
        return base + Math.cos(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    public static double forwardOffset(ZPMHolderLayout layout, float progress) {
        double base = layout == ZPMHolderLayout.ARRAY ? 0.07D : 0.0D;
        return base + Math.sin(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    private static double distance(float progress) {
        return SEATED_OFFSET + (1.0D - progress) * TRAVEL;
    }
}
