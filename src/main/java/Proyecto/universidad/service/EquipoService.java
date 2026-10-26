package Proyecto.universidad.service;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.repository.EquipoRepository;

import java.util.List;

public class EquipoService {

    private final EquipoRepository repository;

    public EquipoService() {
        this.repository = new EquipoRepository();
    }

    public List<Equipo> obtenerTodos() {
        return repository.listarTodos();
    }

    public void registrarEquipo(String codigo, String nombre, Integer idTipo, Integer idLab, String marca, String modelo, String serie) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new Exception("El código del equipo es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new Exception("El nombre del equipo es obligatorio.");
        }
        if (idTipo == null) {
            throw new Exception("Debe seleccionar un tipo de equipo.");
        }
        if (idLab == null) {
            throw new Exception("Debe seleccionar un laboratorio.");
        }

        // Regla RN-04: Código de equipo único
        Equipo existente = repository.buscarPorCodigo(codigo.trim());
        if (existente != null) {
            throw new Exception("Ya existe un equipo registrado con el código: " + codigo);
        }

        Equipo eq = new Equipo();
        eq.setStrCodigo(codigo.trim());
        eq.setStrNombre(nombre.trim());
        eq.setIdTipoEquipo(idTipo);
        eq.setIdLaboratorio(idLab);
        eq.setEstado(EstadoEquipo.DISPONIBLE); // Estado inicial obligatorio por especificación
        eq.setStrMarca(marca != null ? marca.trim() : "");
        eq.setStrModelo(modelo != null ? modelo.trim() : "");
        eq.setStrSerie(serie != null ? serie.trim() : "");

        boolean guardado = repository.guardar(eq);
        if (!guardado) {
            throw new Exception("No se pudo guardar el equipo en la base de datos.");
        }
    }

    public void actualizarEquipo(Equipo eq) throws Exception {
        if (eq.getStrCodigo() == null || eq.getStrCodigo().isBlank()) {
            throw new Exception("El código es obligatorio.");
        }

        // RN-04: Verificar unicidad del código al actualizar
        Equipo existente = repository.buscarPorCodigo(eq.getStrCodigo().trim());
        if (existente != null && !existente.getIdEquipo().equals(eq.getIdEquipo())) {
            throw new Exception("El código '" + eq.getStrCodigo() + "' ya pertenece a otro equipo.");
        }

        boolean actualizado = repository.actualizar(eq);
        if (!actualizado) {
            throw new Exception("No se pudo actualizar el equipo.");
        }
    }

    public Equipo buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return repository.buscarFichaPorCodigo(codigo.trim());
    }
}