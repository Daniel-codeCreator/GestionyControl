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
        if (solicitante == null || solicitante.isBlank()) {
            throw new Exception("El nombre del solicitante es obligatorio.");
        }

        // RN-05: Al menos un equipo[cite: 11]
        if (detalles == null || detalles.isEmpty()) {
            throw new Exception("Debe agregar al menos un equipo al préstamo.");
        }

        if (fechaEsperada == null) {
            throw new Exception("Debe seleccionar una fecha de devolución válida.");
        }

        // RN-06: No duplicados[cite: 11]
        Set<Integer> ids = new HashSet<>();
        for (DetallePrestamo d : detalles) {
            if (!ids.add(d.getIdEquipo())) {
                throw new Exception("El equipo " + d.getCodigoEquipo() + " está duplicado en la selección.");
            }
        }

        // RN-07, RN-08 y RN-20: Verificar disponibilidad en BD desde Service[cite: 11, 13]
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

        int idUsuario = idUsuarioRegistra != null ? idUsuarioRegistra : 1;

        boolean ok = repository.registrarPrestamoMultiple(solicitante.trim(), detalles, idUsuario, fechaEsperada, observaciones);
        if (!ok) {
            throw new Exception("No se pudo completar el registro del préstamo.");
        }
    }

    public void procesarDevolucion(int idPrestamo, int idEquipo, int idUsuarioRecibe, String condicion, String obsDevolucion) throws Exception {
        int idUsuario = idUsuarioRecibe > 0 ? idUsuarioRecibe : 1;
        String cond = (condicion != null && !condicion.isBlank()) ? condicion : "BUENO";

        boolean ok = repository.registrarDevolucion(idPrestamo, idEquipo, idUsuario, cond, obsDevolucion);
        if (!ok) {
            throw new Exception("No se pudo procesar la devolución. Verifique si el equipo ya fue devuelto.");
        }
    }
}