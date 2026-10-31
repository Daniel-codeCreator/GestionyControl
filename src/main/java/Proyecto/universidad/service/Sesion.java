package Proyecto.universidad.service;

import Proyecto.universidad.enums.RolUsuario;
import Proyecto.universidad.exception.AccesoDenegadoException;
import Proyecto.universidad.model.Usuario;

/** Usuario con sesión iniciada y validación de permisos por rol. */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    /** Cierra e invalida la sesión del usuario actual. */
    public static void limpiar() {
        usuarioActual = null;
    }

    public static Usuario getUsuario() {
        return usuarioActual;
    }

    public static RolUsuario getRol() {
        return usuarioActual == null ? null : RolUsuario.desdeId(usuarioActual.getIntRol());
    }

    public static boolean tieneRol(RolUsuario... permitidos) {
        RolUsuario rol = getRol();
        if (rol == null) return false;
        for (RolUsuario permitido : permitidos) {
            if (rol == permitido) return true;
        }
        return false;
    }

    /** Lanza AccesoDenegadoException si el rol actual no está entre los permitidos. */
    public static void exigirRol(String accion, RolUsuario... permitidos) {
        if (!tieneRol(permitidos)) {
            RolUsuario rol = getRol();
            throw new AccesoDenegadoException("Su rol (" + (rol == null ? "sin sesión" : rol.name())
                    + ") no tiene permiso para " + accion + ".");
        }
    }
}