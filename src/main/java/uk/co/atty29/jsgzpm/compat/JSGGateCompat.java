package uk.co.atty29.jsgzpm.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Runtime bridge for JSG gate interaction.
 *
 * JSG 5.1.x intentionally exposes a small addon API while its internal DHD
 * implementation is not a stable public contract.  JSG-ZPM therefore owns its
 * DHD block/entity and only discovers/operates the linked Pegasus gate through
 * methods that exist on the installed JSG runtime.  This avoids hard-linking to
 * private package names that changed between JSG 5.1 and the 6.0 development line.
 */
public final class JSGGateCompat {
    private static final int DEFAULT_LINK_RADIUS = 32;
    private static final String[] PEGASUS_SYMBOL_CLASSES = {
            "dev.tauri.jsg.stargate.network.SymbolPegasusEnum",
            "dev.tauri.jsg.api.stargate.network.address.symbol.types.SymbolPegasusEnum"
    };

    private JSGGateCompat() {
    }

    @Nullable
    public static BlockPos findNearestPegasusGate(ServerLevel level, BlockPos origin) {
        long radiusSq = (long) DEFAULT_LINK_RADIUS * DEFAULT_LINK_RADIUS;
        List<BlockPos> candidates = new ArrayList<>();
        int minChunkX = (origin.getX() - DEFAULT_LINK_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + DEFAULT_LINK_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - DEFAULT_LINK_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + DEFAULT_LINK_RADIUS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isPegasusGate(blockEntity)) continue;
                    BlockPos pos = blockEntity.getBlockPos();
                    if (distanceSquared(origin, pos) <= radiusSq) candidates.add(pos.immutable());
                }
            }
        }

        return candidates.stream()
                .min(Comparator.comparingLong(pos -> distanceSquared(origin, pos)))
                .orElse(null);
    }

    public static boolean isPegasusGate(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) return false;
        String className = blockEntity.getClass().getName().toLowerCase(Locale.ROOT);
        if (!hasMethod(blockEntity,"getDialedAddress") && !hasMethod(blockEntity,"getDialingManager")) return false;
        if (className.startsWith("dev.tauri.jsg.") && className.contains("stargate") && className.contains("pegasus") && className.contains("base")) return true;

        var key = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (key == null) return false;
        String path = key.getPath().toLowerCase(Locale.ROOT);
        return "jsg".equals(key.getNamespace()) && path.contains("stargate") && path.contains("pegasus");
    }

    public static boolean isIncoming(@Nullable BlockEntity gate) {
        if (gate == null) return false;
        Object state = invokeNoArgs(gate, "getStargateState");
        if (state == null) {
            Object dialingManager = invokeNoArgs(gate, "getDialingManager");
            state = invokeNoArgs(dialingManager, "getStargateState");
        }
        if (state == null) return false;

        Boolean incoming = invokeBooleanNoArgs(state, "incoming", "isIncoming");
        if (incoming != null) return incoming;
        return state.toString().toUpperCase(Locale.ROOT).contains("INCOMING");
    }

    public static boolean hasProtection(@Nullable BlockEntity gate) {
        Object target = irisTarget(gate);
        if (target == null) return false;
        Boolean hasIris = invokeBooleanNoArgs(target, "hasIris");
        if (hasIris != null) return hasIris;

        Object irisType = invokeNoArgs(target, "getIrisType");
        if (irisType == null && target != gate) irisType = invokeNoArgs(gate, "getIrisType");
        if (irisType != null) {
            String value = irisType.toString().toUpperCase(Locale.ROOT);
            return !value.contains("NONE") && !value.contains("NULL");
        }

        return hasMethod(target, "toggleIris");
    }

    public static boolean usesShield(@Nullable BlockEntity gate) {
        Object target = irisTarget(gate);
        if (target == null) return false;
        Boolean shield = invokeBooleanNoArgs(target, "hasShield");
        if (shield != null) return shield;

        Object irisType = invokeNoArgs(target, "getIrisType");
        if (irisType == null && target != gate) irisType = invokeNoArgs(gate, "getIrisType");
        return irisType != null && irisType.toString().toUpperCase(Locale.ROOT).contains("SHIELD");
    }

    public static boolean isProtectionClosed(@Nullable BlockEntity gate) {
        Object target = irisTarget(gate);
        if (target == null) return false;
        Boolean closed = invokeBooleanNoArgs(target, "isIrisClosed");
        if (closed == null && target != gate) closed = invokeBooleanNoArgs(gate, "isIrisClosed");
        return Boolean.TRUE.equals(closed);
    }

    /** @return true when the requested state is reached or was already set. */
    public static boolean setProtectionClosed(@Nullable BlockEntity gate, boolean closed) {
        if (gate == null || !hasProtection(gate)) return false;
        Object target = irisTarget(gate);
        if (target == null) return false;

        Boolean alreadyClosed = invokeBooleanNoArgs(target, "isIrisClosed");
        Boolean alreadyOpen = invokeBooleanNoArgs(target, "isIrisOpened");
        if (target != gate) {
            if (alreadyClosed == null) alreadyClosed = invokeBooleanNoArgs(gate, "isIrisClosed");
            if (alreadyOpen == null) alreadyOpen = invokeBooleanNoArgs(gate, "isIrisOpened");
        }
        if (closed && Boolean.TRUE.equals(alreadyClosed)) return true;
        if (!closed && Boolean.TRUE.equals(alreadyOpen)) return true;

        Object result = invokeNoArgs(target, "toggleIris");
        if (result == null && target != gate) result = invokeNoArgs(gate, "toggleIris");
        if (result instanceof Boolean bool) return bool;

        // Some JSG builds expose toggleIris as void. Re-read the state after invocation.
        return isProtectionClosed(gate) == closed;
    }

    public static int getPegasusSymbolCount() {
        return getPressableSymbols().size();
    }

    public static boolean pressPegasusSymbol(@Nullable BlockEntity gate, int index, ServerPlayer player) {
        if (!isPegasusGate(gate)) return false;
        List<Object> symbols = getPressableSymbols();
        if (index < 0 || index >= symbols.size()) return false;
        Object symbol = symbols.get(index);

        Object stateTarget=invokeNoArgs(gate,"getDialingManager");
        if(stateTarget==null)stateTarget=gate;
        Object state=invokeNoArgs(stateTarget,"getStargateState");
        boolean core=Boolean.TRUE.equals(invokeBooleanNoArgs(symbol,"brb"));
        if(Boolean.TRUE.equals(invokeBooleanNoArgs(state,"engaged"))) {
            if(!core || !Boolean.TRUE.equals(invokeBooleanNoArgs(state,"initiating")))return false;
            for(Method method:stateTarget.getClass().getMethods()) {
                if(!method.getName().equals("attemptClose")||method.getParameterCount()!=1)continue;
                Object[] reasons=method.getParameterTypes()[0].getEnumConstants();if(reasons==null)continue;
                for(Object reason:reasons)if(((Enum<?>)reason).name().equals("REQUESTED")) {
                    try{method.invoke(stateTarget,reason);return true;}catch(ReflectiveOperationException ignored){return false;}
                }
            }
            return false;
        }
        if(isIncoming(gate))return false;
        if(!Boolean.TRUE.equals(invokeBooleanNoArgs(state,"idle")) && !Boolean.TRUE.equals(invokeBooleanNoArgs(state,"dialing")))return false;
        // Released Pegasus exposes the player-aware overload (also handles its core).
        Object pegasus = invokeTwoArgsAssignable(gate, "addSymbolToAddressDHD", symbol, player);
        if (pegasus != Invocation.NO_METHOD) return !(pegasus instanceof Boolean bool) || bool;
        Object manager = invokeNoArgs(gate, "getDialingManager");
        if (manager != null) {
            try {
                if (Boolean.TRUE.equals(invokeBooleanNoArgs(symbol,"brb")) &&
                    Boolean.TRUE.equals(invokeBooleanNoArgs(invokeNoArgs(manager,"getStargateState"),"idle"))) {
                    Object result=invokeNoArgs(manager,"attemptOpenDialed");
                    return Boolean.TRUE.equals(invokeBooleanNoArgs(result,"ok"));
                }
                for(Method method:manager.getClass().getMethods()) {
                    if(method.getName().equals("engageSymbolDHD") && method.getParameterCount()==3) {
                        Object result=method.invoke(manager,symbol,false,false);
                        return result!=null && result.toString().equals("OK");
                    }
                }
            } catch(ReflectiveOperationException | IllegalArgumentException ignored) { return false; }
        }
        // JSG 5.1 runtime path used by its public StargateClassicController.
        Object direct = invokeOneArgAssignable(gate, "addSymbolToAddressDHD", symbol);
        if (direct != Invocation.NO_METHOD) return !(direct instanceof Boolean bool) || bool;

        // Public controller fallback. This keeps the bridge usable if gate internals move.
        try {
            Class<?> controllerClass = Class.forName("dev.tauri.jsg.api.controller.StargateClassicController");
            Method getController = findAssignableStaticMethod(controllerClass, "getController", gate.getClass());
            if (getController != null) {
                Object controller = getController.invoke(null, gate);
                Method addSymbol = findTwoArgMethod(controller.getClass(), "addSymbolToAddress", boolean.class, symbol.getClass());
                if (addSymbol != null) {
                    Object result = addSymbol.invoke(controller, true, symbol);
                    return !(result instanceof Boolean bool) || bool;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fall through to manual-address fallback.
        }

        Object manual = invokeTwoArgsAssignable(gate, "addSymbolToAddressManual", symbol, null);
        return manual != Invocation.NO_METHOD && (!(manual instanceof Boolean bool) || bool);
    }

    @Nullable
    public static BlockEntity getLinkedGate(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null) return null;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return null;
        BlockEntity blockEntity = chunk.getBlockEntity(pos);
        return isPegasusGate(blockEntity) ? blockEntity : null;
    }

    private static List<Object> cachedSymbols;
    public static List<Object> getPressableSymbols() {
        if(cachedSymbols!=null)return cachedSymbols;
        for (String className : PEGASUS_SYMBOL_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                Object[] constants = type.getEnumConstants();
                if (constants == null) continue;
                List<Object> symbols = new ArrayList<>();
                for (Object constant : constants) {
                    Boolean pressable = invokeBooleanNoArgs(constant, "canBePressed");
                    if ((pressable == null || pressable) && !((Enum<?>)constant).name().startsWith("UNKNOW")) symbols.add(constant);
                }
                if (!symbols.isEmpty()) {
                    symbols.sort(Comparator.comparingInt(JSGDHDCompat::symbolId));
                    cachedSymbols=List.copyOf(symbols);return cachedSymbols;
                }
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Try the next known JSG package layout.
            }
        }
        return List.of();
    }

    @Nullable
    private static Object irisTarget(@Nullable BlockEntity gate) {
        if (gate == null) return null;
        Object manager = invokeNoArgs(gate, "getIrisManager");
        return manager != null ? manager : gate;
    }

    @Nullable
    private static Object invokeNoArgs(@Nullable Object target, String... methodNames) {
        if (target == null) return null;
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (ReflectiveOperationException | SecurityException ignored) {
                // Try next name.
            }
        }
        return null;
    }

    @Nullable
    private static Boolean invokeBooleanNoArgs(@Nullable Object target, String... methodNames) {
        Object value = invokeNoArgs(target, methodNames);
        return value instanceof Boolean bool ? bool : null;
    }

    private static boolean hasMethod(Object target, String methodName) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName)) return true;
        }
        return false;
    }

    private static Object invokeOneArgAssignable(Object target, String name, Object arg) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(arg.getClass())) continue;
            try {
                return method.invoke(target, arg);
            } catch (ReflectiveOperationException ignored) {
                return Invocation.NO_METHOD;
            }
        }
        return Invocation.NO_METHOD;
    }

    private static Object invokeTwoArgsAssignable(Object target, String name, Object first, @Nullable Object second) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 2) continue;
            Class<?>[] params = method.getParameterTypes();
            if (!params[0].isAssignableFrom(first.getClass())) continue;
            if (second != null && !params[1].isAssignableFrom(second.getClass())) continue;
            try {
                return method.invoke(target, first, second);
            } catch (ReflectiveOperationException ignored) {
                return Invocation.NO_METHOD;
            }
        }
        return Invocation.NO_METHOD;
    }

    @Nullable
    private static Method findAssignableStaticMethod(Class<?> type, String name, Class<?> argType) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0].isAssignableFrom(argType)) return method;
        }
        return null;
    }

    @Nullable
    private static Method findTwoArgMethod(Class<?> type, String name, Class<?> firstType, Class<?> secondType) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 2) continue;
            Class<?>[] params = method.getParameterTypes();
            boolean firstMatches = params[0] == firstType || params[0].isAssignableFrom(firstType);
            boolean secondMatches = params[1].isAssignableFrom(secondType);
            if (firstMatches && secondMatches) return method;
        }
        return null;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private enum Invocation { NO_METHOD }
}
