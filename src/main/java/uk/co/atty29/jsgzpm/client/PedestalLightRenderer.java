package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Small emissive overlay only; the black casing and metal rings keep normal shading. */
public final class PedestalLightRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("jsgzpm", "textures/block/ancient/light.png");
    private PedestalLightRenderer() {}

    public static void render(PoseStack stack, MultiBufferSource buffers, Direction facing) {
        stack.pushPose();
        stack.translate(.5D, 0, .5D);
        // Authored block geometry faces north; blockstate Y rotations use the opposite sign.
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        stack.translate(-.5D, 0, -.5D);
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        PoseStack.Pose pose = stack.last();
        for (float[][] group : PedestalLightGeometry.GROUPS) {
            for (float[] face : group) {
                for (int i = 0; i < 4; i++) {
                    int p = i * 3;
                    out.vertex(pose.pose(), face[p], face[p + 1], face[p + 2])
                            .color(face[12], face[13], face[14], 1.0F)
                            .uv(i == 0 || i == 1 ? 0 : 1, i == 0 || i == 3 ? 0 : 1)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BRIGHT)
                            .normal(pose.normal(), 0, 1, 0).endVertex();
                }
            }
        }
        stack.popPose();
    }
}
