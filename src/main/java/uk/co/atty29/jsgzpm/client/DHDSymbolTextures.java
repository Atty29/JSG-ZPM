package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Preserve JSG's glyph alpha masks; white pixels allow the native hint colors to illuminate them. */
@Mod.EventBusSubscriber(modid="jsgzpm",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DHDSymbolTextures {
    private static final Map<ResourceLocation,ResourceLocation> CACHE=new HashMap<>();
    public static ResourceLocation get(ResourceLocation source) {
        if(CACHE.containsKey(source))return CACHE.get(source);
        ResourceLocation result=null;
        var minecraft=Minecraft.getInstance();
        try(var stream=minecraft.getResourceManager().open(source)) {
            NativeImage image=NativeImage.read(stream);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
                image.setPixelRGBA(x,y,(image.getPixelRGBA(x,y)&0xff000000)|0x00ffffff);
            result=minecraft.getTextureManager().register("jsgzpm_dhd_glyph",new DynamicTexture(image));
        }catch(IOException ignored){}
        CACHE.put(source,result);return result;
    }
    @SubscribeEvent public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)manager->{
            CACHE.values().stream().filter(java.util.Objects::nonNull).forEach(Minecraft.getInstance().getTextureManager()::release);
            CACHE.clear();
        });
    }
}
