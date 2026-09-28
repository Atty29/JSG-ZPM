package uk.co.atty29.jsgzpm.holder;

/** Visual mounting axes for the wall array and horizontal column. */
public final class WallHolderGeometry {
    private WallHolderGeometry() {}

    public static final float MODULE_SCALE = 0.483333F;
    public static final double TRAVEL = 0.36D;
    public static final double SEATED_OFFSET = -0.08D;

    public static float tiltDegrees(ZPMHolderLayout layout) {
        return layout == ZPMHolderLayout.ARRAY ? 20.0F : 90.0F;
    }

    public static double centreY(ZPMHolderLayout layout, int slot, float progress) {
        double base = layout == ZPMHolderLayout.ARRAY ? 0.53D : 0.50D + slot - 1;
        return base + Math.cos(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    public static double forwardOffset(ZPMHolderLayout layout, float progress) {
        double base = layout == ZPMHolderLayout.ARRAY ? 0.02D : 0.08D;
        return base + Math.sin(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    private static double distance(float progress) {
        return SEATED_OFFSET + (1.0D - progress) * TRAVEL;
    }
}
