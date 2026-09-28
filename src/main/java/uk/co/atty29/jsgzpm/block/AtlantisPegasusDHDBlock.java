package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/** Master/control block for the five-block Atlantis Pegasus DHD console. */
public final class AtlantisPegasusDHDBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);

    public AtlantisPegasusDHDBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 30.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtlantisPegasusDHDBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModRegistries.ATLANTIS_PEGASUS_DHD_BLOCK_ENTITY.get(), AtlantisPegasusDHDBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction front = ctx.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, front);
        for (PartPlacement part : getParts(ctx.getClickedPos(), state)) {
            if (!ctx.getLevel().getBlockState(part.pos()).canBeReplaced(ctx)) return null;
        }
        return state;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction front = frontDirection(state);
        for (PartPlacement part : getParts(pos, state)) {
            level.setBlock(part.pos(), ModRegistries.ATLANTIS_PEGASUS_DHD_PART.get().defaultBlockState()
                    .setValue(AtlantisPegasusDHDPartBlock.FACING, front)
                    .setValue(AtlantisPegasusDHDPartBlock.PART, part.index()), 3);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AtlantisPegasusDHDBlockEntity dhd)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;

        if (player.isShiftKeyDown()) {
            player.sendSystemMessage(dhd.relink());
            return InteractionResult.CONSUME;
        }

        int index = gridIndex(state, pos, hit);
        if (!dhd.pressSymbol(index, serverPlayer)) {
            player.sendSystemMessage(Component.translatable("message.jsgzpm.dhd.no_button"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) removeParts(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) removeParts(level, pos, level.getBlockState(pos));
        super.wasExploded(level, pos, explosion);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof AtlantisPegasusDHDBlockEntity dhd && dhd.isAnyAlarmActive()) return 15;
        return 0;
    }

    public static Direction frontDirection(BlockState state) {
        return state.getValue(FACING);
    }

    public static BlockPos masterFromPart(BlockPos partPos, Direction front, int part) {
        Direction right = front.getClockWise();
        return switch (part) {
            case 0 -> partPos.relative(right);
            case 1 -> partPos.relative(right.getOpposite());
            case 2 -> partPos.relative(front.getOpposite()).relative(right);
            case 3 -> partPos.relative(front.getOpposite()).relative(right.getOpposite());
            default -> partPos;
        };
    }

    private static PartPlacement[] getParts(BlockPos master, BlockState state) {
        Direction front = frontDirection(state);
        Direction right = front.getClockWise();
        return new PartPlacement[]{
                new PartPlacement(master.relative(right.getOpposite()), 0),
                new PartPlacement(master.relative(right), 1),
                new PartPlacement(master.relative(front).relative(right.getOpposite()), 2),
                new PartPlacement(master.relative(front).relative(right), 3)
        };
    }

    private static void removeParts(Level level, BlockPos master, BlockState state) {
        if (!state.hasProperty(FACING)) return;
        for (PartPlacement part : getParts(master, state)) {
            BlockState child = level.getBlockState(part.pos());
            if (child.is(ModRegistries.ATLANTIS_PEGASUS_DHD_PART.get())) {
                level.removeBlock(part.pos(), false);
            }
        }
    }

    private static int gridIndex(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction front = frontDirection(state);
        Direction right = front.getClockWise();
        double dx = hit.getLocation().x - (pos.getX() + 0.5D);
        double dz = hit.getLocation().z - (pos.getZ() + 0.5D);
        double localX = 0.5D + dx * right.getStepX() + dz * right.getStepZ();
        double localZ = 0.5D + dx * front.getStepX() + dz * front.getStepZ();
        int col = Mth.clamp((int) Math.floor(localX * 7.0D), 0, 6);
        int row = Mth.clamp((int) Math.floor(localZ * 6.0D), 0, 5);
        return row * 7 + col;
    }

    private record PartPlacement(BlockPos pos, int index) {
    }
}
