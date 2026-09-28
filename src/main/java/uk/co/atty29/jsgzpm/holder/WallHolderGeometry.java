package uk.co.atty29.jsgzpm.holder;

/** Visual mounting axes for the sloped array and sideways corner column. */
public final class WallHolderGeometry {
    private WallHolderGeometry() {}

    public static final float MODULE_SCALE = 0.483333F;
    public static final double TRAVEL = 0.54D;
    public static final double SEATED_OFFSET = -0.512D * MODULE_SCALE;

    public static float tiltDegrees(ZPMHolderLayout layout) {
        return 45.0F;
    }

    public static double centreY(ZPMHolderLayout layout, int slot, float progress) {
        double base = layout == ZPMHolderLayout.ARRAY ? 0.50D : 0.50D + slot - 1;
        if (layout == ZPMHolderLayout.COLUMN) return base;
        return base + Math.cos(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    public static float rollDegrees(ZPMHolderLayout layout) {
        return layout == ZPMHolderLayout.COLUMN ? -90.0F : 0.0F;
    }

    public static double sideOffset(ZPMHolderLayout layout, int slot, float progress) {
        return layout == ZPMHolderLayout.ARRAY ? slot - 1
                : -Math.cos(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    public static double forwardOffset(ZPMHolderLayout layout, float progress) {
        double base = 0.0D;
        return base + Math.sin(Math.toRadians(tiltDegrees(layout))) * distance(progress);
    }

    private static double distance(float progress) {
        return SEATED_OFFSET + (1.0D - progress) * TRAVEL;
    }
}
