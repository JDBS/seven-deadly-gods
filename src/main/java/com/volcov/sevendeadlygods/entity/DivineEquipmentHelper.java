package com.volcov.sevendeadlygods.entity;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.Optional;

/**
 * Utilidad para equipar a los Enviados, Acolitos y Avatares (incluyendo encantamientos mas alla del limite
 * de la mesa de encantamientos para Avatares y Reliquias de Campeon +91..+100).
 */
public final class DivineEquipmentHelper {
    private DivineEquipmentHelper() {
    }

    public static void equipEnvoySkeleton(ServerLevel level, Mob skeleton, SinGod god) {
        ItemStack weapon = switch (god) {
            case GREED, PRIDE -> new ItemStack(Items.GOLDEN_SWORD);
            case WRATH -> new ItemStack(Items.IRON_AXE);
            case GLUTTONY, SLOTH, ENVY, LUST -> new ItemStack(Items.IRON_SWORD);
        };
        applyEnchant(level, weapon, Enchantments.SHARPNESS, 3);
        applyEnchant(level, weapon, Enchantments.UNBREAKING, 3);

        ItemStack helmet = new ItemStack(god == SinGod.GREED || god == SinGod.PRIDE ? Items.GOLDEN_HELMET : Items.IRON_HELMET);
        ItemStack chest = new ItemStack(god == SinGod.GREED || god == SinGod.PRIDE ? Items.GOLDEN_CHESTPLATE : Items.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);

        applyEnchant(level, helmet, Enchantments.PROTECTION, 2);
        applyEnchant(level, chest, Enchantments.PROTECTION, 2);

        skeleton.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
        skeleton.setItemSlot(EquipmentSlot.CHEST, chest);
        skeleton.setItemSlot(EquipmentSlot.LEGS, legs);
        skeleton.setItemSlot(EquipmentSlot.FEET, boots);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            skeleton.setDropChance(slot, 0.05F);
        }
    }

    public static void equipAcolyte(ServerLevel level, GodAcolyteEntity acolyte, SinGod god) {
        ItemStack weapon = switch (god) {
            case GREED -> new ItemStack(Items.GOLDEN_SWORD);
            case WRATH -> new ItemStack(Items.DIAMOND_AXE);
            case GLUTTONY -> new ItemStack(Items.IRON_AXE);
            case PRIDE -> new ItemStack(Items.DIAMOND_SWORD);
            case ENVY -> new ItemStack(Items.TRIDENT);
            case SLOTH -> new ItemStack(Items.MACE);
            case LUST -> new ItemStack(Items.GOLDEN_SWORD);
        };
        applyEnchant(level, weapon, Enchantments.SHARPNESS, 4);
        applyEnchant(level, weapon, Enchantments.UNBREAKING, 5);
        acolyte.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        acolyte.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    /**
     * Equipa al Avatar de un Dios con armas y armaduras con encantamientos sobre-nivel
     * (por encima del limite normal de una mesa de encantamientos: ej. Filo VIII, Proteccion VII, Espinas V).
     */
    public static void equipAvatarWithOverleveledGear(ServerLevel level, GodAvatarEntity avatar, SinGod god) {
        ItemStack weapon = createOverleveledRelicWeapon(level, god);

        boolean goldenTheme = (god == SinGod.GREED || god == SinGod.PRIDE || god == SinGod.LUST);
        ItemStack helmet = new ItemStack(goldenTheme ? Items.GOLDEN_HELMET : Items.NETHERITE_HELMET);
        ItemStack chest = new ItemStack(goldenTheme ? Items.GOLDEN_CHESTPLATE : Items.NETHERITE_CHESTPLATE);
        ItemStack legs = new ItemStack(goldenTheme ? Items.GOLDEN_LEGGINGS : Items.NETHERITE_LEGGINGS);
        ItemStack boots = new ItemStack(goldenTheme ? Items.GOLDEN_BOOTS : Items.NETHERITE_BOOTS);

        for (ItemStack armor : List.of(helmet, chest, legs, boots)) {
            applyEnchant(level, armor, Enchantments.PROTECTION, 5);
            applyEnchant(level, armor, Enchantments.UNBREAKING, 10);
            applyEnchant(level, armor, Enchantments.THORNS, 5);
        }

        avatar.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        avatar.setItemSlot(EquipmentSlot.HEAD, helmet);
        avatar.setItemSlot(EquipmentSlot.CHEST, chest);
        avatar.setItemSlot(EquipmentSlot.LEGS, legs);
        avatar.setItemSlot(EquipmentSlot.FEET, boots);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            avatar.setDropChance(slot, 0.0F);
        }
    }

    /**
     * Crea una Reliquia Divina sobre-encantada (Filo VIII, Botin VI, Aspecto Igneo IV, Irrompibilidad X)
     * para el Avatar o como regalo a un Campeon Divino (+91 a +100 de favor).
     */
    public static ItemStack createOverleveledRelicWeapon(ServerLevel level, SinGod god) {
        Item baseItem = switch (god) {
            case GREED -> Items.GOLDEN_SWORD;
            case WRATH -> Items.NETHERITE_AXE;
            case GLUTTONY -> Items.NETHERITE_SWORD;
            case PRIDE -> Items.GOLDEN_SWORD;
            case ENVY -> Items.TRIDENT;
            case SLOTH -> Items.MACE;
            case LUST -> Items.DIAMOND_SWORD;
        };

        ItemStack relic = new ItemStack(baseItem);
        relic.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ ")
                        .append(Component.translatable("relic.sevendeadlygods." + god.getId() + ".name"))
                        .append(" ✦")
                        .withStyle(god.getChatColor(), ChatFormatting.BOLD));
        relic.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("relic.sevendeadlygods.generic_lore_1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("relic.sevendeadlygods.generic_lore_2",
                        god.getDisplayNameComponent(),
                        god.getTitleComponent()
                ).withStyle(god.getChatColor())
        )));

        applyEnchant(level, relic, Enchantments.SHARPNESS, 8);
        applyEnchant(level, relic, Enchantments.LOOTING, 6);
        applyEnchant(level, relic, Enchantments.FIRE_ASPECT, 4);
        applyEnchant(level, relic, Enchantments.UNBREAKING, 10);
        applyEnchant(level, relic, Enchantments.MENDING, 1);

        return relic;
    }

    /**
     * Crea un Trofeo de Deicidio conmemorativo cuando el Avatar de un Dios es abatido en combate mortal.
     */
    public static ItemStack createDeicideTrophy(ServerLevel level, SinGod god, ServerPlayer killer) {
        ItemStack trophy = new ItemStack(Items.NETHER_STAR);
        trophy.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ ")
                        .append(Component.translatable("trophy.sevendeadlygods.deicide.name", god.getDisplayNameComponent()))
                        .append(" ✦")
                        .withStyle(god.getChatColor(), ChatFormatting.BOLD));
        trophy.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("trophy.sevendeadlygods.deicide.lore_1", god.getTitleComponent()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("trophy.sevendeadlygods.deicide.lore_2", killer.getScoreboardName()).withStyle(ChatFormatting.GOLD),
                Component.translatable("trophy.sevendeadlygods.deicide.lore_3").withStyle(ChatFormatting.DARK_PURPLE),
                Component.translatable("trophy.sevendeadlygods.deicide.lore_4").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)
        )));
        applyEnchant(level, trophy, Enchantments.UNBREAKING, 1);
        return trophy;
    }

    private static void applyEnchant(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key, int enchantLevel) {
        Optional<Registry<Enchantment>> regOpt = level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (regOpt.isPresent()) {
            Optional<Holder.Reference<Enchantment>> holderOpt = regOpt.get().get(key);
            holderOpt.ifPresent(holder -> stack.enchant(holder, enchantLevel));
        }
    }
}
