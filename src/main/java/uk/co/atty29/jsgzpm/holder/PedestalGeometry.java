package uk.co.atty29.jsgzpm.holder;

/** Fixed display cradle: only the crystal tip enters the waist-high machine. */
public final class PedestalGeometry {
    private PedestalGeometry() {}
    public static final float MODULE_SCALE = 0.4F;
    public static final double TOP_RING_Y = 1.02D;
    public static final double DECK_Y = 0.68D;
    public static final double MODULE_CENTRE_Y = TOP_RING_Y - 0.512D * MODULE_SCALE;
}
