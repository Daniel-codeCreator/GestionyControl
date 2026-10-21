package Proyecto.universidad.service;

import Proyecto.universidad.model.TipoEquipo;
import Proyecto.universidad.repository.TipoEquipoRepository;

import java.util.List;

public class TipoEquipoService {

    private final TipoEquipoRepository repository;

    public TipoEquipoService() {
        this.repository = new TipoEquipoRepository();
    }

    public List<TipoEquipo> obtenerTodos() {
        return repository.listarTodos();
    }

    public void registrarTipoEquipo(String nombre, String descripcion) throws Exception {
        if (nombre == null || nombre.isBlank()) {
            throw new Exception("El nombre del tipo de equipo es obligatorio.");
        }

        TipoEquipo tipo = new TipoEquipo();
        tipo.setStrNombre(nombre.trim());
        tipo.setStrDescripcion(descripcion != null ? descripcion.trim() : "");

        boolean guardado = repository.guardar(tipo);
        if (!guardado) {
            throw new Exception("Ocurrió un error al guardar el tipo de equipo.");
        }
    }
}