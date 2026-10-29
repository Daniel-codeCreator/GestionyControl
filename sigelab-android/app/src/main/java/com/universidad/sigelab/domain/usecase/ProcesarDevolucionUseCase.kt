package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoPrestamo
import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.model.ResultadoDevolucion
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.rules.ReglasEquipo
import javax.inject.Inject

/** Lo que pasará al confirmar; se muestra antes de enviar la devolución. */
data class VistaPreviaDevolucion(
    val nuevosEstados: Map<Int, EstadoEquipo>,
    val pendientesRestantes: Int,
) {
    /** Diagrama: "¿Quedan equipos pendientes?" NO → préstamo FINALIZADO; SÍ → sigue ACTIVO. */
    val prestamoFinalizado: Boolean get() = pendientesRestantes == 0
}

class ProcesarDevolucionUseCase @Inject constructor(
    private val repo: PrestamoRepository,
) {
    fun validar(prestamo: Prestamo, items: List<ItemDevolucion>): List<String> = buildList {
        if (prestamo.estado != EstadoPrestamo.ACTIVO) add("El préstamo #${prestamo.id} no está ACTIVO")
        if (items.isEmpty()) add("Seleccione al menos un equipo a devolver")

        val repetidos = items.groupingBy { it.idEquipo }.eachCount().filterValues { it > 1 }.keys
        if (repetidos.isNotEmpty()) add("Equipos repetidos en la devolución: ${codigos(prestamo, repetidos)}")

        val pendientes = prestamo.pendientes.map { it.idEquipo }.toSet()
        val ajenos = items.map { it.idEquipo }.filterNot { it in pendientes }.toSet()
        if (ajenos.isNotEmpty()) add("No están pendientes en este préstamo: ${codigos(prestamo, ajenos)}")
    }

    fun vistaPrevia(prestamo: Prestamo, items: List<ItemDevolucion>): VistaPreviaDevolucion {
        val devueltos = items.map { it.idEquipo }.toSet()
        return VistaPreviaDevolucion(
            nuevosEstados = items.associate { it.idEquipo to ReglasEquipo.estadoTrasDevolucion(it.condicion) },
            pendientesRestantes = prestamo.pendientes.count { it.idEquipo !in devueltos },
        )
    }

    suspend operator fun invoke(prestamo: Prestamo, items: List<ItemDevolucion>): Result<ResultadoDevolucion> {
        val errores = validar(prestamo, items)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores))
        return repo.registrarDevolucion(prestamo.id, items)
    }

    private fun codigos(prestamo: Prestamo, ids: Set<Int>) = ids.joinToString { id ->
        prestamo.detalles.firstOrNull { it.idEquipo == id }?.codigoInventario ?: "#$id"
    }
}
