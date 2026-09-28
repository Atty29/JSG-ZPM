import uk.co.atty29.jsgzpm.holder.WallHolderGeometry;
import uk.co.atty29.jsgzpm.holder.ZPMHolderLayout;

/** Standalone visual transform checks; no game runtime or gameplay mutations. */
public final class WallHolderGeometryCheck {
    private static void near(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.00001) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        double scale = WallHolderGeometry.MODULE_SCALE;
        near(1.05 * scale, 0.50749965);
        if (0.269 * scale >= 0.156) throw new AssertionError("Crystal hits bore");
        near(WallHolderGeometry.SEATED_OFFSET + .512 * scale, 0);
        double seatedTip = WallHolderGeometry.SEATED_OFFSET - .525 * scale;
        if (seatedTip <= -.525 || seatedTip >= -.49) throw new AssertionError("Insufficient socket depth");
        if (seatedTip + WallHolderGeometry.TRAVEL <= .020) throw new AssertionError("Released tip catches lip");
        for (ZPMHolderLayout layout : new ZPMHolderLayout[]{ZPMHolderLayout.ARRAY, ZPMHolderLayout.COLUMN}) {
            double angle = 45;
            near(WallHolderGeometry.tiltDegrees(layout), angle);
            double sin = Math.sin(Math.toRadians(angle)), cos = Math.cos(Math.toRadians(angle));
            for (int slot = 0; slot < 3; slot++) {
                double baseY = layout == ZPMHolderLayout.ARRAY ? 0.50 : 0.50 + slot - 1;
                double socketForward = 0.0;
                for (int step = 0; step <= 20; step++) {
                    float progress = step / 20.0F;
                    double travel = -.512 * scale + (1 - progress) * 0.54;
                    near(WallHolderGeometry.centreY(layout, slot, progress), baseY + (layout == ZPMHolderLayout.ARRAY ? cos * travel : 0));
                    near(WallHolderGeometry.sideOffset(layout, slot, progress), layout == ZPMHolderLayout.ARRAY ? slot - 1 : -cos * travel);
                    near(WallHolderGeometry.forwardOffset(layout, progress), socketForward + sin * travel);
                    // Tip remains in front of the cup floor at every animation sample.
                    if (travel - 0.525 * scale <= -0.525) throw new AssertionError("Tip hits floor");
                    double roll = Math.toRadians(WallHolderGeometry.rollDegrees(layout));
                    double localX = -Math.sin(roll) * cos;
                    double localY = Math.cos(roll) * cos;
                    near(localY, layout == ZPMHolderLayout.ARRAY ? cos : 0);
                    // Compare the renderer's yaw-then-pitch axis to each cardinal facing.
                    double[][] facings = {{0,-1,180},{1,0,270},{0,1,0},{-1,0,90}};
                    for (double[] facing : facings) {
                        double yaw = Math.toRadians(-facing[2]);
                        near(Math.cos(yaw) * localX + Math.sin(yaw) * sin, facing[0] * sin + facing[1] * localX);
                        near(-Math.sin(yaw) * localX + Math.cos(yaw) * sin, facing[1] * sin - facing[0] * localX);
                        near(Math.cos(Math.toRadians(angle)), cos);
                    }
                }

            }
        }
        System.out.println("Wall holder axes, slot centres, travel and bore clearance passed.");
    }
}
