package uk.co.atty29.jsgzpm.compat;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Adapts released notebooks and newer JSG Core pages without linking private classes. */
public final class JSGDHDCompat {
    private JSGDHDCompat() {}
    public static Object call(Object target,String name,Object... args) {
        if(target==null)return null;
        Class<?> type=target instanceof Class<?> c?c:target.getClass();
        for(Method m:type.getMethods()) {
            if(!m.getName().equals(name)||m.getParameterCount()!=args.length)continue;
            try{return m.invoke(target instanceof Class<?>?null:target,args);}
            catch(ReflectiveOperationException|IllegalArgumentException|LinkageError ignored) {}
        }
        return null;
    }
    public static Object staticCall(String type,String name,Object... args) {
        try{return call(Class.forName(type),name,args);}catch(ClassNotFoundException|LinkageError ignored){return null;}
    }
    public static int symbolId(Object symbol) {
        Object id=call(symbol,"getId");return id instanceof Number n?n.intValue():-1;
    }
    public static int[] ids(Object address) {
        Object size=call(address,"size");if(!(size instanceof Number))size=call(address,"getSize");
        if(!(size instanceof Number n)||n.intValue()<0||n.intValue()>9)return new int[0];
        int[] ids=new int[n.intValue()];
        for(int i=0;i<ids.length;i++)ids[i]=symbolId(call(address,"get",i));
        return ids;
    }
    public static boolean engaged(BlockEntity gate) {
        Object target=call(gate,"getDialingManager");if(target==null)target=gate;
        return Boolean.TRUE.equals(call(call(target,"getStargateState"),"engaged"));
    }
    public static int[] dialed(BlockEntity gate) {
        if(!JSGGateCompat.isPegasusGate(gate))return new int[0];
        Object address=call(gate,"getDialedAddress");
        if(address==null)address=call(call(gate,"getDialingManager"),"getDialedAddress");
        return ids(address);
    }
    private static Object field(Object target,String name) {
        if(target==null)return null;
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass()) {
            try {var f=c.getDeclaredField(name);if(f.trySetAccessible())return f.get(target);}
            catch(ReflectiveOperationException|RuntimeException ignored) {}
        }
        return null;
    }
    /** Read JSG's own pending input so hints advance before a chevron finishes. */
    public static int[] entered(BlockEntity gate) {
        if(!JSGGateCompat.isPegasusGate(gate))return new int[0];
        LinkedHashSet<Integer> input=new LinkedHashSet<>();
        for(int id:dialed(gate))if(id>=0)input.add(id);
        Object manager=call(gate,"getDialingManager"), pending;
        if(manager==null) {
            if(Boolean.TRUE.equals(call(call(gate,"getStargateState"),"dialing"))) {
                int id=symbolId(field(gate,"targetRingSymbol"));if(id>=0)input.add(id);
            }
            pending=field(gate,"toDialSymbols");
        } else {
            Object spin=call(manager,"getSpinHelper");
            if(Boolean.TRUE.equals(call(spin,"isSpinning"))) {
                int id=symbolId(call(spin,"getTargetSymbol"));if(id>=0)input.add(id);
            }
            pending=call(field(manager,"addressBuffer"),"first");
        }
        if(pending instanceof Iterable<?> symbols)for(Object symbol:symbols) {
            int id=symbolId(symbol);if(id>=0)input.add(id);
        }
        return input.stream().mapToInt(Integer::intValue).toArray();
    }
    public static ResourceLocation icon(int index) {
        var symbols=JSGGateCompat.getPressableSymbols();
        if(index<0||index>=symbols.size())return null;
        Object symbol=symbols.get(index), resource=null;
        try{resource=symbol.getClass().getField("iconResource").get(symbol);}catch(ReflectiveOperationException ignored){}
        if(resource==null)resource=call(symbol,"getIconResource",(Object)null);
        return resource instanceof ResourceLocation r?r:null;
    }
    public record Page(int[] symbols,int[] visible) {}
    public static Page page(ItemStack stack) {
        if(stack.isEmpty()||!stack.hasTag())return null;
        CompoundTag tag=stack.getTag();Object address=null;int[] visible=new int[0];
        if(tag.contains("pages")) {
            Object selected=staticCall("dev.tauri.jsg.core.common.item.notebook.NotebookItem","getSelectedPageFromCompound",tag);
            if(!(selected instanceof CompoundTag c))return null;tag=c;
        } else if(tag.contains("addressList")) {
            Object selected=staticCall("dev.tauri.jsg.item.notebook.NotebookItem","getSelectedPageFromCompound",tag);
            if(!(selected instanceof CompoundTag c))return null;tag=c;
        }
        Object typed=staticCall("dev.tauri.jsg.core.common.entity.NotebookPageType","pageDataFromCompound",tag);
        Object data=call(typed,"data");
        if(data!=null) {
            address=call(data,"getAddress");Object v=call(data,"getSymbolsToDisplay");
            if(v instanceof int[] a)visible=a;
        } else {
            address=staticCall("dev.tauri.jsg.api.item.NotebookPageSerialization","getDeserializedAddress",tag);
            // Public 5.0 predates NotebookPageSerialization and constructs this address directly.
            if(address==null && tag.contains("address",10) && tag.contains("symbolType")) {
                try{address=Class.forName("dev.tauri.jsg.stargate.network.StargateAddress").getConstructor(CompoundTag.class).newInstance(tag.getCompound("address"));}
                catch(ReflectiveOperationException|LinkageError ignored){}
            }
            visible=tag.getIntArray("symbolsToDisplay");
        }
        var symbols=JSGGateCompat.getPressableSymbols();
        if(address==null||symbols.isEmpty())return null;
        Object type=call(address,"getSymbolType");
        if(!Objects.equals(type,call(symbols.get(0),"getSymbolType")))return null;
        int[] ids=ids(address);if(ids.length==0)return null;
        if(visible.length==0 && tag.contains("hasUpgrade")) {
            visible=java.util.stream.IntStream.rangeClosed(1,tag.getBoolean("hasUpgrade")?8:6).toArray();
        }
        return new Page(ids,visible);
    }
    public static int hintColor(boolean origin,boolean extra) {
        String field=origin?"pageHintColorOrigin":extra?"pageHintColorExtra":"pageHintColorNormal";
        for(String type:List.of("dev.tauri.jsg.config.JSGConfig$DialHomeDevice","dev.tauri.jsg.api.config.JSGConfig$DialHomeDevice")) {
            try {
                Class<?> c=Class.forName(type);
                Object enabled=call(c.getField("enablePageHint").get(null),"get");
                if(Boolean.FALSE.equals(enabled))return -1;
                Object value=c.getField(field).get(null),text=call(value,"get");
                if(text instanceof String s)return Integer.decode(s)&0xffffff;
                Object r=call(value,"getRed"),g=call(value,"getGreen"),b=call(value,"getBlue");
                if(r instanceof Number rn && g instanceof Number gn && b instanceof Number bn)return rn.intValue()<<16|gn.intValue()<<8|bn.intValue();
            }catch(ReflectiveOperationException|IllegalArgumentException|LinkageError ignored){}
        }
        return origin?0x55ff88:extra?0xffb347:0x55e7ff;
    }
}
