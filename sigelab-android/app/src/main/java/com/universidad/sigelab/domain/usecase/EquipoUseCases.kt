package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.rules.QrEquipo
import javax.inject.Inject

enum class CampoEquipo { NOMBRE, TIPO, LABORATORIO, MARCA, MODELO, SERIE }

data class ErrorEquipo(val campo: CampoEquipo, val mensaje: String)

private const val LARGO_TEXTO = 100

/**
 * Reglas de EquipoService (escritorio): nombre, tipo y laboratorio obligatorios; marca, modelo y
 * serie opcionales. El código EQP00001 lo genera el servidor y el equipo nace DISPONIBLE.
 *
 * Diferencia intencional con escritorio: al actualizar no se cambia el estado a mano. Solo los
 * préstamos, devoluciones y mantenimientos lo mueven, para que no quede un equipo PRESTADO sin préstamo.
 */
class GuardarEquipoUseCase @Inject constructor(
    private val repo: EquipoRepository,
) {
    fun validar(equipo: Equipo): List<ErrorEquipo> = buildList {
        when {
            equipo.nombre.isBlank() -> add(ErrorEquipo(CampoEquipo.NOMBRE, "El nombre del equipo es obligatorio."))
            equipo.nombre.trim().length > LARGO_TEXTO -> add(ErrorEquipo(CampoEquipo.NOMBRE, "Máximo $LARGO_TEXTO caracteres"))
        }
        if (equipo.idTipoEquipo <= 0) add(ErrorEquipo(CampoEquipo.TIPO, "Debe seleccionar un tipo de equipo."))
        if (equipo.idLaboratorio <= 0) add(ErrorEquipo(CampoEquipo.LABORATORIO, "Debe seleccionar un laboratorio."))
        if (equipo.marca.trim().length > LARGO_TEXTO) add(ErrorEquipo(CampoEquipo.MARCA, "Máximo $LARGO_TEXTO caracteres"))
        if (equipo.modelo.trim().length > LARGO_TEXTO) add(ErrorEquipo(CampoEquipo.MODELO, "Máximo $LARGO_TEXTO caracteres"))
        if (equipo.numeroSerie.trim().length > LARGO_TEXTO) add(ErrorEquipo(CampoEquipo.SERIE, "Máximo $LARGO_TEXTO caracteres"))
    }

    suspend operator fun invoke(rolActual: Rol, equipo: Equipo): Result<Equipo> {
        if (!rolActual.puedeEditar(Modulo.EQUIPOS)) return Result.failure(AppError.SinPermiso())
        val errores = validar(equipo)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores.map { it.mensaje }))
        return repo.guardar(
            equipo.copy(
                nombre = equipo.nombre.trim(),
                marca = equipo.marca.trim(),
                modelo = equipo.modelo.trim(),
                numeroSerie = equipo.numeroSerie.trim(),
            ),
        )
    }
}

/**
 * Lectura de la etiqueta QR. Solo consulta: el diagrama exige que leer el QR
 * no cambie el estado del equipo por sí solo.
 */
class ConsultarEquipoPorQrUseCase @Inject constructor(
    private val repo: EquipoRepository,
) {
    suspend operator fun invoke(textoLeido: String): Result<Equipo> {
        val codigo = QrEquipo.extraerCodigo(textoLeido)
            ?: return Result.failure(AppError.Validacion(listOf("No se encontró un código QR legible.")))
        return repo.buscarPorCodigo(codigo)
    }
}

/** Filtro de la lista de equipos: por estado y por texto en código, nombre o serie. */
fun filtrarEquipos(equipos: List<Equipo>, estado: EstadoEquipo?, texto: String): List<Equipo> {
    val buscado = texto.trim()
    return equipos.filter { equipo ->
        (estado == null || equipo.estado == estado) &&
            (buscado.isEmpty() ||
                equipo.codigoInventario.contains(buscado, ignoreCase = true) ||
                equipo.nombre.contains(buscado, ignoreCase = true) ||
                equipo.numeroSerie.contains(buscado, ignoreCase = true))
    }
}
