# ✦ Seven Deadly Gods

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-informational?style=flat-square&logo=minecraft)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.3.0.99+-orange?style=flat-square)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-JDK_25-red?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![AI](https://img.shields.io/badge/AI-Google_Gemini-4285F4?style=flat-square&logo=google)](https://aistudio.google.com/)

**Seven Deadly Gods** es una experiencia inmersiva para **Minecraft 26.3 (NeoForge)** que encarna a los **7 Pecados Capitales** como Dioses conscientes dotados de Inteligencia Artificial (**Google Gemini**, con motor temático local de respaldo). 

Las deidades observan cada una de tus acciones en tiempo real, conversan contigo a través del chat, juzgan tus sacrificios en sus altares asimétricos, libran **Guerras Santas territoriales por el control de las aldeas**, envían acólitos y descienden físicamente en persona como **Avatares colosales**.

---

## 🏛️ Los 7 Dioses del Pecado

| Pecado | Dios | Dominio Principal | Qué le complace (+Favor) | Qué le enfurece (-Favor) |
| :--- | :--- | :--- | :--- | :--- |
| **Avaricia** (`greed`) | **Mammon** | Riqueza áurea y diezmos | Tributar minerales refinados, gemas y netherite en su altar | Portar riquezas sin pagar diezmo; ofrecer oro en bruto |
| **Ira** (`wrath`) | **Khorvath** | Guerra, sangre y masacre | Derramar sangre, matar monstruos con poca salud y Deicidio | Usar escudos para protegerse; dormir en camas |
| **Gula** (`gluttony`) | **Beelze** | El Banquete y la nutrición | Degustar platos selectos y servir manjares en su altar | Pasar hambre severa; comer alimentos podridos o venenosos |
| **Soberbia** (`pride`) | **Lucian** | La Cúspide y la gloria | Abatir enemigos poderosos, reverencia arrodillada (`Shift`) | Caer de forma humillante; tocar su altar sin agacharse |
| **Envidia** (`envy`) | **Leviatán** | El Abismo y el despojo | Saquear aldeas, destruir botín ajeno y romper lazos con otros | Comerciar pacíficamente; superar +20 de favor con otros dioses |
| **Pereza** (`sloth`) | **Belphegor** | La Inercia y el letargo | Dormir en camas (reinicia fatiga), caminar lento y reposar | Esprintar sin parar; trabajar en exceso sin descansar |
| **Lujuria** (`lust`) | **Asmodeus** | Pasión, belleza y fertilidad | Procrear ganado, vestir Armor Trims, flores y música | Destruir flores ornamentales; matar crías inocentes |

---

## 📚 Documentación y Wiki Oficial

La documentación detallada del mod ha sido completamente modularizada para facilitar su lectura y consulta en GitHub:

### 📖 [Acceder al Índice Maestro de la Wiki (`docs/README.md`)](docs/README.md)

* 🚀 **[Guía de Instalación y Requisitos](docs/guia_inicio/instalacion.md)**: Instalación con NeoForge 26.3, Java 25 y cómo compilar el mod.
* 🤖 **[Configuración de Inteligencia Artificial](docs/guia_inicio/gemini_ai_setup.md)**: Cómo vincular tu API Key de Gemini y funcionamiento del motor temático local.
* ⚖️ **[Escala de Favor e Ira Divina (-100 a +100)](docs/mecanicas_principales/favor_e_ira.md)**: Progresión acumulativa, beneficios de gracia y castigos de ira.
* 💬 **[Chat Inteligente y Reacciones Físicas](docs/mecanicas_principales/chat_inteligente.md)**: Cómo hablar con los dioses, cálculo de probabilidades y súplicas.
* 👑 **[Acólitos, Avatares y Deicidio](docs/mecanicas_principales/avatares_y_acolitos.md)**: Regla de Avatar Único, estadísticas de jefe y recompensas de favor.
* 🔨 **[Guía de Crafteo de Altares y Rituales](docs/dioses_y_altares/guia_altares_crafteo.md)**: Patrón en T y guías individuales para [Mammon](docs/dioses_y_altares/mammon_avaricia.md), [Khorvath](docs/dioses_y_altares/khorvath_ira.md), [Beelze](docs/dioses_y_altares/beelze_gula.md), [Lucian](docs/dioses_y_altares/lucian_soberbia.md), [Leviatán](docs/dioses_y_altares/leviatan_envidia.md), [Belphegor](docs/dioses_y_altares/belphegor_pereza.md) y [Asmodeus](docs/dioses_y_altares/asmodeus_lujuria.md).
* 🏰 **[Aldeas Consagradas, Facciones y Culturas](docs/aldeas_consagradas/facciones_y_culturas.md)**: Estructuras Jigsaw (20%), estados territoriales y rasgos únicos de cada pueblo.
* 📦 **[Comercio Divino y Alta Traición](docs/aldeas_consagradas/comercio_divino.md)**: Catálogo de 5 tiers de Acólitos y penalización de -60 de favor por traición.
* ⚔️ **[Guerras Santas y Cuerno de Guerra](docs/aldeas_consagradas/guerras_santas.md)**: Asaltos autónomos, asedios convocados por el jugador y Choque de Dioses.
* 🕯️ **[Desolación y Caravana de Peregrinos](docs/aldeas_consagradas/desolacion_y_milagros.md)**: Ciclo de vida ante pérdida de civiles y milagros de restauración.
* 📜 **[Biblioteca Sagrada y Lore (42 Libros)](docs/lore_y_avances/biblioteca_sagrada.md)**: Distribución en cofres del mundo y árbol de avances.
* 💻 **[Referencia de Comandos (`/gods ...`)](docs/administracion/comandos.md)**: Comandos para jugadores y herramientas de operador.
* ⚙️ **[Configuración del Mod](docs/administracion/configuracion.md)**: Opciones de `sevendeadlygods-common.toml`.

---

## ⚡ Inicio Rápido

### 1. Compilación y Ejecución
```bash
# Compilar el archivo .jar
./gradlew build

# Iniciar el cliente de desarrollo directamente
./gradlew runClient
```

### 2. Configurar Google Gemini *(Opcional)*
Añade tu clave en `config/sevendeadlygods-common.toml` o mediante la variable de entorno del sistema:
```bash
export GEMINI_API_KEY="AIzaSyTuClaveDeApi"
```
*(Si juegas sin clave, el mod utilizará automáticamente su motor temático local integrado sin perder ninguna mecánica).*

---

## 📜 Licencia

Este proyecto está distribuido bajo la licencia **MIT**, permitiendo su libre uso, modificación y distribución. Consulta el archivo [LICENSE](LICENSE) para más detalles.
