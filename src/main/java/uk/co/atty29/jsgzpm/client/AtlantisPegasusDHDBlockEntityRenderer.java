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

public final class AtlantisPegasusDHDBlockEntityRenderer implements BlockEntityRenderer<AtlantisPegasusDHDBlockEntity> {
    private static final ResourceLocation LIGHT=new ResourceLocation("jsgzpm","textures/block/ancient/light.png");
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
        int hint=page!=null&&dhd.hasLinkedGate()&&!dhd.isGateEngaged()?DHDGeometry.next(page.symbols(),page.visible(),dialed,origin,core):-1;
        Direction front=dhd.getBlockState().getValue(AtlantisPegasusDHDBlock.FACING),right=front.getClockWise();
        for(int i=0;i<symbols.size();i++) {
            int id=JSGDHDCompat.symbolId(symbols.get(i));boolean active=i==dhd.getLastPressedIndex() || (i==DHDGeometry.CORE && dhd.isGateEngaged());
            for(int value:dialed)if(value==id)active=true;
            int color=active?0xffb653:0x786852;
            if(id==hint){int configured=JSGDHDCompat.hintColor(id==origin||id==core,dialed.length>=6);if(configured>=0)color=configured;}
            if(i==DHDGeometry.CORE && !active && id!=hint)color=0x24616c;
            boolean glow=active||(id==hint && JSGDHDCompat.hintColor(id==origin||id==core,dialed.length>=6)>=0);
            double x=DHDGeometry.x(i),z=DHDGeometry.z(i);
            var out=buffers.getBuffer(RenderType.entityTranslucentEmissive(LIGHT));
            quad(pose,out,front,right,x,z,.941,DHDGeometry.RX*.92,DHDGeometry.RZ*.92,color,true,LightTexture.FULL_BRIGHT);
            if(i!=DHDGeometry.CORE) {
                ResourceLocation icon=JSGDHDCompat.icon(i);
                if(icon!=null)icon=DHDSymbolTextures.get(icon);
                if(icon!=null) {
                    out=buffers.getBuffer(glow?RenderType.entityTranslucentEmissive(icon):RenderType.entityCutoutNoCull(icon));
                    quad(pose,out,front,right,x,z,.943,.029,.029,glow?color:0xd9bd83,false,glow?LightTexture.FULL_BRIGHT:light);
                }
            }
        }
    }
    private static void quad(PoseStack pose,VertexConsumer out,Direction front,Direction right,double x,double z,double y,double rx,double rz,int color,boolean diamond,int light) {
        double[][] points=diamond?new double[][]{{-rx,0},{0,rz},{rx,0},{0,-rz}}:new double[][]{{-rx,-rz},{-rx,rz},{rx,rz},{rx,-rz}};
        for(int i=0;i<4;i++) {
            double u=x+points[i][0],v=z+points[i][1];
            out.vertex(pose.last().pose(),(float)(.5+u*right.getStepX()+v*front.getStepX()),(float)y,(float)(.5+u*right.getStepZ()+v*front.getStepZ()))
                .color((color>>16)&255,(color>>8)&255,color&255,255).uv(i<2?0:1,i==0||i==3?0:1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
        }
    }
    @Override public boolean shouldRenderOffScreen(AtlantisPegasusDHDBlockEntity dhd){return true;}
}
