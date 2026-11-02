package com.volcov.sevendeadlygods.favor;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Datos persistentes individuales de cada jugador (compatible con servidores multijugador).
 * Guarda el marcador de Favor/Ira (-100 a +100) con cada uno de los 7 Pecados Capitales,
 * el progreso fraccional amortiguado y los temporizadores individuales de castigos y bendiciones.
 */
public class PlayerSinData implements ValueIOSerializable {
    public static final int MIN_FAVOR = -100;
    public static final int MAX_FAVOR = 100;

    private final EnumMap<SinGod, Integer> favorMap = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Float> subFavorEffort = new EnumMap<>(SinGod.class);

    // Temporizadores por dios (en gameTime ticks)
    private final EnumMap<SinGod, Long> nextMinorWrathTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextEnvoyTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextHostileAcolyteTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextMinorBlessingTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextAlliedAcolyteTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextAvatarManifestTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> nextAvatarInvocationTick = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Boolean> claimedRelicMap = new EnumMap<>(SinGod.class);
    private final EnumMap<SinGod, Long> godSlumberUntilTick = new EnumMap<>(SinGod.class);

    // Libros sagrados unicos leidos para progresion de logros
    private final Set<String> readLoreBooks = new HashSet<>();

    // Contadores especificos de mecanicas de los pecados
    private long lastGreedTributeTick = 0L;
    private long lastWrathKillTick = 0L;
    private long lastChatEvalTick = 0L;
    private int slothStillTicks = 0;
    private int slothLaborBlocks = 0;
    private long nextPeriodicInterventionTick = 0L;
    private int minInterventionTicks = 12000;
    private int maxInterventionTicks = 48000;

    public PlayerSinData() {
        for (SinGod god : SinGod.values()) {
            favorMap.put(god, 0);
            subFavorEffort.put(god, 0.0F);
            nextMinorWrathTick.put(god, 0L);
            nextEnvoyTick.put(god, 0L);
            nextHostileAcolyteTick.put(god, 0L);
            nextMinorBlessingTick.put(god, 0L);
            nextAlliedAcolyteTick.put(god, 0L);
            nextAvatarManifestTick.put(god, 0L);
            nextAvatarInvocationTick.put(god, 0L);
            claimedRelicMap.put(god, false);
            godSlumberUntilTick.put(god, 0L);
        }
    }

    public int getFavor(SinGod god) {
        return favorMap.getOrDefault(god, 0);
    }

    public void setFavorDirect(SinGod god, int value) {
        favorMap.put(god, Mth.clamp(value, MIN_FAVOR, MAX_FAVOR));
        subFavorEffort.put(god, 0.0F);
    }

    /**
     * Ajusta el favor entero (p. ej. tras superar una tirada probabilistica en el chat)
     * conservando el esfuerzo fraccional acumulado de acciones y altares, salvo que se alcance
     * el limite absoluto (+100 o -100) en esa misma direccion.
     */
    public int adjustFavorPreservingEffort(SinGod god, int delta) {
        if (delta == 0) {
            return 0;
        }
        int oldFavor = getFavor(god);
        int newFavor = Mth.clamp(oldFavor + delta, MIN_FAVOR, MAX_FAVOR);
        favorMap.put(god, newFavor);

        float accumulated = getSubFavorEffort(god);
        if (newFavor >= MAX_FAVOR && accumulated > 0.0F) {
            subFavorEffort.put(god, 0.0F);
        } else if (newFavor <= MIN_FAVOR && accumulated < 0.0F) {
            subFavorEffort.put(god, 0.0F);
        }
        return newFavor - oldFavor;
    }

    public float getSubFavorEffort(SinGod god) {
        return subFavorEffort.getOrDefault(god, 0.0F);
    }

