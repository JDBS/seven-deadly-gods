package com.volcov.sevendeadlygods.effect;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Efecto positivo unico de larga duracion (20 a 40 minutos) otorgado por un dios del pecado
 * cada vez que un jugador con favor >= +61 completa un ritual en su altar.
 */
public class SinRitualBlessingEffect extends MobEffect {
    private final SinGod god;

    public SinRitualBlessingEffect(SinGod god) {
        super(MobEffectCategory.BENEFICIAL, god.getHexColor());
        this.god = god;
    }

    public SinGod getGod() {
        return this.god;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Ejecutar logica pasiva cada 40 ticks (2 segundos)
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) {
            return true;
        }

        switch (this.god) {
            case GREED -> {
                // Toque de Midas: Prisa minera constante + Suerte elevada
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 2, true, false, true));
            }
            case WRATH -> {
                // Frenesi de Khorvath: Fuerza y Velocidad de combate
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 2, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1, true, false, true));
            }
            case GLUTTONY -> {
                // Festin de Beelze: Saturacion continua y Absorcion regenerativa
                player.getFoodData().eat(1, 0.5F);
                if (player.getAbsorptionAmount() < 8.0F) {
                    player.setAbsorptionAmount(8.0F);
                }
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.POISON);
            }
            case PRIDE -> {
                // Majestad Imperial de Lucian: Resistencia II y debilita enemigos cercanos que se atrevan a acercarse
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1, true, false, true));
                AABB aura = player.getBoundingBox().inflate(6.0D);
                List<Monster> monsters = level.getEntitiesOfClass(Monster.class, aura);
                for (Monster monster : monsters) {
                    monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true));
                }
            }
            case ENVY -> {
                // Mirada Abisal de Leviatan: Vision nocturna + Invisibilidad al agacharse
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                if (player.isCrouching()) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, true));
                }
            }
            case SLOTH -> {
                // Aura de Letargo de Belphegor: Regeneracion al caminar lento o estar quieto + Lentitud III a enemigos cercanos
                if (!player.isSprinting()) {
                    player.heal(1.0F);
                }
                AABB slothAura = player.getBoundingBox().inflate(8.0D);
                List<Mob> hostiles = level.getEntitiesOfClass(Mob.class, slothAura, m -> m.getTarget() == player);
                for (Mob mob : hostiles) {
                    mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2, true, true));
                }
            }
            case LUST -> {
                // Encanto de Asmodeus: Regeneracion II + Hero of the Village
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 100, 1, true, false, true));
            }
        }
        return true;
    }
}
