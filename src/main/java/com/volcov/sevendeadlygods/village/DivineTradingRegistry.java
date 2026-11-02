package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo completo de Comercio Divino (5 niveles de devoción) para los Acólitos Mercaderes
 * de las Aldeas Consagradas a cada uno de los 7 Dioses del Pecado.
 */
public final class DivineTradingRegistry {
    private DivineTradingRegistry() {
    }

    public static void openTrading(GodAcolyteEntity acolyte, ServerPlayer player) {
        if (!(acolyte.level() instanceof ServerLevel level)) {
            return;
        }
        SinGod god = acolyte.getGod();
        int favor = SinFavorManager.getFavor(player, god);
        MerchantOffers offers = buildOffersFor(level, god, favor);

        acolyte.overrideOffers(offers);
        acolyte.setTradingPlayer(player);

        int traderLevel = favor >= 60 ? 5 : favor >= 35 ? 4 : favor >= 15 ? 3 : favor >= 5 ? 2 : 1;
        if (favor >= 60) {
            ModAdvancements.award(player, "divine_trade_master");
        }
        Component title = Component.literal(god.getDisplayName() + " — Mercader del Templo")
                .withStyle(god.getChatColor(), ChatFormatting.BOLD);
        acolyte.openTradingScreen(player, title, traderLevel);
    }

