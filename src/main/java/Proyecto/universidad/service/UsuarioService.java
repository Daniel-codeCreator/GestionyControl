package Proyecto.universidad.service;

import Proyecto.universidad.model.Rol;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.RolRepository;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt; // O tu clase de utilidad de seguridad (ej. PasswordUtils.hashPassword)

import java.util.List;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
        this.rolRepository = new RolRepository();
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.listarTodos();
    }

    public List<Rol> obtenerRoles() {
        return rolRepository.listarTodos();
    }

    public void registrarUsuario(String nombre, String apellido, String usuario, String password, String correo, Integer idRol) throws Exception {
        if (nombre == null || nombre.isBlank()) throw new Exception("El nombre es obligatorio.");
        if (apellido == null || apellido.isBlank()) throw new Exception("El apellido es obligatorio.");
        if (usuario == null || usuario.isBlank()) throw new Exception("El nombre de usuario es obligatorio.");
        if (password == null || password.isBlank()) throw new Exception("La contraseña es obligatoria.");
        if (correo == null || correo.isBlank()) throw new Exception("El correo electrónico es obligatorio.");
        if (idRol == null) throw new Exception("Debe seleccionar un rol.");

        // ENCRIPTACIÓN DE LA CONTRASEÑA:
        // Genera el hash de BCrypt para guardarlo de forma segura
        String passwordHash = BCrypt.hashpw(password.trim(), BCrypt.gensalt(12));

        Usuario creado = usuarioRepository.crearUsuarioConSP(
                nombre.trim(),
                apellido.trim(),
                usuario.trim(),
                passwordHash, // <--- Se envía la contraseña cifrada
                correo.trim(),
                idRol
        );

        if (creado == null) {
            throw new Exception("No se pudo registrar el usuario.");
        }
    }

    /**
     * RN-18: desactiva lógicamente un usuario y deja el rastro de auditoría
     * USUARIO_DESACTIVADO.
     */
    public void desactivarUsuario(Integer intIdUsuario, Integer intIdUsuarioAccion) throws Exception {
        if (intIdUsuario == null || intIdUsuario <= 0) {
            throw new Exception("Debe indicar el usuario a desactivar.");
        }

        int intAccion = intIdUsuarioAccion != null && intIdUsuarioAccion > 0 ? intIdUsuarioAccion : 1;

        boolean ok = usuarioRepository.desactivarUsuario(intIdUsuario, intAccion);
        if (!ok) {
            throw new Exception("No se pudo desactivar el usuario (no existe o ya estaba inactivo).");
        }
    }
}