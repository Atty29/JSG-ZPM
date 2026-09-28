package uk.co.atty29.jsgzpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import uk.co.atty29.jsgzpm.blockentity.AncientPowerControllerBlockEntity;
import uk.co.atty29.jsgzpm.power.BankDischargeMode;
import uk.co.atty29.jsgzpm.registry.ModRegistries;
import uk.co.atty29.jsgzpm.util.EnergyFormat;

public final class AncientPowerControllerBlock extends BaseEntityBlock {
    public AncientPowerControllerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 8.0F)
                .sound(SoundType.METAL));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AncientPowerControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModRegistries.ANCIENT_POWER_CONTROLLER_BLOCK_ENTITY.get(), AncientPowerControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AncientPowerControllerBlockEntity controller)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            BankDischargeMode mode = controller.cycleDischargeMode();
            player.displayClientMessage(Component.translatable(
                    "message.jsgzpm.controller.mode_changed",
                    Component.translatable(mode.translationKey())
            ), false);
            return InteractionResult.CONSUME;
        }

        controller.refreshNetwork();
        player.displayClientMessage(Component.translatable(
                "message.jsgzpm.controller.status",
                Component.translatable(controller.getDischargeMode().translationKey()),
                controller.getLinkedHolderCount(),
                controller.getOnlineHolderCount(),
                controller.getActiveZPMCount(),
                controller.getInstalledZPMCount(),
                EnergyFormat.format(controller.getTotalEnergyLong()),
                EnergyFormat.format(controller.getTotalCapacityLong())
        ), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AncientPowerControllerBlockEntity controller) {
                controller.releaseAllClaims();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
