package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import uk.co.atty29.jsgzpm.block.AtlantisPegasusDHDBlock;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;

/**
 * Release-safe first-pass symbol display for the Atlantis console.
 *
 * The previous implementation directly linked to JSG 6.0 development notebook
 * and symbol classes.  This renderer intentionally owns its visual state so a
 * normal JSG 5.1.x installation can load JSG-ZPM.  Notebook guidance can be
 * restored later through the same reflection compatibility layer used by the
 * functional DHD once its released NBT format is verified in-game.
 */
public final class AtlantisPegasusDHDBlockEntityRenderer implements BlockEntityRenderer<AtlantisPegasusDHDBlockEntity> {
    private final Font font;

    public AtlantisPegasusDHDBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(AtlantisPegasusDHDBlockEntity dhd, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (dhd.getLevel() == null) return;

        int activeIndex = dhd.getLastPressedIndex();
        float rotation = dhd.getBlockState().getValue(AtlantisPegasusDHDBlock.FACING).toYRot();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.635D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.0105F, -0.0105F, 0.0105F);

        for (int i = 0; i < AtlantisPegasusDHDBlockEntity.DISPLAY_BUTTON_COUNT; i++) {
            int col = i % 7;
            int row = i / 7;
            float x = (col - 3) * 11.0F - 2.0F;
            float y = (row - 2.5F) * 11.0F;
            boolean active = i == activeIndex;
            int colour = active ? 0xFF55E7FF : 0xFF55666F;
            int light = active ? LightTexture.FULL_BRIGHT : packedLight;
            font.drawInBatch("◆", x, y, colour, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, light);
        }

        if (dhd.isAnyAlarmActive()) {
            int colour = dhd.isGeneralAlarmActive() ? 0xFFFF4545 : 0xFFFF8C32;
            font.drawInBatch("ALARM", -17.0F, 42.0F, colour, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }
}
