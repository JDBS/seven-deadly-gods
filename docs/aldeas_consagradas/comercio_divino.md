# ⚖️ Comercio Divino y Alta Traición

Los Acólitos Centinelas de las aldeas consagradas no solo protegen el templo: también actúan como **Mercaderes Sagrados**, ofreciendo artefactos arcanos y códices sobre-encantados a los fieles según su devoción.

---

## 🎭 Actitud Mercantil según tu Favor

Cuando interactúas con un Acólito Centinela en una aldea consagrada:

1. **Devoto / Fiel (`Favor >= 0`)**:
   * Abre la interfaz de **Comercio Divino**, mostrando ofertas exclusivas según tu nivel de devoción.
2. **Desconocido o Disgustado (`-39 <= Favor < 0`)**:
   * Te mira con desdén y emite el sonido de rechazo (*Villager No*), negándose a comerciar. No te atacará a menos que tú lo provoques.
3. **Hereje Declarado (`Favor <= -40`)**:
   * Te declara enemigo del templo y te atacará a muerte a la vista junto a toda la guarnición de la aldea.
   * Si tenías la interfaz de comercio abierta y tu favor cae por debajo de 0 en mitad del trato, **la interfaz se cierra de golpe** y el acólito desenfunda su arma.

---

## 📦 Catálogo de Comercio Divino (5 Niveles de Devoción)

A medida que aumentas tu favor con el dios patrón de la aldea, se desbloquean ofertas de nivel superior:

### 1. 🪙 Comercio de Mammon (Avaricia)
* **Nivel 1 & 2 (`Favor >= 0`)**: 8 Lingotes de Oro por 5 Esmeraldas | 32 Esmeraldas por 4 Diamantes.
* **Nivel 3 (`Favor >= 15`)**: *Bolsa de Monedas Bendita de Mammon* (atrae vetas de oro subterráneas).
* **Nivel 4 (`Favor >= 35`)**: *Lingote Real de la Bóveda de Mammon* + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: **Códice Áureo: Fortuna IV** (libro encantado que supera el límite vanilla de Fortuna III).

### 2. 🩸 Comercio de Khorvath (Ira)
* **Nivel 1 & 2 (`Favor >= 0`)**: Calavera de Esqueleto + 5 Lingotes de Hierro por *Espada Pesada de Acólito* (Filo III, Irrompibilidad III) | *Ceniza de Furia Sangrienta*.
* **Nivel 3 (`Favor >= 15`)**: *Piedra de Afilado Sanguínea* (Netherite scrap abrasivo).
* **Nivel 4 (`Favor >= 35`)**: *Hacha de Guerra del Carnicero* (Filo V, Aspecto Ígneo II) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: **Tomo de Masacre: Filo VI** (libro encantado que supera el límite vanilla de Filo V).

### 3. 🍖 Comercio de Beelze (Gula)
* **Nivel 1 & 2 (`Favor >= 0`)**: 4 Esmeraldas por 12 Zanahorias Doradas | *Pastel Inagotable de Beelze*.
* **Nivel 3 (`Favor >= 15`)**: *Néctar de Ambrosía de Beelze* (saciedad extrema).
* **Nivel 4 (`Favor >= 35`)**: *Cuchillo de Carnicero Divino* (Botín V, Aspecto Ígneo II) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: *Manjar Supremo del Glotón* (Manzana Dorada Encantada bendecida).

### 4. ☀️ Comercio de Lucian (Soberbia)
* **Nivel 1 & 2 (`Favor >= 0`)**: 10 Esmeraldas por 16 Bloques de Cuarzo | Tótem de la Inmortalidad por 3 Diamantes + 16 Esmeraldas.
* **Nivel 3 (`Favor >= 15`)**: *Diadema Solar de Lucian* (Casco de oro con Protección V e Irrompibilidad V).
* **Nivel 4 (`Favor >= 35`)**: *Espejo del Amor Propio* (Catalejo imperial) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: *Tótem de Soberbia Inquebrantable* (Tótem con Irrompibilidad III).

### 5. 🌊 Comercio de Leviatán (Envidia)
* **Nivel 1 & 2 (`Favor >= 0`)**: 8 Carne Podrida por 2 Esmeraldas | 12 Esmeraldas por 4 Perlas de Ender.
* **Nivel 3 (`Favor >= 15`)**: *Esencia Robada del Mercado Negro* (8 Frascos con experiencia).
* **Nivel 4 (`Favor >= 35`)**: *Talismán del Ojo Codicioso* (Corazón del mar) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: *Daga Abisal de Leviatán* (Espada de diamante con Filo VI y Botín IV).

### 6. 💤 Comercio de Belphegor (Pereza)
* **Nivel 1 & 2 (`Favor >= 0`)**: 5 Lana Blanca por 2 Esmeraldas | *Lecho de Ensueño de Belphegor*.
* **Nivel 3 (`Favor >= 15`)**: *Sandalias de Inercia* (Botas con Caída de Pluma V e Irrompibilidad V).
* **Nivel 4 (`Favor >= 35`)**: *Embudo Imantado de la Pereza* (Tolva bendecida) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: *Núcleo de Gólem Perezoso* (Estrella del Nether bendecida).

### 7. 💜 Comercio de Asmodeus (Lujuria)
* **Nivel 1 & 2 (`Favor >= 0`)**: 4 Amapolas por 2 Esmeraldas | 10 Esmeraldas por 8 Frascos con experiencia.
* **Nivel 3 (`Favor >= 15`)**: *Esencia de Feromonas de Asmodeus* (Panal afrodisíaco).
* **Nivel 4 (`Favor >= 35`)**: *Lazo de Afinidad Carmesí* (Rienda de seda) + **Cuerno de Guerra Divino**.
* **Nivel 5 (`Favor >= 60`)**: *Fruto de Pasión Cautivadora* (Manzana dorada con Irrompibilidad III).

---

## ⚡ El Juicio de la Alta Traición

La lealtad hacia los cultos es sagrada. Si mantienes una relación no hostil (**`Favor >= 0`**) con un dios y cometes cualquiera de los siguientes actos dentro de sus dominios:

1. **Atacar o herir a un Acólito Centinela** de ese dios.
2. **Herir o matar a un Aldeano civil** dentro de su aldea consagrada.
3. **Picar, profanar o destruir el Altar Mayor** de su templo.

### Consecuencias Inmediatas:
* **Penalización Fulminante de `-60 Puntos de Favor Directos`**: Caes instantáneamente en desgracia (por ejemplo, de +20 pasarás a -40, convirtiéndote en enemigo declarado de la aldea).
* **Estruendo Celestial**: Suena el rugido del Wither (`WITHER_SPAWN`).
* **Sentencia en el Chat**: La deidad traicionada te repudia públicamente:
  * *«¡TRAIDOR! ¿Te atreves a derramar la sangre de mis fieles? ¡Tu alma pagará esta afrenta!»*
* Los Acólitos de la aldea desenvainan sus armas y se abalanzan sobre ti de inmediato.
