package com.volcov.sevendeadlygods;

import com.mojang.logging.LogUtils;
import com.volcov.sevendeadlygods.chat.DivineChatRouter;
import com.volcov.sevendeadlygods.command.GodsCommands;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.favor.PlayerSinBehaviorEvents;
import com.volcov.sevendeadlygods.registry.ModAttachments;
import com.volcov.sevendeadlygods.registry.ModBlockEntities;
import com.volcov.sevendeadlygods.registry.ModBlocks;
import com.volcov.sevendeadlygods.registry.ModEntities;
import com.volcov.sevendeadlygods.registry.ModItems;
import com.volcov.sevendeadlygods.registry.ModLootModifiers;
import com.volcov.sevendeadlygods.registry.ModMobEffects;
import com.volcov.sevendeadlygods.village.GodVillageRaidManager;
import com.volcov.sevendeadlygods.worldgen.VillageStructureInjector;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(SevenDeadlyGodsMod.MOD_ID)
public class SevenDeadlyGodsMod {
    public static final String MOD_ID = "sevendeadlygods";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SevenDeadlyGodsMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.LOCAL, ModConfig.SPEC);

        // Registros de NeoForge
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(ModEntities::onRegisterAttributes);

        // Eventos del juego en el bus de NeoForge
        NeoForge.EVENT_BUS.register(new PlayerSinBehaviorEvents());
        NeoForge.EVENT_BUS.register(new DivineChatRouter());
        NeoForge.EVENT_BUS.register(new GodsCommands());
        NeoForge.EVENT_BUS.register(new VillageStructureInjector());
        NeoForge.EVENT_BUS.register(new GodVillageRaidManager());

        LOGGER.info("Seven Deadly Gods inicializado para NeoForge 26.3 — Los 7 Pecados Capitales observan el mundo.");
    }
}
