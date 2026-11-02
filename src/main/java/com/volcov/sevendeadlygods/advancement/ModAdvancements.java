package com.volcov.sevendeadlygods.advancement;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utilitario centralizado para conceder avances/logros del mod de forma segura y consistente.
 */
public final class ModAdvancements {
    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String advancementPath) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        Identifier advId = Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, advancementPath);
        AdvancementHolder adv = player.level().getServer().getAdvancements().get(advId);
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criterion);
                }
            }
        }
    }
}
