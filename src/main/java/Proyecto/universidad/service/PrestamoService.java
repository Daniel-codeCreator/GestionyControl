package Proyecto.universidad.service;

import Proyecto.universidad.model.Prestamo;
import Proyecto.universidad.repository.PrestamoRepository;

import java.sql.Timestamp;
import java.util.List;

public class PrestamoService {

    private final PrestamoRepository repository;

    public PrestamoService() {
        this.repository = new PrestamoRepository();
    }

    public List<Prestamo> obtenerPrestamosActivos() {
        return repository.listarActivos();
    }

    public void realizarPrestamo(String solicitante, Integer idEquipo, Integer idUsuarioRegistra, Timestamp fechaEsperada, String observaciones) throws Exception {
        if (solicitante == null || solicitante.isBlank()) {
            throw new Exception("El nombre del solicitante es obligatorio.");
        }
        if (idEquipo == null) {
            throw new Exception("Debe seleccionar un equipo disponible.");
        }
        if (fechaEsperada == null) {
            throw new Exception("Debe seleccionar una fecha de devolución válida.");
        }

        int idUsuario = idUsuarioRegistra != null ? idUsuarioRegistra : 1;

        boolean ok = repository.registrarPrestamo(solicitante.trim(), idEquipo, idUsuario, fechaEsperada, observaciones);
        if (!ok) {
            throw new Exception("No se pudo completar el registro del préstamo.");
        }
    }

    public void procesarDevolucion(int idPrestamo, int idEquipo, int idUsuarioRecibe, String condicion, String obsDevolucion) throws Exception {
        int idUsuario = idUsuarioRecibe > 0 ? idUsuarioRecibe : 1;
        String cond = (condicion != null && !condicion.isBlank()) ? condicion : "BUENO";

        boolean ok = repository.registrarDevolucion(idPrestamo, idEquipo, idUsuario, cond, obsDevolucion);
        if (!ok) {
            throw new Exception("No se pudo procesar la devolución del equipo.");
        }
    }
}