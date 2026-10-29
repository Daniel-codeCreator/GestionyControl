package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.repository.CatalogoRepository
import javax.inject.Inject

enum class CampoCatalogo { NOMBRE, UBICACION, CAPACIDAD, DESCRIPCION }

data class ErrorCatalogo(val campo: CampoCatalogo, val mensaje: String)

// Largos de las columnas VARCHAR en SQL Server.
private const val LARGO_NOMBRE = 100
private const val LARGO_UBICACION = 200
private const val LARGO_DESCRIPCION = 200

/** Mismo texto que escritorio cuando la capacidad escrita no es un número. */
const val MENSAJE_CAPACIDAD_INVALIDA = "La capacidad debe ser un número entero válido."

/**
 * Reglas de LaboratorioService (escritorio): nombre obligatorio y capacidad mayor que cero.
 * El código LAB-00001 lo genera el servidor; la ubicación es opcional.
 */
class GuardarLaboratorioUseCase @Inject constructor(
    private val repo: CatalogoRepository,
) {
    fun validar(laboratorio: Laboratorio): List<ErrorCatalogo> = buildList {
        val nombre = laboratorio.nombre.trim()
        when {
            nombre.isEmpty() -> add(ErrorCatalogo(CampoCatalogo.NOMBRE, "El nombre del laboratorio es obligatorio."))
            nombre.length > LARGO_NOMBRE -> add(ErrorCatalogo(CampoCatalogo.NOMBRE, "Máximo $LARGO_NOMBRE caracteres"))
        }
        if (laboratorio.ubicacion.trim().length > LARGO_UBICACION) {
            add(ErrorCatalogo(CampoCatalogo.UBICACION, "Máximo $LARGO_UBICACION caracteres"))
        }
        if (laboratorio.capacidad <= 0) {
            add(ErrorCatalogo(CampoCatalogo.CAPACIDAD, "La capacidad debe ser un número mayor que cero."))
        }
    }

    suspend operator fun invoke(rolActual: Rol, laboratorio: Laboratorio): Result<Laboratorio> {
        if (!rolActual.puedeEditar(Modulo.LABORATORIOS)) return Result.failure(AppError.SinPermiso())
        val errores = validar(laboratorio)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores.map { it.mensaje }))
        return repo.guardarLaboratorio(
            laboratorio.copy(nombre = laboratorio.nombre.trim(), ubicacion = laboratorio.ubicacion.trim()),
        )
    }
}

/**
 * Reglas de TipoEquipoService: nombre obligatorio y descripción opcional. Escritorio solo permite
 * crear tipos; el nombre tiene UNIQUE en la base, así que se avisa antes de enviarlo.
 */
class CrearTipoEquipoUseCase @Inject constructor(
    private val repo: CatalogoRepository,
) {
    fun validar(tipo: TipoEquipo, existentes: List<TipoEquipo>): List<ErrorCatalogo> = buildList {
        val nombre = tipo.nombre.trim()
        when {
            nombre.isEmpty() -> add(ErrorCatalogo(CampoCatalogo.NOMBRE, "El nombre del tipo de equipo es obligatorio."))
            nombre.length > LARGO_NOMBRE -> add(ErrorCatalogo(CampoCatalogo.NOMBRE, "Máximo $LARGO_NOMBRE caracteres"))
            existentes.any { it.nombre.trim().equals(nombre, ignoreCase = true) } ->
                add(ErrorCatalogo(CampoCatalogo.NOMBRE, "Ya existe el tipo «$nombre»"))
        }
        if (tipo.descripcion.orEmpty().trim().length > LARGO_DESCRIPCION) {
            add(ErrorCatalogo(CampoCatalogo.DESCRIPCION, "Máximo $LARGO_DESCRIPCION caracteres"))
        }
    }

    suspend operator fun invoke(rolActual: Rol, tipo: TipoEquipo, existentes: List<TipoEquipo>): Result<TipoEquipo> {
        if (!rolActual.puedeEditar(Modulo.TIPOS_EQUIPO)) return Result.failure(AppError.SinPermiso())
        val errores = validar(tipo, existentes)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores.map { it.mensaje }))
        return repo.crearTipoEquipo(
            TipoEquipo(id = 0, nombre = tipo.nombre.trim(), descripcion = tipo.descripcion?.trim()?.ifEmpty { null }),
        )
    }
}
