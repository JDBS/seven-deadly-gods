package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Gestor de reclutamiento demografico y consagracion organica de aldeanos a centinelas.
 * Evalua la poblacion civil, preserva el suelo demografico minimo para garantizar el ciclo
 * de reproduccion y convierte a devotos en guardias permanentes del santuario.
 */
public final class VillageRecruitmentManager {

    private VillageRecruitmentManager() {
    }

    public static boolean tickVillageRecruitment(
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            long gameTime
    ) {
        if (village.state() != GodVillageState.CONSAGRADA || !ModConfig.SENTINEL_RECRUITMENT_ENABLED.get()) {
            return false;
        }

        SinGod god = village.patronGod();

        // 1. Envidia (Leviatán): Los asentamientos arrasados son completamente estériles (sin reposición)
        if (god == SinGod.ENVY || village.state() == GodVillageState.ARRASADA) {
            return false;
        }

        // 2. Mecánica Asimétrica de Belphegor (Pereza): Sustento Onírico a los Nitwits dormidos
        if (god == SinGod.SLOTH) {
            tickSlothDreamSustenance(level, village, gameTime);
        }

        BlockPos altarPos = village.altarPos();

        // 3. Optimización: Solo evaluar si hay algún jugador cerca para presenciar o activar el entorno
        if (!level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 96.0D)) {
            return false;
        }

        // 4. Cooldown temporal (por defecto 24.000 ticks = 1 día solar de Minecraft)
        long cooldown = ModConfig.SENTINEL_RECRUITMENT_COOLDOWN_TICKS.get();
        if (gameTime - village.lastRecruitmentTick() < cooldown) {
            return false;
        }

        AABB villageBox = new AABB(altarPos).inflate(village.radius());

        // 5. Conteo de centinelas vivos actuales vs Cupo Máximo
        List<GodAcolyteEntity> existingSentinels = level.getEntitiesOfClass(
                GodAcolyteEntity.class,
                villageBox,
                a -> a.isAlive() && a.isVillageSentinel() && a.getGod() == god && altarPos.equals(a.getHomeAltarPos())
        );

        int maxSentinels = getMaxSentinelsForGod(god);
        if (existingSentinels.size() >= maxSentinels) {
            return false; // Cupo cubierto
        }

        // 6. Conteo de aldeanos civiles adultos vivos y Regla de Prudencia Demográfica
        List<Villager> adultVillagers = level.getEntitiesOfClass(
                Villager.class,
                villageBox,
                v -> v.isAlive() && !v.isBaby()
        );

        int minCivilianFloor = ModConfig.MIN_CIVILIAN_VILLAGERS_FLOOR.get();
        // Requiere al menos (minCivilianFloor + 1) adultos para que tras la ordenación permanezcan al menos minCivilianFloor
        if (adultVillagers.size() <= minCivilianFloor) {
            return false;
        }

        // 7. Encontrar el candidato idóneo
        Optional<Villager> bestCandidate = selectBestCandidate(adultVillagers);
        if (bestCandidate.isEmpty()) {
            return false;
        }

