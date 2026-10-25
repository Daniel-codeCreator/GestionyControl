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

    public void ingresarAMantenimiento(Integer idEquipo, String tipoMantenimiento, String descripcion,
                                       String responsable, Integer idUsuarioRegistra) throws Exception {
        if (idUsuarioRegistra == null || idUsuarioRegistra <= 0) {
            throw new Exception("No hay un usuario con sesión iniciada.");
        }
        if (idEquipo == null) {
            throw new Exception("Debe seleccionar un equipo.");
        }

        String tipo = tipoMantenimiento == null ? "" : tipoMantenimiento.trim().toUpperCase();
        if (!tipo.equals("PREVENTIVO") && !tipo.equals("CORRECTIVO")) {
            throw new Exception("Seleccione un tipo de mantenimiento válido (PREVENTIVO o CORRECTIVO).");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new Exception("Debe ingresar una descripción del problema o mantenimiento.");
        }
        if (responsable == null || responsable.isBlank()) {
            throw new Exception("Debe indicar el nombre del técnico o responsable.");
        }

        // Se valida el estado real del equipo en la BD (no depende de la pantalla)
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

        boolean ok = repository.registrarIngreso(idEquipo, tipo, descripcion.trim(), responsable.trim(), idUsuarioRegistra);
        if (!ok) {
            throw new Exception("No se pudo registrar el ingreso del equipo a mantenimiento.");
        }
    }

    public void cerrarMantenimiento(Integer idMantenimiento, String resultado, String observaciones,
                                    Integer idUsuario) throws Exception {
        if (idUsuario == null || idUsuario <= 0) {
            throw new Exception("No hay un usuario con sesión iniciada.");
        }
        if (idMantenimiento == null) {
            throw new Exception("Debe seleccionar un mantenimiento en proceso de la tabla.");
        }

        String res = resultado == null ? "" : resultado.trim().toUpperCase();
        if (!res.equals("REPARADO") && !res.equals("NO_REPARABLE")) {
            throw new Exception("Indique el resultado del cierre: REPARADO o NO_REPARABLE.");
        }

        // El equipo se toma del registro de mantenimiento en la BD, no de la pantalla
        Mantenimiento m = repository.buscarPorId(idMantenimiento);
        if (m == null) {
            throw new Exception("El mantenimiento seleccionado no existe.");
        }
        if (m.getFechaSalida() != null) {
            throw new Exception("Este mantenimiento ya fue finalizado anteriormente.");
        }

        Equipo eq = equipoRepository.obtenerPorId(m.getIdEquipo());
        if (eq == null || eq.getEstado() != EstadoEquipo.MANTENIMIENTO) {
            throw new Exception("El equipo no se encuentra en estado MANTENIMIENTO.");
        }

        String obs = (observaciones == null || observaciones.isBlank()) ? null : observaciones.trim();

        boolean ok = repository.finalizarMantenimiento(idMantenimiento, m.getIdEquipo(), res, obs, idUsuario);
        if (!ok) {
            throw new Exception("No se pudo registrar el cierre del mantenimiento.");
        }
    }
}