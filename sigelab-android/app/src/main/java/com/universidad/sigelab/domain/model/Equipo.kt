package com.universidad.sigelab.domain.model

/** Valores de Equipo.Estado (CK_Equipo_Estado), igual que el enum EstadoEquipo de escritorio. */
enum class EstadoEquipo(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    PRESTADO("Prestado"),
    DANADO("Dañado"),
    MANTENIMIENTO("En mantenimiento"),
    BAJA("De baja");

    companion object {
        /** Acepta las variantes que puede guardar SQL Server: "DAÑADO", "DE BAJA", "En mantenimiento"… */
        fun desde(valor: String): EstadoEquipo {
            val normalizado = valor.trim().uppercase().replace('Ñ', 'N').replace(' ', '_')
            return when (normalizado) {
                "DE_BAJA" -> BAJA
                "EN_MANTENIMIENTO" -> MANTENIMIENTO
                else -> entries.firstOrNull { it.name == normalizado }
                    ?: throw IllegalArgumentException("Estado de equipo desconocido: $valor")
            }
        }
    }
}

/** Estado de Laboratorio, TipoEquipo, Rol y Usuario (CHECK ACTIVO/INACTIVO). */
enum class EstadoRegistro {
    ACTIVO,
    INACTIVO;

    companion object {
        /** El procedimiento CrearUsuario guarda "Activo", así que se compara sin distinguir mayúsculas. */
        fun desde(valor: String?): EstadoRegistro =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) } ?: ACTIVO
    }
}

/** Tabla Laboratorio. El código "LAB-00001" lo genera el servidor al guardar, como en escritorio. */
data class Laboratorio(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val ubicacion: String,
    val capacidad: Int,
    val estado: EstadoRegistro = EstadoRegistro.ACTIVO,
)

data class TipoEquipo(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
)

/**
 * Tabla Equipo con los mismos datos que muestra escritorio (Gestión de Equipos).
 * El código "EQP00001" lo genera el servidor al dar de alta.
 */
data class Equipo(
    val id: Int,
    val codigoInventario: String,
    val nombre: String,
    val idTipoEquipo: Int,
    val tipo: String,
    val idLaboratorio: Int,
    val laboratorio: String,
    val estado: EstadoEquipo,
    val marca: String = "",
    val modelo: String = "",
    val numeroSerie: String = "",
)