    public static MerchantOffers buildOffersFor(ServerLevel level, SinGod god, int favor) {
        MerchantOffers offers = new MerchantOffers();

        switch (god) {
            case GREED -> buildGreedOffers(level, offers, favor);
            case WRATH -> buildWrathOffers(level, offers, favor);
            case GLUTTONY -> buildGluttonyOffers(level, offers, favor);
            case PRIDE -> buildPrideOffers(level, offers, favor);
            case ENVY -> buildEnvyOffers(level, offers, favor);
            case SLOTH -> buildSlothOffers(level, offers, favor);
            case LUST -> buildLustOffers(level, offers, favor);
        }

        if (favor >= 35) {
            ItemStack warHorn = new ItemStack(ModItems.DIVINE_WAR_HORN.get());
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 32),
                    Optional.of(new ItemCost(Items.GOLD_INGOT, 4)),
                    warHorn,
                    2, 25, 0.05F
            ));
        }

        return offers;
    }

    private static void buildGreedOffers(ServerLevel level, MerchantOffers offers, int favor) {
        // Nivel 1 & 2 (Disponibles desde Favor >= 0)
        offers.add(new MerchantOffer(
                new ItemCost(Items.GOLD_INGOT, 8),
                new ItemStack(Items.EMERALD, 5),
                16, 5, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 32),
                new ItemStack(Items.DIAMOND, 4),
                8, 10, 0.05F
        ));
        // Nivel 3 (Favor >= 15)
        if (favor >= 15) {
            ItemStack coinPouch = createNamedArtifact(
                    Items.RAW_GOLD_BLOCK,
                    SinGod.GREED,
                    "trading.sevendeadlygods.mammon_coin_pouch.name",
                    "trading.sevendeadlygods.mammon_coin_pouch.lore"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 20),
                    Optional.of(new ItemCost(Items.GOLD_INGOT, 4)),
                    coinPouch,
                    6, 15, 0.05F
            ));
        }
        // Nivel 4 (Favor >= 35)
        if (favor >= 35) {
            ItemStack vaultGem = createNamedArtifact(
                    Items.GOLD_BLOCK,
                    SinGod.GREED,
                    "trading.sevendeadlygods.mammon_vault_ingot.name",
                    "trading.sevendeadlygods.mammon_vault_ingot.lore"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 28),
                    vaultGem,
                    6, 20, 0.05F
            ));
        }
        // Nivel 5 (Favor >= 60): Libro Encantado Fortuna IV
        if (favor >= 60) {
            ItemStack fortuneBook = createEnchantedBook(
                    level,
                    SinGod.GREED,
                    Enchantments.FORTUNE,
                    4,
                    "trading.sevendeadlygods.book_fortune_4"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 48),
                    Optional.of(new ItemCost(Items.DIAMOND, 4)),
                    fortuneBook,
                    3, 30, 0.05F
            ));
        }
    }

    private static void buildWrathOffers(ServerLevel level, MerchantOffers offers, int favor) {
        ItemStack heavySword = createNamedArtifact(
                Items.IRON_SWORD,
                SinGod.WRATH,
                "trading.sevendeadlygods.khorvath_heavy_sword.name",
                    "trading.sevendeadlygods.khorvath_heavy_sword.lore"
        );
        applyEnchant(level, heavySword, Enchantments.SHARPNESS, 3);
        applyEnchant(level, heavySword, Enchantments.UNBREAKING, 3);

        offers.add(new MerchantOffer(
                new ItemCost(Items.SKELETON_SKULL, 1),
                Optional.of(new ItemCost(Items.IRON_INGOT, 5)),
                heavySword,
                12, 5, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 15),
                createNamedArtifact(
                        Items.BLAZE_POWDER,
                        SinGod.WRATH,
                        "trading.sevendeadlygods.khorvath_fury_ash.name",
                    "trading.sevendeadlygods.khorvath_fury_ash.lore"
                ),
                8, 8, 0.05F
        ));
        if (favor >= 15) {
            ItemStack whetstone = createNamedArtifact(
                    Items.NETHERITE_SCRAP,
                    SinGod.WRATH,
                    "trading.sevendeadlygods.khorvath_whetstone.name",
                    "trading.sevendeadlygods.khorvath_whetstone.lore"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 24),
                    whetstone,
                    6, 15, 0.05F
            ));
        }
        if (favor >= 35) {
            ItemStack butcherAxe = createNamedArtifact(
                    Items.DIAMOND_AXE,
                    SinGod.WRATH,
                    "trading.sevendeadlygods.khorvath_butcher_axe.name",
                    "trading.sevendeadlygods.khorvath_butcher_axe.lore"
            );
            applyEnchant(level, butcherAxe, Enchantments.SHARPNESS, 5);
            applyEnchant(level, butcherAxe, Enchantments.FIRE_ASPECT, 2);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 36),
                    Optional.of(new ItemCost(Items.DIAMOND, 2)),
                    butcherAxe,
                    4, 20, 0.05F
            ));
        }
        if (favor >= 60) {
            ItemStack sharpnessBook = createEnchantedBook(
                    level,
                    SinGod.WRATH,
                    Enchantments.SHARPNESS,
                    6,
                    "trading.sevendeadlygods.book_sharpness_6"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 52),
                    Optional.of(new ItemCost(Items.NETHERITE_SCRAP, 2)),
                    sharpnessBook,
                    2, 30, 0.05F
            ));
        }
    }

    private static void buildGluttonyOffers(ServerLevel level, MerchantOffers offers, int favor) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 4),
                new ItemStack(Items.GOLDEN_CARROT, 12),
                16, 4, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 8),
                createNamedArtifact(
                        Items.CAKE,
                        SinGod.GLUTTONY,
                        "trading.sevendeadlygods.beelze_cake.name",
                    "trading.sevendeadlygods.beelze_cake.lore"
                ),
                8, 8, 0.05F
        ));
        if (favor >= 15) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 14),
                    createNamedArtifact(
                            Items.HONEY_BOTTLE,
                            SinGod.GLUTTONY,
                            "trading.sevendeadlygods.beelze_ambrosia.name",
                    "trading.sevendeadlygods.beelze_ambrosia.lore"
                    ),
                    10, 12, 0.05F
            ));
        }
        if (favor >= 35) {
            ItemStack cleaver = createNamedArtifact(
                    Items.IRON_SWORD,
                    SinGod.GLUTTONY,
                    "trading.sevendeadlygods.beelze_cleaver.name",
                    "trading.sevendeadlygods.beelze_cleaver.lore"
            );
            applyEnchant(level, cleaver, Enchantments.LOOTING, 5);
            applyEnchant(level, cleaver, Enchantments.FIRE_ASPECT, 2);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 28),
                    cleaver,
                    4, 20, 0.05F
            ));
        }
        if (favor >= 60) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 40),
                    Optional.of(new ItemCost(Items.GOLD_BLOCK, 2)),
                    createNamedArtifact(
                            Items.ENCHANTED_GOLDEN_APPLE,
                            SinGod.GLUTTONY,
                            "trading.sevendeadlygods.beelze_supreme_feast.name",
                    "trading.sevendeadlygods.beelze_supreme_feast.lore"
                    ),
                    2, 30, 0.05F
            ));
        }
    }

    private static void buildPrideOffers(ServerLevel level, MerchantOffers offers, int favor) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 10),
                new ItemStack(Items.QUARTZ_BLOCK, 16),
                16, 6, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.DIAMOND, 3),
                Optional.of(new ItemCost(Items.EMERALD, 16)),
                new ItemStack(Items.TOTEM_OF_UNDYING, 1),
                4, 12, 0.05F
        ));
        if (favor >= 15) {
            ItemStack crownHelmet = createNamedArtifact(
                    Items.GOLDEN_HELMET,
                    SinGod.PRIDE,
                    "trading.sevendeadlygods.lucian_solar_crown.name",
                    "trading.sevendeadlygods.lucian_solar_crown.lore"
            );
            applyEnchant(level, crownHelmet, Enchantments.PROTECTION, 5);
            applyEnchant(level, crownHelmet, Enchantments.UNBREAKING, 5);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 24),
                    Optional.of(new ItemCost(Items.GOLD_INGOT, 8)),
                    crownHelmet,
                    4, 16, 0.05F
            ));
        }
        if (favor >= 35) {
            ItemStack mirror = createNamedArtifact(
                    Items.SPYGLASS,
                    SinGod.PRIDE,
                    "trading.sevendeadlygods.lucian_self_love_mirror.name",
                    "trading.sevendeadlygods.lucian_self_love_mirror.lore"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 30),
                    mirror,
                    4, 20, 0.05F
            ));
        }
        if (favor >= 60) {
            ItemStack prideTotem = createNamedArtifact(
                    Items.TOTEM_OF_UNDYING,
                    SinGod.PRIDE,
                    "trading.sevendeadlygods.lucian_unbroken_totem.name",
                    "trading.sevendeadlygods.lucian_unbroken_totem.lore"
            );
            applyEnchant(level, prideTotem, Enchantments.UNBREAKING, 3);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 45),
                    Optional.of(new ItemCost(Items.DIAMOND, 5)),
                    prideTotem,
                    2, 30, 0.05F
            ));
        }
    }

    private static void buildEnvyOffers(ServerLevel level, MerchantOffers offers, int favor) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.ROTTEN_FLESH, 8),
                new ItemStack(Items.EMERALD, 2),
                16, 4, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 12),
                new ItemStack(Items.ENDER_PEARL, 4),
                8, 8, 0.05F
        ));
        if (favor >= 15) {
            ItemStack lootBag = createNamedArtifact(
                    Items.EXPERIENCE_BOTTLE,
                    SinGod.ENVY,
                    "trading.sevendeadlygods.leviathan_black_market.name",
                    "trading.sevendeadlygods.leviathan_black_market.lore"
            );
            lootBag.setCount(8);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 16),
                    lootBag,
                    8, 14, 0.05F
            ));
        }
        if (favor >= 35) {
            ItemStack talisman = createNamedArtifact(
                    Items.HEART_OF_THE_SEA,
                    SinGod.ENVY,
                    "trading.sevendeadlygods.leviathan_talisman.name",
                    "trading.sevendeadlygods.leviathan_talisman.lore"
            );
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 32),
                    talisman,
                    3, 20, 0.05F
            ));
        }
        if (favor >= 60) {
            ItemStack dagger = createNamedArtifact(
                    Items.DIAMOND_SWORD,
                    SinGod.ENVY,
                    "trading.sevendeadlygods.leviathan_dagger.name",
                    "trading.sevendeadlygods.leviathan_dagger.lore"
            );
            applyEnchant(level, dagger, Enchantments.SHARPNESS, 6);
            applyEnchant(level, dagger, Enchantments.LOOTING, 4);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 46),
                    Optional.of(new ItemCost(Items.ENDER_PEARL, 8)),
                    dagger,
                    2, 30, 0.05F
            ));
        }
    }

    private static void buildSlothOffers(ServerLevel level, MerchantOffers offers, int favor) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.WOOL.white(), 5),
                new ItemStack(Items.EMERALD, 2),
                16, 3, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 6),
                createNamedArtifact(
                        Items.BED.blue(),
                        SinGod.SLOTH,
                        "trading.sevendeadlygods.belphegor_dream_bed.name",
                    "trading.sevendeadlygods.belphegor_dream_bed.lore"
                ),
                8, 5, 0.05F
        ));
        if (favor >= 15) {
            ItemStack boots = createNamedArtifact(
                    Items.IRON_BOOTS,
                    SinGod.SLOTH,
                    "trading.sevendeadlygods.belphegor_inertia_boots.name",
                    "trading.sevendeadlygods.belphegor_inertia_boots.lore"
            );
            applyEnchant(level, boots, Enchantments.FEATHER_FALLING, 5);
            applyEnchant(level, boots, Enchantments.UNBREAKING, 5);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 18),
                    boots,
                    6, 14, 0.05F
            ));
        }
        if (favor >= 35) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 24),
                    createNamedArtifact(
                            Items.HOPPER,
                            SinGod.SLOTH,
                            "trading.sevendeadlygods.belphegor_magnetic_hopper.name",
                    "trading.sevendeadlygods.belphegor_magnetic_hopper.lore"
                    ),
                    8, 20, 0.05F
            ));
        }
        if (favor >= 60) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 40),
                    Optional.of(new ItemCost(Items.IRON_BLOCK, 4)),
                    createNamedArtifact(
                            Items.NETHER_STAR,
                            SinGod.SLOTH,
                            "trading.sevendeadlygods.belphegor_lazy_core.name",
                    "trading.sevendeadlygods.belphegor_lazy_core.lore"
                    ),
                    2, 30, 0.05F
            ));
        }
    }

    private static void buildLustOffers(ServerLevel level, MerchantOffers offers, int favor) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.POPPY, 4),
                new ItemStack(Items.EMERALD, 2),
                16, 3, 0.05F
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 10),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 8),
                12, 6, 0.05F
        ));
        if (favor >= 15) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 14),
                    createNamedArtifact(
                            Items.HONEYCOMB,
                            SinGod.LUST,
                            "trading.sevendeadlygods.asmodeus_pheromone.name",
                    "trading.sevendeadlygods.asmodeus_pheromone.lore"
                    ),
                    10, 12, 0.05F
            ));
        }
        if (favor >= 35) {
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 22),
                    createNamedArtifact(
                            Items.LEAD,
                            SinGod.LUST,
                            "trading.sevendeadlygods.asmodeus_affinity_cord.name",
                    "trading.sevendeadlygods.asmodeus_affinity_cord.lore"
                    ),
                    6, 18, 0.05F
            ));
        }
        if (favor >= 60) {
            ItemStack ring = createNamedArtifact(
                    Items.GOLDEN_APPLE,
                    SinGod.LUST,
                    "trading.sevendeadlygods.asmodeus_passion_fruit.name",
                    "trading.sevendeadlygods.asmodeus_passion_fruit.lore"
            );
            applyEnchant(level, ring, Enchantments.UNBREAKING, 3);
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 38),
                    Optional.of(new ItemCost(Items.AMETHYST_SHARD, 8)),
                    ring,
                    4, 30, 0.05F
            ));
        }
    }

    private static ItemStack createNamedArtifact(Item item, SinGod god, String nameKey, String loreKey) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ ")
                        .append(Component.translatable(nameKey))
                        .append(" ✦")
                        .withStyle(god.getChatColor(), ChatFormatting.BOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable(loreKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("trading.sevendeadlygods.sacred_trade_by", god.getDisplayNameComponent()).withStyle(god.getChatColor())
        )));
        return stack;
    }

    private static ItemStack createEnchantedBook(
            ServerLevel level,
            SinGod god,
            ResourceKey<Enchantment> enchantmentKey,
            int enchantLevel,
            String customTitleKey
    ) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ ")
                        .append(Component.translatable(customTitleKey))
                        .append(" ✦")
                        .withStyle(god.getChatColor(), ChatFormatting.BOLD));
        book.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("trading.sevendeadlygods.forbidden_tome_lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("trading.sevendeadlygods.consecrated_by", god.getDisplayNameComponent()).withStyle(god.getChatColor())
        )));
        applyEnchant(level, book, enchantmentKey, enchantLevel);
        return book;
    }

    private static void applyEnchant(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key, int enchantLevel) {
        Optional<Registry<Enchantment>> regOpt = level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (regOpt.isPresent()) {
            Optional<Holder.Reference<Enchantment>> holderOpt = regOpt.get().get(key);
            holderOpt.ifPresent(holder -> stack.enchant(holder, enchantLevel));
        }
    }
}