    /**
     * Aplica esfuerzo hacia subir (+effort) o bajar (-effort) el favor.
     * Cada punto entero requiere el esfuerzo definido por {@link #calculateRequiredEffort(int, boolean)}
     * segun el tramo actual, siendo agil entre -20 y +20 y progresivamente mucho mas costoso
     * a medida que se acerca a +-100. El esfuerzo fraccional sobrante se conserva.
     *
     * @return El cambio entero total aplicado.
     */
    public int addDampedEffort(SinGod god, float effortDelta) {
        if (effortDelta == 0.0F) {
            return 0;
        }
        int initial = getFavor(god);
        int current = initial;
        if ((current >= MAX_FAVOR && effortDelta > 0) || (current <= MIN_FAVOR && effortDelta < 0)) {
            return 0;
        }

        float accumulated = getSubFavorEffort(god) + effortDelta;

        while (accumulated > 0.0F && current < MAX_FAVOR) {
            boolean movingAwayFromZero = current >= 0;
            float requiredThreshold = calculateRequiredEffort(Math.abs(current), movingAwayFromZero);
            if (accumulated >= requiredThreshold) {
                accumulated -= requiredThreshold;
                current++;
            } else {
                break;
            }
        }

        while (accumulated < 0.0F && current > MIN_FAVOR) {
            boolean movingAwayFromZero = current <= 0;
            float requiredThreshold = calculateRequiredEffort(Math.abs(current), movingAwayFromZero);
            if (accumulated <= -requiredThreshold) {
                accumulated += requiredThreshold;
                current--;
            } else {
                break;
            }
        }

        if (current >= MAX_FAVOR && accumulated > 0.0F) {
            accumulated = 0.0F;
        } else if (current <= MIN_FAVOR && accumulated < 0.0F) {
            accumulated = 0.0F;
        }

        favorMap.put(god, current);
        subFavorEffort.put(god, accumulated);
        return current - initial;
    }

    /**
     * Calcula el esfuerzo requerido para mover el marcador 1 punto entero segun el tramo actual.
     * - Entre -20 y +20: 1.0 (oscilacion agil)
     * - Entre +-21 y +-60: 3.0
     * - Entre +-61 y +-85: 6.0
     * - Entre +-86 y +-100: 12.0 (extremo muy dificil)
     */
    public static float calculateRequiredEffort(int absFavor, boolean movingAwayFromZero) {
        if (!movingAwayFromZero) {
            // Regresar hacia 0 es mas sencillo que alcanzar un extremo
            return absFavor > 60 ? 2.0F : 1.0F;
        }
        if (absFavor <= 20) {
            return 1.0F;
        } else if (absFavor <= 60) {
            return 3.0F;
        } else if (absFavor <= 85) {
            return 6.0F;
        } else {
            return 12.0F;
        }
    }

    public long getNextMinorWrathTick(SinGod god) {
        return nextMinorWrathTick.getOrDefault(god, 0L);
    }

    public void setNextMinorWrathTick(SinGod god, long tick) {
        nextMinorWrathTick.put(god, tick);
    }

    public long getNextEnvoyTick(SinGod god) {
        return nextEnvoyTick.getOrDefault(god, 0L);
    }

    public void setNextEnvoyTick(SinGod god, long tick) {
        nextEnvoyTick.put(god, tick);
    }

    public long getNextHostileAcolyteTick(SinGod god) {
        return nextHostileAcolyteTick.getOrDefault(god, 0L);
    }

    public void setNextHostileAcolyteTick(SinGod god, long tick) {
        nextHostileAcolyteTick.put(god, tick);
    }

    public long getNextMinorBlessingTick(SinGod god) {
        return nextMinorBlessingTick.getOrDefault(god, 0L);
    }

    public void setNextMinorBlessingTick(SinGod god, long tick) {
        nextMinorBlessingTick.put(god, tick);
    }

    public long getNextAlliedAcolyteTick(SinGod god) {
        return nextAlliedAcolyteTick.getOrDefault(god, 0L);
    }

    public void setNextAlliedAcolyteTick(SinGod god, long tick) {
        nextAlliedAcolyteTick.put(god, tick);
    }

    public long getNextAvatarManifestTick(SinGod god) {
        return nextAvatarManifestTick.getOrDefault(god, 0L);
    }

    public void setNextAvatarManifestTick(SinGod god, long tick) {
        nextAvatarManifestTick.put(god, tick);
    }

    public long getNextAvatarInvocationTick(SinGod god) {
        return nextAvatarInvocationTick.getOrDefault(god, 0L);
    }

    public void setNextAvatarInvocationTick(SinGod god, long tick) {
        nextAvatarInvocationTick.put(god, tick);
    }

    public boolean hasClaimedRelic(SinGod god) {
        return claimedRelicMap.getOrDefault(god, false);
    }

    public void setClaimedRelic(SinGod god, boolean claimed) {
        claimedRelicMap.put(god, claimed);
    }

    public boolean isGodInSlumber(SinGod god, long gameTime) {
        return gameTime < getGodSlumberUntil(god);
    }

