package com.volcov.sevendeadlygods.item;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import com.volcov.sevendeadlygods.village.GodVillageRaidManager;
import com.volcov.sevendeadlygods.village.GodVillageSavedData;
import com.volcov.sevendeadlygods.village.GodVillageState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Cuerno de Guerra Divino (Heraldo del Pecado).
 * Permite a un jugador con Favor >= +40 hacia su dios principal iniciar un asedio
 * contra una aldea rival consagrada, invocando un escuadrón de 6 Acólitos de asalto.
 */
public class DivineWarHornItem extends Item {
    public DivineWarHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        // 1. Determinar el dios con el que el jugador tiene mayor devoción (requiere >= +40)
        SinGod playerGod = findPrimaryPatronGod(serverPlayer);
        if (playerGod == null) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Ningún dios te considera digno de liderar un asedio (necesitas al menos +40 de Favor).")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }

        // 2. Verificar que estamos dentro de una aldea consagrada enemiga
        GodVillageSavedData data = GodVillageSavedData.get(serverLevel);
        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = data.findVillageAt(serverPlayer.blockPosition());

        if (villageOpt.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("No te encuentras dentro de ninguna aldea consagrada que asediar.")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.FAIL;
        }

        GodVillageSavedData.GodVillageRecord village = villageOpt.get();
        if (village.patronGod() == playerGod) {
            serverPlayer.sendSystemMessage(
                    playerGod.getFormattedPrefix().append(
                            Component.literal("«¿Pretendes tocar el cuerno de guerra contra mi propio templo sagrado? ¡Insensato!»")
                                    .withStyle(ChatFormatting.RED)
                    )
            );
            return InteractionResult.FAIL;
        }

        if (village.state() == GodVillageState.EN_ASEDIO) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Esta aldea ya se encuentra bajo asedio.")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return InteractionResult.FAIL;
        }

        // 3. Ejecutar el llamado: iniciar el asedio de aldea y desplegar los 6 Acólitos aliados
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 3.0F, 1.0F);
        serverPlayer.getCooldowns().addCooldown(stack, 20 * 60); // 60 segundos de cooldown
        ModAdvancements.award(serverPlayer, "divine_war_horn_use");

        GodVillageRaidManager.startRaidOnVillage(
                serverLevel.getServer(),
                serverLevel,
                data,
                village,
                playerGod,
                serverPlayer
        );

        spawnPlayerAssaultSquad(serverLevel, serverPlayer, playerGod, village.altarPos());

        serverPlayer.sendSystemMessage(
                playerGod.getFormattedPrefix().append(
                        Component.literal("«¡Que arda el santuario de " + village.patronGod().getDisplayName() + "! ¡Mis Acólitos marchan a tu lado!»")
                                .withStyle(playerGod.getChatColor(), ChatFormatting.BOLD)
                )
        );

        return InteractionResult.SUCCESS;
    }

    private static void spawnPlayerAssaultSquad(
            ServerLevel level,
            ServerPlayer commander,
            SinGod attackerGod,
            BlockPos targetAltarPos
    ) {
        BlockPos basePos = commander.blockPosition();
        for (int i = 0; i < 6; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                double angle = (2.0D * Math.PI / 6.0D) * i;
                int x = (int) Math.round(basePos.getX() + 4.0D * Math.cos(angle));
                int z = (int) Math.round(basePos.getZ() + 4.0D * Math.sin(angle));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                acolyte.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                acolyte.configureAcolyte(level, attackerGod, commander.getUUID(), true);
                acolyte.setHomeAltarPos(targetAltarPos);
                acolyte.getNavigation().moveTo(targetAltarPos.getX() + 0.5D, targetAltarPos.getY(), targetAltarPos.getZ() + 0.5D, 1.2D);
                level.addFreshEntity(acolyte);
            }
        }
    }

    @Nullable
    public static SinGod findPrimaryPatronGod(ServerPlayer player) {
        SinGod best = null;
        int maxFavor = 39; // Umbral mínimo: +40
        for (SinGod god : SinGod.values()) {
            int favor = SinFavorManager.getFavor(player, god);
            if (favor > maxFavor) {
                maxFavor = favor;
                best = god;
            }
        }
        return best;
    }
}
