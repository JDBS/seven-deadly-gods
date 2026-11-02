package com.volcov.sevendeadlygods.item;

import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Huevo Generador del Acolito / Centinela dedicado para cada Dios del Pecado:
 * - Clic derecho normal: genera un Acolito armado del dios.
 * - Clic derecho con Shift (agachado): genera un Centinela Permanente pacífico para devotos con comercio divino.
 */
public class GodAcolyteSpawnEggItem extends SpawnEggItem {
    private final SinGod god;

    public GodAcolyteSpawnEggItem(SinGod god, Item.Properties properties) {
        super(createPropertiesWithGodData(god, properties));
        this.god = god;
    }

    private static Item.Properties createPropertiesWithGodData(SinGod god, Item.Properties properties) {
        CompoundTag tag = new CompoundTag();
        tag.putString("god", god.getId());
        return properties
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(ModEntities.GOD_ACOLYTE.get(), tag))
                .requiredFeatures(ModEntities.GOD_ACOLYTE.get().requiredFeatures());
    }

    public SinGod getGod() {
        return this.god;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState blockState = level.getBlockState(pos);

        if (level.getBlockEntity(pos) instanceof Spawner spawnerHolder) {
            spawnerHolder.setEntityId(ModEntities.GOD_ACOLYTE.get(), level.getRandom());
            level.sendBlockUpdated(pos, blockState, blockState, 3);
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
            itemStack.consume(1, context.getPlayer());
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos = blockState.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(clickedFace);
        spawnAcolyte(serverLevel, context.getPlayer(), spawnPos, itemStack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
            return InteractionResult.PASS;
        }
        if (level.mayInteract(player, pos) && player.mayUseItemAt(pos, hitResult.getDirection(), itemStack)) {
            spawnAcolyte(serverLevel, player, pos, itemStack);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private void spawnAcolyte(ServerLevel level, Player player, BlockPos pos, ItemStack itemStack) {
        GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (acolyte != null) {
            boolean sentinel = player != null && player.isShiftKeyDown();
            if (sentinel) {
                acolyte.configureVillageSentinel(level, this.god, pos);
            } else {
                acolyte.configureAcolyte(level, this.god, null, false);
            }
            acolyte.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntityWithPassengers(acolyte);
            acolyte.playAmbientSound();
            itemStack.consume(1, player);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }
}
