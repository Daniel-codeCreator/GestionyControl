package Proyecto.universidad.controller;

import Proyecto.universidad.model.Rol;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.RolService;
import Proyecto.universidad.service.UsuarioService;

import java.util.List;

/**
 * Coordina la vista de creación de usuarios: obtiene los roles activos
 * para el ComboBox y delega la creación en la capa de servicio.
 */
public class CrearUsuarioController {

    private final UsuarioService usuarioService;
    private final RolService rolService;

    public CrearUsuarioController() {
        this.usuarioService = new UsuarioService();
        this.rolService = new RolService();
    }

    /**
     * Lista de roles ACTIVOS desde la tabla Rol para poblar el ComboBox.
     */
    public List<Rol> obtenerRoles() {
        return rolService.obtenerActivos();
    }

    /**
     * Crea el usuario validando en Service y ejecutando dbo.sp_CrearUsuario,
     * que también registra la auditoría USUARIO_CREADO.
     */
    public Usuario crearUsuario(
            String strNombre,
            String strApellido,
            String strUsuario,
            String strPassword,
            String strConfirmacion,
            String strCorreo,
            int intRol
    ) {
        return usuarioService.crearUsuario(
                strNombre,
                strApellido,
                strUsuario,
                strPassword,
                strConfirmacion,
                strCorreo,
                intRol
        );
    }
}
