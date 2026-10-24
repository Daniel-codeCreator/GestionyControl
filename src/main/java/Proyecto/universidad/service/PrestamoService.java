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

    public void realizarPrestamoMultiple(String solicitante, List<DetallePrestamo> detalles, Integer idUsuarioRegistra, Timestamp fechaEsperada, String observaciones) throws Exception {
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
            throw new Exception("Debe seleccionar un observacion es obligatorio.");
        }

        if (fechaEsperada.toLocalDateTime().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new Exception(("La fecha de devolucion no puede ser menor a la de hoy"));
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
                throw new Exception("El equipo " + eq.getCodigoInventario() + " (" + eq.getNombre() +
                        ") no está disponible. Estado actual: " + eq.getEstado());
            }
        }

        boolean ok = repository.registrarPrestamoMultiple(solicitante.trim(), detalles, idUsuarioRegistra, fechaEsperada, observaciones);
        if (!ok) {
            throw new Exception("No se pudo completar el registro del préstamo.");
        }
    }

    public void procesarDevolucion(int idPrestamo, int idEquipo, int idUsuarioRecibe, String condicion, String obsDevolucion) throws Exception {
        if (idUsuarioRecibe <= 0) {
            throw new Exception("No hay un usuario con sesión iniciada.");
        }

        String cond = (condicion != null && !condicion.isBlank()) ? condicion : "BUENO";

        boolean ok = repository.registrarDevolucion(idPrestamo, idEquipo, idUsuarioRecibe, cond, obsDevolucion);
        if (!ok) {
            throw new Exception("No se pudo procesar la devolución. Verifique si el equipo ya fue devuelto.");
        }
    }
}