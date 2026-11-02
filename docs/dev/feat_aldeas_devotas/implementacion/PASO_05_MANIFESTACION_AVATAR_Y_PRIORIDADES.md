# Paso 5: Manifestación del Avatar (5%), Regla de Avatar Único y Prioridad de Asedio

Este paso implementa el clímax de los asedios territoriales: la tirada del **5% de Manifestación Divina**, respetando la regla fundamental de **Avatar Único** e imponiendo la **Prioridad Absoluta del Asedio sobre las Cazas de Ira**.

---

## 🎯 Objetivos del Paso
1. Ejecutar una tirada del 5% durante el inicio del asedio a la aldea para descender un **Avatar Divino (`GodAvatarEntity`)**.
2. Comprobar `DivineInterventionScheduler.getActiveAvatar(server, god)` para no duplicar nunca un Avatar activo.
3. Si el Avatar estaba cazando a un jugador insolente (`isWrathHunter() == true`), pausar la caza, avisar al jugador insolente y teletransportar al Avatar a la aldea asediada preservando su salud.
4. Al concluir la batalla en la aldea, si el Avatar sobrevivió, **reubicarlo para reanudar la cacería pendiente**.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Gestor** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageRaidManager.java` | Lógica de manifestación, posposición y reanudación. |
| **Planificador** | `src/main/java/com/volcov/sevendeadlygods/intervention/DivineInterventionScheduler.java` | Registro global `ACTIVE_AVATARS` y trigger de caza. |

---

## 💻 Implementación Técnica

### 5.1 Método de Invocación con Prioridad Territorial (`GodVillageRaidManager.java`)
```java
private static final Map<SinGod, UUID> POSTPONED_WRATH_HUNTS = new EnumMap<>(SinGod.class);

public static void evaluateAvatarManifestation(
        MinecraftServer server,
        ServerLevel level,
        BlockPos altarPos,
        SinGod defenderGod,
        SinGod attackerGod
) {
    // 5% de probabilidad estricta
    if (level.random.nextFloat() >= 0.05F) {
        return;
    }

    // Elegir quién desciende (50% Defensor, 50% Atacante)
    SinGod appearingGod = level.random.nextBoolean() ? defenderGod : attackerGod;

    // --- REGLA DE AVATAR ÚNICO ---
    GodAvatarEntity existingAvatar = DivineInterventionScheduler.getActiveAvatar(server, appearingGod);

    if (existingAvatar != null) {
        // El Avatar ya existe en el servidor. Si está en una Caza de Ira, la aldea es PRIORITARIA:
        if (existingAvatar.isWrathHunter() && existingAvatar.getTargetPlayerUuid() != null) {
            UUID huntedPlayerUuid = existingAvatar.getTargetPlayerUuid();
            POSTPONED_WRATH_HUNTS.put(appearingGod, huntedPlayerUuid);

            ServerPlayer huntedPlayer = server.getPlayerList().getPlayer(huntedPlayerUuid);
            if (huntedPlayer != null) {
                huntedPlayer.sendSystemMessage(
                        appearingGod.getFormattedPrefix().append(
                                Component.literal("«¡Mortal insolente! Tu juicio queda aplazado: mi templo sagrado clama por mi presencia divina. ¡Considera este día un préstamo de vida!»")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                        )
                );
            }

            // Efectos de salida en la ubicación anterior y teletransporte al altar preservando vida
            ServerLevel oldLevel = (ServerLevel) existingAvatar.level();
            oldLevel.sendParticles(ParticleTypes.PORTAL, existingAvatar.getX(), existingAvatar.getY() + 1.0D, existingAvatar.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);
            float currentHealth = existingAvatar.getHealth();

            if (oldLevel == level) {
                existingAvatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
                existingAvatar.getNavigation().stop();
                existingAvatar.initializeAvatar(level, appearingGod, null, false);
                existingAvatar.setHealth(currentHealth);
            } else {
                existingAvatar.discard();
                GodAvatarEntity transferred = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
                if (transferred != null) {
                    transferred.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
                    transferred.initializeAvatar(level, appearingGod, null, false);
                    transferred.setHealth(currentHealth);
                    level.addFreshEntity(transferred);
                }
            }
        } else {
            // Ya estaba en la aldea o como avatar campeón: se reposiciona al altar
            existingAvatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
        }
    } else {
        // No existe: spawneo de nuevo Avatar colosal
        GodAvatarEntity avatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
        if (avatar != null) {
            avatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
            avatar.initializeAvatar(level, appearingGod, null, false);
            level.addFreshEntity(avatar);
        }
    }

    // Anuncio del milagro cósmico en el chat global
    Component announcement = Component.literal("⚡ ¡MANIFESTACIÓN DIVINA! ")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
            .append(Component.literal("El clamor de la sangre ha desgarrado el velo: ").withStyle(ChatFormatting.WHITE))
            .append(appearingGod.getDisplayName().copy().withStyle(appearingGod.getColor(), ChatFormatting.BOLD))
            .append(Component.literal(" ha descendido para juzgar la batalla!").withStyle(ChatFormatting.WHITE));

    server.getPlayerList().broadcastSystemMessage(announcement, false);
}
```

### 5.2 Reanudación de la Caza Pospuesta
```java
public static void onVillageRaidEnded(MinecraftServer server, ServerLevel level, SinGod god) {
    UUID targetUuid = POSTPONED_WRATH_HUNTS.remove(god);
    if (targetUuid == null) return;

    ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUuid);
    GodAvatarEntity avatar = DivineInterventionScheduler.getActiveAvatar(server, god);

    if (targetPlayer != null && targetPlayer.isAlive() && avatar != null && avatar.isAlive()) {
        targetPlayer.sendSystemMessage(
                god.getFormattedPrefix().append(
                        Component.literal("«Mi templo sagrado está a salvo. Ahora... volvamos a nuestra deuda pendiente, mortal.»")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                )
        );
        // Teletransporta al Avatar cerca del jugador insolente y reanuda el combate a muerte
        DivineInterventionScheduler.triggerWrathAvatarHunt(targetPlayer, god);
    }
}
```

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Provocar una caza de ira contra un jugador (`/gods wrath trigger ...`).
2. [ ] Forzar la manifestación del 5% del mismo dios en una aldea asediada.
3. [ ] Confirmar que el jugador cazado recibe el mensaje de aplazamiento y el Avatar se traslada inmediatamente a la aldea conservando su salud.
4. [ ] Eliminar a los atacantes de la aldea y confirmar que el Avatar superviviente se teletransporta de vuelta para reanudar la caza del jugador original.
