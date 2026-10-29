package com.universidad.sigelab.domain.model

import java.time.Instant

data class DashboardMetricas(
    val totalEquipos: Int,
    val disponibles: Int,
    val prestados: Int,
    val danados: Int,
    val enMantenimiento: Int,
    val deBaja: Int,
    val prestamosActivos: Int,
    val prestamosVencidos: Int,
    val actualizadoEn: Instant,
)
