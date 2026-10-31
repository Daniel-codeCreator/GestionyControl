package Proyecto.universidad.model;

import java.util.List;

/** Resultado genérico de un reporte: columnas y filas listas para mostrar en un JTable. */
public record ReporteTabla(String[] columnas, List<Object[]> filas) {
}