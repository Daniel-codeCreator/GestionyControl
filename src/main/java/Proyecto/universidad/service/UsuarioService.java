package Proyecto.universidad.service;

import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this.usuarioRepository = new UsuarioRepository();
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.listarTodos(); // Llama al método de tu UsuarioRepository
    }

    /**
     * Valida y crea un usuario mediante el procedimiento dbo.sp_CrearUsuario.
     * La contraseña se cifra aquí con BCrypt; el SP solo almacena el hash.
     */
    public Usuario crearUsuario(
            String strNombre,
            String strApellido,
            String strUsuario,
            String strPassword,
            String strConfirmacionPassword,
            String strCorreo,
            int intRol
    ) {
        if (estaVacio(strNombre)) {
            throw new RuntimeException("El nombre es obligatorio.");
        }
        if (estaVacio(strApellido)) {
            throw new RuntimeException("El apellido es obligatorio.");
        }
        if (estaVacio(strUsuario)) {
            throw new RuntimeException("El nombre de usuario es obligatorio.");
        }
        if (strUsuario.trim().length() > 50) {
            throw new RuntimeException("El nombre de usuario no puede superar los 50 caracteres.");
        }
        if (estaVacio(strPassword) || strPassword.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (!strPassword.equals(strConfirmacionPassword)) {
            throw new RuntimeException("La contraseña y su confirmación no coinciden.");
        }
        if (estaVacio(strCorreo) || !strCorreo.contains("@") || !strCorreo.contains(".")) {
            throw new RuntimeException("El correo no tiene un formato válido.");
        }
        if (strCorreo.trim().length() > 150) {
            throw new RuntimeException("El correo no puede superar los 150 caracteres.");
        }
        if (intRol <= 0) {
            throw new RuntimeException("Debe seleccionar un rol.");
        }

        String strPasswordHash = BCrypt.hashpw(strPassword, BCrypt.gensalt(12));

        return usuarioRepository.crearUsuario(
                strNombre.trim(),
                strApellido.trim(),
                strUsuario.trim(),
                strPasswordHash,
                strCorreo.trim(),
                intRol
        );
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}