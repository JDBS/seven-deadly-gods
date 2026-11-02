package com.volcov.sevendeadlygods.village;

/**
 * Estados del ciclo de vida de una aldea dentro del sistema de Facciones Divinas.
 */
public enum GodVillageState {
    /** Aldea normal civil sin culto activo. */
    MUNDANA,
    /** Aldea consagrada activa con templo y milicia de acólitos. */
    CONSAGRADA,
    /** Asedio o guerra santa activa en curso. */
    EN_ASEDIO,
    /** 0 aldeanos vivos; el altar humea esperando peregrinos o restauración. */
    DESOLADA,
    /** Aldea arrasada en ruinas bajo el dominio abisal de Leviatán (Envidia). */
    ARRASADA
}
