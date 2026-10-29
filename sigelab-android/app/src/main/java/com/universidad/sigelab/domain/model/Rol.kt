package com.universidad.sigelab.domain.model

/** Botones del menú principal, en el mismo orden que la versión de escritorio. */
enum class Modulo(val titulo: String) {
    LABORATORIOS("Gestión de Laboratorios"),
    TIPOS_EQUIPO("Tipos de Equipo"),
    EQUIPOS("Gestión de Equipos"),
    PRESTAMOS("Préstamos y Devoluciones"),
    MANTENIMIENTO("Mantenimiento"),
    USUARIOS("Crear Usuario"),
    REPORTES("Reportes"),
}

enum class Acceso { NINGUNO, LECTURA, TOTAL }

enum class Rol {
    ADMIN,
    ENCARGADO,
    DOCENTE;

    /**
     * Matriz de permisos. El backend debe aplicar la misma regla con el rol del token:
     * ocultar un botón en la app no es una medida de seguridad.
     */
    fun acceso(modulo: Modulo): Acceso = when (this) {
        ADMIN -> Acceso.TOTAL
        ENCARGADO -> if (modulo == Modulo.USUARIOS) Acceso.NINGUNO else Acceso.TOTAL
        DOCENTE -> if (modulo == Modulo.EQUIPOS) Acceso.LECTURA else Acceso.NINGUNO
    }

    fun puedeVer(modulo: Modulo) = acceso(modulo) != Acceso.NINGUNO
    fun puedeEditar(modulo: Modulo) = acceso(modulo) == Acceso.TOTAL

    /**
     * La auditoría (logins fallidos, quién hizo qué) es información de control, no operativa:
     * dentro de Reportes solo la ve ADMIN.
     */
    val veAuditoria: Boolean get() = this == ADMIN

    companion object {
        /** Tolera mayúsculas/espacios ("Admin", "ENCARGADO "). Devuelve null si el rol no existe. */
        fun desde(valor: String): Rol? =
            entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
    }
}
