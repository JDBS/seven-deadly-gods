package com.volcov.sevendeadlygods.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.lore.SinLoreLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
        );
    }
}
