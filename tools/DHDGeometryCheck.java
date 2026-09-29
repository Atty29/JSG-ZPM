import uk.co.atty29.jsgzpm.holder.DHDGeometry;
import java.util.Arrays;
public final class DHDGeometryCheck {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        for(int i=0;i<37;i++) {
            double x=DHDGeometry.x(i),z=DHDGeometry.z(i);
            check(DHDGeometry.hit(x,z,DHDGeometry.y(z),true)==i);
            check(DHDGeometry.hit(x,z,DHDGeometry.y(z),false)==-1);
            check(DHDGeometry.hit(x,z,DHDGeometry.y(z)-.1,true)==-1);
            check(DHDGeometry.ray(x,2,z,0,-1,0)==i);
            double dy=DHDGeometry.y(z)-1.62,dz=z-1.8,length=Math.sqrt(dy*dy+dz*dz);
            check(DHDGeometry.ray(x,1.62,1.8,0,dy/length,dz/length)==i);
            check(DHDGeometry.ray(x,0,z,0,1,0)==-1);
            // Minecraft only discovers the block while traversing its own cell.
            // The old raised back row could be hit entirely above that cell.
            check(DHDGeometry.y(z)>0 && DHDGeometry.y(z)<1);
            check(z>-.5 && z<.5);
            // Rotate to each world facing and inverse-transform the hit.
            for(int k=0;k<4;k++) {
                double a=k*Math.PI/2,wx=x*Math.cos(a)-z*Math.sin(a),wz=x*Math.sin(a)+z*Math.cos(a);
                check(DHDGeometry.hit(wx*Math.cos(a)+wz*Math.sin(a),-wx*Math.sin(a)+wz*Math.cos(a),DHDGeometry.y(z),true)==i);
            }
        }
        check(DHDGeometry.hit(-.8,.49,DHDGeometry.y(.49),true)==-1);
        check(Math.abs(DHDGeometry.x(36)-.63)<1e-6);
        check(Math.abs(DHDGeometry.z(36))<1e-6);
        // The six removed triangles must be filled by the central hexagon.
        // Every shared lattice edge has exactly two owners: no internal holes.
        var edges=new java.util.HashMap<String,Integer>();
        for(var poly:uk.co.atty29.jsgzpm.holder.DHDLayout.BUTTONS)for(int j=0;j<poly.length;j++) {
            double[] a=poly[j],b=poly[(j+1)%poly.length];
            String aa=Math.round(a[0]*1000000)+","+Math.round(a[1]*1000000);
            String bb=Math.round(b[0]*1000000)+","+Math.round(b[1]*1000000);
            String key=aa.compareTo(bb)<0?aa+":"+bb:bb+":"+aa;
            edges.merge(key,1,Integer::sum);
        }
        check(edges.values().stream().allMatch(n->n==1||n==2));
        check(edges.values().stream().filter(n->n==1).count()==16);
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
