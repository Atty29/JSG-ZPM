package uk.co.atty29.jsgzpm.holder;

public final class DHDGeometry {
    private DHDGeometry() {}
    public static final int SYMBOLS=36, CORE=36;
    public static final double TOP=.94, RX=.065, RZ=.050;
    public static double x(int i){return i==CORE?0:(i%6-2.5)*.145;}
    public static double z(int i){return i==CORE?.405:(i/6-2.5)*.115-.035;}
    public static int hit(double x,double z,double y,boolean topFace) {
        if(!topFace||Math.abs(y-TOP)>.025)return -1;
        if(Math.hypot(x,z-.405)<.064)return CORE;
        for(int i=0;i<SYMBOLS;i++)if(Math.abs(x-x(i))/RX+Math.abs(z-z(i))/RZ<=1)return i;
        return -1;
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
