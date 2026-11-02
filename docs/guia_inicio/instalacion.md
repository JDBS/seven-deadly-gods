# 🚀 Instalación y Requisitos Técnicos

Esta guía detalla los requisitos y pasos necesarios para instalar, compilar o ejecutar **Seven Deadly Gods** en tu entorno.

---

## 📋 Requisitos del Sistema

* **Minecraft**: `26.3`
* **Mod Loader**: `NeoForge 26.3.0.99` o superior
* **Java Development Kit (JDK)**: `Java 25` (requerido para compilar y ejecutar Minecraft 26.3)
* **Entornos soportados**: Cliente individual (Singleplayer), Servidor dedicado (Multiplayer) y Entorno de Desarrollo (Dev Workspace).

---

## 📦 Instalación para Jugadores

1. Asegúrate de tener instalado el lanzador de Minecraft con el perfil de **NeoForge 26.3.0.99+**.
2. Descarga el archivo compilado `sevendeadlygods-1.2.0.jar` (desde la pestaña de *Releases* en GitHub).
3. Coloca el archivo `.jar` dentro de la carpeta `.minecraft/mods/` de tu instalación.
4. *(Opcional)* Configura tu clave de API de Gemini si deseas que los dioses conversen dinámicamente con IA (ver [Configuración de IA Gemini](gemini_ai_setup.md)).
5. Inicia el juego desde el lanzador de NeoForge.

---

## 🛠️ Compilación desde el Código Fuente

Si deseas compilar el mod tú mismo a partir de este repositorio:

```bash
# Clonar el repositorio
git clone https://github.com/JDBS/seven-deadly-gods.git
cd seven-deadly-gods

# Compilar el archivo .jar (se generará en build/libs/sevendeadlygods-1.2.0.jar)
./gradlew build

# Ejecutar el cliente de desarrollo directamente
./gradlew runClient

# Ejecutar un servidor de desarrollo
./gradlew runServer
```

---

## 🌐 Compatibilidad con Servidores Dedicados

* El mod es **completamente funcional en servidores dedicados**.
* La persistencia de datos territoriales de las aldeas y el favor de los jugadores se almacenan en el directorio del mundo (`world/data/sevendeadlygods_villages.dat` y `world/playerdata/`), por lo que no se pierde información entre reinicios.
* En multijugador, cada jugador tiene su propia relación y marcadores de Favor/Ira independientes con cada uno de los 7 Dioses.
