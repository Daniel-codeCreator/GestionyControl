package Proyecto.universidad.service;

import Proyecto.universidad.model.Auditoria;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.repository.AuditoriaRepository;
import Proyecto.universidad.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

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
        registrarAuditoriaLogin(
                usuario.getIdUsuario(),
                "LOGIN_EXITOSO",
                "Inicio de sesión exitoso con validación facial aprobada para usuario: " + usuario.getUsername()
        );
    }

    public void registrarAuditoriaLoginFallido(Integer idUsuario, String strUsuario, String motivo) {
        registrarAuditoriaLogin(
                idUsuario,
                "LOGIN_FALLIDO",
                "Intento de login fallido para '" + strUsuario + "'. Motivo: " + motivo
        );
    }

    private void registrarAuditoriaLogin(Integer idUsuario, String accion, String descripcion) {
        Auditoria aud = new Auditoria();
        aud.setIdUsuario(idUsuario);
        aud.setAccion(accion);
        aud.setDescripcion(limitarTexto(descripcion, 500));

        auditoriaRepository.registrarLogin(aud);
    }

    private String limitarTexto(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }
}