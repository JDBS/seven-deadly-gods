package com.volcov.sevendeadlygods.village;

/**
 * Estados del ciclo de vida de una aldea dentro del sistema de Facciones Divinas.
 */
public enum GodVillageState {
    /** Aldea normal civil sin culto activo. */
    MUNDANA("Aldea Mundana (Neutral)"),
    /** Aldea consagrada activa con templo y milicia de acólitos. */
    CONSAGRADA("Santuario Consagrado Activo"),
    /** Asedio o guerra santa activa en curso. */
    EN_ASEDIO("En Asedio (Guerra Santa)"),
    /** 0 aldeanos vivos; el altar humea esperando peregrinos o restauración. */
    DESOLADA("Desolada (Esperando Peregrinos)"),
    /** Aldea arrasada en ruinas bajo el dominio abisal de Leviatán (Envidia). */
    ARRASADA("Asentamiento Arrasado en Ruinas");

    private final String displayName;

    GodVillageState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
