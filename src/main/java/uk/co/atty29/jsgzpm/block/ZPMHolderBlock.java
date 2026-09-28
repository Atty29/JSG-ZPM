package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.blockentity.ZPMHolderBlockEntity;
import uk.co.atty29.jsgzpm.holder.ZPMHolderLayout;
import uk.co.atty29.jsgzpm.holder.HubGeometry;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import java.util.List;

public final class ZPMHolderBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

    private final ZPMHolderLayout layout;

    public ZPMHolderBlock(ZPMHolderLayout layout) {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 8.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIT) ? 12 : 0));
        this.layout = layout;
        int defaultPart = layout == ZPMHolderLayout.COLUMN ? 0 : 1;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, defaultPart).setValue(LIT, false));
    }

    public ZPMHolderLayout layout() {
        return layout;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (layout == ZPMHolderLayout.PEDESTAL) return Block.box(1, 0, 1, 15, 16, 15);
        return super.getShape(state, level, pos, context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        if (layout == ZPMHolderLayout.ARRAY) {
            Direction side = facing.getClockWise();
            if (!level.getBlockState(pos.relative(side)).canBeReplaced(context)
                    || !level.getBlockState(pos.relative(side.getOpposite())).canBeReplaced(context)) {
                return null;
            }
            return defaultBlockState().setValue(FACING, facing).setValue(PART, 1);
        }

        if (layout == ZPMHolderLayout.COLUMN) {
            if (!level.getBlockState(pos.above()).canBeReplaced(context)
                    || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
                return null;
            }
            return defaultBlockState().setValue(FACING, facing).setValue(PART, 0);
        }

        return defaultBlockState().setValue(FACING, facing).setValue(PART, 1);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        if (layout == ZPMHolderLayout.ARRAY) {
            Direction side = facing.getClockWise();
            level.setBlock(pos.relative(side.getOpposite()), state.setValue(PART, 0), Block.UPDATE_ALL);
            level.setBlock(pos.relative(side), state.setValue(PART, 2), Block.UPDATE_ALL);
        } else if (layout == ZPMHolderLayout.COLUMN) {
            level.setBlock(pos.above(), state.setValue(PART, 1), Block.UPDATE_ALL);
            level.setBlock(pos.above(2), state.setValue(PART, 2), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) != 1) return null;
        return new ZPMHolderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != 1) return null;
        return createTickerHelper(
                type,
                ModRegistries.ZPM_HOLDER_BLOCK_ENTITY.get(),
                level.isClientSide ? ZPMHolderBlockEntity::clientTick : ZPMHolderBlockEntity::serverTick
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos controllerPos = controllerPos(pos, state, layout);
        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        if (!(blockEntity instanceof ZPMHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }

        int slot = resolveSlot(state, pos, hit);
        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && held.is(ModRegistries.ZERO_POINT_MODULE.get())) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (holder.insertZPM(slot, held)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.translatable("message.jsgzpm.holder.inserted", slot + 1), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        if (!held.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            ItemStack removed = holder.removeZPM(slot);
            if (!removed.isEmpty()) {
                if (!player.getInventory().add(removed)) player.drop(removed, false);
                player.displayClientMessage(Component.translatable("message.jsgzpm.holder.removed", slot + 1), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        if (holder.toggleSlot(slot)) {
            player.displayClientMessage(Component.translatable("message.jsgzpm.holder.toggled", slot + 1), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    private int resolveSlot(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (layout == ZPMHolderLayout.PEDESTAL) return 0;
        if (layout != ZPMHolderLayout.HUB) return state.getValue(PART);

        double x = hit.getLocation().x - pos.getX();
        double z = hit.getLocation().z - pos.getZ();
        Direction facing = state.getValue(FACING);
        Direction side = facing.getClockWise();
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < ZPMHolderBlockEntity.SLOT_COUNT; i++) {
            double dx = x - (0.5D + side.getStepX() * HubGeometry.sideOffset(i) + facing.getStepX() * HubGeometry.forwardOffset(i));
            double dz = z - (0.5D + side.getStepZ() * HubGeometry.sideOffset(i) + facing.getStepZ() * HubGeometry.forwardOffset(i));
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (layout == ZPMHolderLayout.PEDESTAL && !state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ZPMHolderBlockEntity holder) holder.dropContents();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos controller = controllerPos(pos, state, layout);
            BlockEntity blockEntity = level.getBlockEntity(controller);
            if (blockEntity instanceof ZPMHolderBlockEntity holder) holder.dropContents();

            for (BlockPos structurePos : structurePositions(controller, state.getValue(FACING), layout)) {
                if (!structurePos.equals(pos) && level.getBlockState(structurePos).is(this)) {
                    level.setBlock(structurePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    public static BlockPos controllerPos(BlockPos pos, BlockState state, ZPMHolderLayout layout) {
        int part = state.getValue(PART);
        if (layout == ZPMHolderLayout.HUB || layout == ZPMHolderLayout.PEDESTAL) return pos;
        if (layout == ZPMHolderLayout.COLUMN) return pos.above(1 - part);
        Direction side = state.getValue(FACING).getClockWise();
        return pos.relative(side, 1 - part);
    }

    public static List<BlockPos> structurePositions(BlockPos controller, Direction facing, ZPMHolderLayout layout) {
        if (layout == ZPMHolderLayout.HUB || layout == ZPMHolderLayout.PEDESTAL) return List.of(controller);
        if (layout == ZPMHolderLayout.COLUMN) return List.of(controller.below(), controller, controller.above());
        Direction side = facing.getClockWise();
        return List.of(controller.relative(side.getOpposite()), controller, controller.relative(side));
    }
}
