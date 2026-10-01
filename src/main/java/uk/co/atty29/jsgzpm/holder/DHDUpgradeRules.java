package uk.co.atty29.jsgzpm.holder;
/** Server-side address limits, independent of JSG's private DHD classes. */
public final class DHDUpgradeRules {
    private DHDUpgradeRules() {}
    public static boolean allows(boolean control,boolean glyph,boolean core,boolean origin,int entered,boolean engaged) {
        if(!control)return false;
        if(core)return engaged || entered<=(glyph?9:7);
        if(origin)return entered<(glyph?9:7);
        return entered<(glyph?8:6);
    }
}
