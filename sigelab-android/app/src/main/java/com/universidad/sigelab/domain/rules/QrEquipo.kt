package com.universidad.sigelab.domain.rules

import com.universidad.sigelab.domain.model.Equipo

/**
 * Formato de la etiqueta QR: contiene el CódigoInventario tal cual, sin prefijos, igual que
 * QrService de escritorio (imágenes ImagenesQR/QR_<código>.png). Leerla nunca cambia estados (RN-19).
 */
object QrEquipo {

    private const val LARGO_MAXIMO = 64

    fun contenido(equipo: Equipo): String = equipo.codigoInventario.trim()

    /** Nombre de archivo que usa escritorio al guardar el PNG. */
    fun nombreArchivo(equipo: Equipo): String = "QR_${contenido(equipo)}.png"

    /** Texto leído por la cámara → código de inventario, o null si no puede ser una etiqueta de equipo. */
    fun extraerCodigo(texto: String): String? {
        val limpio = texto.trim()
        return limpio.takeIf { it.isNotEmpty() && it.length <= LARGO_MAXIMO && it.none(Char::isISOControl) }
    }
}
