package Proyecto.universidad.service;

import Proyecto.universidad.enums.RolUsuario;
import Proyecto.universidad.model.Rol;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.RolRepository;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

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

    public void registrarUsuario(String nombre, String apellido, String usuario, String password,
                                 String correo, Integer idRol) throws Exception {
        Sesion.exigirRol("crear usuarios", RolUsuario.ADMIN);

        if (nombre == null || nombre.isBlank()) throw new Exception("El nombre es obligatorio.");
        if (apellido == null || apellido.isBlank()) throw new Exception("El apellido es obligatorio.");
        if (usuario == null || usuario.isBlank()) throw new Exception("El nombre de usuario es obligatorio.");
        if (password == null || password.isBlank()) throw new Exception("La contraseña es obligatoria.");
        if (correo == null || correo.isBlank()) throw new Exception("El correo electrónico es obligatorio.");
        if (idRol == null) throw new Exception("Debe seleccionar un rol.");

        // Contraseña cifrada con BCrypt
        String passwordHash = BCrypt.hashpw(password.trim(), BCrypt.gensalt(12));

        Usuario creado = usuarioRepository.crearUsuarioConSP(
                nombre.trim(),
                apellido.trim(),
                usuario.trim(),
                passwordHash,
                correo.trim(),
                idRol
        );

        if (creado == null) {
            throw new Exception("No se pudo registrar el usuario.");
        }
    }

    public void cambiarEstado(Integer idUsuario, String nuevoEstado) {
        Sesion.exigirRol("cambiar el estado de usuarios", RolUsuario.ADMIN);

        if (idUsuario == null) {
            throw new IllegalArgumentException("Debe seleccionar un usuario.");
        }

        String estado = nuevoEstado == null ? "" : nuevoEstado.trim().toUpperCase();
        if (!estado.equals("ACTIVO") && !estado.equals("INACTIVO")) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }

        Usuario actor = Sesion.getUsuario();
        Usuario objetivo = usuarioRepository.buscarPorId(idUsuario);

        if (objetivo == null) {
            throw new IllegalArgumentException("El usuario seleccionado no existe.");
        }
        if (estado.equalsIgnoreCase(objetivo.getStrEstado())) {
            throw new IllegalArgumentException("El usuario ya se encuentra en estado " + estado + ".");
        }

        if (estado.equals("INACTIVO")) {
            // No puede desactivarse a sí mismo
            if (objetivo.getIdUsuario() == actor.getIdUsuario()) {
                throw new IllegalStateException("No puede desactivar su propio usuario.");
            }
            // Siempre debe quedar al menos un ADMIN activo
            if (objetivo.getIntRol() == RolUsuario.ADMIN.getId()
                    && usuarioRepository.contarAdministradoresActivos() <= 1) {
                throw new IllegalStateException("No se puede desactivar al último administrador activo del sistema.");
            }
        }

        String accion = estado.equals("INACTIVO") ? "USUARIO_DESACTIVADO" : "USUARIO_ACTIVADO";
        String descripcion = "Usuario " + objetivo.getStrUsuario() + " cambiado a " + estado
                + " por " + actor.getStrUsuario();

        boolean ok = usuarioRepository.cambiarEstadoConAuditoria(
                idUsuario, estado, actor.getIdUsuario(), accion, descripcion);

        if (!ok) {
            throw new IllegalStateException("No se pudo cambiar el estado del usuario. Intente de nuevo.");
        }
    }
}