    public long getGodSlumberUntil(SinGod god) {
        return godSlumberUntilTick.getOrDefault(god, 0L);
    }

    public void setGodSlumberUntil(SinGod god, long untilTick) {
        this.godSlumberUntilTick.put(god, Math.max(0L, untilTick));
    }

    public long getSlumberRemainingTicks(SinGod god, long gameTime) {
        return Math.max(0L, getGodSlumberUntil(god) - gameTime);
    }

    public long getLastGreedTributeTick() {
        return lastGreedTributeTick;
    }

    public void setLastGreedTributeTick(long lastGreedTributeTick) {
        this.lastGreedTributeTick = lastGreedTributeTick;
    }

    public long getLastWrathKillTick() {
        return lastWrathKillTick;
    }

    public void setLastWrathKillTick(long lastWrathKillTick) {
        this.lastWrathKillTick = lastWrathKillTick;
    }

    public long getLastChatEvalTick() {
        return lastChatEvalTick;
    }

    public void setLastChatEvalTick(long lastChatEvalTick) {
        this.lastChatEvalTick = lastChatEvalTick;
    }

    public int getSlothStillTicks() {
        return slothStillTicks;
    }

    public void setSlothStillTicks(int slothStillTicks) {
        this.slothStillTicks = slothStillTicks;
    }

    public int getSlothLaborBlocks() {
        return slothLaborBlocks;
    }

    public void setSlothLaborBlocks(int slothLaborBlocks) {
        this.slothLaborBlocks = Math.max(0, slothLaborBlocks);
    }

    public int incrementSlothLaborBlocks() {
        this.slothLaborBlocks = Math.max(0, this.slothLaborBlocks) + 1;
        return this.slothLaborBlocks;
    }

    public long getNextPeriodicInterventionTick() {
        return nextPeriodicInterventionTick;
    }

    public void setNextPeriodicInterventionTick(long nextPeriodicInterventionTick) {
        this.nextPeriodicInterventionTick = nextPeriodicInterventionTick;
    }

    public int getMinInterventionTicks() {
        return minInterventionTicks > 0 ? minInterventionTicks : 12000;
    }

    public void setMinInterventionTicks(int minInterventionTicks) {
        this.minInterventionTicks = minInterventionTicks;
    }

    public int getMaxInterventionTicks() {
        return maxInterventionTicks > 0 ? maxInterventionTicks : 48000;
    }

    public void setMaxInterventionTicks(int maxInterventionTicks) {
        this.maxInterventionTicks = maxInterventionTicks;
    }

    public Map<SinGod, Integer> getAllFavors() {
        return Map.copyOf(favorMap);
    }

    public boolean markLoreBookRead(String bookId) {
        if (bookId == null || bookId.isBlank()) {
            return false;
        }
        return this.readLoreBooks.add(bookId);
    }

    public int getReadLoreBooksCount() {
        return this.readLoreBooks.size();
    }

    public Set<String> getReadLoreBooks() {
        return Collections.unmodifiableSet(this.readLoreBooks);
    }

    public void copyFrom(PlayerSinData other) {
        for (SinGod god : SinGod.values()) {
            this.favorMap.put(god, other.getFavor(god));
            this.subFavorEffort.put(god, other.getSubFavorEffort(god));
            this.nextMinorWrathTick.put(god, other.getNextMinorWrathTick(god));
            this.nextEnvoyTick.put(god, other.getNextEnvoyTick(god));
            this.nextHostileAcolyteTick.put(god, other.getNextHostileAcolyteTick(god));
            this.nextMinorBlessingTick.put(god, other.getNextMinorBlessingTick(god));
            this.nextAlliedAcolyteTick.put(god, other.getNextAlliedAcolyteTick(god));
            this.nextAvatarManifestTick.put(god, other.getNextAvatarManifestTick(god));
            this.nextAvatarInvocationTick.put(god, other.getNextAvatarInvocationTick(god));
            this.claimedRelicMap.put(god, other.hasClaimedRelic(god));
            this.godSlumberUntilTick.put(god, other.getGodSlumberUntil(god));
        }
        this.readLoreBooks.clear();
        this.readLoreBooks.addAll(other.readLoreBooks);
        this.lastGreedTributeTick = other.lastGreedTributeTick;
        this.lastWrathKillTick = other.lastWrathKillTick;
        this.lastChatEvalTick = other.lastChatEvalTick;
        this.slothStillTicks = other.slothStillTicks;
        this.slothLaborBlocks = other.slothLaborBlocks;
        this.nextPeriodicInterventionTick = other.nextPeriodicInterventionTick;
        this.minInterventionTicks = other.minInterventionTicks;
        this.maxInterventionTicks = other.maxInterventionTicks;
    }

