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

    public String registrarLaboratorio(String nombre, String ubicacion, int capacidad) throws Exception {
        if (nombre == null || nombre.isBlank()) {
            throw new Exception("El nombre del laboratorio es obligatorio.");
        }
        if (capacidad <= 0) {
            throw new Exception("La capacidad debe ser un número mayor que cero.");
        }

        // El código se genera automáticamente
        String codigo = repository.generarSiguienteCodigo();
        if (codigo == null) {
            throw new Exception("No se pudo generar el código del laboratorio. Intente de nuevo.");
        }

        // Regla RN-03: el código debe ser único (la BD también lo garantiza con UNIQUE)
        if (repository.buscarPorCodigo(codigo) != null) {
            throw new Exception("Ya existe un laboratorio registrado con el código: " + codigo);
        }

        Laboratorio lab = new Laboratorio();
        lab.setStrCodigo(codigo);
        lab.setStrNombre(nombre.trim());
        lab.setStrUbicacion(ubicacion != null ? ubicacion.trim() : "");
        lab.setIntCapacidad(capacidad);
        lab.setStrEstado("ACTIVO");

        boolean guardado = repository.guardar(lab);
        if (!guardado) {
            throw new Exception("No se pudo guardar el laboratorio. Verifique que el nombre no esté repetido.");
        }

        return codigo;
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