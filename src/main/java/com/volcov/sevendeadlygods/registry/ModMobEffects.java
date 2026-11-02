package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.effect.SinRitualBlessingEffect;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> GREED_BLESSING =
            MOB_EFFECTS.register("greed_blessing", () -> new SinRitualBlessingEffect(SinGod.GREED));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> WRATH_BLESSING =
            MOB_EFFECTS.register("wrath_blessing", () -> new SinRitualBlessingEffect(SinGod.WRATH));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> GLUTTONY_BLESSING =
            MOB_EFFECTS.register("gluttony_blessing", () -> new SinRitualBlessingEffect(SinGod.GLUTTONY));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> PRIDE_BLESSING =
            MOB_EFFECTS.register("pride_blessing", () -> new SinRitualBlessingEffect(SinGod.PRIDE));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> ENVY_BLESSING =
            MOB_EFFECTS.register("envy_blessing", () -> new SinRitualBlessingEffect(SinGod.ENVY));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> SLOTH_BLESSING =
            MOB_EFFECTS.register("sloth_blessing", () -> new SinRitualBlessingEffect(SinGod.SLOTH));

    public static final DeferredHolder<MobEffect, SinRitualBlessingEffect> LUST_BLESSING =
            MOB_EFFECTS.register("lust_blessing", () -> new SinRitualBlessingEffect(SinGod.LUST));

    private ModMobEffects() {
    }

    public static Holder<MobEffect> getBlessingFor(SinGod god) {
        return switch (god) {
            case GREED -> GREED_BLESSING;
            case WRATH -> WRATH_BLESSING;
            case GLUTTONY -> GLUTTONY_BLESSING;
            case PRIDE -> PRIDE_BLESSING;
            case ENVY -> ENVY_BLESSING;
            case SLOTH -> SLOTH_BLESSING;
            case LUST -> LUST_BLESSING;
        };
    }
}