    @Override
    public void serialize(ValueOutput output) {
        for (SinGod god : SinGod.values()) {
            String key = god.getId();
            output.putInt("favor_" + key, getFavor(god));
            output.putFloat("effort_" + key, getSubFavorEffort(god));
            output.putLong("wrath_tick_" + key, getNextMinorWrathTick(god));
            output.putLong("envoy_tick_" + key, getNextEnvoyTick(god));
            output.putLong("acolyte_h_tick_" + key, getNextHostileAcolyteTick(god));
            output.putLong("bless_tick_" + key, getNextMinorBlessingTick(god));
            output.putLong("acolyte_a_tick_" + key, getNextAlliedAcolyteTick(god));
            output.putLong("avatar_tick_" + key, getNextAvatarManifestTick(god));
            output.putLong("avatar_invoc_tick_" + key, getNextAvatarInvocationTick(god));
            output.putBoolean("relic_claimed_" + key, hasClaimedRelic(god));
            output.putLong("slumber_tick_" + key, getGodSlumberUntil(god));
        }
        if (!this.readLoreBooks.isEmpty()) {
            output.putString("read_lore_books", String.join(";;", this.readLoreBooks));
        }
        output.putLong("last_greed_tribute", this.lastGreedTributeTick);
        output.putLong("last_wrath_kill", this.lastWrathKillTick);
        output.putLong("last_chat_eval", this.lastChatEvalTick);
        output.putInt("sloth_labor_blocks", this.slothLaborBlocks);
        output.putLong("next_periodic_intervention", this.nextPeriodicInterventionTick);
        output.putInt("min_interval_ticks", this.minInterventionTicks);
        output.putInt("max_interval_ticks", this.maxInterventionTicks);
    }

    @Override
    public void deserialize(ValueInput input) {
        for (SinGod god : SinGod.values()) {
            String key = god.getId();
            favorMap.put(god, Mth.clamp(input.getIntOr("favor_" + key, 0), MIN_FAVOR, MAX_FAVOR));
            subFavorEffort.put(god, input.getFloatOr("effort_" + key, 0.0F));
            nextMinorWrathTick.put(god, input.getLongOr("wrath_tick_" + key, 0L));
            nextEnvoyTick.put(god, input.getLongOr("envoy_tick_" + key, 0L));
            nextHostileAcolyteTick.put(god, input.getLongOr("acolyte_h_tick_" + key, 0L));
            nextMinorBlessingTick.put(god, input.getLongOr("bless_tick_" + key, 0L));
            nextAlliedAcolyteTick.put(god, input.getLongOr("acolyte_a_tick_" + key, 0L));
            nextAvatarManifestTick.put(god, input.getLongOr("avatar_tick_" + key, 0L));
            nextAvatarInvocationTick.put(god, input.getLongOr("avatar_invoc_tick_" + key, 0L));
            claimedRelicMap.put(god, input.getBooleanOr("relic_claimed_" + key, false));
            godSlumberUntilTick.put(god, input.getLongOr("slumber_tick_" + key, 0L));
        }
        this.readLoreBooks.clear();
        String loreBooksRaw = input.getStringOr("read_lore_books", "");
        if (!loreBooksRaw.isEmpty()) {
            for (String bookId : loreBooksRaw.split(";;")) {
                if (!bookId.isBlank()) {
                    this.readLoreBooks.add(bookId);
                }
            }
        }
        this.lastGreedTributeTick = input.getLongOr("last_greed_tribute", 0L);
        this.lastWrathKillTick = input.getLongOr("last_wrath_kill", 0L);
        this.lastChatEvalTick = input.getLongOr("last_chat_eval", 0L);
        this.slothLaborBlocks = Math.max(0, input.getIntOr("sloth_labor_blocks", 0));
        this.nextPeriodicInterventionTick = input.getLongOr("next_periodic_intervention", 0L);
        this.minInterventionTicks = input.getIntOr("min_interval_ticks", 12000);
        this.maxInterventionTicks = input.getIntOr("max_interval_ticks", 48000);
    }
}
