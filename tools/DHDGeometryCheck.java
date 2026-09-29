import uk.co.atty29.jsgzpm.holder.DHDGeometry;
import java.util.Arrays;
public final class DHDGeometryCheck {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int i=0;i<37;i++) {
            double x=DHDGeometry.x(i),z=DHDGeometry.z(i);
            check(DHDGeometry.hit(x,z,DHDGeometry.y(z),true)==i);
            check(DHDGeometry.hit(x,z,DHDGeometry.y(z),false)==-1);
            check(DHDGeometry.hit(x,z,.8,true)==-1);
            check(DHDGeometry.ray(x,2,z,0,-1,0)==i);
            double dy=DHDGeometry.y(z)-1.62,dz=z-1.8,length=Math.sqrt(dy*dy+dz*dz);
            check(DHDGeometry.ray(x,1.62,1.8,0,dy/length,dz/length)==i);
            check(DHDGeometry.ray(x,0,z,0,1,0)==-1);
            // Rotate to each world facing and inverse-transform the hit.
            for(int k=0;k<4;k++) {
                double a=k*Math.PI/2,wx=x*Math.cos(a)-z*Math.sin(a),wz=x*Math.sin(a)+z*Math.cos(a);
                check(DHDGeometry.hit(wx*Math.cos(a)+wz*Math.sin(a),-wx*Math.sin(a)+wz*Math.cos(a),DHDGeometry.y(z),true)==i);
            }
        }
        check(DHDGeometry.hit(-.8,.49,DHDGeometry.y(.49),true)==-1);
        int[] page={0,1,2,3,4,5,6,7},six={1,2,3,4,5,6},eight={1,2,3,4,5,6,7,8};
        for(int i=0;i<6;i++)check(DHDGeometry.next(page,six,Arrays.copyOf(page,i),14,38)==page[i]);
        check(DHDGeometry.next(page,six,Arrays.copyOf(page,6),14,38)==14);
        check(DHDGeometry.next(page,six,new int[]{0,1,2,3,4,5,14},14,38)==38);
        check(DHDGeometry.next(page,eight,Arrays.copyOf(page,6),14,38)==6);
        check(DHDGeometry.next(page,eight,page,14,38)==14);
        check(DHDGeometry.next(page,six,new int[]{9},14,38)==-1);
        check(DHDGeometry.next(page,new int[]{1,3},new int[]{0},14,38)==-1);
        check(DHDGeometry.next(page,new int[0],new int[0],14,38)==-1);
        System.out.println("DHD hit areas, rotations, address order, origin/core and partial-page checks passed.");
    }
}
