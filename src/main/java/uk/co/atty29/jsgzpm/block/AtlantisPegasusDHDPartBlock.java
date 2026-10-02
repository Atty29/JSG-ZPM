package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/** Physical wing/rear sections of the floor-integrated Atlantis DHD. */
public final class AtlantisPegasusDHDPartBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public AtlantisPegasusDHDPartBlock() {
        super(Properties.of()
                .strength(3.0F, 30.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos masterPos = AtlantisPegasusDHDBlock.masterFromPart(pos, state.getValue(FACING), state.getValue(PART));
        if (!(level.getBlockEntity(masterPos) instanceof AtlantisPegasusDHDBlockEntity dhd)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;

        BlockState master=level.getBlockState(masterPos);
        return master.getBlock().use(master,level,masterPos,player,hand,hit);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos masterPos = AtlantisPegasusDHDBlock.masterFromPart(pos, state.getValue(FACING), state.getValue(PART));
            if (level.getBlockState(masterPos).is(ModRegistries.ATLANTIS_PEGASUS_DHD.get())) {
                level.destroyBlock(masterPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(FACING) && state.hasProperty(PART)) {
                BlockPos masterPos = AtlantisPegasusDHDBlock.masterFromPart(pos, state.getValue(FACING), state.getValue(PART));
                if (level.getBlockState(masterPos).is(ModRegistries.ATLANTIS_PEGASUS_DHD.get())) {
                    level.destroyBlock(masterPos, true);
                }
            }
        }
        super.wasExploded(level, pos, explosion);
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
