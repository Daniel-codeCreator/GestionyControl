package Proyecto.universidad.service;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.DetallePrestamo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Prestamo;
import Proyecto.universidad.repository.EquipoRepository;
import Proyecto.universidad.repository.PrestamoRepository;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrestamoService {

    private final PrestamoRepository repository;
    private final EquipoRepository equipoRepository;

    public PrestamoService() {
        this.repository = new PrestamoRepository();
        this.equipoRepository = new EquipoRepository();
    }

    public List<Prestamo> obtenerPrestamosActivos() {
        return repository.listarActivos();
    }

    public void realizarPrestamoMultiple(String solicitante, List<DetallePrestamo> detalles, Integer idUsuarioRegistra,
                                         Timestamp fechaEsperada, String observaciones) throws Exception {
        if (idUsuarioRegistra == null || idUsuarioRegistra <= 0) {
            throw new Exception("No hay un usuario con sesión iniciada.");
        }

        if (solicitante == null || solicitante.isBlank()) {
            throw new Exception("El nombre del solicitante es obligatorio.");
        }

        // RN-05: al menos un equipo
        if (detalles == null || detalles.isEmpty()) {
            throw new Exception("Debe agregar al menos un equipo al préstamo.");
        }

        if (fechaEsperada == null) {
            throw new Exception("Debe seleccionar una fecha de devolución válida.");
        }

        if (observaciones == null || observaciones.isBlank()) {
            throw new Exception("Las observaciones del préstamo son obligatorias.");
        }

        if (fechaEsperada.toLocalDateTime().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new Exception("La fecha de devolución no puede ser anterior a hoy.");
        }

        // RN-06: el mismo equipo no puede aparecer dos veces
        Set<Integer> ids = new HashSet<>();
        for (DetallePrestamo d : detalles) {
            if (!ids.add(d.getIdEquipo())) {
                throw new Exception("El equipo " + d.getCodigoEquipo() + " está duplicado en la selección.");
            }
        }

        // RN-07, RN-08 y RN-20: la disponibilidad se verifica en la BD desde el Service
        for (DetallePrestamo d : detalles) {
            Equipo eq = equipoRepository.obtenerPorId(d.getIdEquipo());
            if (eq == null) {
                throw new Exception("El equipo " + d.getCodigoEquipo() + " ya no existe.");
            }
            if (eq.getEstado() != EstadoEquipo.DISPONIBLE) {
                throw new Exception("El equipo " + eq.getStrCodigo() + " (" + eq.getStrNombre() +
                        ") no está disponible. Estado actual: " + textoEstado(eq.getEstado()));
            }
        }

        boolean ok = repository.registrarPrestamoMultiple(solicitante.trim(), detalles, idUsuarioRegistra,
                fechaEsperada, observaciones.trim());
        if (!ok) {
            throw new Exception("No se pudo completar el préstamo. Verifique que los equipos sigan disponibles e intente de nuevo.");
        }
    }

    public void procesarDevolucion(int idPrestamo, int idEquipo, int idUsuarioRecibe,
                                   String condicion, String obsDevolucion) throws Exception {
        if (idUsuarioRecibe <= 0) {
            throw new Exception("No hay un usuario con sesión iniciada.");
        }
        if (idPrestamo <= 0 || idEquipo <= 0) {
            throw new Exception("Seleccione un préstamo activo de la tabla para recibir el equipo.");
        }

        // RN-12 y RN-13: la condición solo puede ser BUENO o DANADO
        String cond = condicion == null ? "" : condicion.trim().toUpperCase().replace("DAÑADO", "DANADO");
        if (!cond.equals("BUENO") && !cond.equals("DANADO")) {
            throw new Exception("La condición de devolución debe ser BUENO o DAÑADO.");
        }

        // RN-10: el equipo debe estar realmente PRESTADO
        Equipo eq = equipoRepository.obtenerPorId(idEquipo);
        if (eq == null) {
            throw new Exception("El equipo seleccionado no existe.");
        }
        if (eq.getEstado() != EstadoEquipo.PRESTADO) {
            throw new Exception("El equipo " + eq.getStrCodigo() + " no está PRESTADO (estado actual: "
                    + textoEstado(eq.getEstado()) + "), por lo que no se puede devolver.");
        }

        String obs = (obsDevolucion == null || obsDevolucion.isBlank()) ? null : obsDevolucion.trim();

        boolean ok = repository.registrarDevolucion(idPrestamo, idEquipo, idUsuarioRecibe, cond, obs);
        if (!ok) {
            // RN-10 y RN-11: no está pendiente en un préstamo activo o ya fue devuelto
            throw new Exception("No se pudo procesar la devolución. El equipo ya fue devuelto o no está pendiente en ese préstamo.");
        }
    }

    private String textoEstado(EstadoEquipo estado) {
        return estado == null ? "DESCONOCIDO" : estado.name().replace("DANADO", "DAÑADO");
    }
}