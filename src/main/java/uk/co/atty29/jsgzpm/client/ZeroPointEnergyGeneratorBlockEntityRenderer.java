package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import uk.co.atty29.jsgzpm.blockentity.ZeroPointEnergyGeneratorBlockEntity;
import uk.co.atty29.jsgzpm.generator.GeneratorGeometry;
import uk.co.atty29.jsgzpm.generator.RechargerGeometry;

public final class ZeroPointEnergyGeneratorBlockEntityRenderer implements BlockEntityRenderer<ZeroPointEnergyGeneratorBlockEntity> {
    private static final ResourceLocation DARK = texture("block/ancient/pedestal_dark");
    private static final ResourceLocation METAL = texture("block/ancient/pedestal_metal");
    private static final ResourceLocation LIGHT = texture("block/ancient/light");
    private static final ResourceLocation MIST = texture("entity/recharger_mist");
    private static final ResourceLocation SHIELD = texture("entity/recharger_shield");

    public ZeroPointEnergyGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    private static ResourceLocation texture(String name) { return new ResourceLocation("jsgzpm", "textures/" + name + ".png"); }

    @Override
    public void render(ZeroPointEnergyGeneratorBlockEntity generator, float partialTick, PoseStack stack, MultiBufferSource buffers, int packedLight, int overlay) {
        if (!generator.isFormed()) return;
        Direction normal = generator.getMountNormal();
        Basis basis = new Basis(normal);
        double time = generator.getLevel() == null ? 0 : generator.getLevel().getGameTime() + partialTick;
        float gas = RechargerGeometry.clamp(generator.getCosmicProgress());
        float shield = RechargerGeometry.clamp(generator.getShieldProgress());
        stack.pushPose();
        stack.translate(.5D, .5D, .5D);
        for (int material=0; material<2; material++) {
            VertexConsumer surface = buffers.getBuffer(RenderType.entitySolid(material==0?DARK:METAL));
            for (float[] face : RechargerMesh.FACES) {
                if ((int)face[15]==material) draw(stack,surface,basis,face,1,1,1,1,packedLight,false);
            }
        }
        // Two counter-running light patterns around the fixed instrument rim.
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(LIGHT));
        float activity = Math.max(gas, shield * .25F);
        for (int i=0; i<48; i++) {
            double a=i*Math.PI/24;
            float intensity=.08F+activity*(.22F+.70F*RechargerGeometry.sweep(a,time));
            draw(stack,glow,basis,segment(a-.010,a+.010,1.075,1.25,1.84),intensity*.68F,intensity*.94F,intensity,1,LightTexture.FULL_BRIGHT,false);
        }
        for (int i=0; i<12; i++) {
            double a=i*Math.PI/6;
            float intensity=.06F+activity*(.30F+.64F*RechargerGeometry.sweep(a,-time*.7));
            draw(stack,glow,basis,segment(a-.042,a+.042,1.315,1.405,1.858),intensity*.20F,intensity,intensity*.36F,1,LightTexture.FULL_BRIGHT,false);
        }
        stack.popPose();
        renderZPMs(generator, normal, stack, buffers, packedLight, activity);
        // Translucent chamber contents are depth layers rather than expanding glass cubes.
        stack.pushPose();stack.translate(.5D,.5D,.5D);
        if (gas > .001F) {
            VertexConsumer mist = buffers.getBuffer(RenderType.entityTranslucentEmissive(MIST));
            for (int layer=0;layer<8;layer++) {
                double depth=.68+layer*.135;
                double turn=time*(layer%2==0?.003:-.002)+layer*.81;
                double radius=.88+Math.sin(time*.025+layer)*.025;
                float[] cloud=cloudQuad(radius,depth,turn);
                draw(stack,mist,basis,cloud,.73F,.96F,.86F,RechargerGeometry.gasAlpha(gas),LightTexture.FULL_BRIGHT,false);
            }
        }
        if (shield > .001F) {
            VertexConsumer film=buffers.getBuffer(RenderType.entityTranslucentEmissive(SHIELD));
            double inner=RechargerGeometry.shieldInnerRadius(shield);
            for (int i=0;i<64;i++) {
                draw(stack,film,basis,segment(i*Math.PI/32,(i+1)*Math.PI/32,inner,.94,RechargerGeometry.SHIELD_DEPTH),.48F,.85F,1,.65F,LightTexture.FULL_BRIGHT,true);
            }
        }
        stack.popPose();
    }

    private static float[] segment(double a,double b,double inner,double outer,double depth) {
        return new float[]{(float)(inner*Math.cos(a)),(float)(inner*Math.sin(a)),(float)depth,
                (float)(outer*Math.cos(a)),(float)(outer*Math.sin(a)),(float)depth,
                (float)(outer*Math.cos(b)),(float)(outer*Math.sin(b)),(float)depth,
                (float)(inner*Math.cos(b)),(float)(inner*Math.sin(b)),(float)depth,0,0,1};
    }
    private static float[] cloudQuad(double radius,double depth,double angle) {
        float[] result=new float[15];
        double[][] points={{-radius,-radius},{radius,-radius},{radius,radius},{-radius,radius}};
        for(int i=0;i<4;i++) {
            result[i*3]=(float)(points[i][0]*Math.cos(angle)-points[i][1]*Math.sin(angle));
            result[i*3+1]=(float)(points[i][0]*Math.sin(angle)+points[i][1]*Math.cos(angle));
            result[i*3+2]=(float)depth;
        }
        result[14]=1;return result;
    }
    private static void draw(PoseStack stack,VertexConsumer out,Basis basis,float[] face,float r,float g,float b,float alpha,int light,boolean radialUV) {
        PoseStack.Pose pose=stack.last();
        for(int vertex=0;vertex<4;vertex++) {
            int i=basis.flip?(4-vertex)%4:vertex,p=i*3;
            float x=face[p],y=face[p+1],z=face[p+2];
            float u=radialUV?.5F+x/1.88F:(i==0||i==3?0:1);
            float v=radialUV?.5F-y/1.88F:(i<2?1:0);
            out.vertex(pose.pose(),basis.x(x,y,z),basis.y(x,y,z),basis.z(x,y,z))
                    .color(r,g,b,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.normal(),basis.x(face[12],face[13],face[14]),basis.y(face[12],face[13],face[14]),basis.z(face[12],face[13],face[14])).endVertex();
        }
    }
    private static final class Basis {
        final Direction u,v,n;final boolean flip;
        Basis(Direction normal) {
            n=normal;u=GeneratorGeometry.uAxis(n);v=GeneratorGeometry.vAxis(n);
            int det=(u.getStepY()*v.getStepZ()-u.getStepZ()*v.getStepY())*n.getStepX()
                    +(u.getStepZ()*v.getStepX()-u.getStepX()*v.getStepZ())*n.getStepY()
                    +(u.getStepX()*v.getStepY()-u.getStepY()*v.getStepX())*n.getStepZ();
            flip=det<0;
        }
        float x(float a,float b,float c){return a*u.getStepX()+b*v.getStepX()+c*n.getStepX();}
        float y(float a,float b,float c){return a*u.getStepY()+b*v.getStepY()+c*n.getStepY();}
        float z(float a,float b,float c){return a*u.getStepZ()+b*v.getStepZ()+c*n.getStepZ();}
    }
    private void renderZPMs(ZeroPointEnergyGeneratorBlockEntity generator,Direction normal,PoseStack stack,MultiBufferSource buffers,int packedLight,float activity) {
        for(int slot=0;slot<ZeroPointEnergyGeneratorBlockEntity.ZPM_SLOT_COUNT;slot++) {
            ItemStack zpm=generator.getZPM(slot);if(zpm.isEmpty())continue;
            Vec3 offset=GeneratorGeometry.localOffset(normal,RechargerGeometry.slotU(slot),0,RechargerGeometry.MODULE_DEPTH);
            stack.pushPose();stack.translate(.5+offset.x,.5+offset.y,.5+offset.z);
            if(normal.getAxis()==Direction.Axis.Y)stack.mulPose(Axis.XP.rotationDegrees(90));
            stack.scale(RechargerGeometry.MODULE_SCALE,RechargerGeometry.MODULE_SCALE,RechargerGeometry.MODULE_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(zpm,ItemDisplayContext.FIXED,activity>.1F?LightTexture.FULL_BRIGHT:packedLight,OverlayTexture.NO_OVERLAY,stack,buffers,generator.getLevel(),slot+100);
            stack.popPose();
        }
    }
    @Override public boolean shouldRenderOffScreen(ZeroPointEnergyGeneratorBlockEntity blockEntity){return true;}
}
