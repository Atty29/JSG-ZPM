package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import uk.co.atty29.jsgzpm.block.AtlantisPegasusDHDBlock;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;
import uk.co.atty29.jsgzpm.compat.*;
import uk.co.atty29.jsgzpm.holder.DHDGeometry;
import uk.co.atty29.jsgzpm.holder.DHDLayout;

public final class AtlantisPegasusDHDBlockEntityRenderer implements BlockEntityRenderer<AtlantisPegasusDHDBlockEntity> {
    private static final ResourceLocation GLASS=new ResourceLocation("jsgzpm","textures/block/ancient/crystal_warm.png");
    public AtlantisPegasusDHDBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(AtlantisPegasusDHDBlockEntity dhd,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var player=Minecraft.getInstance().player;if(player==null)return;
        var symbols=JSGGateCompat.getPressableSymbols();
        if(symbols.size()!=37)return; // Unknown layouts fail closed rather than label the wrong symbol.
        var page=JSGDHDCompat.page(player.getMainHandItem());
        if(page==null)page=JSGDHDCompat.page(player.getOffhandItem());
        int origin=-1,core=-1;
        for(Object symbol:symbols){
            if(Boolean.TRUE.equals(JSGDHDCompat.call(symbol,"origin")))origin=JSGDHDCompat.symbolId(symbol);
            if(Boolean.TRUE.equals(JSGDHDCompat.call(symbol,"brb")))core=JSGDHDCompat.symbolId(symbol);
        }
        int[] dialed=dhd.getDialedSymbols();
        int hint=page!=null&&dhd.inventory.control()&&dhd.hasLinkedGate()&&!dhd.isGateEngaged()?DHDGeometry.next(page.symbols(),page.visible(),dialed,origin,core):-1;
        if(hint>=0&&dialed.length>=6&&!dhd.inventory.has("crystal_glyph_dhd")&&hint!=origin&&hint!=core)hint=origin;
        Direction front=dhd.getBlockState().getValue(AtlantisPegasusDHDBlock.FACING),right=front.getCounterClockWise();
        for(int i=0;i<symbols.size();i++) {
            int id=JSGDHDCompat.symbolId(symbols.get(i));boolean active=i==dhd.getLastPressedIndex() || (i==DHDGeometry.CORE && dhd.isGateEngaged());
            for(int value:dialed)if(value==id)active=true;
            int color=active?0xc5ecff:0xffce86;
            if(id==hint){int configured=JSGDHDCompat.hintColor(id==origin||id==core,dialed.length>=6);if(configured>=0)color=configured;}
            if(i==DHDGeometry.CORE && !active && id!=hint)color=0x24616c;
            boolean glow=active||(id==hint && JSGDHDCompat.hintColor(id==origin||id==core,dialed.length>=6)>=0);
            double x=DHDGeometry.x(i),z=DHDGeometry.z(i);
            var out=buffers.getBuffer(RenderType.entityTranslucentEmissive(GLASS));
            var polygon=DHDLayout.BUTTONS[i];
            for(int k=1;k<polygon.length-1;k++) {
                double[][] points={polygon[0],polygon[k],polygon[k+1],polygon[k+1]};
                face(pose,out,front,right,points,.0015,color,LightTexture.FULL_BRIGHT,x,z,.82);
            }
            if(i!=DHDGeometry.CORE) {
                ResourceLocation icon=JSGDHDCompat.icon(i);
                if(icon!=null)icon=DHDSymbolTextures.get(icon);
                if(icon!=null) {
                    out=buffers.getBuffer(RenderType.entityTranslucentEmissive(icon));
                    double r=.031;
                    face(pose,out,front,right,new double[][]{{x-r,z-r},{x-r,z+r},{x+r,z+r},{x+r,z-r}},.003,glow?0xffffff:0xfff1cb,LightTexture.FULL_BRIGHT,x,z,1);
                }
            }
        }
    }
    private static void face(PoseStack pose,VertexConsumer out,Direction front,Direction right,double[][] points,double offset,int color,int light,double cx,double cz,double scale) {
        float inv=(float)(1/Math.sqrt(1+DHDGeometry.SLOPE*DHDGeometry.SLOPE));
        for(int i=0;i<4;i++) {
            double u=cx+(points[i][0]-cx)*scale,v=cz+(points[i][1]-cz)*scale;
            out.vertex(pose.last().pose(),(float)(.5+u*right.getStepX()+v*front.getStepX()),(float)(DHDGeometry.y(v)+offset),(float)(.5+u*right.getStepZ()+v*front.getStepZ()))
                .color((color>>16)&255,(color>>8)&255,color&255,255).uv(i<2?0:1,i==0||i==3?0:1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),(float)(front.getStepX()*DHDGeometry.SLOPE)*inv,inv,(float)(front.getStepZ()*DHDGeometry.SLOPE)*inv).endVertex();
        }
    }
    @Override public boolean shouldRenderOffScreen(AtlantisPegasusDHDBlockEntity dhd){return true;}
}
