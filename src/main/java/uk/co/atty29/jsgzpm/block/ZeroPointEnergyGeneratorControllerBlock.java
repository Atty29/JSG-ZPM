package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.blockentity.ZeroPointEnergyGeneratorBlockEntity;
import uk.co.atty29.jsgzpm.generator.GeneratorState;
import uk.co.atty29.jsgzpm.registry.ModItemTags;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

public final class ZeroPointEnergyGeneratorControllerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public ZeroPointEnergyGeneratorControllerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 8.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.jsgzpm.generator_facing"));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZeroPointEnergyGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                ModRegistries.ZERO_POINT_ENERGY_GENERATOR_BLOCK_ENTITY.get(),
                level.isClientSide ? ZeroPointEnergyGeneratorBlockEntity::clientTick : ZeroPointEnergyGeneratorBlockEntity::serverTick
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ZeroPointEnergyGeneratorBlockEntity generator)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && held.is(ModRegistries.ZERO_POINT_MODULE.get())) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            int slot = generator.insertZPM(held);
            if (slot >= 0) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.translatable("message.jsgzpm.generator.zpm_inserted", slot + 1), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.jsgzpm.generator.locked_or_full"), true);
            return InteractionResult.FAIL;
        }

        if (!held.isEmpty() && held.is(ModItemTags.EFFICIENCY_UPGRADE_CRYSTAL)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (generator.insertEfficiencyUpgrade(held)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.translatable(
                        "message.jsgzpm.generator.upgrade_inserted",
                        generator.getEfficiencyUpgradeCount(),
                        generator.getEfficiencyPercent()
                ), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.jsgzpm.generator.locked_or_full"), true);
            return InteractionResult.FAIL;
        }

        if (!held.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            ItemStack removed = generator.removeLastZPM();
            if (removed.isEmpty()) removed = generator.removeLastEfficiencyUpgrade();
            if (!removed.isEmpty()) {
                if (!player.getInventory().add(removed)) player.drop(removed, false);
                player.displayClientMessage(Component.translatable("message.jsgzpm.generator.item_removed"), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.jsgzpm.generator.nothing_removable"), true);
            return InteractionResult.FAIL;
        }

        if (generator.getGeneratorState() == GeneratorState.IDLE) {
            generator.refreshStructure();
            if (!generator.isFormed()) {
                player.displayClientMessage(Component.translatable("message.jsgzpm.generator.structure_incomplete"), true);
                return InteractionResult.FAIL;
            }
            if (generator.getInstalledZPMCount() == 0) {
                player.displayClientMessage(Component.translatable("message.jsgzpm.generator.no_zpm"), true);
                return InteractionResult.FAIL;
            }
            if (!generator.hasChargeableZPM()) {
                player.displayClientMessage(Component.translatable("message.jsgzpm.generator.already_full"), true);
                return InteractionResult.FAIL;
            }
            if (generator.startCharging()) {
                player.displayClientMessage(Component.translatable(
                        "message.jsgzpm.generator.started",
                        generator.getEfficiencyPercent()
                ), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        if (generator.stopCharging()) {
            player.displayClientMessage(Component.translatable("message.jsgzpm.generator.stopped"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ZeroPointEnergyGeneratorBlockEntity generator) generator.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
