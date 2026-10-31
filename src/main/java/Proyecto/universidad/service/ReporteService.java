package Proyecto.universidad.service;

import Proyecto.universidad.model.ReporteTabla;
import Proyecto.universidad.repository.ReporteRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReporteService {

    private static final List<String> ESTADOS_EQUIPO =
            List.of("DISPONIBLE", "PRESTADO", "DANADO", "MANTENIMIENTO", "BAJA");
    private static final List<String> FILTROS_PRESTAMO =
            List.of("TODOS", "ACTIVOS", "FINALIZADOS", "VENCIDOS");
    private static final List<String> ESTADOS_MANTENIMIENTO =
            List.of("TODOS", "EN_PROCESO", "FINALIZADO");

    private final ReporteRepository repository = new ReporteRepository();

    public record HistorialEquipo(String encabezado, ReporteTabla prestamos, ReporteTabla mantenimientos) {
    }

    // 1. Equipos por estado
    public ReporteTabla equiposPorEstado(String estado) throws Exception {
        String valor = null;

        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("TODOS")) {
            valor = estado.trim().toUpperCase().replace("DAÑADO", "DANADO");
            if (!ESTADOS_EQUIPO.contains(valor)) {
                throw new Exception("Seleccione un estado de equipo válido.");
            }
        }
        return repository.equiposPorEstado(valor);
    }

    public String resumenEquipos() {
        Map<String, Integer> conteo = repository.conteoEquiposPorEstado();
        int total = conteo.values().stream().mapToInt(Integer::intValue).sum();

        return "Total: " + total +
                "  |  Disponibles: " + conteo.getOrDefault("DISPONIBLE", 0) +
                "  |  Prestados: " + conteo.getOrDefault("PRESTADO", 0) +
                "  |  Dañados: " + conteo.getOrDefault("DANADO", 0) +
                "  |  Mantenimiento: " + conteo.getOrDefault("MANTENIMIENTO", 0) +
                "  |  Baja: " + conteo.getOrDefault("BAJA", 0);
    }

    // 2. Préstamos activos / finalizados / vencidos
    public ReporteTabla prestamos(String filtro) throws Exception {
        String valor = filtro == null ? "TODOS" : filtro.trim().toUpperCase();
        if (!FILTROS_PRESTAMO.contains(valor)) {
            throw new Exception("Seleccione un filtro de préstamos válido.");
        }
        return repository.prestamos(valor, null, null);
    }

    // 3. Préstamos entre fechas
    public ReporteTabla prestamosEntreFechas(Date desde, Date hasta) throws Exception {
        if (desde == null || hasta == null) {
            throw new Exception("Debe indicar la fecha inicial y la fecha final.");
        }

        LocalDate fechaDesde = desde.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate fechaHasta = hasta.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (fechaDesde.isAfter(fechaHasta)) {
            throw new Exception("La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
        }

        Timestamp inicio = Timestamp.valueOf(fechaDesde.atStartOfDay());
        Timestamp fin = Timestamp.valueOf(fechaHasta.atTime(23, 59, 59));

        return repository.prestamos("TODOS", inicio, fin);
    }

    // 4. Historial de un equipo (préstamos + mantenimientos)
    public HistorialEquipo historialEquipo(String codigo) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new Exception("Ingrese el código de inventario del equipo.");
        }

        String codigoLimpio = codigo.trim();
        String[] equipo = repository.buscarEquipo(codigoLimpio);

        if (equipo == null) {
            throw new Exception("No existe un equipo con el código: " + codigoLimpio);
        }

        int idEquipo = Integer.parseInt(equipo[0]);
        String estado = equipo[2] == null ? "" : equipo[2].replace("DANADO", "DAÑADO");
        String encabezado = codigoLimpio + " - " + equipo[1] + "  (Estado actual: " + estado + ")";

        return new HistorialEquipo(
                encabezado,
                repository.historialPrestamosEquipo(idEquipo),
                repository.mantenimientos(idEquipo, null));
    }

    // 5. Historial de mantenimiento
    public ReporteTabla mantenimientos(String estado) throws Exception {
        String valor = estado == null ? "TODOS" : estado.trim().toUpperCase();
        if (!ESTADOS_MANTENIMIENTO.contains(valor)) {
            throw new Exception("Seleccione un estado de mantenimiento válido.");
        }
        return repository.mantenimientos(null, valor.equals("TODOS") ? null : valor);
    }

    // 6. Auditoría
    public ReporteTabla auditoria(String accion) {
        boolean todas = accion == null || accion.isBlank() || accion.equalsIgnoreCase("TODAS");
        return repository.auditoria(todas ? null : accion.trim());
    }

    public List<String> acciones() {
        return repository.acciones();
    }
}