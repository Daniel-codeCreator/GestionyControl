package Proyecto.universidad.service;

import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService() {
        usuarioRepository = new UsuarioRepository();
    }

    public Usuario buscarPorUsuario(String strUsuario) {

        return usuarioRepository.buscarPorUsuario(strUsuario);
    }

    public Usuario crearUsuario(
            String strUsuario,
            String strPassword,
            int intRol
    ) {

        String strPasswordHash = BCrypt.hashpw(
                strPassword,
                BCrypt.gensalt(12)
        );

        return usuarioRepository.crearUsuario(
                "Administrador",
                "Sistema",
                strUsuario,
                strPasswordHash,
                "admin@sistema.com",
                intRol
        );
    }

    public Usuario autenticar(
            String strUsuario,
            String strPassword
    ) {

        Usuario usuario = usuarioRepository.buscarPorUsuario(strUsuario);

        if (usuario == null) {
            return null;
        }

        if (!"ACTIVO".equals(usuario.getStrEstado())) {
            return null;
        }

        boolean passwordCorrecta = BCrypt.checkpw(
                strPassword,
                usuario.getStrPassword()
        );

        if (!passwordCorrecta) {
            return null;
        }

        return usuario;
    }
}