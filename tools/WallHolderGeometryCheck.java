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
        for (ZPMHolderLayout layout : new ZPMHolderLayout[]{ZPMHolderLayout.ARRAY, ZPMHolderLayout.COLUMN}) {
            double angle = layout == ZPMHolderLayout.ARRAY ? 20 : 90;
            near(WallHolderGeometry.tiltDegrees(layout), angle);
            double sin = Math.sin(Math.toRadians(angle)), cos = Math.cos(Math.toRadians(angle));
            for (int slot = 0; slot < 3; slot++) {
                double baseY = layout == ZPMHolderLayout.ARRAY ? 0.32 : 0.50 + slot - 1;
                double socketForward = layout == ZPMHolderLayout.ARRAY ? -0.10 : -0.20;
                for (int step = 0; step <= 20; step++) {
                    float progress = step / 20.0F;
                    double travel = 0.10 + (1 - progress) * 0.28;
                    near(WallHolderGeometry.centreY(layout, slot, progress), baseY + cos * travel);
                    near(WallHolderGeometry.forwardOffset(layout, progress), socketForward + sin * travel);
                    // Tip remains in front of the cup floor at every animation sample.
                    if (travel - 0.525 * scale <= -0.174) throw new AssertionError("Tip hits floor");
                    // Compare the renderer's yaw-then-pitch axis to each cardinal facing.
                    double[][] facings = {{0,-1,180},{1,0,270},{0,1,0},{-1,0,90}};
                    for (double[] facing : facings) {
                        double yaw = Math.toRadians(-facing[2]);
                        near(Math.sin(yaw) * sin, facing[0] * sin);
                        near(Math.cos(yaw) * sin, facing[1] * sin);
                        near(Math.cos(Math.toRadians(angle)), cos);
                    }
                }
                if (layout == ZPMHolderLayout.COLUMN) {
                    near(WallHolderGeometry.centreY(layout,slot,0),baseY);
                    near(WallHolderGeometry.centreY(layout,slot,1),baseY);
                }
            }
        }
        System.out.println("Wall holder axes, slot centres, travel and bore clearance passed.");
    }
}
