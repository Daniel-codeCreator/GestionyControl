package Proyecto.universidad.service;

import Proyecto.universidad.model.Auditoria;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.AuditoriaRepository;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public AuthService() {
        this.usuarioRepository = new UsuarioRepository();
        this.auditoriaRepository = new AuditoriaRepository();
    }

    public Usuario buscarPorUsuario(String strUsuario) {
        return usuarioRepository.buscarPorUsuario(strUsuario);
    }

    public Usuario crearUsuario(String strUsuario, String strPassword, int intRol) {
        String strPasswordHash = BCrypt.hashpw(strPassword, BCrypt.gensalt(12));

        return usuarioRepository.crearUsuario(
                "Administrador",
                "Sistema",
                strUsuario,
                strPasswordHash,
                "admin@sistema.com",
                intRol
        );
    }

    public Usuario autenticar(String strUsuario, String strPassword) {
        Usuario usuario = usuarioRepository.buscarPorUsuario(strUsuario);

        if (usuario == null) {
            return null;
        }

        if (!"ACTIVO".equals(usuario.getStrEstado())) {
            return null;
        }

        boolean passwordCorrecta = BCrypt.checkpw(strPassword, usuario.getStrPassword());

        if (!passwordCorrecta) {
            return null;
        }

        return usuario;
    }

    public void registrarAuditoriaLoginExitoso(Usuario usuario) {
        Auditoria aud = new Auditoria();
        aud.setIdUsuario(usuario.getIdUsuario());
        aud.setFechaHora(LocalDateTime.now());
        aud.setAccion("LOGIN_EXITOSO");
        aud.setEntidad("Usuario");
        aud.setIdRegistro(usuario.getIdUsuario());
        aud.setDescripcion("Inicio de sesión exitoso con validación facial aprobada para usuario: " + usuario.getUsername());

        auditoriaRepository.registrar(aud);
    }

    public void registrarAuditoriaLoginFallido(Integer idUsuario, String strUsuario, String motivo) {
        Auditoria aud = new Auditoria();
        aud.setIdUsuario(idUsuario);
        aud.setFechaHora(LocalDateTime.now());
        aud.setAccion("LOGIN_FALLIDO");
        aud.setEntidad("Usuario");
        aud.setIdRegistro(idUsuario);
        aud.setDescripcion("Intento de login fallido para '" + strUsuario + "'. Motivo: " + motivo);

        auditoriaRepository.registrar(aud);
    }
}