        // 8. Ejecutar la consagración
        ordainVillagerAsSentinel(level, bestCandidate.get(), god, altarPos);
        data.updateVillage(village.withLastRecruitmentTick(gameTime));
        return true;
    }

    public static int getMaxSentinelsForGod(SinGod god) {
        if (god == SinGod.ENVY) {
            return 0;
        }
        if (god == SinGod.WRATH) {
            return ModConfig.MAX_SENTINELS_WRATH.get();
        }
        if (god == SinGod.SLOTH) {
            return 3;
        }
        return ModConfig.MAX_SENTINELS_DEFAULT.get();
    }

    public static Optional<Villager> selectBestCandidate(List<Villager> candidates) {
        long totalFarmers = candidates.stream()
                .filter(v -> v.getVillagerData().profession().is(VillagerProfession.FARMER))
                .count();

        return candidates.stream()
                .filter(v -> isEligibleForOrdination(v, totalFarmers))
                .max(Comparator.comparingInt(VillageRecruitmentManager::calculateCandidateScore));
    }

    public static boolean isEligibleForOrdination(Villager v, long totalFarmers) {
        if (v.hasCustomName()) {
            return false;
        }
        if (v.isPassenger() || v.isVehicle()) {
            return false;
        }
        if (v.getTradingPlayer() != null) {
            return false;
        }
        if (v.isBaby()) {
            return false;
        }

        VillagerData data = v.getVillagerData();
        if (data.level() >= 2) {
            return false;
        }
        if (data.profession().is(VillagerProfession.FARMER) && totalFarmers <= 1) {
            return false;
        }
        return true;
    }

    public static int calculateCandidateScore(Villager v) {
        VillagerData data = v.getVillagerData();
        if (data.profession().is(VillagerProfession.NITWIT)) {
            return 100;
        }
        if (data.profession().is(VillagerProfession.NONE)) {
            return 50;
        }
        return Math.max(1, 20 - (data.level() * 5));
    }

    public static void ordainVillagerAsSentinel(
            ServerLevel level,
            Villager candidate,
            SinGod god,
            BlockPos altarPos
    ) {
        double x = candidate.getX();
        double y = candidate.getY();
        double z = candidate.getZ();
        float yRot = candidate.getYRot();
        float xRot = candidate.getXRot();
        Component customName = candidate.getCustomName();

        triggerOrdinationEffects(level, x, y, z, god);

        candidate.discard();

        GodAcolyteEntity sentinel = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.CONVERSION);
        if (sentinel != null) {
            sentinel.snapTo(x, y, z, yRot, xRot);
            sentinel.setYHeadRot(yRot);
            if (customName != null) {
                sentinel.setCustomName(customName);
            }
            sentinel.configureVillageSentinel(level, god, altarPos);
            level.addFreshEntity(sentinel);
        }
    }

    public static void triggerOrdinationEffects(ServerLevel level, double x, double y, double z, SinGod god) {
        switch (god) {
            case WRATH -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 1.0D, z, 30, 0.4D, 0.6D, 0.4D, 0.05D);
                level.sendParticles(ParticleTypes.FLAME, x, y + 1.2D, z, 20, 0.3D, 0.5D, 0.3D, 0.02D);
                level.playSound(null, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            case GREED -> {
                level.sendParticles(ParticleTypes.WAX_ON, x, y + 1.0D, z, 35, 0.5D, 0.7D, 0.5D, 0.05D);
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.1F);
                level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case LUST -> {
                level.sendParticles(ParticleTypes.HEART, x, y + 1.2D, z, 15, 0.5D, 0.5D, 0.5D, 0.02D);
                level.sendParticles(ParticleTypes.NOTE, x, y + 1.5D, z, 10, 0.4D, 0.4D, 0.4D, 0.5D);
                level.playSound(null, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            case GLUTTONY -> {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.0D, z, 25, 0.4D, 0.6D, 0.4D, 0.02D);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.2D, z, 12, 0.3D, 0.4D, 0.3D, 0.02D);
                level.playSound(null, x, y, z, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.9F);
            }
            case PRIDE -> {
                level.sendParticles(ParticleTypes.END_ROD, x, y + 1.2D, z, 30, 0.4D, 0.7D, 0.4D, 0.05D);
                level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F);
            }
            case SLOTH -> {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.8D, z, 20, 0.4D, 0.4D, 0.4D, 0.01D);
                level.playSound(null, x, y, z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            case ENVY -> {
                level.sendParticles(ParticleTypes.ASH, x, y + 1.0D, z, 25, 0.4D, 0.5D, 0.4D, 0.02D);
                level.playSound(null, x, y, z, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
            }
        }
    }

    public static void tickSlothDreamSustenance(ServerLevel level, GodVillageSavedData.GodVillageRecord village, long gameTime) {
        if (gameTime % 600L != 0) {
            return;
        }

        BlockPos altarPos = village.altarPos();
        AABB villageBox = new AABB(altarPos).inflate(village.radius());
        List<Villager> sleepingNitwits = level.getEntitiesOfClass(
                Villager.class,
                villageBox,
                v -> v.isAlive() && v.isSleeping() && v.getVillagerData().profession().is(VillagerProfession.NITWIT)
        );

        for (Villager nitwit : sleepingNitwits) {
            if (nitwit.getInventory().countItem(Items.BREAD) < 3) {
                nitwit.getInventory().addItem(new ItemStack(Items.BREAD, 3));
            }
        }
    }
}
