package Proyecto.universidad.service;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Mantenimiento;
import Proyecto.universidad.repository.EquipoRepository;
import Proyecto.universidad.repository.MantenimientoRepository;

import java.util.List;

public class MantenimientoService {

    private final MantenimientoRepository repository;
    private final EquipoRepository equipoRepository;

    public MantenimientoService() {
        this.repository = new MantenimientoRepository();
        this.equipoRepository = new EquipoRepository();
    }

    public List<Mantenimiento> obtenerEnProceso() {
        return repository.listarEnProceso();
    }

    public void ingresarAMantenimiento(Integer idEquipo, String tipoMantenimiento, String descripcion, String responsable, Integer idUsuarioRegistra) throws Exception {
        if (idEquipo == null) {
            throw new Exception("Debe seleccionar un equipo.");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new Exception("Debe ingresar una descripción del problema o mantenimiento.");
        }
        if (responsable == null || responsable.isBlank()) {
            throw new Exception("Debe indicar el nombre del técnico o responsable.");
        }
        if (idUsuarioRegistra == null) {
            throw new Exception("Debe especificar el usuario que registra el mantenimiento.");
        }

        // RN-14: Validar estado del equipo directamente desde la BD[cite: 11, 12, 13]
        Equipo eq = equipoRepository.obtenerPorId(idEquipo);
        if (eq == null) {
            throw new Exception("El equipo seleccionado no existe.");
        }

        if (eq.getEstado() == EstadoEquipo.PRESTADO) {
            throw new Exception("Un equipo PRESTADO no puede enviarse a mantenimiento. Debe devolverse primero.");
        }
        if (eq.getEstado() == EstadoEquipo.BAJA) {
            throw new Exception("Un equipo en BAJA ya no puede ingresar a mantenimiento.");
        }
        if (eq.getEstado() == EstadoEquipo.MANTENIMIENTO) {
            throw new Exception("El equipo ya se encuentra en mantenimiento actualmente.");
        }

        String tipo = ("PREVENTIVO".equalsIgnoreCase(tipoMantenimiento)) ? "PREVENTIVO" : "CORRECTIVO";

        boolean ok = repository.registrarIngreso(idEquipo, tipo, descripcion.trim(), responsable.trim(), idUsuarioRegistra);
        if (!ok) {
            throw new Exception("No se pudo registrar el ingreso del equipo a mantenimiento.");
        }
    }

    public void cerrarMantenimiento(Integer idMantenimiento, Integer idEquipo, String resultado, String observaciones) throws Exception {
        if (idMantenimiento == null || idEquipo == null) {
            throw new Exception("Debe seleccionar un mantenimiento en proceso de la tabla.");
        }
        if (resultado == null || resultado.isBlank()) {
            throw new Exception("Debe indicar el resultado del cierre (REPARADO o NO_REPARABLE).");
        }

        boolean ok = repository.finalizarMantenimiento(idMantenimiento, idEquipo, resultado, observaciones);
        if (!ok) {
            throw new Exception("No se pudo registrar el cierre del mantenimiento.");
        }
    }
}