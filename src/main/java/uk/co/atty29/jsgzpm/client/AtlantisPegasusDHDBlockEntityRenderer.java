package uk.co.atty29.jsgzpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tauri.jsg.api.entity.StargateAddressData;
import dev.tauri.jsg.api.registry.JSGSymbolTypes;
import dev.tauri.jsg.api.stargate.network.address.symbol.types.SymbolPegasusEnum;
import dev.tauri.jsg.core.common.blockstate.JSGProperties;
import dev.tauri.jsg.core.common.entity.NotebookPageType;
import dev.tauri.jsg.core.common.item.notebook.NotebookItem;
import dev.tauri.jsg.core.common.symbol.SymbolInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;

import java.util.Collection;
import java.util.List;

/**
 * First-pass physical symbol display for the Atlantis console.
 * It deliberately uses JSG's notebook page data rather than inventing a second
 * address format, so the next Pegasus symbol glows from the same notebooks.
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

        SymbolInterface suggested = getSuggestedSymbol(dhd);
        Collection<SymbolInterface> active = dhd.getStateManager().getButtonsState().getActivatedButtons();
        List<SymbolPegasusEnum> symbols = AtlantisPegasusDHDBlockEntity.getPressableSymbols();
        int rotation = dhd.getBlockState().getValue(JSGProperties.ROTATION_PROPERTY);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.635D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.0105F, -0.0105F, 0.0105F);

        for (int i = 0; i < symbols.size() && i < 42; i++) {
            SymbolPegasusEnum symbol = symbols.get(i);
            int col = i % 7;
            int row = i / 7;
            float x = (col - 3) * 11.0F - 2.0F;
            float y = (row - 2.5F) * 11.0F;
            boolean isActive = active.contains(symbol);
            boolean isSuggested = suggested == symbol;
            int colour = isActive ? 0xFF55E7FF : (isSuggested ? 0xFFFFC857 : 0xFF55666F);
            int light = (isActive || isSuggested) ? LightTexture.FULL_BRIGHT : packedLight;
            font.drawInBatch("◆", x, y, colour, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, light);
        }

        if (dhd.isAnyAlarmActive()) {
            int colour = dhd.isOffworldAlarmActive() ? 0xFFFF8C32 : 0xFFFF4545;
            font.drawInBatch("ALARM", -17.0F, 42.0F, colour, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    @Nullable
    private static SymbolInterface getSuggestedSymbol(AtlantisPegasusDHDBlockEntity dhd) {
        CompoundTag compound = getHeldTag();
        if (compound == null) return null;
        if (compound.contains("pages")) compound = NotebookItem.getSelectedPageFromCompound(compound);
        if (compound == null) return null;

        var page = NotebookPageType.pageDataFromCompound(compound);
        if (page == null || !(page.data() instanceof StargateAddressData data)) return null;
        var address = data.getAddress();
        if (address.getSymbolType() != JSGSymbolTypes.PEGASUS.get()) return null;

        int activated = 0;
        for (SymbolInterface symbol : dhd.getStateManager().getButtonsState().getActivatedButtons()) {
            if (!symbol.brb()) activated++;
        }
        if (activated < address.size()) return address.get(activated);
        return JSGSymbolTypes.PEGASUS.get().getOrigin();
    }

    @Nullable
    private static CompoundTag getHeldTag() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.hasTag()) return main.getTag();
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        return off.hasTag() ? off.getTag() : null;
    }
}
