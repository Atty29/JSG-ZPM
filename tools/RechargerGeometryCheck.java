import uk.co.atty29.jsgzpm.client.RechargerMesh;
import uk.co.atty29.jsgzpm.generator.RechargerGeometry;

public final class RechargerGeometryCheck {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(RechargerMesh.FACES.length>1000);
        for(float[] f:RechargerMesh.FACES) {
            for(float value:f) check(Float.isFinite(value));
            check(Math.abs(f[12]*f[12]+f[13]*f[13]+f[14]*f[14]-1)<.0001);
            for(int i=0;i<4;i++) {
                check(Math.hypot(f[i*3],f[i*3+1])<1.50);
                check(f[i*3+2]>=.50 && f[i*3+2]<=1.86);
            }
        }
        for(int slot=0;slot<3;slot++) {
            check(Math.hypot(Math.abs(RechargerGeometry.slotU(slot))+.12,.22)<RechargerGeometry.BORE_RADIUS);
            check(RechargerGeometry.MODULE_DEPTH+.12<RechargerGeometry.SHIELD_DEPTH);
        }
        check(RechargerGeometry.shieldInnerRadius(0)==RechargerGeometry.BORE_RADIUS);
        check(RechargerGeometry.shieldInnerRadius(1)==0);
        check(RechargerGeometry.gasAlpha(0)==0);
        for(int i=0;i<100;i++) {
            float p=i/100f;
            check(RechargerGeometry.shieldInnerRadius(p)>=RechargerGeometry.shieldInnerRadius(p+.01f));
            check(RechargerGeometry.gasAlpha(p)>=0 && RechargerGeometry.gasAlpha(p)<=.24f);
            float sweep=RechargerGeometry.sweep(i*.2,i);
            check(sweep>=0 && sweep<=1);
        }
        check(RechargerGeometry.sweep(0,0)>RechargerGeometry.sweep(0,20));
        System.out.println("Recharger mesh, module clearance, shield and gas checks passed.");
    }
}
