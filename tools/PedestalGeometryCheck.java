import uk.co.atty29.jsgzpm.holder.PedestalGeometry;
import uk.co.atty29.jsgzpm.client.PedestalLightGeometry;

public final class PedestalGeometryCheck {
    public static void main(String[] args) {
        double scale = PedestalGeometry.MODULE_SCALE;
        double cap = PedestalGeometry.MODULE_CENTRE_Y + .512 * scale;
        if (Math.abs(cap - PedestalGeometry.TOP_RING_Y) > 1e-6) throw new AssertionError("Cap is not flush");
        double penetration = PedestalGeometry.DECK_Y - (PedestalGeometry.MODULE_CENTRE_Y - .525 * scale);
        if (penetration < .04 || penetration > .10) throw new AssertionError("Crystal must only enter slightly");
        if (.269 * scale >= .112 * Math.cos(Math.PI / 24)) throw new AssertionError("Crystal clips the rings");
        if (PedestalGeometry.TOP_RING_Y < .95 || PedestalGeometry.TOP_RING_Y > 1.05) throw new AssertionError("Waist height");
        int cyan=0, white=0;
        for (float[][] group : PedestalLightGeometry.GROUPS) for (float[] face : group) {
            if (face.length != 15) throw new AssertionError("Malformed emissive quad");
            for (float f : face) if (!Float.isFinite(f)) throw new AssertionError("Non-finite vertex");
            for (int i=0; i<12; i+=3) {
                if (face[i]<0 || face[i]>1 || face[i+1]<0 || face[i+1]>1.02 || face[i+2]<0 || face[i+2]>1) throw new AssertionError("Light outside pedestal");
            }
            if (face[12]<.5) cyan++; else white++;
        }
        if (cyan<30 || white<30) throw new AssertionError("Missing cyan or white illumination");
        System.out.println("Pedestal flush cap, shallow seating, ring clearance and emissive geometry passed.");
    }
}
