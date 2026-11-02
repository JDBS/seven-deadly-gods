# 🤖 Configuración de Inteligencia Artificial (Google Gemini)

Los dioses de **Seven Deadly Gods** están dotados de consciencia gracias a la integración con **Google Gemini**, lo que les permite analizar tus actos, responder en el chat en tiempo real, recordar tus hazañas o traiciones y juzgar tus rituales con personalidad propia.

---

## 🔑 1. Obtener una API Key de Google Gemini

1. Accede a [Google AI Studio](https://aistudio.google.com/).
2. Inicia sesión con tu cuenta de Google.
3. Haz clic en **Get API key** y selecciona **Create API key in new project**.
4. Copia la clave generada (empieza por `AIzaSy...`).

> [!NOTE]
> Google AI Studio ofrece un nivel de uso gratuito generoso (*Free Tier*) suficiente para jugar partidas individuales o pequeños servidores entre amigos.

---

## ⚙️ 2. Formas de Configurar la API Key

El mod busca la clave en dos ubicaciones:

### Opción A: Archivo de Configuración del Mod (Recomendado para la mayoría de usuarios)
Tras iniciar el juego por primera vez, se genera automáticamente el archivo `config/sevendeadlygods-common.toml` en la raíz de tu carpeta `.minecraft/` o del servidor:

```toml
[gemini_ai]
    # Clave de API de Google Gemini (AI Studio).
    geminiApiKey = "AIzaSyTuClaveDeApiAqui"

    # Modelo de Gemini a utilizar para las respuestas
    geminiModel = "gemini-3.8-flash"

    # Tiempo máximo de espera en segundos antes de usar fallback local
    apiTimeoutSeconds = 30

    # Tiempo mínimo en segundos entre intervenciones espontáneas en el chat
    chatEvaluationCooldownSeconds = 8

    # Si es true, usa el motor temático local si no hay internet o falla la API
    enableLocalFallback = true
```

### Opción B: Variable de Entorno del Sistema (Ideal para Servidores y Docker)
Puedes definir la variable de entorno en tu sistema operativo:

* **En Linux / macOS**:
  ```bash
  export GEMINI_API_KEY="AIzaSyTuClaveDeApiAqui"
  # O alternativamente:
  export GOOGLE_API_KEY="AIzaSyTuClaveDeApiAqui"
  ```
* **En Windows (PowerShell)**:
  ```powershell
  $env:GEMINI_API_KEY="AIzaSyTuClaveDeApiAqui"
  ```

---

## 🛡️ 3. Motor Temático Local de Respaldo (*Local Fallback*)

Si no dispones de una clave de API, juegas sin conexión a internet o los servidores de Google experimentan latencia:

* El mod activa automáticamente su **motor temático local** (`enableLocalFallback = true`).
* Los dioses seguirán hablando en el chat utilizando cientos de diálogos pregrabados específicos para cada pecado, evaluarán las palabras clave de tus mensajes y reaccionarán físicamente (rayos, maldiciones, bendiciones y súplicas) sin interrumpir la partida.
