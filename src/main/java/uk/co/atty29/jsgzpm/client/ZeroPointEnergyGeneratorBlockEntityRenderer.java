package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import uk.co.atty29.jsgzpm.blockentity.ZeroPointEnergyGeneratorBlockEntity;
import uk.co.atty29.jsgzpm.generator.GeneratorGeometry;
import uk.co.atty29.jsgzpm.generator.GeneratorState;

public final class ZeroPointEnergyGeneratorBlockEntityRenderer implements BlockEntityRenderer<ZeroPointEnergyGeneratorBlockEntity> {
    public ZeroPointEnergyGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ZeroPointEnergyGeneratorBlockEntity generator, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction normal = generator.getMountNormal();
        renderZPMs(generator, normal, poseStack, buffers, packedLight);

        float shield = generator.getShieldProgress();
        if (shield > 0.001F) {
            renderShield(normal, shield, poseStack, buffers);
        }

        float cosmic = generator.getCosmicProgress();
        if (cosmic > 0.001F) {
            renderCosmicField(generator, normal, cosmic, poseStack, buffers);
        }
    }

    private void renderZPMs(ZeroPointEnergyGeneratorBlockEntity generator, Direction normal, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        for (int slot = 0; slot < ZeroPointEnergyGeneratorBlockEntity.ZPM_SLOT_COUNT; slot++) {
            ItemStack zpm = generator.getZPM(slot);
            if (zpm.isEmpty()) continue;

            Vec3 offset = GeneratorGeometry.localOffset(normal, (slot - 1) * 0.52D, 0.0D, 0.30D);
            poseStack.pushPose();
            poseStack.translate(0.5D + offset.x, 0.5D + offset.y, 0.5D + offset.z);
            alignItemToNormal(poseStack, normal);
            // Keep the installed crystal height at 0.42 blocks with the recreated item.
            poseStack.scale(0.40F, 0.40F, 0.40F);

            boolean charging = generator.getGeneratorState() == GeneratorState.CHARGING;
            int light = charging ? LightTexture.FULL_BRIGHT : packedLight;
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    zpm,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffers,
                    generator.getLevel(),
                    slot + 100
            );
            poseStack.popPose();
        }
    }

    private void renderShield(Direction normal, float progress, PoseStack poseStack, MultiBufferSource buffers) {
        Vec3 offset = GeneratorGeometry.localOffset(normal, 0.0D, 0.0D, 0.72D);
        float plane = 0.20F + 1.75F * progress;
        renderScaledBlock(
                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                normal,
                offset,
                plane,
                0.075F,
                poseStack,
                buffers
        );
    }

    private void renderCosmicField(ZeroPointEnergyGeneratorBlockEntity generator, Direction normal, float progress, PoseStack poseStack, MultiBufferSource buffers) {
        long time = generator.getLevel() == null ? 0L : generator.getLevel().getGameTime();
        float pulseA = 1.0F + 0.08F * (float) Math.sin((time + 0.0D) * 0.11D);
        float pulseB = 1.0F + 0.06F * (float) Math.sin((time + 13.0D) * 0.09D);

        Vec3 inner = GeneratorGeometry.localOffset(normal, 0.0D, 0.0D, 0.34D);
        Vec3 outer = GeneratorGeometry.localOffset(normal, 0.0D, 0.0D, 0.48D);
        renderScaledBlock(
                Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                normal,
                inner,
                1.48F * progress * pulseA,
                0.38F * progress,
                poseStack,
                buffers
        );
        renderScaledBlock(
                Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(),
                normal,
                outer,
                1.15F * progress * pulseB,
                0.26F * progress,
                poseStack,
                buffers
        );
    }

    private void renderScaledBlock(net.minecraft.world.level.block.state.BlockState state, Direction normal, Vec3 offset, float planeSize, float depth, PoseStack poseStack, MultiBufferSource buffers) {
        if (planeSize <= 0.001F || depth <= 0.001F) return;

        float sx = planeSize;
        float sy = planeSize;
        float sz = planeSize;
        if (normal.getAxis() == Direction.Axis.X) sx = depth;
        if (normal.getAxis() == Direction.Axis.Y) sy = depth;
        if (normal.getAxis() == Direction.Axis.Z) sz = depth;

        poseStack.pushPose();
        poseStack.translate(0.5D + offset.x, 0.5D + offset.y, 0.5D + offset.z);
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state,
                poseStack,
                buffers,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }

    private void alignItemToNormal(PoseStack poseStack, Direction normal) {
        switch (normal) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case UP -> {
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ZeroPointEnergyGeneratorBlockEntity blockEntity) {
        return true;
    }
}
