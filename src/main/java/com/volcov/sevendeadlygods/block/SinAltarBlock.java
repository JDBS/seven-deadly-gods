package com.volcov.sevendeadlygods.block;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SinAltarBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 3.0D, 15.0D),
            Block.box(3.0D, 3.0D, 3.0D, 13.0D, 11.0D, 13.0D),
            Block.box(0.0D, 11.0D, 0.0D, 16.0D, 15.0D, 16.0D)
    );

    private final SinGod god;

    public SinAltarBlock(SinGod god, BlockBehaviour.Properties properties) {
        super(properties);
        this.god = god;
    }

    public SinGod getGod() {
        return this.god;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SinAltarBlockEntity(pos, state, this.god);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SinAltarBlockEntity altar) {
            return altar.handlePlayerInteraction(serverLevel, serverPlayer, stack, hand);
        }
        return InteractionResult.PASS;
    }
}
