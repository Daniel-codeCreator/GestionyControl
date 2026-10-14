package Proyecto.universidad.service;

import Proyecto.universidad.model.Laboratorio;
import Proyecto.universidad.repository.LaboratorioRepository;

import java.util.List;

public class LaboratorioService {

    private final LaboratorioRepository repository;

    public LaboratorioService() {
        this.repository = new LaboratorioRepository();
    }

    public List<Laboratorio> obtenerTodos() {
        return repository.listarTodos();
    }

    public void registrarLaboratorio(String codigo, String nombre, String ubicacion, int capacidad) throws Exception {
        if (codigo == null || codigo.isBlank()) {
            throw new Exception("El código del laboratorio es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new Exception("El nombre del laboratorio es obligatorio.");
        }

        // Regla RN-03: El código del laboratorio debe ser único
        Laboratorio existente = repository.buscarPorCodigo(codigo.trim());
        if (existente != null) {
            throw new Exception("Ya existe un laboratorio registrado con el código: " + codigo);
        }

        Laboratorio lab = new Laboratorio();
        lab.setStrCodigo(codigo.trim());
        lab.setStrNombre(nombre.trim());
        lab.setStrUbicacion(ubicacion != null ? ubicacion.trim() : "");
        lab.setIntCapacidad(capacidad);
        lab.setStrEstado("ACTIVO");

        boolean guardado = repository.guardar(lab);
        if (!guardado) {
            throw new Exception("Ocurrió un error al intentar guardar el laboratorio en la base de datos.");
        }
    }

    public void actualizarLaboratorio(Laboratorio lab) throws Exception {
        if (lab.getStrCodigo() == null || lab.getStrCodigo().isBlank()) {
            throw new Exception("El código es obligatorio.");
        }

        // Verificar que si cambió de código, este no pertenezca a otro registro (RN-03)
        Laboratorio existente = repository.buscarPorCodigo(lab.getStrCodigo().trim());
        if (existente != null && !existente.getIdLaboratorio().equals(lab.getIdLaboratorio())) {
            throw new Exception("El código '" + lab.getStrCodigo() + "' ya pertenece a otro laboratorio.");
        }

        boolean actualizado = repository.actualizar(lab);
        if (!actualizado) {
            throw new Exception("No se pudo actualizar el laboratorio.");
        }
    }
}