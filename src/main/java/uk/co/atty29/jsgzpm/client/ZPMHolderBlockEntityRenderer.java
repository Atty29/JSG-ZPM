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
import uk.co.atty29.jsgzpm.block.ZPMHolderBlock;
import uk.co.atty29.jsgzpm.blockentity.ZPMHolderBlockEntity;
import uk.co.atty29.jsgzpm.holder.ZPMHolderLayout;
import uk.co.atty29.jsgzpm.holder.HubGeometry;
import uk.co.atty29.jsgzpm.holder.PedestalGeometry;
import uk.co.atty29.jsgzpm.holder.WallHolderGeometry;
import uk.co.atty29.jsgzpm.holder.ZPMSlotState;
import uk.co.atty29.jsgzpm.item.ZPMItem;

public final class ZPMHolderBlockEntityRenderer implements BlockEntityRenderer<ZPMHolderBlockEntity> {
    public ZPMHolderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ZPMHolderBlockEntity holder, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(holder.getBlockState().getBlock() instanceof ZPMHolderBlock block)) return;

        ZPMHolderLayout layout = block.layout();
        Direction facing = holder.getBlockState().getValue(ZPMHolderBlock.FACING);
        Direction side = facing.getClockWise();

        if (layout == ZPMHolderLayout.PEDESTAL && holder.getBlockState().getValue(ZPMHolderBlock.LIT)) {
            PedestalLightRenderer.render(poseStack, buffers, facing);
        }

        for (int slot = 0; slot < ZPMHolderBlockEntity.SLOT_COUNT; slot++) {
            ItemStack zpm = holder.getZPM(slot);
            if (zpm.isEmpty()) continue;

            float progress = holder.getAnimationProgress(slot);
            double x = 0.5D;
            double y = 0.70D + (1.0D - progress) * 0.28D;
            double z = 0.5D;

            if (layout == ZPMHolderLayout.HUB) {
                x += side.getStepX() * HubGeometry.sideOffset(slot) + facing.getStepX() * HubGeometry.forwardOffset(slot);
                z += side.getStepZ() * HubGeometry.sideOffset(slot) + facing.getStepZ() * HubGeometry.forwardOffset(slot);
                y = HubGeometry.DOWN_CENTRE_Y + (1.0D - progress) * HubGeometry.TRAVEL;
            } else if (layout == ZPMHolderLayout.PEDESTAL) {
                y = PedestalGeometry.MODULE_CENTRE_Y;
            } else {
                double sideways = WallHolderGeometry.sideOffset(layout, slot, progress);
                double forward = WallHolderGeometry.forwardOffset(layout, progress);
                x += side.getStepX() * sideways + facing.getStepX() * forward;
                z += side.getStepZ() * sideways + facing.getStepZ() * forward;
                y = WallHolderGeometry.centreY(layout, slot, progress);
            }

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            if (layout == ZPMHolderLayout.ARRAY || layout == ZPMHolderLayout.COLUMN) {
                // Local +Z points out of the facing after the existing yaw rotation.
                poseStack.mulPose(Axis.ZP.rotationDegrees(WallHolderGeometry.rollDegrees(layout)));
                poseStack.mulPose(Axis.XP.rotationDegrees(WallHolderGeometry.tiltDegrees(layout)));
            }
            // The new item is 1.05 blocks tall; retain the other holders' installed height.
            float scale = layout == ZPMHolderLayout.PEDESTAL ? PedestalGeometry.MODULE_SCALE
                    : layout == ZPMHolderLayout.HUB ? HubGeometry.MODULE_SCALE : WallHolderGeometry.MODULE_SCALE;
            poseStack.scale(scale, scale, scale);

            ZPMSlotState state = holder.getSlotState(slot);
            boolean active = state.isDown() && ZPMItem.getStoredEnergy(zpm) > 0L;
            int light = active ? LightTexture.FULL_BRIGHT : packedLight;

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    zpm,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffers,
                    holder.getLevel(),
                    slot
            );
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ZPMHolderBlockEntity blockEntity) {
        return true;
    }
}
