package Proyecto.universidad.service;

import Proyecto.universidad.model.Rol;
import Proyecto.universidad.repository.RolRepository;

import java.util.List;

public class RolService {

    private final RolRepository rolRepository;

    public RolService() {
        this.rolRepository = new RolRepository();
    }

    public List<Rol> obtenerActivos() {
        return rolRepository.listarActivos();
    }
}
