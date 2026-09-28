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
}
