package uk.co.atty29.jsgzpm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import uk.co.atty29.jsgzpm.block.AtlantisPegasusDHDBlock;
import uk.co.atty29.jsgzpm.block.AtlantisPegasusDHDPartBlock;
import uk.co.atty29.jsgzpm.blockentity.AtlantisAlarmEmitterBlockEntity;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

/**
 * Manual, persistent DHD-to-emitter linker. One stored DHD can be applied to
 * many emitters, which avoids runtime world scans and makes large bases cheap.
 */
public final class AncientAlarmLinkerItem extends Item {
    private static final String TAG_DIMENSION = "AlarmDhdDimension";
    private static final String TAG_POS = "AlarmDhdPos";

    public AncientAlarmLinkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos clicked = context.getClickedPos();
        ItemStack linker = context.getItemInHand();

        BlockPos dhdPos = resolveDhdPos(level, clicked);
        if (dhdPos != null) {
            if (!level.isClientSide) {
                CompoundTag tag = linker.getOrCreateTag();
                tag.putString(TAG_DIMENSION, level.dimension().location().toString());
                tag.putLong(TAG_POS, dhdPos.asLong());
                player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.dhd_saved", dhdPos.getX(), dhdPos.getY(), dhdPos.getZ()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.getBlockEntity(clicked) instanceof AtlantisAlarmEmitterBlockEntity emitter) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    emitter.clearLink();
                    player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.emitter_cleared"), true);
                    return InteractionResult.CONSUME;
                }

                CompoundTag tag = linker.getTag();
                if (tag == null || !tag.contains(TAG_DIMENSION) || !tag.contains(TAG_POS)) {
                    player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.no_dhd"), true);
                    return InteractionResult.CONSUME;
                }

                ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
                if (dimension == null || !level.dimension().location().equals(dimension)) {
                    player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.wrong_dimension"), true);
                    return InteractionResult.CONSUME;
                }

                BlockPos stored = BlockPos.of(tag.getLong(TAG_POS));
                if (!(level.getBlockEntity(stored) instanceof AtlantisPegasusDHDBlockEntity)) {
                    player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.dhd_missing"), true);
                    return InteractionResult.CONSUME;
                }

                emitter.linkTo(dimension, stored);
                player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.emitter_linked"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    tag.remove(TAG_DIMENSION);
                    tag.remove(TAG_POS);
                }
                player.displayClientMessage(Component.translatable("message.jsgzpm.alarm_linker.memory_cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static BlockPos resolveDhdPos(Level level, BlockPos clicked) {
        if (level.getBlockEntity(clicked) instanceof AtlantisPegasusDHDBlockEntity) return clicked;

        BlockState state = level.getBlockState(clicked);
        if (state.is(ModRegistries.ATLANTIS_PEGASUS_DHD_PART.get())) {
            BlockPos master = AtlantisPegasusDHDBlock.masterFromPart(
                    clicked,
                    state.getValue(AtlantisPegasusDHDPartBlock.FACING),
                    state.getValue(AtlantisPegasusDHDPartBlock.PART)
            );
            if (level.getBlockEntity(master) instanceof AtlantisPegasusDHDBlockEntity) return master;
        }
        return null;
    }
}
