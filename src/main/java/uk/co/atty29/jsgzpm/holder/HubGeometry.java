package uk.co.atty29.jsgzpm.holder;

/** Visual socket centres, in blocks relative to the hub centre and facing. */
public final class HubGeometry {
    private HubGeometry() {}

    public static double sideOffset(int slot) {
        return switch (slot) {
            case 0 -> -0.265D;
            case 1 -> 0.0D;
            case 2 -> 0.265D;
            default -> throw new IllegalArgumentException("Invalid hub slot: " + slot);
        };
    }

    public static double forwardOffset(int slot) {
        return slot == 1 ? -0.246D : 0.204D;
    }

    public static final double DOWN_CENTRE_Y = 0.978D;
    public static final double TRAVEL = 0.30D;
    public static final float MODULE_SCALE = 0.40F;
}
