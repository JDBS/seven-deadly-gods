# ⚙️ Archivo de Configuración (`sevendeadlygods-common.toml`)

El archivo de configuración se encuentra en `.minecraft/config/sevendeadlygods-common.toml` (o `config/sevendeadlygods-common.toml` en servidores dedicados) y permite personalizar el comportamiento de la IA y el mundo.

---

## 📄 Estructura del Archivo

```toml
# Configuracion de Inteligencia Artificial (Google Gemini) para Seven Deadly Gods
[gemini_ai]
    # Clave de API de Google Gemini (AI Studio).
    # Si se deja vacia, se buscara en las variables de entorno GEMINI_API_KEY o GOOGLE_API_KEY.
    geminiApiKey = ""

    # Modelo de Gemini a utilizar para las respuestas y decisiones de los dioses.
    # Modelos recomendados: gemini-3.8-flash, gemini-3.1-flash-lite
    geminiModel = "gemini-3.8-flash"

    # Tiempo maximo de espera (en segundos) para la respuesta de la API de Gemini antes de usar fallback.
    # Rango: 5 a 120 segundos. Por defecto: 30
    apiTimeoutSeconds = 30

    # Tiempo minimo en segundos entre intervenciones espontaneas de los dioses en el chat cuando no estan manifestados.
    # Rango: 1 a 300 segundos. Por defecto: 8
    chatEvaluationCooldownSeconds = 8

    # Si es true, cuando no haya API Key configurada o falle la red, los dioses usaran un motor de respuestas tematicas local.
    enableLocalFallback = true

# Configuracion de Lore, Mundo y Aldeas Consagradas
[world]
    # Probabilidad (0.0 a 1.0) de que aparezca un libro sagrado de relato en cofres de estructuras del mundo.
    # Por defecto: 0.35 (35%)
    loreBookChestChance = 0.35

    # Puntos de experiencia otorgados a un jugador la primera vez que asimila o lee un libro sagrado de relato.
    # Rango: 0 a 1000. Por defecto: 25 puntos
    loreBookFirstReadXp = 25

    # Probabilidad (0.0 a 1.0) de que una aldea natural sea consagrada a uno de los 7 Pecados Capitales (el resto permanece vanilla).
    # Rango: 0.0 a 1.0. Por defecto: 0.80 (80% consagradas, 20% vanilla)
    villageConsecrationChance = 0.80

    # Enfriamiento minimo en ticks entre asaltos autonomos a una misma aldea consagrada.
    # Por defecto: 72000 ticks (3 dias de Minecraft)
    villageRaidCooldownTicks = 72000

    # Probabilidad (0.0 a 1.0) de que el Avatar de un Dios se manifieste fisicamente durante un asedio a una aldea.
    # Por defecto: 0.05 (5%)
    villageRaidAvatarChance = 0.05
```
