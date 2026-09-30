package uk.co.atty29.jsgzpm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ZeroPointGeneratorCasingBlock extends Block {
    public ZeroPointGeneratorCasingBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 8.0F)
                .sound(SoundType.METAL));
    }
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.jsgzpm.generator_casing"));
    }

}
