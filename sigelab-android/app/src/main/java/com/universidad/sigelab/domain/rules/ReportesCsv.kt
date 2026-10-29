package com.universidad.sigelab.domain.rules

import com.universidad.sigelab.domain.model.AccionAuditoria
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.usecase.PrestamoVencido
import java.time.format.DateTimeFormatter

/** Reportes en CSV (RFC 4180) con las mismas columnas que las tablas de ReportesView de escritorio. */
object ReportesCsv {

    private val FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun equiposPorEstado(equipos: List<Equipo>): String = tabla(
        listOf("ID", "Código", "Nombre", "Tipo", "Laboratorio", "Estado"),
        equipos.map { listOf(it.id.toString(), it.codigoInventario, it.nombre, it.tipo, it.laboratorio, it.estado.name) },
    )

    fun vencidos(lista: List<PrestamoVencido>): String = tabla(
        listOf("ID Préstamo", "Solicitante", "Fecha Préstamo", "Fecha Esperada", "Estado", "Días de atraso", "Equipos pendientes"),
        lista.map { v ->
            listOf(
                v.prestamo.id.toString(),
                v.prestamo.solicitante,
                FECHA.format(v.prestamo.fechaPrestamo),
                FECHA.format(v.prestamo.fechaDevolucionPrevista),
                v.prestamo.estado.name,
                v.diasDeAtraso.toString(),
                v.pendientes.joinToString(" | ") { "${it.codigoInventario} ${it.nombreEquipo}" },
            )
        },
    )

    fun historialMantenimiento(lista: List<Mantenimiento>): String = tabla(
        listOf("ID Mant.", "Código Equipo", "Equipo", "Tipo", "Ingreso", "Salida", "Resultado", "Técnico"),
        lista.map { m ->
            listOf(
                m.id.toString(),
                m.codigoInventario,
                m.nombreEquipo,
                m.tipo.name,
                FECHA.format(m.fechaIngreso),
                m.fechaSalida?.let(FECHA::format).orEmpty(),
                m.resultado?.name.orEmpty(),
                m.responsable,
            )
        },
    )

    fun auditoria(registros: List<RegistroAuditoria>): String = tabla(
        listOf("Fecha", "Acción", "Entidad", "Usuario", "Descripción"),
        registros.map {
            listOf(FECHA.format(it.fecha), AccionAuditoria.etiquetaDe(it.accion), it.entidad.orEmpty(), it.usuario.orEmpty(), it.detalle)
        },
    )

    internal fun tabla(encabezado: List<String>, filas: List<List<String>>): String =
        (listOf(encabezado) + filas).joinToString("\r\n") { fila -> fila.joinToString(",") { celda(it) } }

    internal fun celda(valor: String): String {
        // Un texto que empieza con = + - @ lo ejecuta Excel como fórmula. Los detalles de auditoría
        // incluyen lo que alguien escribió como usuario en un login fallido, así que se neutraliza.
        val seguro = if (valor.isNotEmpty() && valor[0] in "=+-@\t\r") "'$valor" else valor
        val requiereComillas = seguro.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (requiereComillas) "\"" + seguro.replace("\"", "\"\"") + "\"" else seguro
    }
}
