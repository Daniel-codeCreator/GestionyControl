package Proyecto.universidad.service;

import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.UsuarioRepository;

import java.util.List;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.listarTodos(); // Llama al método de tu UsuarioRepository
    }
}