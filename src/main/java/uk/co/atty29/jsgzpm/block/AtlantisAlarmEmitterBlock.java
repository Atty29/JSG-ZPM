package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.alarm.AtlantisAlarmState;
import uk.co.atty29.jsgzpm.blockentity.AtlantisAlarmEmitterBlockEntity;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

/** A small face-mounted alarm speaker/indicator linked to one Atlantis DHD. */
public final class AtlantisAlarmEmitterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<AtlantisAlarmState> ALARM = EnumProperty.create("alarm", AtlantisAlarmState.class);
    private static final VoxelShape SHAPE = box(3, 3, 3, 13, 13, 13);

    public AtlantisAlarmEmitterBlock() {
        super(Properties.of()
                .strength(2.0F, 12.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(ALARM) == AtlantisAlarmState.IDLE ? 0 : 8));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ALARM, AtlantisAlarmState.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, ALARM);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace()).setValue(ALARM, AtlantisAlarmState.IDLE);
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtlantisAlarmEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistries.ATLANTIS_ALARM_EMITTER_BLOCK_ENTITY.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) ->
                AtlantisAlarmEmitterBlockEntity.serverTick(tickLevel, tickPos, tickState, (AtlantisAlarmEmitterBlockEntity) blockEntity);
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
}
