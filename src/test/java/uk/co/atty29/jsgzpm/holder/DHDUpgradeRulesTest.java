package uk.co.atty29.jsgzpm.holder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DHDUpgradeRulesTest {
    @Test void unpoweredConsoleCannotDialOrActivate(){
        assertFalse(DHDUpgradeRules.allows(false,true,false,false,0,false));
        assertFalse(DHDUpgradeRules.allows(false,true,true,false,7,false));
    }
    @Test void standardAddressAcceptsSixGlyphsThenOrigin(){
        for(int count=0;count<6;count++)assertTrue(DHDUpgradeRules.allows(true,false,false,false,count,false));
        assertFalse(DHDUpgradeRules.allows(true,false,false,false,6,false));
        assertTrue(DHDUpgradeRules.allows(true,false,false,true,6,false));
        assertTrue(DHDUpgradeRules.allows(true,false,true,false,7,false));
    }
    @Test void glyphUpgradeAllowsNineSymbolAddress(){
        assertTrue(DHDUpgradeRules.allows(true,true,false,false,7,false));
        assertTrue(DHDUpgradeRules.allows(true,true,false,true,8,false));
        assertTrue(DHDUpgradeRules.allows(true,true,true,false,9,false));
        assertFalse(DHDUpgradeRules.allows(true,true,false,false,8,false));
    }
    @Test void removingGlyphBlocksExtendedActivationButAllowsClosing(){
        assertFalse(DHDUpgradeRules.allows(true,false,true,false,9,false));
        assertTrue(DHDUpgradeRules.allows(true,false,true,false,9,true));
    }
}
