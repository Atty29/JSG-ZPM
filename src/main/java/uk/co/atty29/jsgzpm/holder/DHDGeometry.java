package uk.co.atty29.jsgzpm.holder;

public final class DHDGeometry {
    private DHDGeometry() {}
    public static final int SYMBOLS=36, CORE=36;
    public static final double TOP=1.025, SLOPE=.34;
    public static double y(double z){return TOP-SLOPE*z;}
    public static double x(int i){double sum=0;for(double[] p:DHDLayout.BUTTONS[i])sum+=p[0];return sum/DHDLayout.BUTTONS[i].length;}
    public static double z(int i){double sum=0;for(double[] p:DHDLayout.BUTTONS[i])sum+=p[1];return sum/DHDLayout.BUTTONS[i].length;}
    public static int hit(double x,double z,double y,boolean topFace) {
        if(!topFace||Math.abs(y-y(z))>.025)return -1;
        for(int i=0;i<37;i++) {
            var poly=DHDLayout.BUTTONS[i];boolean pos=false,neg=false;
            for(int j=0;j<poly.length;j++) {
                var a=poly[j];var b=poly[(j+1)%poly.length];
                double cross=(b[0]-a[0])*(z-a[1])-(b[1]-a[1])*(x-a[0]);
                pos|=cross>1e-8;neg|=cross< -1e-8;
            }
            if(!(pos&&neg))return i;
        }
        return -1;
    }
    public static int ray(double x,double y,double z,double dx,double dy,double dz) {
        double denominator=dy+SLOPE*dz;
        if(denominator>=-1e-7)return -1;
        double t=(TOP-y-SLOPE*z)/denominator;
        if(t<0||t>6)return -1;
        return hit(x+t*dx,z+t*dz,y+t*dy,true);
    }
    /** Never guides through a missing page symbol or a mismatched dialed prefix. */
    public static int next(int[] page,int[] visible,int[] dialed,int origin,int core) {
        int limit=0;for(int v:visible)if(v>=1&&v<=8)limit=Math.max(limit,v);
        if(limit==0||limit>page.length)return -1;
        for(int i=0;i<Math.min(dialed.length,limit);i++)if(dialed[i]!=page[i])return -1;
        if(dialed.length>limit)return dialed.length==limit+1&&dialed[limit]==origin?core:-1;
        if(dialed.length==limit)return origin;
        for(int v:visible)if(v==dialed.length+1)return page[dialed.length];
        return -1;
    }
}
