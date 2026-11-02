package com.volcov.sevendeadlygods.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.lore.SinLoreLibrary;
import com.volcov.sevendeadlygods.registry.ModBlocks;
import com.volcov.sevendeadlygods.village.GodVillageRaidManager;
import com.volcov.sevendeadlygods.village.GodVillageSavedData;
import com.volcov.sevendeadlygods.village.GodVillageState;
import com.volcov.sevendeadlygods.village.VillageCultureApplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

public final class GodsCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("gods")
                .then(Commands.literal("favor")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerSinData data = SinFavorManager.get(player);
                            player.sendSystemMessage(Component.literal("═══ Marcadores de Favor con los 7 Pecados Capitales ═══")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                            for (SinGod god : SinGod.values()) {
                                int fav = data.getFavor(god);
                                player.sendSystemMessage(
                                        Component.literal(" • " + god.getDisplayName() + " (" + god.getSinName() + "): ")
                                                .withStyle(god.getChatColor())
                                                .append(Component.literal(fav + " / 100 ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                                                .append(Component.literal("[" + SinFavorManager.describeStanding(fav) + "]").withStyle(ChatFormatting.GRAY))
                                );
                            }
                            return 1;
                        }))
                .then(Commands.literal("next")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel level = player.level();
                            PlayerSinData data = SinFavorManager.get(player);
                            long now = level.getGameTime();
                            long next = data.getNextPeriodicInterventionTick();
                            long remainingTicks = Math.max(0L, next - now);
                            long remainingSeconds = remainingTicks / 20L;
                            long remainingMinutes = remainingSeconds / 60L;

                            int minTicks = data.getMinInterventionTicks();
                            int maxTicks = data.getMaxInterventionTicks();

                            player.sendSystemMessage(Component.literal("═══ Temporizador de Intervención Divina ═══")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                            player.sendSystemMessage(Component.literal(" • Intervalo de tu personaje: ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal((minTicks / 1200) + " a " + (maxTicks / 1200) + " min (" + minTicks + " a " + maxTicks + " ticks)")
                                            .withStyle(ChatFormatting.WHITE)));
                            player.sendSystemMessage(Component.literal(" • Próxima intervención en: ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(remainingMinutes + "m " + (remainingSeconds % 60) + "s (" + remainingTicks + " ticks)")
                                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

                            java.util.List<String> eligible = new java.util.ArrayList<>();
                            for (SinGod god : SinGod.values()) {
                                int fav = data.getFavor(god);
                                if (fav >= 30 || fav <= -30) {
                                    eligible.add(god.getDisplayName() + " (" + fav + ")");
                                }
                            }
                            if (eligible.isEmpty()) {
                                player.sendSystemMessage(Component.literal(" • Dioses en el bombo: Ninguno (requiere favor >= 30 o <= -30).")
                                        .withStyle(ChatFormatting.GRAY));
                            } else {
                                player.sendSystemMessage(Component.literal(" • Dioses elegibles para actuar: ")
                                        .withStyle(ChatFormatting.GREEN)
                                        .append(Component.literal(String.join(", ", eligible)).withStyle(ChatFormatting.WHITE)));
                            }
                            return 1;
                        }))
                .then(Commands.literal("setinterval")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("min_ticks", IntegerArgumentType.integer(100, 1000000))
                                .then(Commands.argument("max_ticks", IntegerArgumentType.integer(100, 1000000))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int min = IntegerArgumentType.getInteger(ctx, "min_ticks");
                                            int max = IntegerArgumentType.getInteger(ctx, "max_ticks");
                                            if (min > max) {
                                                ctx.getSource().sendFailure(Component.literal("min_ticks no puede ser mayor que max_ticks."));
                                                return 0;
                                            }
                                            PlayerSinData data = SinFavorManager.get(player);
                                            data.setMinInterventionTicks(min);
                                            data.setMaxInterventionTicks(max);
                                            DivineInterventionScheduler.reschedulePlayerIntervention(data, player.level().getGameTime(), player.getRandom());
                                            ctx.getSource().sendSuccess(() -> Component.literal("Intervalo de tu personaje reprogramado a " + min + " - " + max + " ticks."), true);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("min_ticks", IntegerArgumentType.integer(100, 1000000))
                                        .then(Commands.argument("max_ticks", IntegerArgumentType.integer(100, 1000000))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    int min = IntegerArgumentType.getInteger(ctx, "min_ticks");
                                                    int max = IntegerArgumentType.getInteger(ctx, "max_ticks");
                                                    if (min > max) {
                                                        ctx.getSource().sendFailure(Component.literal("min_ticks no puede ser mayor que max_ticks."));
                                                        return 0;
                                                    }
                                                    PlayerSinData data = SinFavorManager.get(target);
                                                    data.setMinInterventionTicks(min);
                                                    data.setMaxInterventionTicks(max);
                                                    DivineInterventionScheduler.reschedulePlayerIntervention(data, target.level().getGameTime(), target.getRandom());
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Intervalo del personaje de " + target.getScoreboardName() + " reprogramado a " + min + " - " + max + " ticks."), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("setfavor")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("god", StringArgumentType.word())
                                .then(Commands.argument("value", IntegerArgumentType.integer(-100, 100))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String godName = StringArgumentType.getString(ctx, "god");
                                            int val = IntegerArgumentType.getInteger(ctx, "value");
                                            SinGod god = SinGod.byId(godName).orElse(null);
                                            if (god == null) {
                                                ctx.getSource().sendFailure(Component.literal("Dios desconocido: " + godName));
                                                return 0;
                                            }
                                            SinFavorManager.setFavorExact(player, god, val);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Favor de " + god.getDisplayName() + " ajustado a " + val), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("manifest")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("god", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.level();
                                    String godName = StringArgumentType.getString(ctx, "god");
                                    SinGod god = SinGod.byId(godName).orElse(SinGod.PRIDE);
                                    int fav = SinFavorManager.getFavor(player, god);
                                    if (fav <= -100) {
                                        DivineInterventionScheduler.triggerWrathAvatarHunt(player, god);
                                    } else {
                                        DivineInterventionScheduler.tryTriggerBenevolentAvatar(level, player, god);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("lorebook")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("god", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String godName = StringArgumentType.getString(ctx, "god");
                                    SinGod god = SinGod.byId(godName).orElse(SinGod.GREED);
                                    ItemStack book = SinLoreLibrary.createRandomLoreBookForGod(god, player.getRandom());
                                    if (!player.getInventory().add(book)) {
                                        player.drop(book, false, net.minecraft.util.Prediction.SERVER_ONLY);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("Entregado libro sagrado de " + god.getDisplayName()), false);
                                    return 1;
                                })))
                .then(Commands.literal("action")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("god", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerLevel level = player.level();
                                    String godName = StringArgumentType.getString(ctx, "god");
                                    SinGod god = SinGod.byId(godName).orElse(null);
                                    if (god == null) {
                                        ctx.getSource().sendFailure(Component.literal("Dios desconocido: " + godName));
                                        return 0;
                                    }
                                    PlayerSinData data = SinFavorManager.get(player);
                                    int fav = data.getFavor(god);
                                    if (fav > -30 && fav < 30) {
                                        ctx.getSource().sendFailure(Component.literal("El favor con " + god.getDisplayName() + " es " + fav + " (debe ser >= 30 o <= -30 para intervenir)."));
                                        return 0;
                                    }
                                    DivineInterventionScheduler.executePeriodicDivineAction(level, player, data, god, level.getRandom());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Acción divina ejecutada para " + god.getDisplayName() + " (Favor: " + fav + ")."), true);
                                    return 1;
                                })))
                .then(Commands.literal("trigger_periodic")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel level = player.level();
                            PlayerSinData data = SinFavorManager.get(player);
                            boolean triggered = DivineInterventionScheduler.forcePeriodicIntervention(level, player, data);
                            if (triggered) {
                                ctx.getSource().sendSuccess(() -> Component.literal("Intervención periódica ejecutada entre los dioses elegibles (>= 30 o <= -30)."), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("No hay ningún dios con favor >= 30 o <= -30 hacia ti."));
                            }
                            return triggered ? 1 : 0;
                        }))
                .then(Commands.literal("village")
                        .then(Commands.literal("info")
                                .executes(ctx -> executeVillageInfo(ctx.getSource())))
                        .then(Commands.literal("consecrate")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("god", StringArgumentType.word())
                                        .executes(ctx -> executeForceConsecrate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "god")
                                        ))))
                        .then(Commands.literal("raid")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> executeForceRaid(ctx.getSource(), null, false))
                                .then(Commands.argument("attacker", StringArgumentType.word())
                                        .executes(ctx -> executeForceRaid(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "attacker"),
                                                false
                                        ))
                                        .then(Commands.argument("force_avatar", BoolArgumentType.bool())
                                                .executes(ctx -> executeForceRaid(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "attacker"),
                                                        BoolArgumentType.getBool(ctx, "force_avatar")
                                                )))))
                        .then(Commands.literal("desolate")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> executeForceDesolate(ctx.getSource()))))
        );
    }

    private static int executeVillageInfo(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> opt = data.getVillageAt(player.blockPosition());

        if (opt.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("No te encuentras dentro del radio de ninguna aldea consagrada (Aldeas registradas en esta dimensión: " + data.getAllVillages().size() + ").")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return 0;
        }

        GodVillageSavedData.GodVillageRecord v = opt.get();
        long now = level.getGameTime();
        long sinceRaid = Math.max(0L, now - v.lastRaidTick());
        long cooldownRem = Math.max(0L, GodVillageRaidManager.RAID_COOLDOWN_TICKS - sinceRaid);

        player.sendSystemMessage(Component.literal("═══ Información de Aldea Consagrada ═══")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(" • Dios Patrón: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(v.patronGod().getDisplayName() + " (" + v.patronGod().getSinName() + ")")
                        .withStyle(v.patronGod().getChatColor(), ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal(" • Estado Territorial: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(v.state().name() + " — " + v.state().getDisplayName())
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal(String.format(" • Altar Mayor: [%d, %d, %d] (Radio: %d bloques)",
                v.altarPos().getX(), v.altarPos().getY(), v.altarPos().getZ(), v.radius()))
                .withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal(" • Cooldown de Asedio Autónomo: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal((cooldownRem / 20L) + "s (" + cooldownRem + " ticks)")
                        .withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int executeForceConsecrate(CommandSourceStack source, String godName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        SinGod god = SinGod.byId(godName).orElse(null);
        if (god == null) {
            source.sendFailure(Component.literal("Dios desconocido: " + godName));
            return 0;
        }

        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> existingOpt = data.getVillageAt(player.blockPosition());
        BlockPos altarPos = existingOpt.isPresent() ? existingOpt.get().altarPos() : player.blockPosition();

        level.setBlockAndUpdate(altarPos, ModBlocks.getAltarFor(god).defaultBlockState());
        if (level.getBlockEntity(altarPos) instanceof SinAltarBlockEntity altarBe) {
            altarBe.setVillageShrine(true);
        }

        GodVillageState targetState = (god == SinGod.ENVY) ? GodVillageState.ARRASADA : GodVillageState.CONSAGRADA;
        GodVillageSavedData.GodVillageRecord updated = data.consecrateOrUpdateVillage(altarPos, god, targetState, level.getGameTime());
        VillageCultureApplier.initializeVillageCulture(level, data, updated);

        source.sendSuccess(() -> Component.literal(String.format(
                "Aldea consagrada a %s (%s) en [%d, %d, %d] con estado %s.",
                god.getDisplayName(), god.getSinName(), altarPos.getX(), altarPos.getY(), altarPos.getZ(), targetState.name()
        )), true);
        return 1;
    }

    private static int executeForceRaid(CommandSourceStack source, String attackerName, boolean forceAvatar) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> opt = data.getVillageAt(player.blockPosition());

        if (opt.isEmpty()) {
            source.sendFailure(Component.literal("No estás dentro de ninguna aldea consagrada. Usa '/gods village consecrate <dios>' primero."));
            return 0;
        }

        GodVillageSavedData.GodVillageRecord village = opt.get();
        SinGod attacker;
        if (attackerName == null || attackerName.isBlank()) {
            attacker = GodVillageRaidManager.pickRivalGod(village.patronGod(), level.getRandom());
        } else {
            attacker = SinGod.byId(attackerName).orElse(null);
            if (attacker == null) {
                source.sendFailure(Component.literal("Dios atacante desconocido: " + attackerName));
                return 0;
            }
        }

        if (attacker == village.patronGod()) {
            source.sendFailure(Component.literal("El dios atacante no puede ser el mismo que el dios patrón de la aldea."));
            return 0;
        }

        GodVillageRaidManager.startRaidOnVillage(level.getServer(), level, data, village, attacker, player);
        if (forceAvatar) {
            GodVillageRaidManager.manifestSingleRaidAvatar(level.getServer(), level, village.altarPos(), attacker);
        }

        source.sendSuccess(() -> Component.literal("Asedio iniciado contra el Templo de " + village.patronGod().getDisplayName() + " por las fuerzas de " + attacker.getDisplayName() + "."), true);
        return 1;
    }

    private static int executeForceDesolate(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> opt = data.getVillageAt(player.blockPosition());

        if (opt.isEmpty()) {
            source.sendFailure(Component.literal("No estás dentro de ninguna aldea consagrada."));
            return 0;
        }

        GodVillageSavedData.GodVillageRecord village = opt.get();
        GodVillageRaidManager.transitionToDesolated(level.getServer(), level, data, village, level.getGameTime());
        source.sendSuccess(() -> Component.literal("La aldea de " + village.patronGod().getDisplayName() + " ha sido puesta en estado DESOLADA."), true);
        return 1;
    }
}
